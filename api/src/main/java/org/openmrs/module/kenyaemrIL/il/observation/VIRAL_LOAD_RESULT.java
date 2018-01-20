package org.openmrs.module.kenyaemrIL.il.observation;

public class VIRAL_LOAD_RESULT {
    private String sample_collection_datetime;
    private String sample_tested_datetime;
    private String vl_results;
    private String sample_type;
    private String sample_rejection;
    private String justification;
    private String regimen;
    private String lab_tested_in;

    public String getSample_collection_datetime() {
        return sample_collection_datetime;
    }

    public void setSample_collection_datetime(String sample_collection_datetime) {
        this.sample_collection_datetime = sample_collection_datetime;
    }
    public String getSample_tested_datetime() {
        return sample_tested_datetime;
    }

    public void setSample_tested_datetime(String sample_tested_datetime) {
        this.sample_tested_datetime = sample_tested_datetime;
    }

    public String getVl_results() {
        return vl_results;
    }

    public void setVl_results(String vl_results) {
        this.vl_results = vl_results;
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
