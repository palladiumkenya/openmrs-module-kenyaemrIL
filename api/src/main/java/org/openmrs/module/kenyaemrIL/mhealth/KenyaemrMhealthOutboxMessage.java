package org.openmrs.module.kenyaemrIL.mhealth;

import org.openmrs.BaseOpenmrsMetadata;

import java.io.Serializable;

/**
 * model for mhealth message outbox sent directly to the mhealth remote servers
 */
public class KenyaemrMhealthOutboxMessage extends BaseOpenmrsMetadata implements Serializable {
    private static final long serialVersionUID = 3062136520728193225L;
    private Integer message_id;
    private Integer message_type;
    private String hl7_type;
    private String status;
    private String source;
    private String message;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public KenyaemrMhealthOutboxMessage() {
    }

    public KenyaemrMhealthOutboxMessage(Integer messageId) {
        this.message_id = messageId;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getId() {
        return this.getMessage_id();
    }

    public void setId(Integer id) {
        this.setMessage_id(id);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
