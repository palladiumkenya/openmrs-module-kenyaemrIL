package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 01/25/18.
 * A fragment controller for an IL Observation Result Unsolicited Message
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

        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName());
        patientname.setMiddle_name(personName.getMiddleName());
        patientname.setLast_name(personName.getFamilyName());
        patientIdentification.setPatient_name(patientname);

        //Set the patient observation results
        List<OBSERVATION_RESULT> observationResults = new ArrayList<>();
        OBSERVATION_RESULT observationResult = null;
        observationResult = new OBSERVATION_RESULT();

       // List<EncounterType> encounterTypes = ILUtils.getAllEncounterTypesOfInterest(); //TODO @stance consider consolidating all encounters
        //EncounterType mchMotherEncounter = Context.getEncounterService().getEncounterTypeByUuid("3ee036d8-7c13-4393-b5d6-036f2fe45126");  //mch mother enrollment

        Encounter hivGreencardEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));   //last greencard followup
        Encounter hivEnrollmentEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));  //hiv enrollment
        Encounter drugOrderEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3"));  //last drug order
        Encounter mchMotherEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("3ee036d8-7c13-4393-b5d6-036f2fe45126"));  //mch mother enrollment
        Encounter labResultEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));  //lab results

        Integer HeightConcept = 5090;
        Integer WeightConcept = 5089;
        Integer HivDiagnosisDateConcept = 160554;
        Integer HivCareInitiationDateConcept = 160555;
        Integer ARTInitiationDateConcept = 159599;
        Integer IspregnantConcept = 5272;
        Integer EDDConcept = 5596;
        Integer DateOfDeliveryConcept = 5599;
        Integer ARVConcept = 1085;
        Integer SmokerConcept = 155600;
        Integer AlcoholUseConcept = 159449;
        Integer CTXStartConcept = 162229;
        Integer TestsOrderedConcept = 1271;
        Integer CD4Concept =5497;
        Integer CD4PercentConcept =730;
        Integer TBdiagnosisDateConcept =1662;
        Integer TBTreatmentStartDateConcept =1113;
        Integer TBTreatmentCompleteDateConcept =164384;
        Integer WhoStageConcept =5356;
        Integer YesConcept = 1065;
        Integer NoConcept = 1066;
        //Enrollment encounter
        if (hivEnrollmentEncounter != null) {
            for (Obs obs : hivEnrollmentEncounter.getObs()) {
                if (obs.getConcept().getConceptId().equals(HeightConcept)) {          //start height
                    observationResult.setObservation_identifier("START_HEIGHT");
                    observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                    observationResult.setCoding_system("");
                    observationResult.setValue_type("NM");
                    observationResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                    observationResult.setUnits("CM");
                    observationResult.setObservation_result_status("F");
                    observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                    observationResult.setAbnormal_flags("N");
                }
                if (obs.getConcept().getConceptId().equals(WeightConcept)) {     // start weight
                    observationResult.setObservation_identifier("START_WEIGHT");
                    observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                    observationResult.setCoding_system("");
                    observationResult.setValue_type("NM");
                    observationResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                    observationResult.setUnits("KG");
                    observationResult.setObservation_result_status("F");
                    observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                    observationResult.setAbnormal_flags("N");
                }
                if (obs.getConcept().getConceptId().equals(HivDiagnosisDateConcept)) {     // diagnosis date
                    observationResult.setObservation_identifier("HIV_DIAGNOSIS");
                    observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                    observationResult.setCoding_system("");
                    observationResult.setValue_type("DT");
                    observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                    observationResult.setUnits("");
                    observationResult.setObservation_result_status("F");
                    observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                    observationResult.setAbnormal_flags("N");
                }
                if (obs.getConcept().getConceptId().equals(HivCareInitiationDateConcept)) {     // hiv care initiation date
                    observationResult.setObservation_identifier("HIV_CARE_INITIATION");
                    observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                    observationResult.setCoding_system("");
                    observationResult.setValue_type("DT");
                    observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                    observationResult.setUnits("");
                    observationResult.setObservation_result_status("F");
                    observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                    observationResult.setAbnormal_flags("N");
                }
            }
        }
            //Greencard encounter
            if (hivGreencardEncounter != null) {
                for (Obs obs : hivGreencardEncounter.getObs()) {
                    if (obs.getConcept().getConceptId().equals(IspregnantConcept) && obs.getValueCoded().equals(YesConcept)) {          //is pregnant
                        observationResult.setObservation_identifier("IS_PREGNANT");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("CE");
                        observationResult.setObservation_value(String.valueOf(obs.getValueCoded()));
                        observationResult.setUnits("YES/NO");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    if (obs.getConcept().getConceptId().equals(EDDConcept)) {                                                      // EDD
                        observationResult.setObservation_identifier("PRENGANT_EDD");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    if (obs.getConcept().getConceptId().equals(CTXStartConcept) && obs.getValueCoded().equals(YesConcept)) {  // CTX start date
                        observationResult.setObservation_identifier("COTRIMOXAZOLE_START");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    if (obs.getConcept().getConceptId().equals(TBdiagnosisDateConcept)) {     // tb diagnosis date
                        observationResult.setObservation_identifier("TB_DIAGNOSIS_DATE");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    if (obs.getConcept().getConceptId().equals(TBTreatmentStartDateConcept)) {     // tb treatment start date
                        observationResult.setObservation_identifier("TB_TREATMENT_START_DATE");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    if (obs.getConcept().getConceptId().equals(TBTreatmentCompleteDateConcept)) {     // tb treatment complete date
                        observationResult.setObservation_identifier("TB_TREATMENT_COMPLETE_DATE");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                    //Greencard encounter
                    if (obs.getConcept().getConceptId().equals(WhoStageConcept)) {                       //  current who stage
                        observationResult.setObservation_identifier("WHO_STAGE");
                        observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("NM");
                        observationResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                        observationResult.setAbnormal_flags("N");
                    }
                }
            }
                //Drug order encounter
                if (drugOrderEncounter != null) {
                    for (Obs obs : drugOrderEncounter.getObs()) {
                        if (obs.getConcept().getConceptId().equals(ARVConcept)) {    //set current regimen
                            observationResult.setObservation_identifier("CURRENT_REGIMEN");
                            observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                            observationResult.setCoding_system("NASCOP_CODES");
                            observationResult.setValue_type("CE");
                            observationResult.setObservation_value(String.valueOf(obs.getValueText()));
                            observationResult.setUnits("");
                            observationResult.setObservation_result_status("F");
                            observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                            observationResult.setAbnormal_flags("N");
                        }
                        if (obs.getConcept().getConceptId().equals(ARTInitiationDateConcept)) {     // art start date
                            observationResult.setObservation_identifier("ART_START");
                            observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                            observationResult.setCoding_system("");
                            observationResult.setValue_type("DT");
                            observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                            observationResult.setUnits("");
                            observationResult.setObservation_result_status("F");
                            observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                            observationResult.setAbnormal_flags("N");
                        }
                    }
                }
                //MCH mother  encounter
                if (mchMotherEncounter != null) {
                    for (Obs obs : mchMotherEncounter.getObs()) {
                        observationResult.setObservation_identifier("PMTCT_INITIATION");          // PMTCT initiation
                        observationResult.setObservation_sub_id(String.valueOf(mchMotherEncounter.getEncounterId()));
                        observationResult.setCoding_system("");
                        observationResult.setValue_type("DT");
                        observationResult.setObservation_value(String.valueOf(mchMotherEncounter.getEncounterDatetime()));
                        observationResult.setUnits("");
                        observationResult.setObservation_result_status("F");
                        observationResult.setObservation_datetime(String.valueOf(mchMotherEncounter.getEncounterDatetime()));
                        observationResult.setAbnormal_flags("N");

                        if (obs.getConcept().getConceptId().equals(DateOfDeliveryConcept)) {          // Childbirth
                            observationResult.setObservation_identifier("CHILD_BIRTH");
                            observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                            observationResult.setCoding_system("");
                            observationResult.setValue_type("DT");
                            observationResult.setObservation_value(String.valueOf(obs.getValueDatetime()));
                            observationResult.setUnits("");
                            observationResult.setObservation_result_status("F");
                            observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                            observationResult.setAbnormal_flags("N");
                        }
                    }
                }
        //lab result encounter
                if (labResultEncounter != null) {
                    for (Obs obs : labResultEncounter.getObs()) {
                        if (obs.getConcept().getConceptId().equals(CD4Concept)) {     // cd4 count
                            observationResult.setObservation_identifier("CD4_COUNT");
                            observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                            observationResult.setCoding_system("");
                            observationResult.setValue_type("NM");
                            observationResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                            observationResult.setUnits("n/dl");
                            observationResult.setObservation_result_status("F");
                            observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                            observationResult.setAbnormal_flags("N");
                        }
                        if (obs.getConcept().getConceptId().equals(CD4PercentConcept)) {     // cd4  percent count
                            observationResult.setObservation_identifier("CD4_PERCENT");
                            observationResult.setObservation_sub_id(String.valueOf(obs.getObsId()));
                            observationResult.setCoding_system("");
                            observationResult.setValue_type("NM");
                            observationResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                            observationResult.setUnits("%");
                            observationResult.setObservation_result_status("F");
                            observationResult.setObservation_datetime(String.valueOf(obs.getObsDatetime()));
                            observationResult.setAbnormal_flags("N");
                        }
                    }
            }
            observationResults.add(observationResult);

        ilMessage.setObservation_result(observationResults.toArray(new OBSERVATION_RESULT[observationResults.size()]));
        return ilMessage;
    }

}
