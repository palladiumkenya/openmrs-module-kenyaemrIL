package org.openmrs.module.kenyaemrIL;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.OpenHIM.OpenhimClient;
import org.openmrs.module.kenyaemrIL.OpenHIM.OpenhimConstants;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaemrMhealthOutboxMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

/**
 * Push message to Mhealth apps througn OpenHIM
 */
public class UshauriDirectPushTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(UshauriDirectPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("OpenHIM USHAURI TASK: Scheduler started....");

        try {
            Context.openSession();


            GlobalProperty gpMhealthMiddleware = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MHEALTH_MIDDLEWARE_TO_USE);
            boolean useILMiddleware = true; // this is also the default if no value is set in the global property
            if (gpMhealthMiddleware != null) {
                String gpMhealthMiddlewarePropValue = gpMhealthMiddleware.getPropertyValue();
                if (StringUtils.isNotBlank(gpMhealthMiddlewarePropValue) && (gpMhealthMiddlewarePropValue.equalsIgnoreCase("OpenHIM") || gpMhealthMiddlewarePropValue.equalsIgnoreCase("Hybrid"))) {
                    useILMiddleware = false;
                }
            }
            // check first if there is internet connectivity before pushing
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            if (!useILMiddleware) {
                List<KenyaemrMhealthOutboxMessage> pendingOutboxes = Context.getService(KenyaEMRILService.class).getKenyaEMROutboxMessagesToSend(false);
                if (pendingOutboxes.size() < 1) {
                    System.out.println("OpenHIM USHAURI TASK: There are no messages to send to Ushauri");
                    return;
                }

                for (KenyaemrMhealthOutboxMessage pendingOutbox : pendingOutboxes) {
                    sendMessageToMhealthApps(pendingOutbox);
                }

            }

            Context.flushSession();
        } catch (IOException ioe) {

            try {
                String text = "IL - OpenHIM USHAURI PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - OpenHIM USHAURI PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Send message through OpenHIM
     *
     * @param outbox
     */
    private void sendMessageToMhealthApps(KenyaemrMhealthOutboxMessage outbox) {
        String messageType = outbox.getHl7_type();
        String openHIMPayload = outbox.getMessage().toUpperCase();
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        String messageChannel = "";

        switch (messageType) {
            case "ADT^A04":
                messageChannel = OpenhimConstants.REGISTRATION_OPENHIM_CHANNEL;
                break;
            case "SIU^S12":
                messageChannel = OpenhimConstants.APPOINTMENT_OPENHIM_CHANNEL;
                break;
            default:{
                log.error(messageChannel + " message type is not yet supported by OpenHIM");
                break;
            }
        }

        try {
            HttpResponse response = OpenhimClient.postMessage(openHIMPayload, messageChannel);

            int statusCode = response.getStatusLine().getStatusCode();
            if ((statusCode >= 200 && statusCode < 300) || statusCode == 500) {
                KenyaEMRILMessageArchive messageArchive = ILUtils.createArchiveForMhealthOutbox(outbox);
                messageArchive.setMiddleware("OpenHIM");
                service.saveKenyaEMRILMessageArchive(messageArchive);
                //Purge from the il_messages table
                service.deleteMhealthOutboxMessage(outbox);

                System.out.println("Message was successfully send to Ushauri via OpenHIM");
            } else {
                String errorsString = "";
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("response");
                System.out.println("Error object" + errorObj.toJSONString());

                if (errorObj != null) {
                    errorsString = (String) errorObj.get("msg");
                }
                log.error("An error occurred while posting the message to the OpenHIM channel. Status code: "
                        + statusCode + " Response body: " + errorsString);

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
                service.saveKenyaEMRILMessageErrorQueue(kenyaEMRILMessageErrorQueue);

                //Purge from the il_messages table
                service.deleteMhealthOutboxMessage(outbox);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("OpenHIM USHAURI TASK: Exception occurred : "+e.getMessage());

        }
    }
}
