package org.openmrs.module.kenyaemrIL.il.observation;

/**
 * @author Stanslaus Odhiambo
 *         Created on 08/01/2018.
 */
public class OBSERVATION_RESULT {
    private String units;
    private String value_type;
    private String observation_value;
    private String observation_datetime;
    private String coding_system;
    private String abnormal_flags;
    private String observation_result_status;
    private String set_id;
    private String observation_identifier;

    public String getSet_id() {
        return set_id;
    }
    public void setSet_id(String set_id) { this.set_id = set_id;
    }
    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getValue_type() {
        return value_type;
    }

    public void setValue_type(String value_type) {
        this.value_type = value_type;
    }

    public String getObservation_value() {
        return observation_value;
    }

    public void setObservation_value(String observation_value) {
        this.observation_value = observation_value;
    }

    public String getObservation_datetime() {
        return observation_datetime;
    }

    public void setObservation_datetime(String observation_datetime) {
        this.observation_datetime = observation_datetime;
    }

    public String getCoding_system() {
        return coding_system;
    }

    public void setCoding_system(String coding_system) {
        this.coding_system = coding_system;
    }

    public String getAbnormal_flags() {
        return abnormal_flags;
    }

    public void setAbnormal_flags(String abnormal_flags) {
        this.abnormal_flags = abnormal_flags;
    }

    public String getObservation_result_status() {
        return observation_result_status;
    }

    public void setObservation_result_status(String observation_result_status) {
        this.observation_result_status = observation_result_status;
    }

    public String getObservation_identifier() {
        return observation_identifier;
    }

    public void setObservation_identifier(String observation_identifier) {
        this.observation_identifier = observation_identifier;
    }

//    public static OBSERVATION_RESULT fill(JsonObject jsonobj) throws IOException {
//        OBSERVATION_RESULT entity = new OBSERVATION_RESULT();
//
//        if (jsonobj.containsKey("SET_ID")) {
//            entity.setSet_id(jsonobj.getString("SET_ID"));
//        }
//        if (jsonobj.containsKey("OBSERVATION_IDENTIFIER")) {
//            entity.setObservation_identifier(jsonobj.getString("OBSERVATION_IDENTIFIER"));
//        }
//        if (jsonobj.containsKey("CODING_SYSTEM")) {
//            entity.setCoding_system(jsonobj.getString("CODING_SYSTEM"));
//        }
//        if (jsonobj.containsKey("VALUE_TYPE")) {
//            entity.setValue_type(jsonobj.getString("VALUE_TYPE"));
//        }
//        if (jsonobj.containsKey("OBSERVATION_VALUE")) {
//            entity.setObservation_value(jsonobj.getString("OBSERVATION_VALUE"));
//        }
//        if (jsonobj.containsKey("UNITS")) {
//            entity.setUnits(jsonobj.getString("UNITS"));
//        }
//        if (jsonobj.containsKey("OBSERVATION_RESULT_STATUS")) {
//            entity.setObservation_result_status(jsonobj.getString("OBSERVATION_RESULT_STATUS"));
//        }
//        if (jsonobj.containsKey("OBSERVATION_DATETIME")) {
//            entity.setObservation_datetime(jsonobj.getString("OBSERVATION_DATETIME"));
//        }
//        if (jsonobj.containsKey("ABNORMAL_FLAGS")) {
//            entity.setAbnormal_flags(jsonobj.getString("ABNORMAL_FLAGS"));
//        }
//        return entity;
//    }
//
//    public static List<OBSERVATION_RESULT> fillList(JsonArray jsonarray) throws IOException {
//        if (jsonarray == null || jsonarray.size() == 0)
//            return null;
//        List<OBSERVATION_RESULT> olist = new ArrayList<OBSERVATION_RESULT>();
//        for (int i = 0; i < jsonarray.size(); i++) {
//            olist.add(fill(jsonarray.getJsonObject(i)));
//        }
//        return olist;
//    }
//
//    @Override
//    public String toString() {
//        return "OBSERVATION_RESULT{" +
//                "units=" + units +
//                ", value_type='" + value_type + '\'' +
//                ", observation_value='" + observation_value + '\'' +
//                ", observation_datetime='" + observation_datetime + '\'' +
//                ", coding_system='" + coding_system + '\'' +
//                ", abnormal_flags='" + abnormal_flags + '\'' +
//                ", observation_result_status='" + observation_result_status + '\'' +
//                ", set_id='" + set_id + '\'' +
//                ", observation_identifier='" + observation_identifier + '\'' +
//                '}';
//    }

}