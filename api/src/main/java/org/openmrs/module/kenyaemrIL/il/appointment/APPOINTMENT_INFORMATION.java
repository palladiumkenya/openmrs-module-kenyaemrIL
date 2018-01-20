package org.openmrs.module.kenyaemrIL.il.appointment;

import java.util.Date;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class APPOINTMENT_INFORMATION {
    private String  appointment_reason;
    private String  action_code;
    private String  appointment_placing_entity;
    private String  appointment_location;
    private String  appointment_status;
    private String  appointment_type;
    private String  appointment_note;
    private String  appointment_date;
    private PLACER_APPOINTMENT_NUMBER  placer_appointment_number;

    public String getAppointment_reason() {
        return appointment_reason;
    }

    public void setAppointment_reason(String appointment_reason) {
        this.appointment_reason = appointment_reason;
    }

    public String getAction_code() {
        return action_code;
    }

    public void setAction_code(String action_code) {
        this.action_code = action_code;
    }

    public String getAppointment_placing_entity() {
        return appointment_placing_entity;
    }

    public void setAppointment_placing_entity(String appointment_placing_entity) {
        this.appointment_placing_entity = appointment_placing_entity;
    }

    public String getAppointment_location(String s) {
        return appointment_location;
    }

    public void setAppointment_location(String appointment_location) {
        this.appointment_location = appointment_location;
    }

    public String getAppointment_status() {
        return appointment_status;
    }

    public void setAppointment_status(String appointment_status) {
        this.appointment_status = appointment_status;
    }

    public String getAppointment_type() {
        return appointment_type;
    }

    public void setAppointment_type(String appointment_type) {
        this.appointment_type = appointment_type;
    }

    public String getAppointment_note() {
        return appointment_note;
    }

    public void setAppointment_note(String appointment_note) {
        this.appointment_note = appointment_note;
    }

    public String getAppointment_date(Date valueDate) {
        return appointment_date;
    }

    public void setAppointment_date(String appointment_date) {
        this.appointment_date = appointment_date;
    }

    public PLACER_APPOINTMENT_NUMBER getPlacer_appointment_number() {
        return placer_appointment_number;
    }

    public void setPlacer_appointment_number(PLACER_APPOINTMENT_NUMBER placer_appointment_number) {
        this.placer_appointment_number = placer_appointment_number;
    }
}

