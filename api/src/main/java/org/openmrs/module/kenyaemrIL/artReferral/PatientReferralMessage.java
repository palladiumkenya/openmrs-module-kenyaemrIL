package org.openmrs.module.kenyaemrIL.artReferral;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class PatientReferralMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private PATIENT_REFERRAL_INFORMATION serviceRequest;

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

    public PATIENT_REFERRAL_INFORMATION getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(PATIENT_REFERRAL_INFORMATION serviceRequest) {
        this.serviceRequest = serviceRequest;
    }
}
