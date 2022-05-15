package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PHARMACY_DISPENSE {
    private String dispensing_notes;
    private String frequency;
    private String quantity_dispensed;
    private String dosage;
    private String coding_system;
    private String strength;
    private String duration;
    private String actual_drugs;
    private String drug_name;
    private String prescription_number;

    public String getPrescription_number() {
        return prescription_number;
    }

    public void setPrescription_number(String prescription_number) {
        this.prescription_number = prescription_number;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }



    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }


    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }


    public String getDispensing_notes() {
        return dispensing_notes;
    }

    public void setDispensing_notes(String dispensing_notes) {
        this.dispensing_notes = dispensing_notes;
    }

    public String getQuantity_dispensed() {
        return quantity_dispensed;

    }

    public void setQuantity_dispensed(String quantity_dispensed) {
        this.quantity_dispensed = quantity_dispensed;
    }

    public String getCoding_system() {
        return coding_system;
    }

    public void setCoding_system(String coding_system) {
        this.coding_system = coding_system;
    }

    public String getActual_drugs() {
        return actual_drugs;
    }

    public void setActual_drugs(String actual_drugs) {
        this.actual_drugs = actual_drugs;
    }

    public String getDrug_name() {
        return drug_name;
    }

    public void setDrug_name(String drug_name) {
        this.drug_name = drug_name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }


}
