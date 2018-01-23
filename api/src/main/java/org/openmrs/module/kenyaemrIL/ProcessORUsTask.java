package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
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
public class ProcessORUsTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessORUsTask.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        log.info("Executing ORU task at " + new Date());
//        Fetch lab results encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("oruTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        TODO - check the list of encounter types of interest.
        EncounterType encounterType = Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28");      //Get encounters of interest
        //Fetch encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
//        TODO - Add all the encoutners of interest
        encounterTypes.add(encounterType);
        List<Encounter> pendingViralLoads = fetchPendingViralLoads(encounterTypes, fetchDate);
        for (Encounter e : pendingViralLoads) {
            Patient p = e.getPatient();
            boolean b = oruEvent(p);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingViralLoads(List<EncounterType> encounterTypes, Date date) {
        return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

    }

    private boolean oruEvent(Patient patient) {
        ILMessage ilMessage = ILPatientViralLoadResults.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logORUs(ilMessage);
    }



}