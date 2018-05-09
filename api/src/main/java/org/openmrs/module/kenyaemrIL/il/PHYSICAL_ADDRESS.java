package org.openmrs.module.kenyaemrIL.il;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class PHYSICAL_ADDRESS {
    private String village;
    private String ward;
    private String sub_county;
    private String county;
    private String gps_location;
    private String nearest_landmark;

    public PHYSICAL_ADDRESS() {
        this.village = "";
        this.ward = "";
        this.sub_county = "";
        this.county = "";
        this.gps_location = "";
        this.nearest_landmark = "";
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getSub_county() {
        return sub_county;
    }

    public void setSub_county(String sub_county) {
        this.sub_county = sub_county;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getGps_location() {
        return gps_location;
    }

    public void setGps_location(String gps_location) {
        this.gps_location = gps_location;
    }

    public String getNearest_landmark() {
        return nearest_landmark;
    }

    public void setNearest_landmark(String nearest_landmark) {
        this.nearest_landmark = nearest_landmark;
    }

    public static PHYSICAL_ADDRESS fill(JsonObject jsonobj) {
        PHYSICAL_ADDRESS entity = new PHYSICAL_ADDRESS();
        if (jsonobj.containsKey("VILLAGE")) {
            entity.setVillage(jsonobj.getString("VILLAGE"));
        }
        if (jsonobj.containsKey("WARD")) {
            entity.setWard(jsonobj.getString("WARD"));
        }
        if (jsonobj.containsKey("SUB_COUNTY")) {
            entity.setSub_county(jsonobj.getString("SUB_COUNTY"));
        }
        if (jsonobj.containsKey("COUNTY")) {
            entity.setCounty(jsonobj.getString("COUNTY"));
        }
        if (jsonobj.containsKey("NEAREST_LANDMARK")) {
            entity.setNearest_landmark(jsonobj.getString("NEAREST_LANDMARK"));
        }
        if (jsonobj.containsKey("GPS_LOCATION")) {
            entity.setGps_location(jsonobj.getString("GPS_LOCATION"));
        }
        return entity;
    }

    public static List<PHYSICAL_ADDRESS> fillList(JsonArray jsonarray) {
        if (jsonarray == null || jsonarray.size() == 0)
            return null;
        List<PHYSICAL_ADDRESS> olist = new ArrayList<>();
        for (int i = 0; i < jsonarray.size(); i++) {
            olist.add(fill(jsonarray.getJsonObject(i)));
        }
        return olist;
    }

    @Override
    public String toString() {
        return "PHYSICAL_ADDRESS{" +
                "village='" + village + '\'' +
                ", ward='" + ward + '\'' +
                ", sub_county='" + sub_county + '\'' +
                ", county='" + county + '\'' +
                ", gps_location='" + gps_location + '\'' +
                ", nearest_landmark='" + nearest_landmark + '\'' +
                '}';
    }
}