package org.openmrs.module.kenyaemrIL.il;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ILPerson {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private PATIENT_VISIT patient_visit;
    private NEXT_OF_KIN[] next_of_kin;

    public NEXT_OF_KIN[] getNext_of_kin() {
        return next_of_kin;
    }

    public void setNext_of_kin(NEXT_OF_KIN[] next_of_kin) {
        this.next_of_kin = next_of_kin;
    }

    public PATIENT_VISIT getPatient_visit() {
        return patient_visit;
    }

    public void setPatient_visit(PATIENT_VISIT patient_visit) {
        this.patient_visit = patient_visit;
    }

    public PATIENT_IDENTIFICATION getPatient_identification() {
        return patient_identification;
    }

    public void setPatient_identification(PATIENT_IDENTIFICATION patient_identification) {
        this.patient_identification = patient_identification;
    }

    public MESSAGE_HEADER getMessage_header() {
        return message_header;
    }

    public void setMessage_header(MESSAGE_HEADER message_header) {
        this.message_header = message_header;
    }

    public static ILPerson fill(JsonObject jsonobj) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ILPerson entity = new ILPerson();
        if (jsonobj.containsKey("PATIENT_VISIT")) {
            PATIENT_VISIT patient_visit = objectMapper.readValue(jsonobj.getJsonObject("PATIENT_VISIT").toString(), PATIENT_VISIT.class);
            entity.setPatient_visit(patient_visit);
        }

        if (jsonobj.containsKey("MESSAGE_HEADER")) {
            MESSAGE_HEADER message_header = objectMapper.readValue(jsonobj.getJsonObject("MESSAGE_HEADER").toString(), MESSAGE_HEADER.class);
            entity.setMessage_header(message_header);
        }
        if (jsonobj.containsKey("PATIENT_IDENTIFICATION")) {
            PATIENT_IDENTIFICATION patient_identification = objectMapper.readValue(jsonobj.getJsonObject("PATIENT_IDENTIFICATION").toString(), PATIENT_IDENTIFICATION.class);
            entity.setPatient_identification(patient_identification);
        }
        if (jsonobj.containsKey("NEXT_OF_KIN")) {
            JsonArray next_of_kin = jsonobj.getJsonArray("NEXT_OF_KIN");

            TypeToken<List<NEXT_OF_KIN>> token = new TypeToken<List<NEXT_OF_KIN>>() {
            };
            Gson gson = new Gson();
            List<NEXT_OF_KIN> list = gson.fromJson(next_of_kin.toString(), token.getType());
            entity.setNext_of_kin((NEXT_OF_KIN[]) list.toArray());
        }
        return entity;
    }

    public static List<ILPerson> fillList(JsonArray jsonarray) throws IOException {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<ILPerson> olist = new ArrayList<ILPerson>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "ILPerson{" +
                "message_header=" + message_header +
                ", patient_identification=" + patient_identification +
                ", next_of_kin=" + next_of_kin +
                '}';
    }
}
