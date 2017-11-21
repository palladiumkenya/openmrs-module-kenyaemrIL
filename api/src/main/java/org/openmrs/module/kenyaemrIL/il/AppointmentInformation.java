package org.openmrs.module.kenyaemrIL.il;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class AppointmentInformation {
    private String appointmentReason;
    private String actionCode;
    private String appointmentPlacingEntity;
    private String appointmentLocation;
    private String appointmentStatus;
    private String appointmentType;
    private String appointmentNote;
    private String appointmentDate;
    private PlacerAppointmentNumber placerAppointmentNumber;

    public String getAppointmentReason() {
        return appointmentReason;
    }


    public void setAppointmentReason(String appointmentReason) {
        this.appointmentReason = appointmentReason;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getAppointmentPlacingEntity() {
        return appointmentPlacingEntity;
    }

    public void setAppointmentPlacingEntity(String appointmentPlacingEntity) {
        this.appointmentPlacingEntity = appointmentPlacingEntity;
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public String getAppointmentLocation() {
        return appointmentLocation;
    }

    public void setAppointmentLocation(String appointmentLocation) {
        this.appointmentLocation = appointmentLocation;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getAppointmentNote() {
        return appointmentNote;
    }

    public void setAppointmentNote(String appointmentNote) {
        this.appointmentNote = appointmentNote;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public PlacerAppointmentNumber getPlacerAppointmentNumber() {
        return placerAppointmentNumber;
    }

    public void setPlacerAppointmentNumber(PlacerAppointmentNumber placerAppointmentNumber) {
        this.placerAppointmentNumber = placerAppointmentNumber;
    }
}
