package org.openmrs.module.kenyaemrIL.programEnrollment;

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Patient;

import java.io.Serializable;
import java.util.Date;

public class ExpectedTransferInPatients extends BaseOpenmrsMetadata implements Serializable {
    private Integer id;
    private Patient patient;
    private Date transferOutDate;
    private Integer transferOutFacility;
    private Date appointmentDate;
    private Date effectiveDiscontinuationDate;
    private String referralStatus;
    private Date toAcceptanceDate;
    private String patientSummary;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Date getTransferOutDate() {
        return transferOutDate;
    }

    public void setTransferOutDate(Date transferOutDate) {
        this.transferOutDate = transferOutDate;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Date getEffectiveDiscontinuationDate() {
        return effectiveDiscontinuationDate;
    }

    public void setEffectiveDiscontinuationDate(Date effectiveDiscontinuationDate) {
        this.effectiveDiscontinuationDate = effectiveDiscontinuationDate;
    }

    public Integer getTransferOutFacility() {
        return transferOutFacility;
    }

    public void setTransferOutFacility(Integer transferOutFacility) {
        this.transferOutFacility = transferOutFacility;
    }

    public Date getToAcceptanceDate() {
        return toAcceptanceDate;
    }

    public void setToAcceptanceDate(Date toAcceptanceDate) {
        this.toAcceptanceDate = toAcceptanceDate;
    }

    public String getReferralStatus() {
        return referralStatus;
    }

    public void setReferralStatus(String referralStatus) {
        this.referralStatus = referralStatus;
    }

    public String getPatientSummary() {
        return patientSummary;
    }

    public void setPatientSummary(String patientSummary) {
        this.patientSummary = patientSummary;
    }
}

