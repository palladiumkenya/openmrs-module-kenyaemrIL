package org.openmrs.module.kenyaemrIL;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientUnsolicitedObservationResults;
import org.openmrs.module.kenyaemrIL.api.ILPrescriptionMessage;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        log.info("Executing Orders task at " + new Date());
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("pharmacyOrderTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

//       list of encounter types of interest.
        EncounterType drugOrderEncounterType = Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3");  //last drug order
        //Fetch encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
//       Add all the encoutners of interest
        encounterTypes.add(drugOrderEncounterType);


        List<Encounter> pendingDrugOrders = fetchPendingOrders(encounterTypes, fetchDate);
        Map<Patient, List<Encounter>> groupedEncounters = groupEncountersByPatient(pendingDrugOrders);
        System.out.println("Active orders:=================" + groupedEncounters.toString());

        for (Map.Entry entry : groupedEncounters.entrySet()) {

            processPendingOrders((Patient) entry.getKey(), (List<Encounter>) entry.getValue());
        }

        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingOrders(List<EncounterType> encounterTypes, Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join orders o on o.encounter_id=e.encounter_id and o.voided=0 and o.order_action='NEW' and o.date_stopped is null " );
        q.append("where e.date_created = '2020-11-10'");
       // q.append("where e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'");
        q.append(" and e.voided = 0 group by e.encounter_id ");

        List<Encounter> encounters = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Encounter e = encounterService.getEncounter(encounterId);
            encounters.add(e);
        }
        System.out.println("No of drug encounters found: " + encounters.size());
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