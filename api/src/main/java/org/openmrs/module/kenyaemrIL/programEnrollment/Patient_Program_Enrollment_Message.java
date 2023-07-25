package org.openmrs.module.kenyaemrIL.programEnrollment;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class Patient_Program_Enrollment_Message {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private Program_Enrollment_Message program_enrollment_message;

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

    public Program_Enrollment_Message getProgram_enrollment_message() {
        return program_enrollment_message;
    }

    public void setProgram_enrollment_message(Program_Enrollment_Message program_enrollment_message) {
        this.program_enrollment_message = program_enrollment_message;
    }
}
