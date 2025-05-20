package org.openmrs.module.kenyaemrIL.dmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openmrs.*;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.dmi.library.*;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.common.Age;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER;
import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.OPENMRS_ID;

public class DmiDataExchange {

	private Log log = LogFactory.getLog(DmiDataExchange.class);

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Generates the payload used to post to DMI server	 *
	 *
	 * @param visit
	 * @return
	 */
	public static JSONArray generateDMIpostPayload(Visit visit, Date fetchDate) {

		JSONArray payload = new JSONArray();
		JSONObject payloadObj = new JSONObject();
		Patient patient = visit.getPatient();
		// Evaluate for flagged clients
		Map<Integer, String> conditionMap = new HashMap<>();
		String conditionName = "";
		Integer conditionId = null;

		// 1. suspected ili case
		CalculationResult iliFlaggedResults = EmrCalculationUtils.evaluateForPatient(IliScreeningCalculation.class, null, patient);
		if (!iliFlaggedResults.isEmpty()) {
			conditionName = "Influenza Like Illness";
			conditionId = 1;
			conditionMap.put(conditionId, conditionName);
		}
		// 2. suspected sari case
		CalculationResult sariFlaggedResults = EmrCalculationUtils.evaluateForPatient(SariScreeningCalculation.class, null, patient);
		if (!sariFlaggedResults.isEmpty()) {
			conditionName = "Severe Acute Respiratory Infection";
			conditionId = 2;
			conditionMap.put(conditionId, conditionName);
		}
		// 3. suspected jaundice case
		CalculationResult jaundiceFlaggedResults = EmrCalculationUtils.evaluateForPatient(AcuteJaundiceScreeningCalculation.class, null, patient);
		if (!jaundiceFlaggedResults.isEmpty()) {
			conditionName = "JAUNDICE";
			conditionId = 3;
			conditionMap.put(conditionId, conditionName);
		}
		// 4. suspected viral haemorrhagic fever case
		CalculationResult vhfFlaggedResults = EmrCalculationUtils.evaluateForPatient(ViralHaemorrhagicFeverCalculation.class, null, patient);
		if (!vhfFlaggedResults.isEmpty()) {
			conditionName = "VIRAL HAEMORRHAGIC FEVER";
			conditionId = 4;
			conditionMap.put(conditionId, conditionName);
		}
		// 5. suspected meningitis
		CalculationResult meningitisFlaggedResults = EmrCalculationUtils.evaluateForPatient(AcuteMeningitisScreeningCalculation.class, null, patient);
		if (!meningitisFlaggedResults.isEmpty()) {
			conditionName = "ACUTE MENINGITIS AND ENCEPHALITIS";
			conditionId = 5;
			conditionMap.put(conditionId, conditionName);
		}
		// 6. Acute Febrile Rash Infection case
		CalculationResult acuteFebrileRash = EmrCalculationUtils.evaluateForPatient(AcuteFebrileRashInfectionScreeningCalculation.class, null, patient);
		if (!acuteFebrileRash.isEmpty()) {
			conditionName = "Acute Febrile Rash Infections";
			conditionId = 6;
			conditionMap.put(conditionId, conditionName);
		}

		// 7. suspected Neurological Syndrome Screening Calculation
		CalculationResult neurologicalFlaggedResults = EmrCalculationUtils.evaluateForPatient(NeurologicalSyndromeScreeningCalculation.class, null, patient);
		if (!neurologicalFlaggedResults.isEmpty()) {
			conditionName = "Neurological Syndrome";
			conditionId = 7;
			conditionMap.put(conditionId, conditionName);
		}
		// 8. Acute Watery Diarrhoeal Disease Screening Calculation
		CalculationResult acuteWateryDiarrhoea = EmrCalculationUtils.evaluateForPatient(AcuteWateryDiarrhoealDiseaseScreeningCalculation.class, null, patient);
		if (!acuteWateryDiarrhoea.isEmpty()) {
			conditionName = "Acute Watery Diarrhoea";
			conditionId = 8;
			conditionMap.put(conditionId, conditionName);
		}
		// 9. Acute Flaccid Paralysis
		CalculationResult polioFlaggedResults = EmrCalculationUtils.evaluateForPatient(PoliomyelitisCalculation.class, null, patient);
		if (!polioFlaggedResults.isEmpty()) {
			conditionName = "Acute Flaccid Paralysis";
			conditionId = 9;
			conditionMap.put(conditionId, conditionName);
		}
		// 10. Mpox
		CalculationResult mpoxFlaggedResults = EmrCalculationUtils.evaluateForPatient(MpoxScreeningCalculation.class, null, patient);
		if (!mpoxFlaggedResults.isEmpty()) {
			conditionName = "Mpox";
			conditionId = 10;
			conditionMap.put(conditionId, conditionName);
		}
		// 11. suspected measles case
		CalculationResult acuteFebrileIllnessScreeningCalculation = EmrCalculationUtils.evaluateForPatient(AcuteFebrileIllnessCalculation.class, null, patient);
		if (!acuteFebrileIllnessScreeningCalculation.isEmpty()) {
			conditionName = "Acute Febrile Illness";
			conditionId = 11;
			conditionMap.put(conditionId, conditionName);
		}
		if (!conditionMap.isEmpty()) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<SimpleObject> labs = new ArrayList<SimpleObject>();
			List<SimpleObject> complaints = new ArrayList<SimpleObject>();
			List<SimpleObject> conditionFlagged = new ArrayList<SimpleObject>();
			List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
			JSONObject subject = new JSONObject();
			List<SimpleObject> vitalSigns = new ArrayList<SimpleObject>();
			List<SimpleObject> riskFactors = new ArrayList<SimpleObject>();
			List<SimpleObject> vaccinations = new ArrayList<SimpleObject>();

			//Unique id
			PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
			PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
			//CaseUniqueId : Use visit ID to link complaints and diagnosis in DMI server, labs do not have visit id
			String caseUniqueId = visit.getVisitId().toString();

			//Nupi id
			PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
			PatientIdentifier nupiId = patient.getPatientIdentifier(nupiIdType);
			//Patient address
			String county = null;
			String subcounty = null;
			String address = null;
			Person person = Context.getPersonService().getPerson(patient.getPatientId());
			if (person.getPersonAddress() != null) {
				county = person.getPersonAddress().getCountyDistrict() != null ? person.getPersonAddress().getCountyDistrict() : null;
				subcounty = person.getPersonAddress().getStateProvince() != null ? person.getPersonAddress().getStateProvince() : null;
				address = person.getPersonAddress().getAddress1() != null ? person.getPersonAddress().getAddress1() : null;

			}

			String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());
			String dob = patient.getBirthdate() != null ? dmiUtils.getSimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(patient.getBirthdate()) : null;
			Age age = new Age(patient.getBirthdate());
			Integer encounterId = null;
			String gender = patient.getGender();
			String encounterDate = null;
			String outPatientDate = null;
			Map<Integer, String> complaintsMap = new HashMap<>();
			String complaint = null;
			Integer complaintId = null;
			String onsetDate = null;
			Double duration = null;
			String diagnosisName = null;
			String diagnosisId = null;
			String diagnosisSystem = "CIEL";
			String orderId = null;
			String testName = null;
			String testResult = null;
			Double temperature = null;
			Integer respiratoryRate = null;
			Integer oxygenSaturation = null;
			String riskFactor = null;
			String riskFactorId = null;
			String vaccination = null;
			String vaccinationId = null;
			String finalOutcome = null;
			String finalOutcomeDate = null;
			String status = "preliminary";
			Integer doses = null;

