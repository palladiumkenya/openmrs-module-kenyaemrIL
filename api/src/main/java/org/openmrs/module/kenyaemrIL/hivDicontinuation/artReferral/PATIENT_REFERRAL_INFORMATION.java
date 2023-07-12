package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;


import org.hl7.fhir.r4.model.ServiceRequest;

public class PATIENT_REFERRAL_INFORMATION {
    private ServiceRequest.ServiceRequestStatus transfer_status;
    private ServiceRequest.ServiceRequestIntent transfer_intent;
    private ServiceRequest.ServiceRequestPriority transfer_priority;
    private String appointment_date;
    private String drug_days;
    private String viral_load;
    private String last_vl_date;
    private String current_regimen;
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

    public String getAppointment_date() {
        return appointment_date;
    }

    public void setAppointment_date(String appointment_date) {
        this.appointment_date = appointment_date;
    }

    public String getDrug_days() {
        return drug_days;
    }

    public void setDrug_days(String drug_days) {
        this.drug_days = drug_days;
    }

    public String getViral_load() {
        return viral_load;
    }

    public void setViral_load(String viral_load) {
        this.viral_load = viral_load;
    }

    public String getLast_vl_date() {
        return last_vl_date;
    }

    public void setLast_vl_date(String last_vl_date) {
        this.last_vl_date = last_vl_date;
    }

    public String getCurrent_regimen() {
        return current_regimen;
    }

    public void setCurrent_regimen(String current_regimen) {
        this.current_regimen = current_regimen;
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
