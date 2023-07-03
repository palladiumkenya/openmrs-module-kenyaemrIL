package org.openmrs.module.kenyaemrIL.il.appointment;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class AppointmentMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private APPOINTMENT_INFORMATION[] appointment_information;


    public MESSAGE_HEADER getMessage_header() {
        return message_header;
    }

    public void setMessage_header(MESSAGE_HEADER message_header) {
        this.message_header = message_header;
    }

    public PATIENT_IDENTIFICATION getPatient_identification() {
        return patient_identification;
    }

    public void setPatient_identification(PATIENT_IDENTIFICATION patient_identification) {
        this.patient_identification = patient_identification;
    }

    public APPOINTMENT_INFORMATION[] getAppointment_information() {
        return appointment_information;
    }

    public void setAppointment_information(APPOINTMENT_INFORMATION[] appointment_information) {
        this.appointment_information = appointment_information;
    }

}
