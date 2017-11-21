package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class OrderingPhysician {

    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
