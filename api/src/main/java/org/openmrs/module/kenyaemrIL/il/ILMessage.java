package org.openmrs.module.kenyaemrIL.il;

import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.AppointmentMessage;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.il.observation.ObservationMessage;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.il.pharmacy.CommonOrderDetails;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.viralload.ViralLoadMessage;

/**
 * @author Stanslaus Odhiambo
 * Created on 08/01/2018.
 */
public class ILMessage {
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private PATIENT_VISIT patient_visit;
    private NEXT_OF_KIN[] next_of_kin;
    private OBSERVATION_RESULT[] observation_result;
    private APPOINTMENT_INFORMATION[] appointment_information;
    private VIRAL_LOAD_RESULT[] viral_load_result;
    private PharmacyDispense[] dispense_information;
    private CommonOrderDetails[] commonOrderDetails;

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

    // Adding pharmacy dispense
    public PharmacyDispense[] getDispense_information() {
        return dispense_information;
    }

    public void setDispense_information(PharmacyDispense[] dispense_information) {
        this.dispense_information = dispense_information;
    }

    public CommonOrderDetails[] getCommon_Order_information() {
        return commonOrderDetails;
    }

    public void setCommon_Order_information(CommonOrderDetails[] commonOrderDetails) {
        this.commonOrderDetails = commonOrderDetails;
    }

   public ILPerson extractILRegistration() {
        ILPerson ilPerson = new ILPerson();
        ilPerson.setMessage_header(this.message_header);
        ilPerson.setPatient_identification(this.patient_identification);
        ilPerson.setNext_of_kin(this.next_of_kin);
        ilPerson.setPatient_visit(this.getPatient_visit());
        return ilPerson;
    }

    public AppointmentMessage extractAppointmentMessage() {
        AppointmentMessage appointmentMessage = new AppointmentMessage();
        appointmentMessage.setMessage_header(this.getMessage_header());
        appointmentMessage.setPatient_identification(this.getPatient_identification());
        appointmentMessage.setAppointment_information(this.getAppointment_information());
        return appointmentMessage;
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

//    public ILPharmacyOrder extractPharmacyOrderMessage() {
//        ILPharmacyOrder pharmacyOrderMessage = new ILPharmacyOrder();
//        pharmacyOrderMessage.setMessage_header(this.message_header);
//        pharmacyOrderMessage.setPatient_identification(this.getPatient_identification());
//       // pharmacyOrderMessage.setCommonOrderDetails(this.getCommonOrderDetails());
//        //pharmacyOrderMessage.setEncodedOrderList(this.getEncodedOrderList());
//        return pharmacyOrderMessage;
//    }

    public ILPharmacyDispense extractPharmacyDispenseMessage() {
        ILPharmacyDispense pharmacyDispenseMessage = new ILPharmacyDispense();
        pharmacyDispenseMessage.setMessage_header(this.message_header);
        pharmacyDispenseMessage.setPatient_identification(this.getPatient_identification());
        pharmacyDispenseMessage.setDispense_information(this.getDispense_information());
        pharmacyDispenseMessage.setCommon_Order_information(this.getCommon_Order_information());
       // pharmacyDispenseMessage.setCommon_Order_information(this.getCommon_Order_information());   //Add pharmacy_encoded_order
        return pharmacyDispenseMessage;
    }
}
