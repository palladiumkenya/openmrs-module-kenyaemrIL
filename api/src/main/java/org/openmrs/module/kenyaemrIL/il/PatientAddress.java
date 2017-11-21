package org.openmrs.module.kenyaemrIL.il;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PatientAddress {
    private PhysicalAddress physicalAddress;
    private String postal_address;

    public PhysicalAddress getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(PhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    public String getPostal_address() {
        return postal_address;
    }

    public void setPostal_address(String postal_address) {
        this.postal_address = postal_address;
    }
}
