package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;

public class PATIENT_NCD {
    private String illness;
    private String onset_date;
    private String is_controlled;

    public PATIENT_NCD() {

    }

    public PATIENT_NCD(String illness, String onset_date, String is_controlled) {
        this.illness = illness;
        this.onset_date = onset_date;
        this.is_controlled = is_controlled;
    }

    public String getIllness() {
        return illness;
    }

    public void setIllness(String illness) {
        this.illness = illness;
    }

    public String getOnset_date() {
        return onset_date;
    }

    public void setOnset_date(String onset_date) {
        this.onset_date = onset_date;
    }

    public String getIs_controlled() {
        return is_controlled;
    }

    public void setIs_controlled(String is_controlled) {
        this.is_controlled = is_controlled;
    }
}
