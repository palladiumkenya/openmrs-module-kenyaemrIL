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
import org.openmrs.module.kenyaemrIL.artReferral.KenyaEMRArtReferralMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

public class ArtReferralDirectPush extends AbstractTask {
    private static final Logger log = LoggerFactory.getLogger(UshauriDirectPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("ART REFERRAL DIRECT PUSH: Scheduler started....");

        try {
            Context.openSession();

            GlobalProperty gpOpenHIMServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_USHAURI_PUSH_SERVER_URL);
            if (gpOpenHIMServerUrl == null) {
                System.out.println("ART REFERRAL DIRECT PUSH: There is no global property for OpenHIM server URL!");
                return;
            }

            if (StringUtils.isBlank(gpOpenHIMServerUrl.getPropertyValue())) {
                System.out.println("ART REFERRAL DIRECT PUSH: OpenHIM server URL has not been set!");
                return;
            }

            GlobalProperty gpInteropMiddleware = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MHEALTH_MIDDLEWARE_TO_USE);
            boolean useILMiddleware = true; // this is also the default if no value is set in the global property
            if (gpInteropMiddleware != null) {
                String gpInteropMiddlewarePropValue = gpInteropMiddleware.getPropertyValue();
                if (StringUtils.isNotBlank(gpInteropMiddlewarePropValue) && (gpInteropMiddlewarePropValue.equalsIgnoreCase("Direct") || gpInteropMiddlewarePropValue.equalsIgnoreCase("Hybrid"))) {
                    useILMiddleware = false;
                }
            }
            // check first if there is internet connectivity before pushing
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            if (!useILMiddleware) {
                List<KenyaEMRArtReferralMessage> pendingOutboxes = Context.getService(KenyaEMRILService.class).getArtReferralOutboxMessageToSend(false);
                if (pendingOutboxes.size() < 1) {
                    System.out.println("ART REFERRAL DIRECT PUSH: There are no messages to send to Ushauri");
                    return;
                }

                for (KenyaEMRArtReferralMessage pendingOutbox : pendingOutboxes) {
                    sendMessageDirectToArtDirectory(pendingOutbox, gpOpenHIMServerUrl.getPropertyValue());
                }

            }

            Context.flushSession();
        } catch (IOException ioe) {

            try {
                String text = "IL - ART REFERRAL DIRECT PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - ART REFERRAL DIRECT PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Send message direct to ART directory
     * @param outbox
     */
    private void sendMessageDirectToArtDirectory(KenyaEMRArtReferralMessage outbox, String gpOpenHIMServerUrl) {

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

            HttpPost postRequest = new HttpPost(gpOpenHIMServerUrl);
            postRequest.addHeader("content-type", "application/json");
            String payload = outbox.getMessage().toUpperCase();
            StringEntity userEntity = new StringEntity(payload);
            postRequest.setEntity(userEntity);
            HttpResponse response = httpClient.execute(postRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
            if (statusCode != 200) {
                String errorsString = "";
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("response");
                System.out.println("Error object" + errorObj.toJSONString());

                if (errorObj != null) {
                    errorsString = (String) errorObj.get("msg");
                }
                System.out.println("Error sending referral message to OpenHIM server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
                log.error("Error sending referral message to OpenHIM server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
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
                Context.getService(KenyaEMRILService.class).deleteArtReferralOutboxMessage(outbox);

            } else {

                KenyaEMRILMessageArchive archiveMessage = new KenyaEMRILMessageArchive();
                archiveMessage.setHl7_type(outbox.getHl7_type());
                archiveMessage.setSource(outbox.getSource());
                archiveMessage.setMessage(outbox.getMessage());
                archiveMessage.setDescription("");
                archiveMessage.setName("");
                archiveMessage.setMessage_type(outbox.getMessage_type());

                archiveMessage.setMiddleware("Direct");
                service.saveKenyaEMRILMessageArchive(archiveMessage);
                //Purge from the il_messages table
                service.deleteArtReferralOutboxMessage(outbox);

                log.info("Successfully sent message to USHAURI server");
                System.out.println("Successfully sent message to USHAURI server");

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }
}
