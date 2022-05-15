package org.openmrs.module.kenyaemrIL;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPrescriptionMessage;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a task that processes drug orders tasks and marks them for sending to IL.
 */
public class ProcessOrdersTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessOrdersTask.class);

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing Orders task at " + new Date());

        GlobalProperty lastEncounterEntryFromGP = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_IL_LAST_PHARMACY_MESSAGE_ENCOUNTER);// this will store the last encounter id prior to task execution

        String lastEncounterSql = "select max(encounter_id) last_id from encounter where voided=0;";
        List<List<Object>> lastEncounterId = Context.getAdministrationService().executeSQL(lastEncounterSql, true);

        Integer lastIdFromEncounterTable = (Integer) lastEncounterId.get(0).get(0);
        lastIdFromEncounterTable = lastIdFromEncounterTable != null ? lastIdFromEncounterTable : 0;

        String lastEncounterValueFromGPStr = lastEncounterEntryFromGP != null && lastEncounterEntryFromGP.getValue() != null ? lastEncounterEntryFromGP.getValue().toString() : "";
        Integer lastEncounterIDFromGP = StringUtils.isNotBlank(lastEncounterValueFromGPStr) ? Integer.parseInt(lastEncounterValueFromGPStr) : 0;

        List<Encounter> pendingDrugOrders = fetchPendingOrders(lastEncounterIDFromGP, lastIdFromEncounterTable);
        Map<Patient, List<Encounter>> groupedEncounters = groupEncountersByPatient(pendingDrugOrders);
        System.out.println("Active orders:=================" + groupedEncounters.toString());

        for (Map.Entry entry : groupedEncounters.entrySet()) {
            processPendingOrders((Patient) entry.getKey(), (List<Encounter>) entry.getValue());
        }

        lastEncounterEntryFromGP.setPropertyValue(lastIdFromEncounterTable.toString());
        Context.getAdministrationService().saveGlobalProperty(lastEncounterEntryFromGP);
    }

    private List<Encounter> fetchPendingOrders( Integer lastEncounterIdFromGP, Integer lastEncounterFromEncounterTable) {

        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join orders o on o.encounter_id=e.encounter_id and o.voided=0 and o.order_action='NEW' and o.date_stopped is null " );

       // q.append("where e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'");

        if (lastEncounterIdFromGP != null && lastEncounterIdFromGP > 0) {
            q.append("where e.encounter_id > " + lastEncounterIdFromGP + " ");
        } else {
            q.append("where e.encounter_id <= " + lastEncounterFromEncounterTable + " ");
        }

        q.append(" and e.voided = 0 group by e.encounter_id ");
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
    private boolean processPendingOrders(Patient patient, List<Encounter> encounters) {
        ILMessage ilMessage = ILPrescriptionMessage.generatePrescriptionMessage(patient, encounters);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logPharmacyOrders(ilMessage);
    }

    /**
     * Processes a list of encounters
     * @return a map containing encounters for each patient
     */
    private Map<Patient, List<Encounter>> groupEncountersByPatient(List<Encounter> encounters) {
        Map<Patient, List<Encounter>> encounterMap = new HashMap<Patient, List<Encounter>>();

        for (Encounter encounter : encounters) {
            if (encounterMap.keySet().contains(encounter.getPatient())) {
                encounterMap.get(encounter.getPatient()).add(encounter);
            } else {
                List<Encounter> eList = new ArrayList<Encounter>();
                eList.add(encounter);
                encounterMap.put(encounter.getPatient(), eList);
            }
        }
        return encounterMap;
    }



}