package org.openmrs.module.kenyaemrIL.il;


import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class PATIENT_NAME
{
    private String first_name;
    private String middle_name;
    private String last_name;

    public PATIENT_NAME() {
        this.first_name = "";
        this.middle_name = "";
        this.last_name = "";
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getMiddle_name() {
        return middle_name;
    }

    public void setMiddle_name(String middle_name) {
        this.middle_name = middle_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public static PATIENT_NAME fill(JsonObject jsonobj){
        PATIENT_NAME entity = new PATIENT_NAME();
        if (jsonobj.containsKey("FIRST_NAME")) {
            entity.setFirst_name(jsonobj.getString("FIRST_NAME"));
        }
        if (jsonobj.containsKey("MIDDLE_NAME")) {
            entity.setMiddle_name(jsonobj.getString("MIDDLE_NAME"));
        }
        if (jsonobj.containsKey("LAST_NAME")) {
            entity.setLast_name(jsonobj.getString("LAST_NAME"));
        }
        return entity;
    }
    public static List<PATIENT_NAME> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PATIENT_NAME> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "PATIENT_NAME{" +
                "first_name='" + first_name + '\'' +
                ", middle_name='" + middle_name + '\'' +
                ", last_name='" + last_name + '\'' +
                '}';
    }
}