package org.openmrs.module.kenyaemrIL.il;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILAppointment {

    private MessageHeader messageHeader;
    private PatientIdentification patientIdentification;
    private List<AppointmentInformation> appointmentList;

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public void setMessageHeader(MessageHeader messageHeader) {
        this.messageHeader = messageHeader;
    }

    public PatientIdentification getPatientIdentification() {
        return patientIdentification;
    }

    public void setPatientIdentification(PatientIdentification patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    public List<AppointmentInformation> getAppointmentList() {
        return appointmentList;
    }

    public void setAppointmentList(List<AppointmentInformation> appointmentList) {
        this.appointmentList = appointmentList;
    }
}
