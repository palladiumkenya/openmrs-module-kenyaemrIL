package org.openmrs.module.kenyaemrIL.il.appointment;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class PLACER_APPOINTMENT_NUMBER {
    private String ENTITY;
    private String NUMBER;

    public String getENTITY() {
        return ENTITY;
    }

    public void setENTITY(String ENTITY) {
        this.ENTITY = ENTITY;
    }

    public String getNUMBER() {
        return NUMBER;
    }

    public void setNUMBER(String NUMBER) {
        this.NUMBER = NUMBER;
    }

    @Override
    public String toString() {
        return "ClassPojo [ENTITY = " + ENTITY + ", NUMBER = " + NUMBER + "]";
    }
}