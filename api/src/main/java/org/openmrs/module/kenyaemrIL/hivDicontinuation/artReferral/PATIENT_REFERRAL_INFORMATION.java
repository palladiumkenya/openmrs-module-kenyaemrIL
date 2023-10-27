package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hl7.fhir.r4.model.ServiceRequest;

public class PATIENT_REFERRAL_INFORMATION {
    private ServiceRequest.ServiceRequestStatus transfer_status;
    private ServiceRequest.ServiceRequestIntent transfer_intent;
    private ServiceRequest.ServiceRequestPriority transfer_priority;
    private String transfer_out_date;
    private String to_acceptance_date;
    private String sending_facility_mflcode;
    private String receiving_facility_mflcode;
    private SERVICE_REQUEST_SUPPORTING_INFO supporting_info;


    public ServiceRequest.ServiceRequestStatus getTransfer_status() {
        return transfer_status;
    }

    public void setTransfer_status(String transfer_status) {
        this.transfer_status = ServiceRequest.ServiceRequestStatus.fromCode(transfer_status.toLowerCase());
    }

    public ServiceRequest.ServiceRequestIntent getTransfer_intent() {
        return transfer_intent;
    }

    public void setTransfer_intent(String transfer_intent) {
        this.transfer_intent = ServiceRequest.ServiceRequestIntent.fromCode(transfer_intent.toLowerCase());
    }

    public ServiceRequest.ServiceRequestPriority getTransfer_priority() {
        return transfer_priority;
    }

    public void setTransfer_priority(String transfer_priority) {
        this.transfer_priority = ServiceRequest.ServiceRequestPriority.fromCode(transfer_priority.toLowerCase());
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

    public String getSending_facility_mflcode() {
        return sending_facility_mflcode;
    }

    public void setSending_facility_mflcode(String sending_facility_mflcode) {
        this.sending_facility_mflcode = sending_facility_mflcode;
    }

    public String getReceiving_facility_mflcode() {
        return receiving_facility_mflcode;
    }

    public void setReceiving_facility_mflcode(String receiving_facility_mflcode) {
        this.receiving_facility_mflcode = receiving_facility_mflcode;
    }

    public SERVICE_REQUEST_SUPPORTING_INFO getSupporting_info() {
        return supporting_info;
    }

    public void setSupporting_info(SERVICE_REQUEST_SUPPORTING_INFO supporting_info) {
        this.supporting_info = supporting_info;
    }
}
