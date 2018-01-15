package org.openmrs.module.kenyaemrIL.il.appointment;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class APPOINTMENT_INFORMATION {
    private String APPOINTMENT_REASON;
    private String ACTION_CODE;
    private String APPOINTMENT_PLACING_ENTITY;
    private String APPOINTMENT_LOCATION;
    private String APPOINTMENT_STATUS;
    private String APPOINTMENT_TYPE;
    private String APPOINTMENT_NOTE;
    private String APPOINTMENT_DATE;
    private PLACER_APPOINTMENT_NUMBER PLACER_APPOINTMENT_NUMBER;

    public String getAPPOINTMENT_REASON() {
        return APPOINTMENT_REASON;
    }

    public void setAPPOINTMENT_REASON(String APPOINTMENT_REASON) {
        this.APPOINTMENT_REASON = APPOINTMENT_REASON;
    }

    public String getACTION_CODE() {
        return ACTION_CODE;
    }

    public void setACTION_CODE(String ACTION_CODE) {
        this.ACTION_CODE = ACTION_CODE;
    }

    public String getAPPOINTMENT_PLACING_ENTITY() {
        return APPOINTMENT_PLACING_ENTITY;
    }

    public void setAPPOINTMENT_PLACING_ENTITY(String APPOINTMENT_PLACING_ENTITY) {
        this.APPOINTMENT_PLACING_ENTITY = APPOINTMENT_PLACING_ENTITY;
    }

    public String getAPPOINTMENT_LOCATION() {
        return APPOINTMENT_LOCATION;
    }

    public void setAPPOINTMENT_LOCATION(String APPOINTMENT_LOCATION) {
        this.APPOINTMENT_LOCATION = APPOINTMENT_LOCATION;
    }

    public String getAPPOINTMENT_STATUS() {
        return APPOINTMENT_STATUS;
    }

    public void setAPPOINTMENT_STATUS(String APPOINTMENT_STATUS) {
        this.APPOINTMENT_STATUS = APPOINTMENT_STATUS;
    }

    public String getAPPOINTMENT_TYPE() {
        return APPOINTMENT_TYPE;
    }

    public void setAPPOINTMENT_TYPE(String APPOINTMENT_TYPE) {
        this.APPOINTMENT_TYPE = APPOINTMENT_TYPE;
    }

    public String getAPPOINTMENT_NOTE() {
        return APPOINTMENT_NOTE;
    }

    public void setAPPOINTMENT_NOTE(String APPOINTMENT_NOTE) {
        this.APPOINTMENT_NOTE = APPOINTMENT_NOTE;
    }

    public String getAPPOINTMENT_DATE() {
        return APPOINTMENT_DATE;
    }

    public void setAPPOINTMENT_DATE(String APPOINTMENT_DATE) {
        this.APPOINTMENT_DATE = APPOINTMENT_DATE;
    }

    public PLACER_APPOINTMENT_NUMBER getPLACER_APPOINTMENT_NUMBER() {
        return PLACER_APPOINTMENT_NUMBER;
    }

    public void setPLACER_APPOINTMENT_NUMBER(PLACER_APPOINTMENT_NUMBER PLACER_APPOINTMENT_NUMBER) {
        this.PLACER_APPOINTMENT_NUMBER = PLACER_APPOINTMENT_NUMBER;
    }

    @Override
    public String toString() {
        return "ClassPojo [APPOINTMENT_REASON = " + APPOINTMENT_REASON + ", ACTION_CODE = " + ACTION_CODE + ", APPOINTMENT_PLACING_ENTITY = " + APPOINTMENT_PLACING_ENTITY + ", APPOINTMENT_LOCATION = " + APPOINTMENT_LOCATION + ", APPOINTMENT_STATUS = " + APPOINTMENT_STATUS + ", APPOINTMENT_TYPE = " + APPOINTMENT_TYPE + ", APPOINTMENT_NOTE = " + APPOINTMENT_NOTE + ", APPOINTMENT_DATE = " + APPOINTMENT_DATE + ", PLACER_APPOINTMENT_NUMBER = " + PLACER_APPOINTMENT_NUMBER + "]";
    }
}

