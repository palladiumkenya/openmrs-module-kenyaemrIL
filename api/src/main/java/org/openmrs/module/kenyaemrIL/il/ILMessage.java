package org.openmrs.module.kenyaemrIL.il;

import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Patient_Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.AppointmentMessage;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.il.observation.ObservationMessage;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.il.pharmacy.COMMON_ORDER_DETAILS;
import org.openmrs.module.kenyaemrIL.il.pharmacy.DispenseMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.OrderMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_DISPENSE;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_ENCODED_ORDER;
import org.openmrs.module.kenyaemrIL.il.viralload.ViralLoadMessage;
import org.openmrs.module.kenyaemrIL.programEnrollment.Patient_Program_Enrollment_Message;
import org.openmrs.module.kenyaemrIL.programEnrollment.Program_Enrollment_Message;

/**
 * @author Stanslaus Odhiambo
 * Created on 08/01/2018.
 */
public class ILMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private PATIENT_IDENTIFICATION_SIMPLE patient_identification_simple;
    private PATIENT_VISIT patient_visit;
    private NEXT_OF_KIN[] next_of_kin;
    private OBSERVATION_RESULT[] observation_result;
    private APPOINTMENT_INFORMATION[] appointment_information;
    private VIRAL_LOAD_RESULT[] viral_load_result;
    private COMMON_ORDER_DETAILS common_order_details;
    private PHARMACY_ENCODED_ORDER[] pharmacy_encoded_order;
    private PHARMACY_DISPENSE[] pharmacy_dispense;
    private Program_Discontinuation_Message discontinuation_message;
    private Program_Enrollment_Message program_enrollment_message;

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

    public VIRAL_LOAD_RESULT[] getViral_load_result() {
        return viral_load_result;
    }

    public void setViral_load_result(VIRAL_LOAD_RESULT[] viral_load_result) {
        this.viral_load_result = viral_load_result;
    }

    public PATIENT_IDENTIFICATION_SIMPLE getPatient_identification_simple() {
        return patient_identification_simple;
    }

    public void setPatient_identification_simple(PATIENT_IDENTIFICATION_SIMPLE patient_identification_simple) {
        this.patient_identification_simple = patient_identification_simple;
    }

    public Program_Discontinuation_Message getDiscontinuation_message() {
        return discontinuation_message;
    }

    public void setDiscontinuation_message(Program_Discontinuation_Message discontinuation_message) {
        this.discontinuation_message = discontinuation_message;
    }

    public void setProgram_enrollment_message(Program_Enrollment_Message program_enrollment_message) {
        this.program_enrollment_message = program_enrollment_message;
    }

    public Program_Enrollment_Message getProgram_enrollment_message() {
        return program_enrollment_message;
    }

    //    public COMMON_ORDER_DETAILS getCommon_Order_Details() {
//        return common_order_details; }
//
//    public void setCommon_Order_Details(COMMON_ORDER_DETAILS common_order_details) {
//        this.common_order_details = common_order_details;
//    }


    public COMMON_ORDER_DETAILS getCommon_order_details() {
        return common_order_details;
    }

    public void setCommon_order_details(COMMON_ORDER_DETAILS common_order_details) {
        this.common_order_details = common_order_details;
    }

    public PHARMACY_ENCODED_ORDER[] getPharmacy_encoded_order() {
        return pharmacy_encoded_order;
    }

    public void setPharmacy_encoded_order(PHARMACY_ENCODED_ORDER[] pharmacy_encoded_order) {
        this.pharmacy_encoded_order = pharmacy_encoded_order;
    }

    public PHARMACY_DISPENSE[] getPharmacy_dispense() {
        return pharmacy_dispense;
    }

    public void setPharmacy_dispense(PHARMACY_DISPENSE[] pharmacy_dispense) {
        this.pharmacy_dispense = pharmacy_dispense;
    }

    public ILPerson extractILRegistration() {
        ILPerson ilPerson = new ILPerson();
        ilPerson.setMessage_header(this.message_header);
        ilPerson.setPatient_identification(this.patient_identification);
        ilPerson.setNext_of_kin(this.next_of_kin);
        ilPerson.setPatient_visit(this.getPatient_visit());
        ilPerson.setObservation_result(this.getObservation_result());
        return ilPerson;
    }

    public AppointmentMessage extractAppointmentMessage() {
        AppointmentMessage appointmentMessage = new AppointmentMessage();
        appointmentMessage.setMessage_header(this.getMessage_header());
        appointmentMessage.setPatient_identification(this.getPatient_identification());
        appointmentMessage.setAppointment_information(this.getAppointment_information());
        appointmentMessage.setObservation_result(this.getObservation_result());
        return appointmentMessage;
    }

    public Patient_Program_Discontinuation_Message extractHivDiscontinuationMessage() {
        Patient_Program_Discontinuation_Message patientProgramDiscontinuationMessage = new Patient_Program_Discontinuation_Message();
        patientProgramDiscontinuationMessage.setMessage_header(this.getMessage_header());
        patientProgramDiscontinuationMessage.setPatient_identification(this.getPatient_identification());
        patientProgramDiscontinuationMessage.setDiscontinuation_message(this.getDiscontinuation_message());
        return patientProgramDiscontinuationMessage;
    }

    public Patient_Program_Enrollment_Message extractProgramEnrollmentMessage() {
        Patient_Program_Enrollment_Message patientProgramEnrollmentMessage = new Patient_Program_Enrollment_Message();
        patientProgramEnrollmentMessage.setMessage_header(this.getMessage_header());
        patientProgramEnrollmentMessage.setPatient_identification(this.getPatient_identification());
        patientProgramEnrollmentMessage.setProgram_enrollment_message(this.getProgram_enrollment_message());
        return patientProgramEnrollmentMessage;
    }

    public ObservationMessage extractORUMessage() {
        ObservationMessage observationMessage = new ObservationMessage();
        observationMessage.setMessage_header(this.message_header);
        observationMessage.setPatient_identification(this.getPatient_identification());
        observationMessage.setObservation_result(this.observation_result);
        return observationMessage;
    }

    public ViralLoadMessage extractViralLoadMessage() {
        ViralLoadMessage viralLoadMessage = new ViralLoadMessage();
        viralLoadMessage.setMessage_header(this.message_header);
        viralLoadMessage.setPatient_identification(this.getPatient_identification());
        viralLoadMessage.setViral_load_result(this.getViral_load_result());
        return viralLoadMessage;
    }

    public DispenseMessage extractPharmacyDispenseMessage() {
        DispenseMessage pharmacyDispenseMessage = new DispenseMessage();
        pharmacyDispenseMessage.setMessage_header(this.message_header);
        pharmacyDispenseMessage.setPatient_identification(this.getPatient_identification());
        pharmacyDispenseMessage.setCommon_Order_Details(this.getCommon_order_details());
        pharmacyDispenseMessage.setEncodedOrderList(this.getPharmacy_encoded_order());
        pharmacyDispenseMessage.setDispense_information(this.getPharmacy_dispense());
        return pharmacyDispenseMessage;
    }

    public OrderMessage extractPharmacyOrderMessage() {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setMessage_header(this.message_header);
        orderMessage.setPatient_identification(this.getPatient_identification_simple());
        orderMessage.setCommon_Order_Details(this.getCommon_order_details());
        orderMessage.setPharmacy_encoded_order(this.getPharmacy_encoded_order());
        orderMessage.setObservation_result(this.observation_result);
        return orderMessage;
    }
}
