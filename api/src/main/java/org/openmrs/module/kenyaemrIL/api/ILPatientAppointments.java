package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.PLACER_APPOINTMENT_NUMBER;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

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


    public static ILMessage iLPatientWrapper(Patient patient) {
        ILMessage ilMessage = new ILMessage();
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;
//set external identifier if available

//        Form the internal patient IDs
        for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
            ipd = new INTERNAL_PATIENT_ID();
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("OpenMRS ID")) {
                ipd.setAssigning_authority("SOURCE_SYSTEM");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("SOURCE_SYSTEM_ID");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("TB Treatment Number")) {
                ipd.setAssigning_authority("TB");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("TB_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("National ID")) {
                ipd.setAssigning_authority("GOK");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NATIONAL_ID");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HTS Number")) {
                ipd.setAssigning_authority("HTS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HTS_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HDSS ID")) {
                ipd.setAssigning_authority("HDSS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HDSS_ID");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("ANC NUMBER")) {
                ipd.setAssigning_authority("ANC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("ANC_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("OPD NUMBER")) {
                ipd.setAssigning_authority("OPD");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("OPD_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("PMTCT NUMBER")) {
                ipd.setAssigning_authority("PMTCT");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PMTCT_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("NHIF NUMBER")) {
                ipd.setAssigning_authority("NHIF");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NHIF");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Patient Clinic Number")) {
                ipd.setAssigning_authority("CLINIC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PATIENT CLINIC NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("MPI GODS NUMBER")) {
                if (patientIdentifier.getIdentifierType().getName() != null) {
                    epd.setAssigning_authority("MPI");
                    epd.setId(patientIdentifier.getIdentifier());
                    epd.setIdentifier_type("GODS_NUMBER");
                    patientIdentification.setExternal_patient_id(epd);
                }
                continue;
            }
            internalPatientIds.add(ipd);
        }

        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);

        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName());
        patientname.setMiddle_name(personName.getMiddleName());
        patientname.setLast_name(personName.getFamilyName());

        patientIdentification.setPatient_name(patientname);

        ilMessage.setPatient_identification(patientIdentification);
//set appointment information
        APPOINTMENT_INFORMATION appointments[] = new APPOINTMENT_INFORMATION[1];
        APPOINTMENT_INFORMATION appointmentInformation = new APPOINTMENT_INFORMATION();
        PLACER_APPOINTMENT_NUMBER placerAppointmentNumber = new PLACER_APPOINTMENT_NUMBER();

        Encounter lastFollowUpEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));   //last greencard followup form

        Integer patientTCAConcept = 5096;
        Integer patientTCAReasonConcept = 160288;
        if (lastFollowUpEncounter != null) {
            for (Obs obs : lastFollowUpEncounter.getObs()) {

                if (obs.getConcept().getConceptId().equals(patientTCAConcept)) {

                    placerAppointmentNumber.setNumber(String.valueOf(obs.getObsId()));                      //set placer appointment number
                    placerAppointmentNumber.setEntity("KENYA EMR");                                      //set Entity
                    appointmentInformation.setAppointment_date(String.valueOf(obs.getValueDate()));      //set patient TCA
                    appointmentInformation.setAction_code("A");                                          //set action code
                    appointmentInformation.setAppointment_note("N/A");                                   //set appointment note
                    appointmentInformation.setAppointment_status("PENDING");                             //set appointment status
                    appointmentInformation.setAppointment_placing_entity("KENYA EMR");                    //set appointment placing entity

                }
                if (obs.getConcept().getConceptId().equals(patientTCAReasonConcept)) {
                    appointmentInformation.setAppointment_reason(appointmentReasonConverter(obs.getValueCoded()));    //set appointment reason
                    appointmentInformation.setAppointment_type(appointmentTypeConverter(obs.getValueCoded()));         //set appointment type
                    appointmentInformation.setAppointment_location(appointmentLocationConverter(obs.getValueCoded()));   //set appointment location
                }

              }
           }
            appointments[0] = appointmentInformation;
            ilMessage.setAppointment_information(appointments);
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
