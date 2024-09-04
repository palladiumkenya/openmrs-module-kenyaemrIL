package org.openmrs.module.kenyaemrIL;

import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a task that processes appointment tasks and marks them for sending to an Interoperability layer.
 */
public class ProcessAppointmentsTask extends AbstractTask {
    private static final Logger log = LoggerFactory.getLogger(ProcessAppointmentsTask.class);
    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);

        log.info("Executing appointments task at " + new Date());

        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("appointmentTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Integer> appointmentMap = fetchPendingAppointments(null, fetchDate);
        System.out.println("Total appointments found: ......." + appointmentMap.size());

        if (appointmentMap.isEmpty()) {
            return;
        }
        ArrayList<Integer> patientIdListFromAppointments = new ArrayList<>(appointmentMap.values());
        Map<Integer, Boolean> patientConsentMap = getLatestConsentForReminder(patientIdListFromAppointments);

        for (String appointmentUuid : appointmentMap.keySet()) {
            Appointment appt = appointmentsService.getAppointmentByUuid(appointmentUuid);
            Integer patientId = appt.getPatient().getPatientId();

            if (patientConsentMap !=null && !patientConsentMap.isEmpty()) {
            boolean consentForReminder = patientConsentMap.get(patientId);
            appointmentsEvent(appt.getPatient(), appt, consentForReminder);
            }
        }

        Date nextProcessingDate = new Date();
        globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
        Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);

    }

    /**
     * Fetch appointments from the Bahmni appointment backend
     * @param appointmentTypes
     * @param date
     * @return a map of appointment uuuid and patient id from appointment model
     */
    private Map<String, Integer> fetchPendingAppointments(List<Integer> appointmentTypes, Date date) {

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select apt.uuid, apt.patient_id " +
                "from patient_appointment apt " +
                "inner join appointment_service aps on aps.appointment_service_id = apt.appointment_service_id and aps.uuid in ('885b4ad3-fd4c-4a16-8ed3-08813e6b01fa', 'a96921a1-b89e-4dd2-b6b4-7310f13bbabe') " +
                "where apt.voided = 0 and apt.status = 'Scheduled' and (apt.date_created >= '" + effectiveDate + "' or apt.date_changed >= '" + effectiveDate + "' )" );

        Map<String, Integer> patientAppointmentMap = new HashMap<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            String appointmentUuid = (String) row.get(0);
            Integer patientId = (Integer) row.get(1);
            patientAppointmentMap.put(appointmentUuid, patientId);
        }
        return patientAppointmentMap;
    }

    /**
     * Get patient consent from the last green card encounter
     * It would be ideal if consent is added to the appointment model
     * @param patientList
     * @return
     */
    private Map<Integer, Boolean> getLatestConsentForReminder(List<Integer> patientList) {
        Map<Integer, Boolean> patientConsentMap = new HashMap<>();
        if (patientList.isEmpty()) {
            return patientConsentMap;
        }
        String joinedIds = StringUtils.join(patientList, ",");
        String query = "select o.person_id, mid(max(concat(o.obs_datetime, o.value_coded)),20) latest_consent\n" +
                "from obs o\n" +
                "inner join encounter e on e.encounter_id = o.encounter_id\n" +
                "inner join form f on f.form_id = e.form_id and f.uuid in ('22c68f86-bbf0-49ba-b2d1-23fa7ccf0259')\n" +
                "inner join encounter_type et on et.encounter_type_id = e.encounter_type\n" +
                "where o.concept_id = 166607 and o.person_id in (" + joinedIds +")\n" +
                "group by o.person_id;";

        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(query, true);
        for (List<Object> row : queryData) {
            Integer patientId = (Integer) row.get(0);
            Integer consentConceptId = Integer.valueOf((String) row.get(1));
            patientConsentMap.put(patientId, consentConceptId == 1065 ? true : false);
        }
        return patientConsentMap;
    }
    private boolean appointmentsEvent(Patient patient, Appointment apt, boolean consentForReminder) {
        ILMessage ilMessage = ILPatientAppointments.iLPatientWrapper(patient, apt, consentForReminder);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        return service.logAppointmentSchedule(ilMessage, apt.getPatient());
    }
}