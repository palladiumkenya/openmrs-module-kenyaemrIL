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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
        String fetchDateString = Context.getAdministrationService().getGlobalProperty("enrolmentTask.lastFetchDateAndTime");
        DateFormat formatter = new SimpleDateFormat("hhmmssyyyyMMdd");
        try {
            fetchDate = formatter.parse(fetchDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeEnrollment = Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc");
        //Fetch all encounters
        List<Encounter> pendingEnrollments = fetchPendingEnrollments(encounterTypeEnrollment, fetchDate);
        for (Encounter e : pendingEnrollments) {
            Patient p = e.getPatient();
            registrationEvent(p);
        }
    }

    private List<Encounter> fetchPendingEnrollments(EncounterType encounterType, Date date) {
//        TODO - fetch pending enrolments
        throw new NotYetImplementedException("Not working yet");
    }

    private void registrationEvent(Patient patient) {
        ILPerson ilPerson = ILPatientRegistration.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        boolean b = service.sendAddPersonRequest(ilPerson);
    }

    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}