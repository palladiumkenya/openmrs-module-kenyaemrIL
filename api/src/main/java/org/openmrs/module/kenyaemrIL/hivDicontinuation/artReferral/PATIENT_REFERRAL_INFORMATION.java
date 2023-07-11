package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;


import org.hl7.fhir.r4.model.ServiceRequest;

public class PATIENT_REFERRAL_INFORMATION {
    private ServiceRequest.ServiceRequestStatus status;
    private ServiceRequest.ServiceRequestIntent intent;
    private ServiceRequest.ServiceRequestPriority priority;
    private String toFacilityMflCode;
    private String tiFacilityMflCode;

    public ServiceRequest.ServiceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceRequest.ServiceRequestStatus status) {
        this.status = status;
    }

    public ServiceRequest.ServiceRequestIntent getIntent() {
        return intent;
    }

    public void setIntent(ServiceRequest.ServiceRequestIntent intent) {
        this.intent = intent;
    }

    public ServiceRequest.ServiceRequestPriority getPriority() {
        return priority;
    }

    public void setPriority(ServiceRequest.ServiceRequestPriority priority) {
        this.priority = priority;
    }

    public String getToFacilityMflCode() {
        return toFacilityMflCode;
    }

    public void setToFacilityMflCode(String toFacilityMflCode) {
        this.toFacilityMflCode = toFacilityMflCode;
    }

    public String getTiFacilityMflCode() {
        return tiFacilityMflCode;
    }

    public void setTiFacilityMflCode(String tiFacilityMflCode) {
        this.tiFacilityMflCode = tiFacilityMflCode;
    }

}
