package org.openmrs.module.kenyaemrIL.il.appointment;

import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class AppointmentMessage extends ILMessage {
    private APPOINTMENT_INFORMATION[] appointment_information;


    public APPOINTMENT_INFORMATION[] getAppointment_information() {
        return appointment_information;
    }

    public void setAppointment_information(APPOINTMENT_INFORMATION[] appointment_information) {
        this.appointment_information = appointment_information;
    }
}
