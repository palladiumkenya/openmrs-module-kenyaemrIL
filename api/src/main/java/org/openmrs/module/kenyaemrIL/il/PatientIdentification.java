package org.openmrs.module.kenyaemrIL.il;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PatientIdentification {
    private ExternalPatientId externalPatientId;
    private List<InternalPatientId> internalPatientIds;
    private PatientName patientName;
    private MotherName motherName;
    private String dateOfBirth;
    private String sex;
    private PatientAddress patientAddress;
    private String phoneNumber;
    private String maritalStatus;
    private String deathDate;
    private String deathIndicator;
    private String dateOfBirthPrecision;

    public ExternalPatientId getExternalPatientId() {
        return externalPatientId;
    }

    public void setExternalPatientId(ExternalPatientId externalPatientId) {
        this.externalPatientId = externalPatientId;
    }

    public List<InternalPatientId> getInternalPatientIds() {
        return internalPatientIds;
    }

    public void setInternalPatientIds(List<InternalPatientId> internalPatientIds) {
        this.internalPatientIds = internalPatientIds;
    }

    public PatientName getPatientName() {
        return patientName;
    }

    public void setPatientName(PatientName patientName) {
        this.patientName = patientName;
    }

    public MotherName getMotherName() {
        return motherName;
    }

    public void setMotherName(MotherName motherName) {
        this.motherName = motherName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public PatientAddress getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(PatientAddress patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(String deathDate) {
        this.deathDate = deathDate;
    }

    public String getDeathIndicator() {
        return deathIndicator;
    }

    public void setDeathIndicator(String deathIndicator) {
        this.deathIndicator = deathIndicator;
    }

    public String getDateOfBirthPrecision() {
        return dateOfBirthPrecision;
    }

    public void setDateOfBirthPrecision(String dateOfBirthPrecision) {
        this.dateOfBirthPrecision = dateOfBirthPrecision;
    }
}
