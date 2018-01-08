package org.openmrs.module.kenyaemrIL.il;

import org.openmrs.BaseOpenmrsMetadata;

import java.io.Serializable;

/**
 * Created by mstan on 08/01/2018.
 */
public class ILTest extends BaseOpenmrsMetadata implements Serializable {
    private static final long serialVersionUID = 3062136520728193223L;
    private Integer iltestid;
    private int messageType;

    public ILTest() {
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public ILTest(Integer iltestid) {
        this.iltestid = iltestid;
    }


    public Integer getIltestid() {
        return iltestid;
    }

    public void setIltestid(Integer iltestid) {
        this.iltestid = iltestid;
    }

    public Integer getId() {
        return this.getIltestid();
    }

    public void setId(Integer id) {
        this.setIltestid(id);
    }
}
