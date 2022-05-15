package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ORDERING_PHYSICIAN {

    private String prefix = "";
    private String first_name = "";
    private String middle_name = "";

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getMiddle_name() {
        return middle_name;
    }

    public void setMiddle_name(String middle_name) {
        this.middle_name = middle_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    private String last_name;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


}
