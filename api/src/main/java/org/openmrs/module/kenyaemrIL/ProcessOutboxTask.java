package org.openmrs.module.kenyaemrIL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes the IL outbox every one minute .
 */
public class ProcessOutboxTask extends AbstractTask {
    //private final String IL_URL = "http://52.178.24.227:9721/api/";

    // Logger
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
                //postRequest.addHeader("apikey", API_KEY);
                //Set the request post body
                String payload = outbox.getMessage().toUpperCase();
                StringEntity userEntity = new StringEntity(payload);
                postRequest.setEntity(userEntity);

                //Send the request; It will immediately return the response in HttpResponse object if any
                HttpResponse response = httpClient.execute(postRequest);

                //verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                 System.err.println("Connection refused : Could not send message to IL");
                 log.error("Could not connect to IL Server");

            } else {
                log.info("Successfully sent message to IL");
                System.out.println("Successfully sent message to IL");
                outbox.setRetired(true);
                getEMRILService().saveKenyaEMRILMessage(outbox);
                //Purge from the il_messages table
                getEMRILService().deleteKenyaEMRILMessage(outbox);
            }
        }catch (Exception e){
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