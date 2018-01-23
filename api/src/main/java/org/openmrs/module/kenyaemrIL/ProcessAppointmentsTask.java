package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILPatientAppointments;
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
 * Implementation of a task that processes enrollments tasks and marks the for sending to IL.
 */
public class ProcessAppointmentsTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessAppointmentsTask.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        log.info("Executing appointments task at " + new Date());
//        Fetch enrolment encounter
//        Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("appointmentTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeAppointment = Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e");
        //Fetch all encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterTypeAppointment);
        List<Encounter> pendingAppointments = fetchPendingAppointments(encounterTypes, fetchDate);
        for (Encounter e : pendingAppointments) {
            Patient p = e.getPatient();
            boolean b = appointmentsEvent(p);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Encounter> fetchPendingAppointments(List<EncounterType> encounterTypes, Date date) {
        return Context.getEncounterService().getEncounters(null, null, date, null, null, encounterTypes, null, null, null, false);

    }

    private boolean appointmentsEvent(Patient patient) {
        ILMessage ilMessage = ILPatientAppointments.iLPatientWrapper(patient);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.processAppointmentSchedule(ilMessage);
    }



}