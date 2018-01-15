package org.openmrs.module.kenyaemrIL;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILPerson;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            ILPerson iLPerson = mapper.readValue(message.toLowerCase(), ILPerson.class);
            switch(iLPerson.getMessage_header().getMessage_type()){
                case "ADT^A04":{
                    returnStatus = getEMRILService().processCreatePatientRequest(iLPerson);
                    break;
                }
                case "ADT^A08":{
                    returnStatus = getEMRILService().processUpdatePatientRequest(iLPerson);
                    break;
                }
                case "RDE^001":{
                    returnStatus = getEMRILService().processPharmacyOrder(iLPerson);
                    break;
                }
                case "RDS^O13":{
                    returnStatus = getEMRILService().processPharmacyDispense(iLPerson);
                    break;
                }
                case "SIU^S12":{
                    returnStatus = getEMRILService().processAppointmentSchedule(iLPerson);
                    break;
                }
                case "ORM^O01":{
                    returnStatus = getEMRILService().processLabOrder(iLPerson);
                    break;
                }
                case "ORU^R01":{
                    returnStatus = getEMRILService().processObservationResult(iLPerson);
                    break;
                }
                case "ORU^VL":{
                    returnStatus = getEMRILService().processViralLoad(iLPerson);
                    break;
                }
                default:{
                    log.error(iLPerson.getMessage_header().getMessage_type() + " message type is not yet support");
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