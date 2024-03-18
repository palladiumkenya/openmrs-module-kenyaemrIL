package org.openmrs.module.kenyaemrIL.dmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openmrs.*;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER;
import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.OPENMRS_ID;

public class DmiDataExchange {

	private Log log = LogFactory.getLog(DmiDataExchange.class);

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Generates the payload used to post to DMI server
	 *
	 * @param visit
	 * @return
	 */
	public static JSONArray generateDMIpostPayload(Visit visit, Date fetchDate) {

		JSONArray payload = new JSONArray();
		JSONObject payloadObj = new JSONObject();

		// Evaluate for flagged clients
		PatientCalculationContext context = Context.getService(PatientCalculationService.class).createCalculationContext();
		// 1. suspected ili case
		CalculationResult iliFlaggedResults = EmrCalculationUtils.evaluateForPatient(IliScreeningCalculation.class, null, visit.getPatient());
		Boolean iliFlagged = (Boolean) iliFlaggedResults.getValue();
		// 2. suspected sari case
		CalculationResult sariFlaggedResults = EmrCalculationUtils.evaluateForPatient(SariScreeningCalculation.class, null, visit.getPatient());
		Boolean sariFlagged = (Boolean) sariFlaggedResults.getValue();
		// 3. suspected cholera case
		CalculationResult choleraFlaggedResults = EmrCalculationUtils.evaluateForPatient(CholeraCalculation.class, null, visit.getPatient());
		Boolean choleraFlagged = (Boolean) choleraFlaggedResults.getValue();
		// 4. suspected dysentery case
		CalculationResult dysenteryFlaggedResults = EmrCalculationUtils.evaluateForPatient(DysenteryCalculation.class, null, visit.getPatient());
		Boolean dysenteryFlagged = (Boolean) dysenteryFlaggedResults.getValue();
		// 5. suspected dysentery case
		CalculationResult chikungunyaFlaggedResults = EmrCalculationUtils.evaluateForPatient(ChikungunyaCalculation.class, null, visit.getPatient());
		Boolean chikungunyaFlagged = (Boolean) chikungunyaFlaggedResults.getValue();
		// 6. suspected viral haemorrhagic fever case
		CalculationResult vhfFlaggedResults = EmrCalculationUtils.evaluateForPatient(ViralHaemorrhagicFeverCalculation.class, null, visit.getPatient());
		Boolean vhfFlagged = (Boolean) vhfFlaggedResults.getValue();
		// 7. suspected viral malaria fever case
		CalculationResult malariaFlaggedResults = EmrCalculationUtils.evaluateForPatient(MalariaCalculation.class, null, visit.getPatient());
		Boolean malariaFlagged = (Boolean) malariaFlaggedResults.getValue();
		// 8. suspected viral measles fever case
		CalculationResult measlesFlaggedResults = EmrCalculationUtils.evaluateForPatient(MeaslesCalculation.class, null, visit.getPatient());
		Boolean measlesFlagged = (Boolean) measlesFlaggedResults.getValue();
		// 9. suspected viral polio fever case
		CalculationResult polioFlaggedResults = EmrCalculationUtils.evaluateForPatient(PoliomyelitisCalculation.class, null, visit.getPatient());
		Boolean polioFlagged = (Boolean) measlesFlaggedResults.getValue();
		// 10. suspected viral polio fever case
		CalculationResult rvfFlaggedResults = EmrCalculationUtils.evaluateForPatient(RiftValleyFeverCalculation.class, null, visit.getPatient());
		Boolean rvfFlagged = (Boolean) rvfFlaggedResults.getValue();

		if (iliFlagged || sariFlagged  || choleraFlagged || dysenteryFlagged || chikungunyaFlagged || vhfFlagged || malariaFlagged || measlesFlagged || polioFlagged || rvfFlagged) {

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<SimpleObject> labs = new ArrayList<SimpleObject>();
			List<SimpleObject> complaints = new ArrayList<SimpleObject>();
			List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
			JSONObject subject = new JSONObject();
			List<SimpleObject> vitalSigns = new ArrayList<SimpleObject>();
			List<SimpleObject> riskFactors = new ArrayList<SimpleObject>();
			List<SimpleObject> vaccinations = new ArrayList<SimpleObject>();
			Patient patient = visit.getPatient();

			//Unique id
			PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
			PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
			//CaseUniqueId : Use visit ID to link complaints and diagnosis in DMI server, labs do not have visit id
			Integer caseUniqueId = visit.getVisitId();

			//Nupi id
			PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
			PatientIdentifier nupiId = patient.getPatientIdentifier(nupiIdType);
			//Patient address
			String county = "";
			String subcounty = "";
			String address = "";
			Person person = Context.getPersonService().getPerson(patient.getPatientId());
			if (person.getPersonAddress() != null) {
				county = person.getPersonAddress().getCountyDistrict() != null ? person.getPersonAddress().getCountyDistrict() : "";
				subcounty = person.getPersonAddress().getStateProvince() != null ? person.getPersonAddress().getStateProvince() : "";
				address = person.getPersonAddress().getAddress1() != null ? person.getPersonAddress().getAddress1() : "";

			}

			String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());
			String dob = patient.getBirthdate() != null ? dmiUtils.getSimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(patient.getBirthdate()) : "";
			Age age = new Age(patient.getBirthdate());
			Integer encounterId = null;
			String gender = patient.getGender();
			String encounterDate = null;
			String outPatientDate = null;
			String complaint = "";
			Integer complaintId = null;
			String onsetDate = "";
			Double duration = null;
			String diagnosisName = "";
			Integer diagnosisId = null;
			String diagnosisSystem = "CIEL";
			Integer orderId = null;
			String testName = "";
			String testResult = "";
			String temperature = "";
			String respiratoryRate = "";
			String oxygenSaturation = "";
			String riskFactor = "";
			Integer riskFactorId = null;
			String vaccination = "";
			Integer vaccinationId = null;
			Integer doses = null;

			//Set<Encounter> encounters = visit.getEncounters();
			if (visit.getEncounters() != null) {
				System.out.println("Count of encounters in visit ==> " + visit.getEncounters().size());
				for (Encounter encounter : visit.getEncounters()) {
					if (encounter != null) {
						if (encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION)) ||
							encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE)) ||
							encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.LAB_RESULTS)) ||
							encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.ILI_SURVEILLANCE)) ||
							encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.SARI_SURVEILLANCE)) ||
							encounter.getEncounterType().equals(MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION))) {
							System.out.println("Encounter of interest ==> ");
							//Diagnosis		
							DiagnosisService diagnosisService = Context.getDiagnosisService();
							List<Diagnosis> allDiagnosis = diagnosisService.getPrimaryDiagnoses(encounter);
							if (!allDiagnosis.isEmpty()) {
								for (Diagnosis diagnosisType : allDiagnosis) {
									diagnosisName = diagnosisType.getDiagnosis().getCoded().getName().getName();
									diagnosisId = diagnosisType.getDiagnosis().getCoded().getId();
									System.out.println("Diagnosis ==> ");
								}
							}


							for (Obs obs : encounter.getObs()) {
								System.out.println("Obsing ==> ");
								encounterId = obs.getEncounter().getId();
								encounterDate = sd.format(encounter.getEncounterDatetime());
								outPatientDate = sd.format(encounter.getEncounterDatetime());

								//Vital signs
								if (obs.getConcept().getConceptId().equals(5088)) {
									temperature = obs.getValueNumeric().toString();
								}
								if (obs.getConcept().getConceptId().equals(5242)) {
									respiratoryRate = obs.getValueNumeric().toString();
								}
								if (obs.getConcept().getConceptId().equals(5092)) {
									oxygenSaturation = obs.getValueNumeric().toString();
								}
								//Complaints
								if (obs.getConcept().getConceptId().equals(5219)) {
									complaint = obs.getValueCoded().getName().getName();
									complaintId = obs.getValueCoded().getConceptId();
								}
								if (obs.getConcept().getConceptId().equals(159948)) {
									onsetDate = formatter.format(obs.getValueDate());
								}
								if (obs.getConcept().getConceptId().equals(159368)) {
									duration = obs.getValueNumeric();
								}

								//Risk factors
								if (obs.getConcept().getConceptId().equals(1284)) {
									riskFactor = obs.getValueCoded().getName().getName();
									riskFactorId = obs.getValueCoded().getConceptId();
								}
								//Vaccinations
								if (obs.getConcept().getConceptId().equals(1198)) {
									vaccination = obs.getValueCoded().getName().getName();
									vaccinationId = obs.getValueCoded().getConceptId();
								}
								//labs
								if (obs.getOrder() != null) {
									if (obs.getOrder().getOrderId() != null) {
										orderId = obs.getOrder().getOrderId();
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

							//add to list
							payloadObj.put("caseUniqueId", caseUniqueId);
							payloadObj.put("hospitalIdNumber", facilityMfl);
							payloadObj.put("status", "final");
							payloadObj.put("finalOutcome", "Discharge from hospital");
							payloadObj.put("finalOutcomeDate", encounterDate);
							payloadObj.put("interviewDate", encounterDate);
							payloadObj.put("admissionDate", null);
							payloadObj.put("outpatientDate", outPatientDate);
							payloadObj.put("createdAt", encounterDate);
							payloadObj.put("updatedAt", null);
							if (person.getId() != null) {
								SimpleObject subjectObject = new SimpleObject();
								subjectObject.put("patientUniqueId", openmrsId != null ? openmrsId.getIdentifier() : "");
								subjectObject.put("nupi", nupiId != null ? nupiId.getIdentifier() : "");
								subjectObject.put("sex", gender != null ? dmiUtils.formatGender(gender) : "");
								subjectObject.put("address", address);
								subjectObject.put("dateOfBirth", dob);
								subjectObject.put("county", county);
								subjectObject.put("subCounty", subcounty);
								payloadObj.put("subject", subjectObject);
							}
							if (person.getId() != null && (temperature != null || respiratoryRate != null || oxygenSaturation != null)) {
								SimpleObject vitalSignObject = new SimpleObject();
								vitalSignObject.put("vitalSignId", encounterId);
								vitalSignObject.put("temperature", temperature);
								vitalSignObject.put("temperatureMode", temperature != null ? "Auxiliary" : "");
								vitalSignObject.put("respiratoryRate", respiratoryRate);
								vitalSignObject.put("oxygenSaturation", oxygenSaturation);
								vitalSignObject.put("oxygenSaturationMode", oxygenSaturation != null ? "Room air" : "");
								vitalSignObject.put("vitalSignDate", encounterDate);
								vitalSignObject.put("voided", false);
								vitalSigns.add(vitalSignObject);
								payloadObj.put("vitalSigns", vitalSigns);
							} else {
								payloadObj.put("vitalSigns", vitalSigns);
							}
							if (diagnosisId != null && diagnosisName != "") {
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
							if (person.getId() != null && riskFactor != "") {
								SimpleObject riskFactorsObject = new SimpleObject();
								riskFactorsObject.put("riskFactorId", riskFactorId);
								riskFactorsObject.put("condition", riskFactor);
								riskFactorsObject.put("voided", false);
								riskFactors.add(riskFactorsObject);
								payloadObj.put("riskFactors", riskFactors);
							} else {
								payloadObj.put("riskFactors", riskFactors);
							}
							if (person.getId() != null && vaccination != "") {
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
							if (complaintId != null && complaint != "") {
								SimpleObject complaintObject = new SimpleObject();
								complaintObject.put("complaintId", complaintId);
								complaintObject.put("complaint", complaint);
								complaintObject.put("voided", false);
								complaintObject.put("onsetDate", onsetDate);
								complaintObject.put("duration", duration);
								complaints.add(complaintObject);
								payloadObj.put("complaintDtoList", complaints);
							} else {
								payloadObj.put("complaintDtoList", complaints);
							}
							if (orderId != null && testName != "") {
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
								payloadObj.put("labDtoList", labs);
							} else {
								payloadObj.put("labDtoList", labs);
							}


							Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);

						}
					}
				}

				System.out.println("Payload generated: " + payload);
				payload.add(payloadObj);
			}

		}
		return payload;
	}
}


