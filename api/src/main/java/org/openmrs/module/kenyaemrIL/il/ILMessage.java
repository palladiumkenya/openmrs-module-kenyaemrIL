package org.openmrs.module.kenyaemrIL.il;

import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.observation.*;

/**
 * @author Stanslaus Odhiambo
 *         Created on 08/01/2018.
 */
public class ILMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private PATIENT_VISIT patient_visit;
    private NEXT_OF_KIN[] next_of_kin;
    private OBSERVATION_RESULT[] observation_result;
    private APPOINTMENT_INFORMATION[] appointment_information;
    private VIRAL_LOAD_RESULT[] VIRAL_LOAD_RESULT;

    public MESSAGE_HEADER getMessage_header() {
        return message_header;
    }

    public void setMessage_header(MESSAGE_HEADER message_header) {
        this.message_header = message_header;
    }

    public PATIENT_IDENTIFICATION getPatient_identification() {
        return patient_identification;
    }

    public void setPatient_identification(PATIENT_IDENTIFICATION patient_identification) {
        this.patient_identification = patient_identification;
    }

    public PATIENT_VISIT getPatient_visit() {
        return patient_visit;
    }

    public void setPatient_visit(PATIENT_VISIT patient_visit) {
        this.patient_visit = patient_visit;
    }

    public NEXT_OF_KIN[] getNext_of_kin() {
        return next_of_kin;
    }

    public void setNext_of_kin(NEXT_OF_KIN[] next_of_kin) {
        this.next_of_kin = next_of_kin;
    }

    public OBSERVATION_RESULT[] getObservation_result() {
        return observation_result;
    }

    public void setObservation_result(OBSERVATION_RESULT[] observation_result) {
        this.observation_result = observation_result;
    }

    public APPOINTMENT_INFORMATION[] getAppointment_information() {
        return appointment_information;
    }

    public void setAppointment_information(APPOINTMENT_INFORMATION[] appointment_information) {
        this.appointment_information = appointment_information;
    }

    public org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT[] getVIRAL_LOAD_RESULT() {
        return VIRAL_LOAD_RESULT;
    }

    public void setVIRAL_LOAD_RESULT(org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT[] VIRAL_LOAD_RESULT) {
        this.VIRAL_LOAD_RESULT = VIRAL_LOAD_RESULT;
    }
}
