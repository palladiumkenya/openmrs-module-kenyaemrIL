package org.openmrs.module.kenyaemrIL.api;

/**
 * Created by mstan on 08/01/2018.
 */
public enum ILMessageType {
    INBOUND(1),
    OUTBOUND(2);


    private int value;

    ILMessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
