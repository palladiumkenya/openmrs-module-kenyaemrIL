package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.MOTHER_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PHYSICAL_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.PLACER_APPOINTMENT_NUMBER;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for an IL Appointment Scheduling
 */
public class ILPatientAppointments {

    private final Log log = LogFactory.getLog(this.getClass());
    static ConceptService conceptService = Context.getConceptService();


    public static ILMessage iLPatientWrapper(Patient patient, Encounter lastFollowUpEncounter) {
        ILMessage ilMessage = new ILMessage();
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;
//set external identifier if available

//        Form the internal patient IDs
        for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
            ipd = new INTERNAL_PATIENT_ID();
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
                internalPatientIds.add(ipd);
                // Form the default external patient IDs
                epd.setAssigning_authority("MPI");
                epd.setIdentifier_type("GODS_NUMBER");
                patientIdentification.setExternal_patient_id(epd);
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Patient Clinic Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PATIENT_CLINIC_NUMBER");
                internalPatientIds.add(ipd);
            }
        }

        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);

        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName() != null ? personName.getGivenName() : "");
        patientname.setMiddle_name(personName.getMiddleName() != null ? personName.getMiddleName() : "");
        patientname.setLast_name(personName.getFamilyName() != null ? personName.getFamilyName() : "");
        patientIdentification.setPatient_name(patientname);

        // Set to empty strings unwanted patient details for viral load
        patientIdentification.setSex("");   //        Set the Gender, phone number and marital status
        patientIdentification.setPhone_number("");
        patientIdentification.setMarital_status("");
        patientIdentification.setDate_of_birth("");
        patientIdentification.setDate_of_birth_precision("");
        patientIdentification.setDeath_date("");
        patientIdentification.setDeath_indicator("");

        PATIENT_ADDRESS patientAddress = new PATIENT_ADDRESS();
        patientAddress.setPostal_address("");
        patientAddress.setPhysical_address(new PHYSICAL_ADDRESS());
        patientIdentification.setPatient_address(patientAddress);

        //Set mothers name
        patientIdentification.setMother_name(new MOTHER_NAME());

        ilMessage.setPatient_identification(patientIdentification);
        //set appointment information
        APPOINTMENT_INFORMATION appointments[] = new APPOINTMENT_INFORMATION[1];
        APPOINTMENT_INFORMATION appointmentInformation = new APPOINTMENT_INFORMATION();
        PLACER_APPOINTMENT_NUMBER placerAppointmentNumber = new PLACER_APPOINTMENT_NUMBER();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Integer patientTCAConcept = 5096;
        Integer consentForReminderConcept = 166607;
        int yesConceptId = 1065;
        int noConceptId = 1066;
        //Integer patientTCAReasonConcept = 160288;
        if (lastFollowUpEncounter != null) {
            for (Obs obs : lastFollowUpEncounter.getObs()) {
                //TODO: We need a way to get obs for the concept ids of interest
                if (obs.getConcept().getConceptId().equals(patientTCAConcept)) {
                    //we use encounter id to take care of changing appointment date for the same encounter
                    placerAppointmentNumber.setNumber(String.valueOf(lastFollowUpEncounter.getEncounterId().intValue()));
                    appointmentInformation.setPlacer_appointment_number(placerAppointmentNumber);
                    appointmentInformation.setVisit_date(formatter.format(lastFollowUpEncounter.getEncounterDatetime()));
                    appointmentInformation.setAppointment_date(formatter.format((obs.getValueDate())));
                    appointmentInformation.setAction_code("A");
                    appointmentInformation.setAppointment_note("N/A");
                    appointmentInformation.setAppointment_status("PENDING");
                    appointmentInformation.setAppointment_placing_entity("KENYAEMR");

                    // the current appointment generated from the emr is the clinical. we'll populate the defaults
                    appointmentInformation.setAppointment_reason("FOLLOWUP");
                    appointmentInformation.setAppointment_type("CLINICAL");
                    appointmentInformation.setAppointment_location("CLINIC");

                    appointments[0] = appointmentInformation;
                    ilMessage.setAppointment_information(appointments);
                } else if (obs.getConcept().getConceptId().equals(consentForReminderConcept)) {
                    if (obs.getValueCoded().getConceptId().equals(yesConceptId)) {
                        appointmentInformation.setConsent_for_reminder("Y");
                    } else {
                        if (obs.getValueCoded().getConceptId().equals(noConceptId)) {
                            appointmentInformation.setConsent_for_reminder("N");
                        }
                    }
                }
            }
        }

        return ilMessage;
    }


    static String appointmentReasonConverter(Concept key) {
        Map<Concept, String> appointmentReasonList = new HashMap<Concept, String>();
        appointmentReasonList.put(conceptService.getConcept(160521), "PHARMACY_REFILL");
        appointmentReasonList.put(conceptService.getConcept(1283), "LAB_TEST");
        appointmentReasonList.put(conceptService.getConcept(160523), "FOLLOWUP");
        return appointmentReasonList.get(key);
    }

    static String appointmentTypeConverter(Concept key) {
        Map<Concept, String> appointmentTypeList = new HashMap<Concept, String>();
        appointmentTypeList.put(conceptService.getConcept(160521), "PHARMACY");
        appointmentTypeList.put(conceptService.getConcept(1283), "INVESTIGATION");
        appointmentTypeList.put(conceptService.getConcept(160523), "CLINICAL");
        return appointmentTypeList.get(key);
    }

    static String appointmentLocationConverter(Concept key) {
        Map<Concept, String> appointmentLocationList = new HashMap<Concept, String>();
        appointmentLocationList.put(conceptService.getConcept(160521), "PHARMACY");
        appointmentLocationList.put(conceptService.getConcept(1283), "LAB");
        appointmentLocationList.put(conceptService.getConcept(160523), "CLINIC");
        return appointmentLocationList.get(key);
    }


}
