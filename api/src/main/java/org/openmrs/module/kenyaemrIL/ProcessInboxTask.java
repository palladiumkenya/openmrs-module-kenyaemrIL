package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes the IL inbox every one minute .
 */
public class ProcessInboxTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessInboxTask.class);
    private ObjectMapper mapper = new ObjectMapper();


    /**
     * @see org.openmrs.scheduler.tasks.AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing task at " + new Date());
//        Fetch non-processed inbox messages
        List<KenyaEMRILMessage> pendingInboxes = fetchILInboxes(false);
        for (final KenyaEMRILMessage pendingInbox : pendingInboxes) {
            processFetchedRecord(pendingInbox);
        }
    }

    private void processFetchedRecord(KenyaEMRILMessage pendingInbox) {
//        Process each message and mark as processed
        String message = pendingInbox.getMessage();
        message = message.substring(message.indexOf("{"), message.lastIndexOf("}") + 1);
        try {
            boolean returnStatus= false;
            ILMessage ilMessage = mapper.readValue(message.toLowerCase(), ILMessage.class);
            switch(ilMessage.getMessage_header().getMessage_type().toUpperCase()){
                case "ADT^A04":{
                    returnStatus = getEMRILService().processCreatePatientRequest(ilMessage);
                    break;
                }
                case "ADT^A08":{
                    returnStatus = getEMRILService().processUpdatePatientRequest(ilMessage);
                    break;
                }
                case "RDE^001":{
                    returnStatus = getEMRILService().processPharmacyOrder(ilMessage);
                    break;
                }
                case "RDS^O13":{
                    returnStatus = getEMRILService().processPharmacyDispense(ilMessage);
                    break;
                }
                case "SIU^S12":{
                    returnStatus = getEMRILService().processAppointmentSchedule(ilMessage);
                    break;
                }
                case "ORM^O01":{
                    returnStatus = getEMRILService().processLabOrder(ilMessage);
                    break;
                }
                case "ORU^R01":{
                    returnStatus = getEMRILService().processObservationResult(ilMessage);
                    break;
                }
                case "MOH731^ADX":{
                    returnStatus = getEMRILService().process731Adx(ilMessage);
                    break;
                }
                case "ORU^VL":{
                    returnStatus = getEMRILService().processViralLoad(ilMessage);
                    break;
                }
                default:{
                    log.error(ilMessage.getMessage_header().getMessage_type() + " message type is not yet supported");
                    break;
                }
            }


            if(returnStatus){
//                if the processing was ok, mark as retired so that it is not processed again;
                pendingInbox.setRetired(returnStatus);
                getEMRILService().saveKenyaEMRILMessage(pendingInbox);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private List<KenyaEMRILMessage> fetchILInboxes(boolean fetchRetired) {
        return getEMRILService().getKenyaEMRILInboxes(fetchRetired);
    }

    private List<KenyaEMRILMessage> fetchILOutboxes(boolean fetchRetired) {
        return getEMRILService().getKenyaEMRILOutboxes(fetchRetired);
    }

    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}