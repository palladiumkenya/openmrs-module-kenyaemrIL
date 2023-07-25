package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;

public class PATIENT_NCD {
    private String illness;
    private String onsetDate;
    private String isControlled;

    public PATIENT_NCD(String illness, String onsetDate, String isControlled) {
        this.illness = illness;
        this.onsetDate = onsetDate;
        this.isControlled = isControlled;
    }

    public String getIllness() {
        return illness;
    }

    public void setIllness(String illness) {
        this.illness = illness;
    }

    public String getOnsetDate() {
        return onsetDate;
    }

    public void setOnsetDate(String onsetDate) {
        this.onsetDate = onsetDate;
    }

    public String getIsControlled() {
        return isControlled;
    }

    public void setIsControlled(String isControlled) {
        this.isControlled = isControlled;
    }
}
