package org.openmrs.module.kenyaemrIL;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientUnsolicitedObservationResults;
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
 * Implementation of a task that processes lab results- viral load tasks and marks the for sending to IL.
 */
public class ProcessORUsTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessORUsTask.class);

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        log.info("Executing ORU task at " + new Date());
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("oruTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

//       list of encounter types of interest.
        /*EncounterType hivGreencardEncounterType = Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e");   //last greencard followup
        EncounterType hivEnrollmentEncounterType = Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc");  //hiv enrollment
        EncounterType drugOrderEncounterType = Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3");  //last drug order
        EncounterType mchMotherEncounterType = Context.getEncounterService().getEncounterTypeByUuid("3ee036d8-7c13-4393-b5d6-036f2fe45126");  //mch mother enrollment
        EncounterType labResultEncounterType = Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28");  //lab results*/
        //Fetch encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
//       Add all the encoutners of interest
        /*encounterTypes.add(hivGreencardEncounterType);
        encounterTypes.add(hivEnrollmentEncounterType);
        encounterTypes.add(drugOrderEncounterType);
        encounterTypes.add(mchMotherEncounterType);
        encounterTypes.add(labResultEncounterType);*/


        List<Encounter> pendingObservations = fetchPendingObservations(encounterTypes, fetchDate);
        for (Encounter e : pendingObservations) {
            Patient p = e.getPatient();
            boolean b = oruEvent(p, e);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    /*private List<Encounter> fetchPendingObservations(List<EncounterType> encounterTypes, Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='a0034eee-1940-4e35-847f-97537a35d05e' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join obs o on o.encounter_id=e.encounter_id and o.voided=0 " +
                " and o.concept_id in (5090,5089)");
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

    }*/

    /**
     * Get HIV discontinuation forms
     * @param encounterTypes
     * @param date
     * @return
     */
    private List<Encounter> fetchPendingObservations(List<EncounterType> encounterTypes, Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='2bdada65-4c72-4a48-8730-859890e25cee' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join obs o on o.encounter_id=e.encounter_id and o.voided=0 ");
        q.append("where e.voided = 0 and e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'" + " or o.date_created >= '" + effectiveDate + "'");
        q.append(" group by e.encounter_id  ");

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
    private boolean oruEvent(Patient patient, Encounter encounter) {
        ILMessage ilMessage = ILPatientUnsolicitedObservationResults.iLPatientWrapper(patient, encounter);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logORUs(ilMessage);
    }



}