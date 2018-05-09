package org.openmrs.module.kenyaemrIL.il;

import org.openmrs.BaseOpenmrsMetadata;

import java.io.Serializable;

/**
 * @author Stanslaus Odhiambo
 *         Created on 08/01/2018.
 */
public class KenyaEMRILMessage extends BaseOpenmrsMetadata implements Serializable {
    private static final long serialVersionUID = 3062136520728193223L;
    private Integer messageId;
    private Integer messageType;
    private String hl7Type;
    private String message;

    public KenyaEMRILMessage() {
    }

    public KenyaEMRILMessage(Integer messageId) {
        this.messageId = messageId;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getHl7Type() {
        return hl7Type;
    }

    public void setHl7Type(String hl7Type) {
        this.hl7Type = hl7Type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getId() {
        return this.getMessageId();
    }

    public void setId(Integer id) {
        this.setMessageId(id);
    }


}
