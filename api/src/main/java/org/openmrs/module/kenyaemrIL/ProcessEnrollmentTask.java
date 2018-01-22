package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientRegistration;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes enrollments tasks and marks the for sending to IL.
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
//        Fetch enrolment encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("enrolmentTask.lastFetchDateAndTime");
       // String fetchID = Context.getAdministrationService().getGlobalProperty("enrolmentTask.lastFetchId");


        try {
            long ts = (long) globalPropertyObject.getValue();
            fetchDate = new Date(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeEnrollment = Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc");
        //Fetch all encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterTypeEnrollment);
        List<Encounter> pendingEnrollments = fetchPendingEnrollments(encounterTypes, fetchDate);
        for (Encounter e : pendingEnrollments) {
            Patient p = e.getPatient();
            boolean b = registrationEvent(p);
        }
        globalPropertyObject.setValue(new Date().getTime());
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingEnrollments(List<EncounterType> encounterTypes, Date date) {
        return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

    }

    private boolean registrationEvent(Patient patient) {
        ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.processCreatePatientRequest(ilMessage);
    }

    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}