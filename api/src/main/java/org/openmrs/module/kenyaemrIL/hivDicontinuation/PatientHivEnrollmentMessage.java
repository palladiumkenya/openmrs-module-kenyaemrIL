package org.openmrs.module.kenyaemrIL.hivDicontinuation;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class PatientHivEnrollmentMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;

    private HivProgramEnrolmentMessage hiv_enrollment_message;

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

    public HivProgramEnrolmentMessage getHiv_enrollment_message() {
        return hiv_enrollment_message;
    }

    public void setHiv_enrollment_message(HivProgramEnrolmentMessage hiv_enrollment_message) {
        this.hiv_enrollment_message = hiv_enrollment_message;
    }
}
