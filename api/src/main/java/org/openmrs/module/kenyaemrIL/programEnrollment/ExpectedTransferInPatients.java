package org.openmrs.module.kenyaemrIL.programEnrollment;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

import java.util.Date;

public class ExpectedTransferInPatients extends BaseOpenmrsData {
    private static final long serialVersionUID = 3062136588828193225L;
    private Integer id;
    private Patient patient;
    private Date transferOutDate;
    private String transferOutFacility;
    private Date appointmentDate;
    private Date effectiveDiscontinuationDate;
    private String referralStatus;
    private Date toAcceptanceDate;
    private String patientSummary;
    private String serviceType;
    private String clientFirstName;
    private String clientMiddleName;
    private String clientLastName;
    private String clientGender;
    private Date clientBirthDate;
    private String nupiNumber;

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

    public String getTransferOutFacility() {
        return transferOutFacility;
    }

    public void setTransferOutFacility(String transferOutFacility) {
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getClientFirstName() {
        return clientFirstName;
    }

    public void setClientFirstName(String clientFirstName) {
        this.clientFirstName = clientFirstName;
    }

    public String getClientMiddleName() {
        return clientMiddleName;
    }

    public void setClientMiddleName(String clientMiddleName) {
        this.clientMiddleName = clientMiddleName;
    }

    public String getClientLastName() {
        return clientLastName;
    }

    public void setClientLastName(String clientLastName) {
        this.clientLastName = clientLastName;
    }

    public String getClientGender() {
        return clientGender;
    }

    public void setClientGender(String clientGender) {
        this.clientGender = clientGender;
    }

    public Date getClientBirthDate() {
        return clientBirthDate;
    }

    public void setClientBirthDate(Date clientBirthDate) {
        this.clientBirthDate = clientBirthDate;
    }

    public String getNupiNumber() {
        return nupiNumber;
    }

    public void setNupiNumber(String nupiNumber) {
        this.nupiNumber = nupiNumber;
    }
}

