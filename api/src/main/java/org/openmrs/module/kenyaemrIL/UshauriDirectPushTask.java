package org.openmrs.module.kenyaemrIL;

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
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaemrMhealthOutboxMessage;
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
 * Directly push messages to Ushauri server
 */
public class UshauriDirectPushTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(UshauriDirectPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("USHAURI DIRECT PUSH: Scheduler started....");

        try {
            Context.openSession();

            GlobalProperty gpUshauriServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_USHAURI_PUSH_SERVER_URL);
            if (gpUshauriServerUrl == null) {
                System.out.println("USHAURI DIRECT PUSH: There is no global property for USHAURI server URL!");
                return;
            }

            if (StringUtils.isBlank(gpUshauriServerUrl.getPropertyValue())) {
                System.out.println("USHAURI DIRECT PUSH: The server URL has not been set!");
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
                List<KenyaemrMhealthOutboxMessage> pendingOutboxes = Context.getService(KenyaEMRILService.class).getKenyaEMROutboxMessagesToSend(false);
                if (pendingOutboxes.size() < 1) {
                    System.out.println("USHAURI Direct PUSH: There are no messages to send to Ushauri");
                    return;
                }

                for (KenyaemrMhealthOutboxMessage pendingOutbox : pendingOutboxes) {
                    sendMessageDirectToUshauriServer(pendingOutbox, gpUshauriServerUrl.getPropertyValue());
                }

            }

            Context.flushSession();
        } catch (IOException ioe) {

            try {
                String text = "IL - USHAURI PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - USHAURI PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Send message direct to Ushauri server
     * @param outbox
     */
    private void sendMessageDirectToUshauriServer(KenyaemrMhealthOutboxMessage outbox, String ushauriServerUrl) {

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

            HttpPost postRequest = new HttpPost(ushauriServerUrl);

            //Set the API media type in http content-type header
            postRequest.addHeader("content-type", "application/json");
            //Set the request post body
            String payload = outbox.getMessage().toUpperCase();
            StringEntity userEntity = new StringEntity(payload);
            postRequest.setEntity(userEntity);
            HttpResponse response = httpClient.execute(postRequest);

            //verify the valid error code first
            int statusCode = response.getStatusLine().getStatusCode();
            //System.out.println("Server response: " + statusCode);
            if (statusCode != 200) {
                String errorsString = "";
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("response");
                if (errorObj != null) {
                    errorsString = (String) errorObj.get("msg");
                }
                System.out.println("Error sending message to USHAURI server! " + "Status code - " + statusCode + ". Msg" + errorsString);
                log.error("Error sending message to USHAURI server! " + "Status code - " + statusCode + ". Msg" + errorsString);
                if (errorsString.length() > 200) {
                    errorsString = errorsString.substring(0, 199);
                }
                KenyaEMRILMessageErrorQueue kenyaEMRILMessageErrorQueue = new KenyaEMRILMessageErrorQueue();
                kenyaEMRILMessageErrorQueue.setHl7_type(outbox.getHl7_type().toUpperCase());
                kenyaEMRILMessageErrorQueue.setSource(outbox.getSource().toUpperCase());
                kenyaEMRILMessageErrorQueue.setMessage(outbox.getMessage());
                kenyaEMRILMessageErrorQueue.setStatus(errorsString);
                kenyaEMRILMessageErrorQueue.setMessage_type(ILMessageType.OUTBOUND.getValue());
                kenyaEMRILMessageErrorQueue.setMiddleware("Direct");
                Context.getService(KenyaEMRILService.class).saveKenyaEMRILMessageErrorQueue(kenyaEMRILMessageErrorQueue);

                //Purge from the il_messages table
                Context.getService(KenyaEMRILService.class).deleteMhealthOutboxMessage(outbox);

            } else {
                log.info("Successfully sent message to USHAURI server");
                System.out.println("Successfully sent message to USHAURI server");
                outbox.setRetired(true);
                Context.getService(KenyaEMRILService.class).saveMhealthOutboxMessage(outbox);
                //Purge from the il_messages table
                Context.getService(KenyaEMRILService.class).deleteMhealthOutboxMessage(outbox);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }
}
