package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for an IL Appointment Scheduling
 */
public class ILPatientUnsolicitedObservationResults {

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


        //Set the patient observation results
        List<OBSERVATION_RESULT> observationResults = new ArrayList<>();
        OBSERVATION_RESULT observationResult = null;


//        TODO -- Check out the observations of interest - I would recommend that we do this out of the code, given the
//        TODO list of observations will increase with time yet we don't have to be rebuilding just do add the processing for a
//        TODO single observation - I would suggest maybe a table for this so that we can add the concept IDs for the new observations of
//        TODO interest - @Patrick check this one out
        List<Encounter> allEncountersOfInterest = ILUtils.getAllEncountersOfInterest();
        Encounter lastDrugOrderEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3"));

        Integer latestVLConcept = 856;
        Integer LDLQuestionConcept = 1305;
        Integer LDLAnswerConcept = 1302;
        Integer ARVConcept = 1085;
        for (Encounter encounter : allEncountersOfInterest) {
            for (Obs obs : encounter.getObs()) {
                observationResult = new OBSERVATION_RESULT();

//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl sample collection date
//                observationResult.setDate_sample_collected(String.valueOf(obs.getObsDatetime()));
//            }
//              else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample  collection date
//                observationResult.setDate_sample_collected(String.valueOf(obs.getObsDatetime()));
//                 }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl sample testing date
//                observationResult.setDate_sample_tested(String.valueOf(obs.getObsDatetime()));
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample testing date
//                observationResult.setDate_sample_tested(String.valueOf(obs.getObsDatetime()));
//            }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl result
//                observationResult.setVl_result(String.valueOf(obs.getValueNumeric()));
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl result
//                observationResult.setVl_result("LDL");
//            }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL sample type
//                observationResult.setSample_type("BLOOD SAMPLE");
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample type
//                observationResult.setSample_type("BLOOD SAMPLE");
//            }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL sample rejection
//                observationResult.setSample_rejection("");
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample rejection
//                observationResult.setSample_rejection("");
//            }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL justification
//                observationResult.setJustification("");
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set  justification
//                observationResult.setJustification("");
//            }
//            if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl Lab Tested In
//                observationResult.setJustification("KEMRI LAB SIAYA");
//            }
//            else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl Lab Tested In
//                observationResult.setJustification("KEMRI LAB SIAYA");
//            }
            }
            observationResults.add(observationResult);
        }
        for (Obs obs : lastDrugOrderEncounter.getObs()) {


//            if (obs.getConcept().getConceptId().equals(ARVConcept)) {    //set current regimen
//                observationResult.setRegimen(obs.getValueText());
//            }
        }


        ilMessage.setObservation_result(observationResults.toArray(new OBSERVATION_RESULT[observationResults.size()]));
        return ilMessage;
    }

}
