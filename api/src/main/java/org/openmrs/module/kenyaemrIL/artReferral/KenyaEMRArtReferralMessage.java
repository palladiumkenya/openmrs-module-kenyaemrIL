package org.openmrs.module.kenyaemrIL.artReferral;

import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.Patient;

import java.io.Serializable;

public class KenyaEMRArtReferralMessage extends BaseOpenmrsMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer message_id;
    private Integer message_type;
    private String hl7_type;
    private String status;
    private String source;
    private String message;
    private Patient patient;

    public Integer getMessage_id() {
        return message_id;
    }

    public void setMessage_id(Integer message_id) {
        this.message_id = message_id;
    }

    public Integer getMessage_type() {
        return message_type;
    }

    public void setMessage_type(Integer message_type) {
        this.message_type = message_type;
    }

    public String getHl7_type() {
        return hl7_type;
    }

    public void setHl7_type(String hl7_type) {
        this.hl7_type = hl7_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public Integer getId() {
        return getMessage_id();
    }

    @Override
    public void setId(Integer integer) {
        setMessage_id(integer);
    }
}
