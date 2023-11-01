package org.openmrs.module.kenyaemrIL;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

/**
 * Directly push messages to interop server
 */
public class KenyaEmrInteropDirectPushTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(KenyaEmrInteropDirectPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("EmrInterop DIRECT PUSH: Scheduler started....");

        try {
            Context.openSession();

            GlobalProperty gpMiddlewareServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_USHAURI_PUSH_SERVER_URL);
            if (gpMiddlewareServerUrl == null) {
                System.out.println("EmrInterop DIRECT PUSH: There is no global property for interop server URL!");
                return;
            }

            if (StringUtils.isBlank(gpMiddlewareServerUrl.getPropertyValue())) {
                System.out.println("EmrInterop DIRECT PUSH: The server URL has not been set!");
                return;
            }

            GlobalProperty gpMhealthMiddleware = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MHEALTH_MIDDLEWARE_TO_USE);
            boolean useILMiddleware = true; // this is also the default if no value is set in the global property
            if (gpMhealthMiddleware != null) {
                String gpMhealthMiddlewarePropValue = gpMhealthMiddleware.getPropertyValue();
                if (StringUtils.isNotBlank(gpMhealthMiddlewarePropValue) && (gpMhealthMiddlewarePropValue.equalsIgnoreCase("Direct") || gpMhealthMiddlewarePropValue.equalsIgnoreCase("Hybrid"))) {
                    useILMiddleware = false;
                }
            }
            // check first if there is internet connectivity before pushing
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            if (!useILMiddleware) {
                List<KenyaEMRInteropMessage> pendingOutboxes = Context.getService(KenyaEMRILService.class).getKenyaEMROutboxMessagesToSend(false);
                if (pendingOutboxes.size() < 1) {
                    System.out.println("EmrInterop Direct PUSH: There are no messages to send to interop server");
                    return;
                }

                for (KenyaEMRInteropMessage pendingOutbox : pendingOutboxes) {
                    Boolean isServerAcceptingRequests = sendMessageDirectToMhealthServer(pendingOutbox, gpMiddlewareServerUrl.getPropertyValue());
                    if (!isServerAcceptingRequests) {
                        System.out.println("OpenHim upstream servers throwing error 429 (Too many Requets)");
                        break;
                    }
                }

            }

            Context.flushSession();
        } catch (IOException ioe) {

            try {
                String text = "IL - EmrInterop PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - EmrInterop PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Send message direct to Mhealth server
     * @param outbox
     */
    private Boolean sendMessageDirectToMhealthServer(KenyaEMRInteropMessage outbox, String serverUrl) {

        SSLConnectionSocketFactory sslsf = null;
        GlobalProperty gpSslVerification = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_USHAURI_SSL_VERIFICATION_ENABLED);

        if (gpSslVerification != null) {
            String sslVerificationEnabled = gpSslVerification.getPropertyValue();
            if (StringUtils.isNotBlank(sslVerificationEnabled)) {
                if (sslVerificationEnabled.equals("true")) {
                    sslsf = ILUtils.sslConnectionSocketFactoryDefault();
                } else {
                    sslsf = ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification();
                }
            }
        }

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        try {

            HttpPost postRequest = new HttpPost(serverUrl);

            //Set the API media type in http content-type header
            postRequest.addHeader("content-type", "application/json");
            //Set the request post body
            String payload = outbox.getMessage().toUpperCase().replace("NULL", "null")
                    .replace("FALSE", "false").replace("TRUE", "true");
            StringEntity userEntity = new StringEntity(payload);
            postRequest.setEntity(userEntity);
            HttpResponse response = httpClient.execute(postRequest);

            //verify the valid error code first
            Integer statusCode = response.getStatusLine().getStatusCode();
            //System.out.println("Server response: " + statusCode);
            KenyaEMRILService service = Context.getService(KenyaEMRILService.class);

            if (statusCode.equals(429)) {
                return false;
            }
            if (statusCode != 200) {
                String errorsString = "";
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("response");
                System.out.println("Error object" + errorObj.toJSONString());

                if (errorObj != null) {
                    errorsString = (String) errorObj.get("msg");
                }
                System.out.println("Error sending message to interop server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
                log.error("Error sending message to interop server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
                System.out.println("Error object" + errorObj.toJSONString());

                if (StringUtils.isNotBlank(errorsString)) {
                    if (errorsString.length() > 200) {
                        errorsString = errorsString.substring(0, 199);
                    }
                } else {
                    errorsString = "No error message";
                }
                KenyaEMRILMessageErrorQueue kenyaEMRILMessageErrorQueue = new KenyaEMRILMessageErrorQueue();
                kenyaEMRILMessageErrorQueue.setHl7_type(outbox.getHl7_type().toUpperCase());
                kenyaEMRILMessageErrorQueue.setSource(outbox.getSource().toUpperCase());
                kenyaEMRILMessageErrorQueue.setMessage(outbox.getMessage());
                kenyaEMRILMessageErrorQueue.setStatus(errorsString);
                kenyaEMRILMessageErrorQueue.setMessage_type(ILMessageType.OUTBOUND.getValue());
                kenyaEMRILMessageErrorQueue.setMiddleware("Direct");
                kenyaEMRILMessageErrorQueue.setPatient(outbox.getPatient());
                Context.getService(KenyaEMRILService.class).saveKenyaEMRILMessageErrorQueue(kenyaEMRILMessageErrorQueue);

                //Purge from the il_messages table
                Context.getService(KenyaEMRILService.class).deleteMhealthOutboxMessage(outbox);

            } else {
                //Purge from the il_messages table
                service.deleteMhealthOutboxMessage(outbox);

                KenyaEMRILMessageArchive messageArchive = ILUtils.createArchiveForMhealthOutbox(outbox);
                messageArchive.setMiddleware("Direct");
                service.saveKenyaEMRILMessageArchive(messageArchive);

                log.info("Successfully sent message to interop server");
                System.out.println("Successfully sent message to interop server");

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return true;
    }
}
