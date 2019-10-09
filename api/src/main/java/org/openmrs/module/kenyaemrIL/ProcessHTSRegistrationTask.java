package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
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
 * Implementation of a task that processes registrations by HTS Initial Positive Test results tasks and marks them for sending to IL.
 */
public class ProcessHTSRegistrationTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessHTSRegistrationTask.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        Fetch HTS encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("htsRegistrationTask.lastFetchDateAndTime");
       // String fetchID = Context.getAdministrationService().getGlobalProperty("enrolmentTask.lastFetchId");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeHTS = Context.getEncounterService().getEncounterTypeByUuid("9c0a7a57-62ff-4f75-babe-5835b0e921b7");
        //Fetch all encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterTypeHTS);
        List<Encounter> pendingHTSRegistrations = fetchPendingHTSRegistrations(encounterTypes, fetchDate);
        for (Encounter e : pendingHTSRegistrations) {
            Patient p = e.getPatient();
            boolean b = registrationEvent(p);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingHTSRegistrations(List<EncounterType> encounterTypes, Date date) {

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='9c0a7a57-62ff-4f75-babe-5835b0e921b7' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                "inner join obs o on o.encounter_id = e.encounter_id and o.voided=0 " +
                " and o.concept_id in (159427) and o.value_coded = 703 ");
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
        ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.sendAddPersonRequest(ilMessage);
    }



}