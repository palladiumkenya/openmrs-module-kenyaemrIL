package org.openmrs.module.kenyaemrIL.il;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class MESSAGE_HEADER
{
    private String sending_application;
    private String sending_facility;
    private String receiving_application;
    private String receiving_facility;
    private String message_datetime;
    private String security;
    private String message_type;
    private String processing_id;

    public String getSending_application() {
        return sending_application;
    }

    public void setSending_application(String sending_application) {
        this.sending_application = sending_application;
    }

    public String getSending_facility() {
        return sending_facility;
    }

    public void setSending_facility(String sending_facility) {
        this.sending_facility = sending_facility;
    }

    public String getReceiving_application() {
        return receiving_application;
    }

    public void setReceiving_application(String receiving_application) {
        this.receiving_application = receiving_application;
    }

    public String getReceiving_facility() {
        return receiving_facility;
    }

    public void setReceiving_facility(String receiving_facility) {
        this.receiving_facility = receiving_facility;
    }

    public String getMessage_datetime() {
        return message_datetime;
    }

    public void setMessage_datetime(String message_datetime) {
        this.message_datetime = message_datetime;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getProcessing_id() {
        return processing_id;
    }

    public void setProcessing_id(String processing_id) {
        this.processing_id = processing_id;
    }

    public static MESSAGE_HEADER fill(JsonObject jsonobj){
        MESSAGE_HEADER entity = new MESSAGE_HEADER();
        if (jsonobj.containsKey("SENDING_APPLICATION")) {
            entity.setSending_application(jsonobj.getString("SENDING_APPLICATION"));
        }
        if (jsonobj.containsKey("SENDING_FACILITY")) {
            entity.setSending_facility(jsonobj.getString("SENDING_FACILITY"));
        }
        if (jsonobj.containsKey("RECEIVING_APPLICATION")) {
            entity.setReceiving_application(jsonobj.getString("RECEIVING_APPLICATION"));
        }
        if (jsonobj.containsKey("RECEIVING_FACILITY")) {
            entity.setReceiving_facility(jsonobj.getString("RECEIVING_FACILITY"));
        }
        if (jsonobj.containsKey("MESSAGE_DATETIME")) {
            entity.setMessage_datetime(jsonobj.getString("MESSAGE_DATETIME"));
        }
        if (jsonobj.containsKey("SECURITY")) {
            entity.setSecurity(jsonobj.getString("SECURITY"));
        }
        if (jsonobj.containsKey("MESSAGE_TYPE")) {
            entity.setMessage_type(jsonobj.getString("MESSAGE_TYPE"));
        }
        if (jsonobj.containsKey("PROCESSING_ID")) {
            entity.setProcessing_id(jsonobj.getString("PROCESSING_ID"));
        }
        return entity;
    }
    public static List<MESSAGE_HEADER> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<MESSAGE_HEADER> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "MESSAGE_HEADER{" +
                "sending_application='" + sending_application + '\'' +
                ", sending_facility='" + sending_facility + '\'' +
                ", receiving_application='" + receiving_application + '\'' +
                ", receiving_facility='" + receiving_facility + '\'' +
                ", message_datetime='" + message_datetime + '\'' +
                ", security='" + security + '\'' +
                ", message_type='" + message_type + '\'' +
                ", processing_id='" + processing_id + '\'' +
                '}';
    }
}