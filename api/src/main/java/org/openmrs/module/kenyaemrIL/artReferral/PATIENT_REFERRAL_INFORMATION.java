package org.openmrs.module.kenyaemrIL.artReferral;


import org.hl7.fhir.r4.model.ServiceRequest;

public class PATIENT_REFERRAL_INFORMATION {
    private ServiceRequest.ServiceRequestStatus status;
    private ServiceRequest.ServiceRequestIntent intent;
    private ServiceRequest.ServiceRequestPriority priority;
    private String toFacilityName;
    private String toFacilityMflCode;
    private String toFacilityPhone;
    private String tiFacilityName;
    private String tiFacilityMflCode;
    private String tiFacilityPhone;

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

    public String getToFacilityName() {
        return toFacilityName;
    }

    public void setToFacilityName(String toFacilityName) {
        this.toFacilityName = toFacilityName;
    }

    public String getToFacilityMflCode() {
        return toFacilityMflCode;
    }

    public void setToFacilityMflCode(String toFacilityMflCode) {
        this.toFacilityMflCode = toFacilityMflCode;
    }

    public String getToFacilityPhone() {
        return toFacilityPhone;
    }

    public void setToFacilityPhone(String toFacilityPhone) {
        this.toFacilityPhone = toFacilityPhone;
    }

    public String getTiFacilityName() {
        return tiFacilityName;
    }

    public void setTiFacilityName(String tiFacilityName) {
        this.tiFacilityName = tiFacilityName;
    }

    public String getTiFacilityMflCode() {
        return tiFacilityMflCode;
    }

    public void setTiFacilityMflCode(String tiFacilityMflCode) {
        this.tiFacilityMflCode = tiFacilityMflCode;
    }

    public String getTiFacilityPhone() {
        return tiFacilityPhone;
    }

    public void setTiFacilityPhone(String tiFacilityPhone) {
        this.tiFacilityPhone = tiFacilityPhone;
    }
}
