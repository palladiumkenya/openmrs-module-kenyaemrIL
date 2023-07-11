package org.openmrs.module.kenyaemrIL.hivDicontinuation;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class PatientHivDiscontinuationMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private HivProgramDiscontinuationMessage patient_hiv_discontinuation_message;

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

    public HivProgramDiscontinuationMessage getPatient_hiv_discontinuation_message() {
        return patient_hiv_discontinuation_message;
    }

    public void setPatient_hiv_discontinuation_message(HivProgramDiscontinuationMessage patient_hiv_discontinuation_message) {
        this.patient_hiv_discontinuation_message = patient_hiv_discontinuation_message;
    }
}
