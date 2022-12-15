package org.openmrs.module.kenyaemrIL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes the IL outbox every one minute .
 */
public class ProcessOutboxTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(ProcessOutboxTask.class);

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing process outbox task at " + new Date());
//        Fetch non-processed inbox messages
        List<KenyaEMRILMessage> pendingOutboxes = fetchILOutboxes(false);
        for (KenyaEMRILMessage pendingOutbox : pendingOutboxes) {
            processFetchedRecord(pendingOutbox);
        }
    }

    private void processFetchedRecord(KenyaEMRILMessage outbox) {
//        Send to IL and mark as sent
        GlobalProperty IL_URL = Context.getAdministrationService().getGlobalPropertyObject("ilServer.address");
        if (IL_URL == null) {
            System.out.println("There is no global property for IL server URL!");
            return;
        }

        if (StringUtils.isBlank(IL_URL.getPropertyValue())) {
            System.out.println("Please set URL for IL server!");
            return;
        }

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                SSLContexts.createDefault(),
                new String[]{"TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

        try {

            //Define a postRequest request
            HttpPost postRequest = new HttpPost(IL_URL.getPropertyValue());

            //Set the API media type in http content-type header
            postRequest.addHeader("content-type", "application/json");
            //Set the request post body
            String payload = outbox.getMessage().toUpperCase();
            StringEntity userEntity = new StringEntity(payload);
            postRequest.setEntity(userEntity);
            HttpResponse response = httpClient.execute(postRequest);

            //verify the valid error code first
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("error");
                System.out.println("Error sending message to IL server! " + "Status code - " + statusCode + ". Msg" + errorObj.get("message"));
                log.error("Error sending message to IL server! " + "Status code - " + statusCode + ". Msg" + errorObj.get("message"));

            } else {

                //Purge from the il_messages table
                KenyaEMRILMessageArchive messageArchive = ILUtils.createArchiveForIlMessage(outbox);
                messageArchive.setMiddleware("IL");
                getEMRILService().saveKenyaEMRILMessageArchive(messageArchive);
                getEMRILService().deleteKenyaEMRILMessage(outbox);

                log.info("Successfully sent message to IL server");
                System.out.println("Successfully sent message to IL server");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    private List<KenyaEMRILMessage> fetchILOutboxes(boolean fetchRetired) {
        return getEMRILService().getKenyaEMRILOutboxes(fetchRetired);
    }

    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}