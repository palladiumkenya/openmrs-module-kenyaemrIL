package org.openmrs.module.kenyaemrIL.api.chore;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemrIL.api.ILPatientDiscontinuation;
import org.openmrs.module.kenyaemrIL.api.ILPatientEnrollment;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component("kenyaemrIL.chore.ProcessArtReferralsChore")
public class ProcessArtReferralsChore extends AbstractChore {
    @Override
    public void perform(PrintWriter printWriter) throws APIException {
        System.out.println("Executing ProcessEnrollment Chore .................");

        //Process transfer outs
        processTransferOuts();

        //Process transfer ins
        processTransferIns();

    }

    private void processTransferOuts() {
        // Fetch all discontinuation encounters
        List<Encounter> pendingHivDiscontinuations = fetchPendingHivDiscontinuations();
        List<String> capturedDiscontinuations = Arrays.asList("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        for (Encounter e : pendingHivDiscontinuations) {
            List<Obs> discontinuationTypeObs = e.getObs().stream().filter(ob -> ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).collect(Collectors.toList());
            if (!discontinuationTypeObs.isEmpty()) {
                if (capturedDiscontinuations.contains(discontinuationTypeObs.get(0).getValueCoded().getUuid())) {
                    discontinuationEvent(e.getPatient(), e);
                }
            }
        }
    }

    private void processTransferIns() {
        List<Encounter> pendingEnrollments = fetchPendingEnrollments();
        for (Encounter e : pendingEnrollments) {
            List<Obs> tiPatientType = e.getObs().stream().filter(obs -> obs.getConcept().getConceptId().equals(164932) && obs.getValueCoded().getConceptId().equals(160563)).collect(Collectors.toList());
            if (!tiPatientType.isEmpty()) {
                programEnrollmentEvent(e.getPatient(), e);
            }
        }
    }

    private List<Encounter> fetchPendingEnrollments() {
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " + "( " + " select encounter_type_id, uuid, name from encounter_type where uuid ='de78a6be-bfc5-4634-adc3-5f1a280455cc' " + " ) et on et.encounter_type_id = e.encounter_type and e.voided = 0 ");
        q.append("where e.date_created >= '2023-08-15' or e.date_changed >= '2023-08-15' ");
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

    private boolean programEnrollmentEvent(Patient patient, Encounter e) {
        ILMessage ilMessage = ILPatientEnrollment.iLPatientWrapper(patient, e);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logCompletedPatientReferrals(ilMessage, e.getPatient());
    }

    private List<Encounter> fetchPendingHivDiscontinuations() {
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " + "( " + " select encounter_type_id, uuid, name from encounter_type where uuid ='2bdada65-4c72-4a48-8730-859890e25cee' " + " ) et on et.encounter_type_id = e.encounter_type and e.voided = 0 " + " where e.date_created >=  '2023-08-15' or e.date_changed >=  '2023-08-15' ");
        q.append(" group by e.patient_id ");

        List<Encounter> encounters = new ArrayList<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Encounter e = Context.getEncounterService().getEncounter(encounterId);
            encounters.add(e);
        }
        return encounters;

    }

    private boolean discontinuationEvent(Patient patient, Encounter e) {
        ILMessage ilMessage = ILPatientDiscontinuation.iLPatientWrapper(patient, e);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logPatientReferrals(ilMessage, e.getPatient());
    }
}
