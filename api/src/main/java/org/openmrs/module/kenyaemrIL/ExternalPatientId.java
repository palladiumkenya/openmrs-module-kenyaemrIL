package org.openmrs.module.kenyaemrIL;

import java.io.Serializable;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ExternalPatientId implements Serializable {
    private String id;
    private String identifierType;
    private String assigningAuthority;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getAssigningAuthority() {
        return assigningAuthority;
    }

    public void setAssigningAuthority(String assigningAuthority) {
        this.assigningAuthority = assigningAuthority;
    }
}
