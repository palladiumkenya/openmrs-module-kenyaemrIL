package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientViralLoadResults;
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
public class ProcessViralLoadTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessViralLoadTask.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**git
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        log.info("Executing vl results task at " + new Date());
        System.out.println("Executing vl results task at " + new Date());
//        Fetch lab results encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("virallaodTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeLabResults = Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28");      //Get lab results encounter
        //Fetch all lab rsults encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterTypeLabResults);
        System.out.println("Encounter types"+encounterTypes);
        System.out.println("FetchDate"+fetchDate);
        List<Encounter> pendingViralLoads = fetchPendingViralLoads(encounterTypes, fetchDate);
        System.out.println("Fetched VL encounters"+pendingViralLoads);
        for (Encounter e : pendingViralLoads) {
            Patient p = e.getPatient();
            boolean b = viralLoadEvent(p);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingViralLoads(List<EncounterType> encounterTypes, Date date) {
        // return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e ");
        q.append("where e.date_created >= '" + effectiveDate + "' ");
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

    private boolean viralLoadEvent(Patient patient) {
        ILMessage ilMessage = ILPatientViralLoadResults.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logViralLoad(ilMessage);
    }



}