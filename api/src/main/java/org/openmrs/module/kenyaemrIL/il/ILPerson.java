package org.openmrs.module.kenyaemrIL.il;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILPerson {
    private MessageHeader messageHeader;
    private PatientIdentification patientIdentification;
    private PatientVisit patientVisit;
    private List<NextOfKin> nextOfKins;

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

    public PatientVisit getPatientVisit() {
        return patientVisit;
    }

    public void setPatientVisit(PatientVisit patientVisit) {
        this.patientVisit = patientVisit;
    }

    public List<NextOfKin> getNextOfKins() {
        return nextOfKins;
    }

    public void setNextOfKins(List<NextOfKin> nextOfKins) {
        this.nextOfKins = nextOfKins;
    }
}
