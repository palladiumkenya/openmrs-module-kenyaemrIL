package org.openmrs.module.kenyaemrIL.il.observation;

import org.openmrs.module.kenyaemrIL.il.MessageHeader;
import org.openmrs.module.kenyaemrIL.il.ObservationResult;
import org.openmrs.module.kenyaemrIL.il.PatientIdentification;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILObservation {

    private MessageHeader messageHeader;
    private PatientIdentification patientIdentification;
    private List<ObservationResult> observationResults;

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

    public List<ObservationResult> getObservationResults() {
        return observationResults;
    }

    public void setObservationResults(List<ObservationResult> observationResults) {
        this.observationResults = observationResults;
    }
}
