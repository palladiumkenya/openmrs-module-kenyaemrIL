package org.openmrs.module.kenyaemrIL.il;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.ServiceRequest;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NEXT_OF_KIN {
    private NOK_NAME nok_name;
    private String relationship;
    private String address;
    private String phone_number;
    private String sex;
    private String date_of_birth;
    private String contact_role;

    public NEXT_OF_KIN() {
        this.nok_name = new NOK_NAME();
        this.relationship = "";
        this.address = "";
        this.phone_number = "";
        this.sex = "";
        this.date_of_birth = "";
        this.contact_role = "";
    }

    public NOK_NAME getNok_name() {
        return nok_name;
    }

    public void setNok_name(NOK_NAME nok_name) {
        this.nok_name = nok_name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getDate_of_birth() {
        return date_of_birth;
    }

    public void setDate_of_birth(String date_of_birth) {
        this.date_of_birth = date_of_birth;
    }

    public String getContact_role() {
        return contact_role;
    }

    public void setContact_role(String contact_role) {
        this.contact_role = contact_role;
    }

    public static NEXT_OF_KIN fill(JsonObject jsonobj) throws IOException {
        NEXT_OF_KIN entity = new NEXT_OF_KIN();
        if (jsonobj.containsKey("NOK_NAME")) {
            ObjectMapper objectMapper = new ObjectMapper();
            NOK_NAME nok_name = objectMapper.readValue(jsonobj.getJsonObject("NOK_NAME").toString(), NOK_NAME.class);
            entity.setNok_name(nok_name);
        }
        if (jsonobj.containsKey("RELATIONSHIP")) {
            entity.setRelationship(jsonobj.getString("RELATIONSHIP"));
        }
        if (jsonobj.containsKey("ADDRESS")) {
            entity.setAddress(jsonobj.getString("ADDRESS"));
        }
        if (jsonobj.containsKey("PHONE_NUMBER")) {
            entity.setPhone_number(jsonobj.getString("PHONE_NUMBER"));
        }
        if (jsonobj.containsKey("SEX")) {
            entity.setSex(jsonobj.getString("SEX"));
        }
        if (jsonobj.containsKey("DATE_OF_BIRTH")) {
            entity.setDate_of_birth(jsonobj.getString("DATE_OF_BIRTH"));
        }
        if (jsonobj.containsKey("CONTACT_ROLE")) {
            entity.setContact_role(jsonobj.getString("CONTACT_ROLE"));
        }
        return entity;
    }

    public static List<NEXT_OF_KIN> fillList(JsonArray jsonarray) throws IOException {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<NEXT_OF_KIN> olist = new ArrayList<NEXT_OF_KIN>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "NEXT_OF_KIN{" +
                "nok_name=" + nok_name +
                ", relationship='" + relationship + '\'' +
                ", address='" + address + '\'' +
                ", phone_number='" + phone_number + '\'' +
                ", sex='" + sex + '\'' +
                ", date_of_birth='" + date_of_birth + '\'' +
                ", contact_role='" + contact_role + '\'' +
                '}';
    }
}