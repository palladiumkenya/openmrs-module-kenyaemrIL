package org.openmrs.module.kenyaemrIL.il.observation;

public class VIRAL_LOAD_RESULT {
    private String date_sample_collected;
    private String date_sample_tested;
    private String vl_result;
    private String sample_type;
    private String sample_rejection;
    private String justification;
    private String regimen;
    private String lab_tested_in;

    public String getDate_sample_collected() {
        return date_sample_collected;
    }

    public void setDate_sample_collected(String date_sample_collected) {
        this.date_sample_collected = date_sample_collected;
    }

    public String getDate_sample_tested() {
        return date_sample_tested;
    }

    public void setDate_sample_tested(String date_sample_tested) {
        this.date_sample_tested = date_sample_tested;
    }

    public String getVl_result() {
        return vl_result;
    }

    public void setVl_result(String vl_result) {
        this.vl_result = vl_result;
    }

    public String getSample_type() {
        return sample_type;
    }

    public void setSample_type(String sample_type) {
        this.sample_type = sample_type;
    }

    public String getSample_rejection() {
        return sample_rejection;
    }

    public void setSample_rejection(String sample_rejection) {
        this.sample_rejection = sample_rejection;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getRegimen() {
        return regimen;
    }

    public void setRegimen(String regimen) {
        this.regimen = regimen;
    }

    public String getLab_tested_in() {
        return lab_tested_in;
    }

    public void setLab_tested_in(String lab_tested_in) {
        this.lab_tested_in = lab_tested_in;
    }
}
