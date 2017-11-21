package org.openmrs.module.kenyaemrIL.il;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ObservationResult {
    private String observationIdentifier;
    private String codingSystem;
    private String valueType;
    private String observationValue;
    private String units;
    private String observationResultStatus;
    private String observationDatetime;
    private String abnormalFlags;

    public String getObservationIdentifier() {
        return observationIdentifier;
    }

    public void setObservationIdentifier(String observationIdentifier) {
        this.observationIdentifier = observationIdentifier;
    }

    public String getCodingSystem() {
        return codingSystem;
    }

    public void setCodingSystem(String codingSystem) {
        this.codingSystem = codingSystem;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getObservationValue() {
        return observationValue;
    }

    public void setObservationValue(String observationValue) {
        this.observationValue = observationValue;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getObservationResultStatus() {
        return observationResultStatus;
    }

    public void setObservationResultStatus(String observationResultStatus) {
        this.observationResultStatus = observationResultStatus;
    }

    public String getObservationDatetime() {
        return observationDatetime;
    }

    public void setObservationDatetime(String observationDatetime) {
        this.observationDatetime = observationDatetime;
    }

    public String getAbnormalFlags() {
        return abnormalFlags;
    }

    public void setAbnormalFlags(String abnormalFlags) {
        this.abnormalFlags = abnormalFlags;
    }
}
