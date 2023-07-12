package org.openmrs.module.kenyaemrIL.hivDicontinuation;

import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;

public class HivProgramEnrolmentMessage {
    private String patientType;
    private String entryPoint;
    private String facilityFrom;
    private PATIENT_REFERRAL_INFORMATION service_request;

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getFacilityFrom() {
        return facilityFrom;
    }

    public void setFacilityFrom(String facilityFrom) {
        this.facilityFrom = facilityFrom;
    }

    public PATIENT_REFERRAL_INFORMATION getService_request() {
        return service_request;
    }

    public void setService_request(PATIENT_REFERRAL_INFORMATION service_request) {
        this.service_request = service_request;
    }
}
