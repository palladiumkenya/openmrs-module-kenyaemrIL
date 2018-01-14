package org.openmrs.module.kenyaemrIL.api;

/**
 * @author Stanslaus Odhiambo
 *         Created on 13/01/2018.
 */
public enum HL7MessageType {
    ADT_A04("ADT^A04"),
    ADT_A08("ADT^A08");


    private String type;

    HL7MessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
