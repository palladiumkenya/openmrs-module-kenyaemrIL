package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientEnrollment;
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
import java.util.stream.Collectors;

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
        List<Patient> patientsStartedOnArt = getArtInitiations(fetchDate);
        for (Encounter e : pendingEnrollments) {
            Patient p = e.getPatient();
            List<Obs> tiPatientType = e.getObs().stream().filter(obs -> obs.getConcept().getConceptId().equals(164932) &&
                    obs.getValueCoded().getConceptId().equals(160563)).collect(Collectors.toList());
            if (!tiPatientType.isEmpty()) {
                programEnrollmentEvent(e.getPatient(), e);
            }
            // check if the patient is also in the list for updates.
            if (patientsStartedOnArt != null && patientsStartedOnArt.size() > 0) {
                if (patientsStartedOnArt.contains(p)) {
                    patientsStartedOnArt.remove(p);// remove so that a patient message is generated just once
                }
            }
            boolean b = registrationEvent(p);
            //process transfer in patients
        }

        if (patientsStartedOnArt != null && patientsStartedOnArt.size() > 0) {
            for (Patient p : patientsStartedOnArt) {
                registrationUpdateEvent(p);
            }
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    /**
     * Fetch new/edited enrollment encounters
     * @param encounterTypes
     * @param date
     * @return
     */
    private List<Encounter> fetchPendingEnrollments(List<EncounterType> encounterTypes, Date date) {
        // return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='de78a6be-bfc5-4634-adc3-5f1a280455cc' " +
                " ) et on et.encounter_type_id = e.encounter_type and e.voided = 0 ");
        q.append("where e.date_created >= '"+effectiveDate+"' or e.date_changed >= '"+effectiveDate+"' ");
        q.append(" group by e.patient_id ");

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

    /**
     * Gets a list of patients who have had art start events since the last timestamp
     * @param date last timestamp
     * @return a list of patients whose initial art initiations are as at the provided timestamp
     */
    private List<Patient> getArtInitiations(Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.patient_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='7dffc392-13e7-11e9-ab14-d663bd873d93' " +
                " ) et on et.encounter_type_id=e.encounter_type and e.voided = 0 ");
        q.append(" group by e.patient_id ");
        q.append("having min(e.date_created) >= '" + effectiveDate + "'");

        List<Patient> patients = new ArrayList<>();
        PatientService patientService = Context.getPatientService();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Patient p = patientService.getPatient(encounterId);
            patients.add(p);
        }
        return patients;
    }


    private boolean registrationEvent(Patient patient) {
        boolean notDuplicate = false;
        PersonAttribute checkDuplicate = patient.getAttribute("IL Patient Source");
        if (checkDuplicate != null) {
            notDuplicate = false;
        } else{
            ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
            KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
            service.sendAddPersonRequest(ilMessage, patient);
            notDuplicate = true;
        }
        return notDuplicate;
    }

    /**
     * Generates the registration update message
     * @param patient
     * @return
     */
    private boolean registrationUpdateEvent(Patient patient) {
        boolean notDuplicate = false;
        PersonAttribute checkDuplicate = patient.getAttribute("IL Patient Source");
        if (checkDuplicate != null) {
            notDuplicate = false;
        } else{
            ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
            KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
            service.sendUpdatePersonRequest(ilMessage, patient);
            notDuplicate = true;
        }
        return notDuplicate;
    }

    private boolean programEnrollmentEvent(Patient patient, Encounter e) {
        ILMessage ilMessage = ILPatientEnrollment.iLPatientWrapper(patient, e);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logCompletedPatientReferrals(ilMessage, e.getPatient());
    }


}