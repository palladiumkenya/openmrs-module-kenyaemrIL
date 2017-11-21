package org.openmrs.module.kenyaemrIL.il;


/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class NextOfKin {
    private NokName nokName;
    private String relationship;
    private String address;
    private String phoneNumber;
    private String sex;
    private String dateOfBirth;
    private String contactRole;

    public NokName getNokName() {
        return nokName;
    }

    public void setNokName(NokName nokName) {
        this.nokName = nokName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getContactRole() {
        return contactRole;
    }

    public void setContactRole(String contactRole) {
        this.contactRole = contactRole;
    }
}
