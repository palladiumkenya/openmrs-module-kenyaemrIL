package org.openmrs.module.kenyaemrIL;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientDiscontinuation;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramDiscontinuationTask extends AbstractTask {
    @Override
    public void execute() {
        System.out.println("Executing ProgramDiscontinuationTask TASK .................");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        /**Fetch the last date of sync*/
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("discontinuationTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fetch all discontinuation encounters
        List<Encounter> pendingHivDiscontinuations = fetchPendingHivDiscontinuations(fetchDate);
        List<String> capturedDiscontinuations = Arrays.asList("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        for (Encounter e : pendingHivDiscontinuations) {
            List<Obs> discontinuationTypeObs = e.getObs().stream().filter(ob -> ob.getConcept().getUuid()
                    .equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).collect(Collectors.toList());
            if (!discontinuationTypeObs.isEmpty()) {
                if (capturedDiscontinuations.contains(discontinuationTypeObs.get(0).getValueCoded().getUuid())) {
                    discontinuationEvent(e.getPatient(), e);
                }
            }
        }

        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
    }

    /**
     * Fetch new/edited discontinuation encounters
     *
     * @param date
     * @return
     */
    private List<Encounter> fetchPendingHivDiscontinuations(Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='2bdada65-4c72-4a48-8730-859890e25cee' " +
                " ) et on et.encounter_type_id = e.encounter_type and e.voided = 0 " +
                " where e.date_created >=  '" + effectiveDate + "' or e.date_changed >=  '" + effectiveDate + "' ");
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
