package org.openmrs.module.kenyaemrIL.il;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Stanslaus Odhiambo
 *         Created on 08/01/2018.
 */
public class EXTERNAL_PATIENT_ID
{
    private String id;
    private String identifier_type;
    private String assigning_authority;

    public EXTERNAL_PATIENT_ID() {
        id="";
        identifier_type="";
        assigning_authority="";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier_type() {
        return identifier_type;
    }

    public void setIdentifier_type(String identifier_type) {
        this.identifier_type = identifier_type;
    }

    public String getAssigning_authority() {
        return assigning_authority;
    }

    public void setAssigning_authority(String assigning_authority) {
        this.assigning_authority = assigning_authority;
    }

    public static EXTERNAL_PATIENT_ID fill(JsonObject jsonobj){
        EXTERNAL_PATIENT_ID entity = new EXTERNAL_PATIENT_ID();
        if (jsonobj.containsKey("ID")) {
            entity.setId(jsonobj.getString("ID"));
        }
        if (jsonobj.containsKey("IDENTIFIER_TYPE")) {
            entity.setIdentifier_type(jsonobj.getString("IDENTIFIER_TYPE"));
        }
        if (jsonobj.containsKey("ASSIGNING_AUTHORITY")) {
            entity.setAssigning_authority(jsonobj.getString("ASSIGNING_AUTHORITY"));
        }
        return entity;
    }
    public static List<EXTERNAL_PATIENT_ID> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<EXTERNAL_PATIENT_ID> olist = new ArrayList<EXTERNAL_PATIENT_ID>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "EXTERNAL_PATIENT_ID{" +
                "id='" + id + '\'' +
                ", identifier_type='" + identifier_type + '\'' +
                ", assigning_authority='" + assigning_authority + '\'' +
                '}';
    }
}