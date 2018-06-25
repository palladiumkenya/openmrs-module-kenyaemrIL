package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for an IL Appointment Scheduling
 */
public class ILPatientViralLoadResults {

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
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
                internalPatientIds.add(ipd);
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("MPI GODS NUMBER")) {
                if (patientIdentifier.getIdentifierType().getName() != null) {
                    epd.setAssigning_authority("MPI");
                    epd.setId(patientIdentifier.getIdentifier());
                    epd.setIdentifier_type("GODS_NUMBER");
                    patientIdentification.setExternal_patient_id(epd);
                }
                continue;
            }
        }
        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName());
        patientname.setMiddle_name(personName.getMiddleName());
        patientname.setLast_name(personName.getFamilyName());
        // Set to empty string unwanted patient details for viral load
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

        patientIdentification.setPatient_name(patientname);
        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);


        //Set the patient viral load results
        VIRAL_LOAD_RESULT vlTestResults[] = new VIRAL_LOAD_RESULT[1];
        VIRAL_LOAD_RESULT viral_load_Result = new VIRAL_LOAD_RESULT();

        Encounter lastLabResultsEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
        Encounter lastDrugOrderEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3"));

        Integer latestVLConcept = 856;
        Integer LDLQuestionConcept = 1305;
        Integer LDLAnswerConcept = 1302;
        Integer ARVConcept = 1085;
        if(lastLabResultsEncounter != null) {
            for (Obs obs : lastLabResultsEncounter.getObs()) {
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl sample collection date
                    viral_load_Result.setDate_sample_collected(String.valueOf(obs.getObsDatetime()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample  collection date
                    viral_load_Result.setDate_sample_collected(String.valueOf(obs.getObsDatetime()));
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl sample testing date
                    viral_load_Result.setDate_sample_tested(String.valueOf(obs.getObsDatetime()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample testing date
                    viral_load_Result.setDate_sample_tested(String.valueOf(obs.getObsDatetime()));
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl result
                    viral_load_Result.setVl_result(String.valueOf(obs.getValueNumeric()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl result
                    viral_load_Result.setVl_result("LDL");
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL sample type
                    viral_load_Result.setSample_type("BLOOD SAMPLE");
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample type
                    viral_load_Result.setSample_type("BLOOD SAMPLE");
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL sample rejection
                    viral_load_Result.setSample_rejection("");
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl sample rejection
                    viral_load_Result.setSample_rejection("");
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set VL justification
                    viral_load_Result.setJustification("");
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set  justification
                    viral_load_Result.setJustification("");
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set regimen
                    viral_load_Result.setRegimen("");
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set regimen
                    viral_load_Result.setRegimen("");
                }
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {    //set vl Lab Tested In
                    viral_load_Result.setLab_tested_in("");
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {    //set ldl Lab Tested In
                    viral_load_Result.setLab_tested_in("");
                }
            }
        }
        if (lastDrugOrderEncounter != null) {
            for (Obs obs : lastDrugOrderEncounter.getObs()) {
                if (obs != null) {

                    if (obs.getConcept().getConceptId().equals(ARVConcept)) {    //set current regimen
                        viral_load_Result.setRegimen(obs.getValueText());
                    }
                }
            }
        }

        vlTestResults[0] = viral_load_Result;
        ilMessage.setPatient_identification(patientIdentification);
        ilMessage.setViral_load_result(vlTestResults);

        return ilMessage;
    }

}
