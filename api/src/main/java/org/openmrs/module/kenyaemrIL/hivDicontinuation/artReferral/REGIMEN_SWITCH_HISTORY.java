package org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral;

import java.util.Set;

public class REGIMEN_SWITCH_HISTORY {
    private String start_date;
    private String end_date;
    private String regimen_short_display;
    private String regimen_line;
    private String regimen_long_display;
    private Set<String> change_reasons;
    private String regimen_uuid;
    private String current;


    public REGIMEN_SWITCH_HISTORY() {
    }

    public REGIMEN_SWITCH_HISTORY(String start_date, String end_date, String regimen_short_display, String regimen_line, String regimen_long_display, Set<String> change_reasons, String regimen_uuid, String current) {
        this.start_date = start_date;
        this.end_date = end_date;
        this.regimen_short_display = regimen_short_display;
        this.regimen_line = regimen_line;
        this.regimen_long_display = regimen_long_display;
        this.change_reasons = change_reasons;
        this.regimen_uuid = regimen_uuid;
        this.current = current;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String getRegimen_short_display() {
        return regimen_short_display;
    }

    public void setRegimen_short_display(String regimen_short_display) {
        this.regimen_short_display = regimen_short_display;
    }

    public String getRegimen_line() {
        return regimen_line;
    }

    public void setRegimen_line(String regimen_line) {
        this.regimen_line = regimen_line;
    }

    public String getRegimen_long_display() {
        return regimen_long_display;
    }

    public void setRegimen_long_display(String regimen_long_display) {
        this.regimen_long_display = regimen_long_display;
    }

    public Set<String> getChange_reasons() {
        return change_reasons;
    }

    public void setChange_reasons(Set<String> change_reasons) {
        this.change_reasons = change_reasons;
    }

    public String getRegimen_uuid() {
        return regimen_uuid;
    }

    public void setRegimen_uuid(String regimen_uuid) {
        this.regimen_uuid = regimen_uuid;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }
}
