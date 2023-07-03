package org.openmrs.module.kenyaemrIL.il;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mstan on 02/10/2017.
 */
public class PATIENT_ADDRESS {
    private PHYSICAL_ADDRESS physical_address;
    private String postal_address;

    public PATIENT_ADDRESS() {
        this.physical_address = new PHYSICAL_ADDRESS();
        this.postal_address = "";
    }

    public PHYSICAL_ADDRESS getPhysical_address() {
        return physical_address;
    }

    public void setPhysical_address(PHYSICAL_ADDRESS physical_address) {
        this.physical_address = physical_address;
    }

    public String getPostal_address() {
        return postal_address;
    }

    public void setPostal_address(String postal_address) {
        this.postal_address = postal_address;
    }

    public static PATIENT_ADDRESS fill(JsonObject jsonobj){
        PATIENT_ADDRESS entity = new PATIENT_ADDRESS();
        if (jsonobj.containsKey("PHYSICAL_ADDRESS")) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                PHYSICAL_ADDRESS physical_address = objectMapper.readValue(jsonobj.getJsonObject("PHYSICAL_ADDRESS").toString(), PHYSICAL_ADDRESS.class);
                entity.setPhysical_address(physical_address);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (jsonobj.containsKey("POSTAL_ADDRESS")) {
            entity.setPostal_address(jsonobj.getString("POSTAL_ADDRESS"));
        }
        return entity;
    }
    public static List<PATIENT_ADDRESS> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PATIENT_ADDRESS> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "PATIENT_ADDRESS{" +
                "physical_address=" + physical_address +
                ", postal_address='" + postal_address + '\'' +
                '}';
    }
}
