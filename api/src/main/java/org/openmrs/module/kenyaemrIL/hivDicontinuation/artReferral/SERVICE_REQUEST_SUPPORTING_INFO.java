package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;

import org.openmrs.ui.framework.SimpleObject;

import java.util.List;

public class SERVICE_REQUEST_SUPPORTING_INFO {
    private String appointment_date;
    private String drug_days;
    private String viral_load;
    private String last_vl_date;
    private String cd4_value;
    private String cd4_date;
    private String current_regimen;
    private String who_stage;
    private String height;
    private String weight;
    private String date_confirmed_positive;
    private String date_first_enrolled;
    private String entry_point;
    private String date_started_art_at_transferring_facility;
    private String tpt_start_date;
    private String tpt_end_date;
    private String tpt_end_reason;
    private String tb_start_date;
    private String tb_end_date;
    private String tb_end_reason;
    private String drug_allergies;
    private String other_allergies;
    private List<PATIENT_NCD> patient_ncds;
    private String arv_adherence_outcome;
    private List<SimpleObject> regimen_change_history;

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

    public void setCd4_value(String cd4_value) {
        this.cd4_value = cd4_value;
    }

    public void setCd4_date(String cd4_date) {
        this.cd4_date = cd4_date;
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

    public String getTpt_start_date() {
        return tpt_start_date;
    }

    public void setTpt_start_date(String tpt_start_date) {
        this.tpt_start_date = tpt_start_date;
    }

    public String getTpt_end_date() {
        return tpt_end_date;
    }

    public void setTpt_end_date(String tpt_end_date) {
        this.tpt_end_date = tpt_end_date;
    }

    public String getTpt_end_reason() {
        return tpt_end_reason;
    }

    public void setTpt_end_reason(String tpt_end_reason) {
        this.tpt_end_reason = tpt_end_reason;
    }

    public String getTb_start_date() {
        return tb_start_date;
    }

    public void setTb_start_date(String tb_start_date) {
        this.tb_start_date = tb_start_date;
    }

    public String getTb_end_date() {
        return tb_end_date;
    }

    public void setTb_end_date(String tb_end_date) {
        this.tb_end_date = tb_end_date;
    }

    public String getTb_end_reason() {
        return tb_end_reason;
    }

    public void setTb_end_reason(String tb_end_reason) {
        this.tb_end_reason = tb_end_reason;
    }

    public String getDrug_allergies() {
        return drug_allergies;
    }

    public void setDrug_allergies(String drug_allergies) {
        this.drug_allergies = drug_allergies;
    }

    public String getOther_allergies() {
        return other_allergies;
    }

    public void setOther_allergies(String other_allergies) {
        this.other_allergies = other_allergies;
    }

    public List<PATIENT_NCD> getPatient_ncds() {
        return patient_ncds;
    }

    public void setPatient_ncds(List<PATIENT_NCD> patient_ncds) {
        this.patient_ncds = patient_ncds;
    }

    public String getArv_adherence_outcome() {
        return arv_adherence_outcome;
    }

    public void setArv_adherence_outcome(String arv_adherence_outcome) {
        this.arv_adherence_outcome = arv_adherence_outcome;
    }

    public String getCd4_value() {
        return cd4_value;
    }

    public String getCd4_date() {
        return cd4_date;
    }

    public List<SimpleObject> getRegimen_change_history() {
        return regimen_change_history;
    }

    public void setRegimen_change_history(List<SimpleObject> regimen_change_history) {
        this.regimen_change_history = regimen_change_history;
    }
}
