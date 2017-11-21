package org.openmrs.module.kenyaemrIL.il.pharmacy;
/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PharmacyEncodedOrder {
    private String frequency;
    private String prescriptionNotes;
    private String dosage;
    private String codingSystem;
    private String quantityPrescribed;
    private String strength;
    private String duration;
    private String drugName;


    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getPrescriptionNotes() {
        return prescriptionNotes;
    }

    public void setPrescriptionNotes(String prescriptionNotes) {
        this.prescriptionNotes = prescriptionNotes;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getCodingSystem() {
        return codingSystem;
    }

    public void setCodingSystem(String codingSystem) {
        this.codingSystem = codingSystem;
    }

    public String getQuantityPrescribed() {
        return quantityPrescribed;
    }

    public void setQuantityPrescribed(String quantityPrescribed) {
        this.quantityPrescribed = quantityPrescribed;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }
}
