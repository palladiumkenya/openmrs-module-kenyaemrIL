package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PharmacyDispense {
    private String dispenseNotes;
    private String frequency;
    private String quantityDispensed;
    private String dosage;
    private String codingSystem;
    private String strength;
    private String duration;
    private String actualDrugs;
    private String drugName;

    public String getDispenseNotes() {
        return dispenseNotes;
    }

    public void setDispenseNotes(String dispenseNotes) {
        this.dispenseNotes = dispenseNotes;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getQuantityDispensed() {
        return quantityDispensed;
    }

    public void setQuantityDispensed(String quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
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

    public String getActualDrugs() {
        return actualDrugs;
    }

    public void setActualDrugs(String actualDrugs) {
        this.actualDrugs = actualDrugs;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }
}
