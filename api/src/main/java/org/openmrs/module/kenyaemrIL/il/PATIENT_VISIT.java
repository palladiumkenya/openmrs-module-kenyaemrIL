package org.openmrs.module.kenyaemrIL.il;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class PATIENT_VISIT {
    private String visit_date;

    private String patient_source;

    private String hiv_care_enrollment_date;

    private String patient_type;

    public String getVisit_date() {
        return visit_date;
    }

    public void setVisit_date(String visit_date) {
        this.visit_date = visit_date;
    }

    public String getPatient_source() {
        return patient_source;
    }

    public void setPatient_source(String patient_source) {
        this.patient_source = patient_source;
    }

    public String getHiv_care_enrollment_date() {
        return hiv_care_enrollment_date;
    }

    public void setHiv_care_enrollment_date(String hiv_care_enrollment_date) {
        this.hiv_care_enrollment_date = hiv_care_enrollment_date;
    }

    public String getPatient_type() {
        return patient_type;
    }

    public void setPatient_type(String patient_type) {
        this.patient_type = patient_type;
    }


    public static PATIENT_VISIT fill(JsonObject jsonobj){
        PATIENT_VISIT entity = new PATIENT_VISIT();
        if (jsonobj.containsKey("VISIT_DATE")) {
            entity.setVisit_date(jsonobj.getString("VISIT_DATE"));
        }
        if (jsonobj.containsKey("PATIENT_TYPE")) {
            entity.setPatient_type(jsonobj.getString("PATIENT_TYPE"));
        }
        if (jsonobj.containsKey("PATIENT_SOURCE")) {
            entity.setPatient_source(jsonobj.getString("PATIENT_SOURCE"));
        }
        if (jsonobj.containsKey("HIV_CARE_ENROLLMENT_DATE")) {
            entity.setHiv_care_enrollment_date(jsonobj.getString("HIV_CARE_ENROLLMENT_DATE"));
        }
        return entity;
    }
    public static List<PATIENT_VISIT> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PATIENT_VISIT> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }
}
