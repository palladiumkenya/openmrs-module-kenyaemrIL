package org.openmrs.module.kenyaemrIL;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientDiscontinuation;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProgramDiscontinuationTask extends AbstractTask {
    @Override
    public void execute() {
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
        List<Encounter> pendingEnrollments = fetchPendingHivDiscontinuations(fetchDate);
        for (Encounter e : pendingEnrollments) {

            discontinuationEvent(e.getPatient(), e);
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
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='2bdada65-4c72-4a48-8730-859890e25cee' " +
                " ) et on et.encounter_type_id = e.encounter_type and e.voided = 0 " +
                " where e.date_created >= '"+effectiveDate+"' or e.date_changed >= '"+effectiveDate+"' ");
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
