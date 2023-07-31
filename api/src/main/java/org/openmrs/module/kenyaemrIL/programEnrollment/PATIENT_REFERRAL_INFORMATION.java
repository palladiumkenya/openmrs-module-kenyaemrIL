package org.openmrs.module.kenyaemrIL.programEnrollment;


import org.hl7.fhir.r4.model.ServiceRequest;

public class PATIENT_REFERRAL_INFORMATION {
    private ServiceRequest.ServiceRequestStatus transfer_status;
    private ServiceRequest.ServiceRequestIntent transfer_intent;
    private ServiceRequest.ServiceRequestPriority transfer_priority;
    private String transfer_out_date;
    private String to_acceptance_date;
    private String sending_facility_mflCode;
    private String receiving_facility_mflCode;

    public ServiceRequest.ServiceRequestStatus getTransfer_status() {
        return transfer_status;
    }

    public void setTransfer_status(ServiceRequest.ServiceRequestStatus transfer_status) {
        this.transfer_status = transfer_status;
    }

    public ServiceRequest.ServiceRequestIntent getTransfer_intent() {
        return transfer_intent;
    }

    public void setTransfer_intent(ServiceRequest.ServiceRequestIntent transfer_intent) {
        this.transfer_intent = transfer_intent;
    }

    public ServiceRequest.ServiceRequestPriority getTransfer_priority() {
        return transfer_priority;
    }

    public void setTransfer_priority(ServiceRequest.ServiceRequestPriority transfer_priority) {
        this.transfer_priority = transfer_priority;
    }

    public String getTransfer_out_date() {
        return transfer_out_date;
    }

    public void setTransfer_out_date(String transfer_out_date) {
        this.transfer_out_date = transfer_out_date;
    }

    public String getTo_acceptance_date() {
        return to_acceptance_date;
    }

    public void setTo_acceptance_date(String to_acceptance_date) {
        this.to_acceptance_date = to_acceptance_date;
    }

    public String getSending_facility_mflCode() {
        return sending_facility_mflCode;
    }

    public void setSending_facility_mflCode(String sending_facility_mflCode) {
        this.sending_facility_mflCode = sending_facility_mflCode;
    }

    public String getReceiving_facility_mflCode() {
        return receiving_facility_mflCode;
    }

    public void setReceiving_facility_mflCode(String receiving_facility_mflCode) {
        this.receiving_facility_mflCode = receiving_facility_mflCode;
    }

}
