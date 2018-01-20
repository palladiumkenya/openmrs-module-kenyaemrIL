package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.cfg.NotYetImplementedException;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientRegistration;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILPerson;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes the IL inbox every one minute .
 */
public class ProcessEnrollmentTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessEnrollmentTask.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * @see org.openmrs.scheduler.tasks.AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing task at " + new Date());
// Fetch enrollment encounter
        Date fetchDate = null;
        EncounterType encounterTypeEnrollment = Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc");
 //Fetch all encounters
        List<Encounter> pendingEnrollments = fetchPendingEnrollments(encounterTypeEnrollment,fetchDate);

        for(Encounter e : pendingEnrollments){
            Patient p = e.getPatient();
            registrationEvent(p);
        }
    }

    private List<Encounter> fetchPendingEnrollments(EncounterType encounterType, Date date) {
       // Context.getEncounterService().getEncounters()
       // Fetch encounters
       throw new NotYetImplementedException("Not working yet");
    }

    private  void  registrationEvent(Patient patient){
        ILPerson ilPerson = ILPatientRegistration.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        boolean b = service.sendAddPersonRequest(ilPerson);
    }
    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}