			//Set<Encounter> encounters = visit.getEncounters();
			if (!visit.getEncounters().isEmpty()) {				
				for (Encounter encounter : visit.getEncounters()) {
					if (encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION)) ||
						encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE)) ||
						encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.LAB_RESULTS)) ||
						encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.ILI_SURVEILLANCE)) ||
						encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.SARI_SURVEILLANCE)) ||
						encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION))) {

						//Diagnosis		
						//Gets final and preliminary diagnosis
						DiagnosisService diagnosisService = Context.getDiagnosisService();
				
						List<Diagnosis> allDiagnosis = diagnosisService.getDiagnosesByEncounter(encounter, false, false);
						if (!allDiagnosis.isEmpty()) {
							for (Diagnosis diagnosisType : allDiagnosis) {
								if(diagnosisType.getCertainty().equals(ConditionVerificationStatus.CONFIRMED)){							
								diagnosisName = diagnosisType.getDiagnosis().getCoded().getName().getName();
								diagnosisId = diagnosisType.getDiagnosis().getCoded().getId().toString();	
								status = "final";								
						    	}else {
									diagnosisName = diagnosisType.getDiagnosis().getCoded().getName().getName();
									diagnosisId = diagnosisType.getDiagnosis().getCoded().getId().toString();
									status = "preliminary";									
								}
							}
						}

						for (Obs obs : encounter.getObs()) {
							encounterId = obs.getEncounter().getId();
							encounterDate = sd.format(encounter.getEncounterDatetime());
							outPatientDate = sd.format(encounter.getEncounterDatetime());
							//Vital signs
							if (obs.getConcept().getConceptId().equals(5088)) {
								temperature = obs.getValueNumeric();
							}
							if (obs.getConcept().getConceptId().equals(5242)) {
								respiratoryRate = obs.getValueNumeric().intValue();
							}
							if (obs.getConcept().getConceptId().equals(5092)) {
								oxygenSaturation = obs.getValueNumeric().intValue();
							}
							//Complaints
							if (obs.getConcept().getConceptId().equals(5219)) {
								complaint = obs.getValueCoded().getName().getName();
								complaintId = obs.getValueCoded().getConceptId();
								complaintsMap.put(complaintId, complaint);
							}
							if (obs.getConcept().getConceptId().equals(159948)) {
								onsetDate = obs.getValueDate() != null ? sd.format(obs.getValueDate()) : null;
							}
							if (obs.getConcept().getConceptId().equals(159368)) {
								duration = obs.getValueNumeric();
							}
							//Final outcome
							if (obs.getConcept().getConceptId().equals(160433)) {
								Concept finalOutcomeAnswerConcept = obs.getValueCoded();
								if (finalOutcomeAnswerConcept.getConceptId().equals(160429)) {
									finalOutcome = "Discharge from hospital";
									finalOutcomeDate = sd.format(obs.getObsDatetime());
								} else if (finalOutcomeAnswerConcept.getConceptId().equals(1693)) {
									finalOutcome = "Referred to another facility";
									finalOutcomeDate = sd.format(obs.getObsDatetime());
								} else if (finalOutcomeAnswerConcept.getConceptId().equals(159)) {
									finalOutcome = "Death";
									finalOutcomeDate = sd.format(obs.getObsDatetime());
								}
							}
							//Risk factors
							if (obs.getConcept().getConceptId().equals(1284)) {
								riskFactor = obs.getValueCoded().getName().getName();
								riskFactorId = obs.getValueCoded().getConceptId().toString();
							}
							//Vaccinations
							if (obs.getConcept().getConceptId().equals(1198)) {
								vaccination = obs.getValueCoded().getName().getName();
								vaccinationId = obs.getValueCoded().getConceptId().toString();
							}
							//labs
							if (obs.getOrder() != null) {
								if (obs.getOrder().getOrderId() != null) {
									orderId = obs.getOrder().getOrderId().toString();
									testName = obs.getConcept().getName().getName();
									if (obs.getValueCoded() != null) {
										testResult = obs.getValueCoded().getName().getName();
									} else if (obs.getValueNumeric() != null) {
										testResult = obs.getValueNumeric().toString();
									} else if (obs.getValueText() != null) {
										testResult = obs.getValueText();
									}
								}
							}
						}
						Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);

					}
				}
				if (!complaintsMap.isEmpty()) {
					//add to list
					payloadObj.put("caseUniqueId", caseUniqueId);
					payloadObj.put("hospitalIdNumber", facilityMfl);
					payloadObj.put("status", status);
					payloadObj.put("finalOutcome", finalOutcome);
					payloadObj.put("finalOutcomeDate", finalOutcomeDate);
					payloadObj.put("interviewDate", encounterDate);
					payloadObj.put("admissionDate", null);
					payloadObj.put("outpatientDate", outPatientDate);
					payloadObj.put("createdAt", encounterDate);
					payloadObj.put("updatedAt", null);
					if (person.getId() != null) {
						SimpleObject subjectObject = new SimpleObject();
						subjectObject.put("patientUniqueId", openmrsId != null ? openmrsId.getIdentifier() : null);
						subjectObject.put("nupi", nupiId != null ? nupiId.getIdentifier() : null);
						subjectObject.put("sex", gender != null ? dmiUtils.formatGender(gender) : null);
						subjectObject.put("address", address);
						subjectObject.put("dateOfBirth", dob);
						subjectObject.put("county", county);
						subjectObject.put("subCounty", subcounty);
						payloadObj.put("subject", subjectObject);
					}
					if (person.getId() != null && riskFactor != null) {
						SimpleObject riskFactorsObject = new SimpleObject();
						riskFactorsObject.put("riskFactorId", riskFactorId);
						riskFactorsObject.put("condition", riskFactor);
						riskFactorsObject.put("voided", false);
						riskFactors.add(riskFactorsObject);
						payloadObj.put("riskFactors", riskFactors);
					} else {
						payloadObj.put("riskFactors", riskFactors);
					}
					if (person.getId() != null && vaccination != null) {
						SimpleObject vaccinationsObject = new SimpleObject();
						vaccinationsObject.put("vaccinationId", vaccinationId);
						vaccinationsObject.put("vaccination", vaccination);
						vaccinationsObject.put("doses", null);
						vaccinationsObject.put("verified", true);
						vaccinationsObject.put("voided", false);
						vaccinations.add(vaccinationsObject);
						payloadObj.put("vaccinations", vaccinations);
					} else {
						payloadObj.put("vaccinations", vaccinations);
					}
					if (orderId != null && testName != null) {
						SimpleObject labsObject = new SimpleObject();
						labsObject.put("orderId", orderId);
						labsObject.put("testName", testName);
						labsObject.put("unit", " ");
						labsObject.put("upperLimit", null);
						labsObject.put("lowerLimit", null);
						labsObject.put("testResult", testResult);
						labsObject.put("labDate", encounterDate);
						labsObject.put("voided", false);
						labs.add(labsObject);
						payloadObj.put("lab", labs);
					} else {
						payloadObj.put("lab", labs);
					}
					if (person.getId() != null && (temperature != null || respiratoryRate != null || oxygenSaturation != null)) {
						SimpleObject vitalSignObject = new SimpleObject();
						vitalSignObject.put("vitalSignId", encounterId.toString());
						vitalSignObject.put("temperature", temperature);
						vitalSignObject.put("temperatureMode", temperature != null ? "Auxiliary" : null);
						vitalSignObject.put("respiratoryRate", respiratoryRate);
						vitalSignObject.put("oxygenSaturation", oxygenSaturation);
						vitalSignObject.put("oxygenSaturationMode", oxygenSaturation != null ? "Room air" : null);
						vitalSignObject.put("vitalSignDate", encounterDate);
						vitalSignObject.put("voided", false);
						vitalSigns.add(vitalSignObject);
						payloadObj.put("vitalSigns", vitalSigns);
					} else {
						payloadObj.put("vitalSigns", vitalSigns);
					}
					if (diagnosisId != null && diagnosisName != null) {
						SimpleObject diagnosisObject = new SimpleObject();
						diagnosisObject.put("diagnosisId", diagnosisId);
						diagnosisObject.put("diagnosisDate", encounterDate);
						diagnosisObject.put("diagnosis", diagnosisName);
						diagnosisObject.put("system", diagnosisSystem);
						diagnosisObject.put("systemCode", diagnosisId);
						diagnosisObject.put("voided", false);
						diagnosis.add(diagnosisObject);
						payloadObj.put("diagnosis", diagnosis);
					} else {
						payloadObj.put("diagnosis", diagnosis);
					}
					if (!complaintsMap.isEmpty()) {
						complaintsMap.remove(1065);
						for (Map.Entry<Integer, String> complaintsEntry : complaintsMap.entrySet()) {
							SimpleObject complaintObject = new SimpleObject();
							complaintObject.put("complaintId", complaintsEntry.getKey().toString());
							complaintObject.put("complaint", complaintsEntry.getValue());
							complaintObject.put("voided", false);
							complaintObject.put("onsetDate", onsetDate);
							complaintObject.put("duration", duration);
							complaints.add(complaintObject);
							payloadObj.put("complaints", complaints);
						}
					} else {
						payloadObj.put("complaints", complaints);
					}
					if (!conditionMap.isEmpty()) {
						for (Map.Entry<Integer, String> conditionEntry : conditionMap.entrySet()) {
							SimpleObject conditionsObject = new SimpleObject();
							conditionsObject.put("conditionId", conditionEntry.getKey());
							conditionsObject.put("conditionName", conditionEntry.getValue());
							conditionsObject.put("voided", false);
							conditionFlagged.add(conditionsObject);
							payloadObj.put("flaggedConditions", conditionFlagged);
						}
					} else {
						payloadObj.put("flaggedConditions", conditionFlagged);
					}
					
					payload.add(payloadObj);
				}
			}

		}
		return payload;
	}
}


