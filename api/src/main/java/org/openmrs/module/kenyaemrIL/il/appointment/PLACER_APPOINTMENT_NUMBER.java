package org.openmrs.module.kenyaemrIL.il.appointment;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class PLACER_APPOINTMENT_NUMBER {
    private String entity;
    private String number;

    public PLACER_APPOINTMENT_NUMBER(){
        this.entity = "KENYAEMR";
        this.number = ""; //TODO: should be set to the encounter id
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}