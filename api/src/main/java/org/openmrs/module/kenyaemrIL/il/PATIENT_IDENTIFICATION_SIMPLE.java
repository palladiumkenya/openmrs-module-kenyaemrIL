package org.openmrs.module.kenyaemrIL.il;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PATIENT_IDENTIFICATION_SIMPLE {
    private EXTERNAL_PATIENT_ID external_patient_id;
    private List<INTERNAL_PATIENT_ID> internal_patient_id;
    private PATIENT_NAME patient_name;


    public PATIENT_IDENTIFICATION_SIMPLE( ) {
        this.external_patient_id = new EXTERNAL_PATIENT_ID();
        this.internal_patient_id = new ArrayList<>();
        this.patient_name = new PATIENT_NAME();
    }

    public EXTERNAL_PATIENT_ID getExternal_patient_id() {
        return external_patient_id;
    }

    public void setExternal_patient_id(EXTERNAL_PATIENT_ID external_patient_id) {
        this.external_patient_id = external_patient_id;
    }

    public List<INTERNAL_PATIENT_ID> getInternal_patient_id() {
        return internal_patient_id;
    }

    public void setInternal_patient_id(List<INTERNAL_PATIENT_ID> internal_patient_id) {
        this.internal_patient_id = internal_patient_id;
    }

    public PATIENT_NAME getPatient_name() {
        return patient_name;
    }

    public void setPatient_name(PATIENT_NAME patient_name) {
        this.patient_name = patient_name;
    }


    public static PATIENT_IDENTIFICATION_SIMPLE fill(JsonObject jsonobj) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        PATIENT_IDENTIFICATION_SIMPLE entity = new PATIENT_IDENTIFICATION_SIMPLE();
        if (jsonobj.containsKey("EXTERNAL_PATIENT_ID")) {
            EXTERNAL_PATIENT_ID external_patient_id = objectMapper.readValue(jsonobj.getJsonObject("EXTERNAL_PATIENT_ID").toString(), EXTERNAL_PATIENT_ID.class);
            entity.setExternal_patient_id(external_patient_id);
        }
        if (jsonobj.containsKey("INTERNAL_PATIENT_ID")) {
            JsonArray internal_patient_id = jsonobj.getJsonArray("INTERNAL_PATIENT_ID");
            TypeToken<List<INTERNAL_PATIENT_ID>> token = new TypeToken<List<INTERNAL_PATIENT_ID>>() {
            };
            Gson gson = new Gson();
            List<INTERNAL_PATIENT_ID> list = gson.fromJson(internal_patient_id.toString(), token.getType());
//            List<INTERNAL_PATIENT_ID> list = objectMapper.readValue(internal_patient_id, TypeFactory.collectionType(List.class, INTERNAL_PATIENT_ID.class));
            entity.setInternal_patient_id(list);
        }
        if (jsonobj.containsKey("PATIENT_NAME")) {
            PATIENT_NAME patient_name = objectMapper.readValue(jsonobj.getJsonObject("PATIENT_NAME").toString(), PATIENT_NAME.class);
            entity.setPatient_name(patient_name);
        }

        return entity;
    }

    public static List<PATIENT_IDENTIFICATION_SIMPLE> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PATIENT_IDENTIFICATION_SIMPLE> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            try {
                olist.add(fill(jsonarray.getJsonObject(i)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return olist;
    }

    @Override
    public String toString() {
        return "PATIENT_IDENTIFICATION{" +
                "external_patient_id=" + external_patient_id +
                ", internal_patient_id=" + internal_patient_id +
                ", patient_name=" + patient_name +
                '}';
    }
}