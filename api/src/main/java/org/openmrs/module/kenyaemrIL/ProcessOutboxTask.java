package org.openmrs.module.kenyaemrIL;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
        try {
            Client restClient = Client.create();
            WebResource webResource = restClient.resource(IL_URL.getPropertyValue());
           // log.info("log info"+outbox.getMessage().toUpperCase());
            //System.out.println("IL URL ==>"+IL_URL.getPropertyValue());
           // System.out.println("Outbox message ==>"+outbox.getMessage().toUpperCase());
            ClientResponse resp = webResource.type("application/json")
                    .post(ClientResponse.class, outbox.getMessage().toUpperCase());

            System.out.println("The status received from the IL server: " + resp.getStatus());
            log.info("The status received from the IL server: " + resp.getStatus());
            if (resp.getStatus() != 200) {
                String message = resp.getEntity(String.class);
                System.err.println(("Failed : HTTP error code : " + resp.getStatus() + ", error message: " + message));
                log.info(("Failed : HTTP error code : " + resp.getStatus() + ", error message: " + message));

            } else {
                log.info("Successfull sent message to IL");
                System.out.println("Successfull sent message to IL");
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