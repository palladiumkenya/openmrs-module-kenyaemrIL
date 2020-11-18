package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientRegistration;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes enrollments tasks and marks them for sending to IL.
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        Fetch enrollment encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("enrolmentTask.lastFetchDateAndTime");
        String fetchID = Context.getAdministrationService().getGlobalProperty("enrolmentTask.lastFetchId");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
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
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingEnrollments(List<EncounterType> encounterTypes, Date date) {
        // return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='de78a6be-bfc5-4634-adc3-5f1a280455cc' " +
                " ) et on et.encounter_type_id=e.encounter_type ");
        q.append("where e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'");
        q.append(" and e.voided = 0  ");

        List<Encounter> encounters = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Encounter e = encounterService.getEncounter(encounterId);
            encounters.add(e);
        }
        return encounters;

    }

    private boolean registrationEvent(Patient patient) {
        boolean notDuplicate = false;
        PersonAttribute checkDuplicate = patient.getAttribute("IL Patient Source");
        if (checkDuplicate != null) {
            notDuplicate = false;
        } else{
            ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
            KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
            service.sendAddPersonRequest(ilMessage);
            notDuplicate = true;
        }
        return notDuplicate;
    }


}