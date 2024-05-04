package org.openmrs.module.kenyaemrIL;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.service.AppointmentsService;
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
 * Implementation of a task that processes appointment tasks and marks them for sending to IL.
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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        log.info("Executing appointments task at " + new Date());

        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("appointmentTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EncounterType encounterTypeGreencard = Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e");
        //Fetch all encounters
        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterTypeGreencard);
        List<Appointment> pendingAppointments = fetchPendingAppointments(null, fetchDate);

        for (Appointment apt : pendingAppointments) {
            Patient p = apt.getPatient();
            appointmentsEvent(p, apt);
        }
        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    private List<Appointment> fetchPendingAppointments(List<Integer> appointmentTypes, Date date) {

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select patient_appointment_id " +
                "from patient_appointment apt " +
                "inner join appointment_service aps on aps.appointment_service_id = apt.appointment_service_id and aps.uuid in ('885b4ad3-fd4c-4a16-8ed3-08813e6b01fa', 'a96921a1-b89e-4dd2-b6b4-7310f13bbabe') " +
                "where apt.voided = 0 and (date_created >= '" + effectiveDate + "' or date_changed >= '" + effectiveDate + "' )" );

        List<Appointment> appointments = new ArrayList<>();
        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer appointmentId = (Integer) row.get(0);
            Appointment appt = appointmentsService.getAppointmentById(appointmentId);
            appointments.add(appt);
        }
        return appointments;

    }

    private boolean appointmentsEvent(Patient patient, Appointment apt) {
        ILMessage ilMessage = ILPatientAppointments.iLPatientWrapper(patient, apt);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logAppointmentSchedule(ilMessage, apt.getPatient());
    }



}