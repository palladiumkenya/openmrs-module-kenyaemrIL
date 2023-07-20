package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;

public class SERVICE_REQUEST_SUPPORTING_INFO {
    private String appointment_date;
    private String drug_days;
    private String viral_load;
    private String last_vl_date;
    private String current_regimen;
    private String who_stage;
    private String height;
    private String weight;
    private String date_confirmed_positive;
    private String date_first_enrolled;
    private String entry_point;
    private String date_started_art_at_transferring_facility;

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

    public String getWho_stage() {
        return who_stage;
    }

    public void setWho_stage(String who_stage) {
        this.who_stage = who_stage;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getDate_confirmed_positive() {
        return date_confirmed_positive;
    }

    public void setDate_confirmed_positive(String date_confirmed_positive) {
        this.date_confirmed_positive = date_confirmed_positive;
    }

    public String getDate_started_art_at_transferring_facility() {
        return date_started_art_at_transferring_facility;
    }

    public void setDate_started_art_at_transferring_facility(String date_started_art_at_transferring_facility) {
        this.date_started_art_at_transferring_facility = date_started_art_at_transferring_facility;
    }

    public String getDate_first_enrolled() {
        return date_first_enrolled;
    }

    public void setDate_first_enrolled(String date_first_enrolled) {
        this.date_first_enrolled = date_first_enrolled;
    }

    public String getEntry_point() {
        return entry_point;
    }

    public void setEntry_point(String entry_point) {
        this.entry_point = entry_point;
    }
}
