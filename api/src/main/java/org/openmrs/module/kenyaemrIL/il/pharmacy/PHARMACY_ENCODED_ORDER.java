package org.openmrs.module.kenyaemrIL.il.pharmacy;
/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PHARMACY_ENCODED_ORDER {
    private String frequency;
    private String prescription_notes;
    private String dosage;
    private String coding_system;
    private String quantity_prescribed;
    private String strength;
    private String duration;
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

    public String getPrescription_notes() {
        return prescription_notes;
    }

    public void setPrescription_notes(String prescription_notes) {
        this.prescription_notes = prescription_notes;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getCoding_system() {
        return coding_system;
    }

    public void setCoding_system(String coding_system) {
        this.coding_system = coding_system;
    }

    public String getQuantity_prescribed() {
        return quantity_prescribed;
    }

    public void setQuantity_prescribed(String quantity_prescribed) {
        this.quantity_prescribed = quantity_prescribed;
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

    public String getDrug_name() {
        return drug_name;
    }

    public void setDrug_name(String drug_name) {
        this.drug_name = drug_name;
    }
}
