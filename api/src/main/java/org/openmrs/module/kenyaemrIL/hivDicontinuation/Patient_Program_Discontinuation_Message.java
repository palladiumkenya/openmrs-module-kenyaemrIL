package org.openmrs.module.kenyaemrIL.hivDicontinuation;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class Patient_Program_Discontinuation_Message {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private Program_Discontinuation_Message discontinuation_message;

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

    public Program_Discontinuation_Message getDiscontinuation_message() {
        return discontinuation_message;
    }

    public void setDiscontinuation_message(Program_Discontinuation_Message discontinuation_message) {
        this.discontinuation_message = discontinuation_message;
    }
}
