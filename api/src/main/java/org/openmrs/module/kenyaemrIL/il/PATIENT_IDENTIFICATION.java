package org.openmrs.module.kenyaemrIL.il;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PATIENT_IDENTIFICATION {
    private EXTERNAL_PATIENT_ID external_patient_id;
    private List<INTERNAL_PATIENT_ID> internal_patient_id;
    private PATIENT_NAME patient_name;
    private MOTHER_NAME mother_name;
    private String date_of_birth;
    private String sex;
    private PATIENT_ADDRESS patient_address;
    private String phone_number;
    private String marital_status;
    private String death_date;
    private String death_indicator;
    private String date_of_birth_precision;

    public PATIENT_IDENTIFICATION( ) {
        this.external_patient_id = new EXTERNAL_PATIENT_ID();
        this.internal_patient_id = new ArrayList<>();
        this.patient_name = new PATIENT_NAME();
        this.mother_name = new MOTHER_NAME();
        this.date_of_birth = "";
        this.sex = "";
        this.phone_number = "";
        this.marital_status = "";
        this.death_date = "";
        this.death_indicator = "";
        this.date_of_birth_precision = "";
        this.patient_address = new PATIENT_ADDRESS();
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

    public MOTHER_NAME getMother_name() {
        return mother_name;
    }

    public void setMother_name(MOTHER_NAME mother_name) {
        this.mother_name = mother_name;
    }

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public PATIENT_ADDRESS getPatient_address() {
        return patient_address;
    }

    public void setPatient_address(PATIENT_ADDRESS patient_address) {
        this.patient_address = patient_address;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getMarital_status() {
        return marital_status;
    }

    public void setMarital_status(String marital_status) {
        this.marital_status = marital_status;
    }

    public String getDeath_date() {
        return death_date;
    }

    public void setDeath_date(String death_date) {
        this.death_date = death_date;
    }

    public String getDeath_indicator() {
        return death_indicator;
    }

    public void setDeath_indicator(String death_indicator) {
        this.death_indicator = death_indicator;
    }

    public String getDate_of_birth_precision() {
        return date_of_birth_precision;
    }

    public void setDate_of_birth_precision(String date_of_birth_precision) {
        this.date_of_birth_precision = date_of_birth_precision;
    }

    public static PATIENT_IDENTIFICATION fill(JsonObject jsonobj) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        PATIENT_IDENTIFICATION entity = new PATIENT_IDENTIFICATION();
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
        if (jsonobj.containsKey("MOTHER_NAME")) {
            MOTHER_NAME mother_name = objectMapper.readValue(jsonobj.getJsonObject("MOTHER_NAME").toString(), MOTHER_NAME.class);
            entity.setMother_name(mother_name);
        }
        if (jsonobj.containsKey("DATE_OF_BIRTH")) {
            entity.setDate_of_birth(jsonobj.getString("DATE_OF_BIRTH"));
        }
        if (jsonobj.containsKey("SEX")) {
            entity.setSex(jsonobj.getString("SEX"));
        }
        if (jsonobj.containsKey("PATIENT_ADDRESS")) {
            PATIENT_ADDRESS patient_address = objectMapper.readValue(jsonobj.getJsonObject("PATIENT_ADDRESS").toString(), PATIENT_ADDRESS.class);
            entity.setPatient_address(patient_address);
        }
        if (jsonobj.containsKey("PHONE_NUMBER")) {
            entity.setPhone_number(jsonobj.getString("PHONE_NUMBER"));
        }
        if (jsonobj.containsKey("MARITAL_STATUS")) {
            entity.setMarital_status(jsonobj.getString("MARITAL_STATUS"));
        }
        if (jsonobj.containsKey("DEATH_DATE")) {
            entity.setDeath_date(jsonobj.getString("DEATH_DATE"));
        }
        if (jsonobj.containsKey("DEATH_INDICATOR")) {
            entity.setDeath_indicator(jsonobj.getString("DEATH_INDICATOR"));
        }
        if (jsonobj.containsKey("DATE_OF_BIRTH_PRECISION")) {
            entity.setDate_of_birth_precision(jsonobj.getString("DATE_OF_BIRTH_PRECISION"));
        }
        return entity;
    }

    public static List<PATIENT_IDENTIFICATION> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PATIENT_IDENTIFICATION> olist = new ArrayList<>();
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
                ", mother_name='" + mother_name + '\'' +
                ", date_of_birth='" + date_of_birth + '\'' +
                ", sex='" + sex + '\'' +
                ", patient_address=" + patient_address +
                ", phone_number='" + phone_number + '\'' +
                ", marital_status='" + marital_status + '\'' +
                ", death_date='" + death_date + '\'' +
                ", death_indicator='" + death_indicator + '\'' +
                '}';
    }
}