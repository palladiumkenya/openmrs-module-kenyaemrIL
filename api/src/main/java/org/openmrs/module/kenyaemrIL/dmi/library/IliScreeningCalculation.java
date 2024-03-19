/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrIL.dmi.library;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.*;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Calculates the eligibility for ILI screening flag for  patients
 * @should calculate cough for <= 10 days
 * @should calculate fever for <= 10 days
 * @should calculate temperature  for >= 38.0 same day
 * @should calculate not admitted
 * @should calculate duration < 10 days
 */
public class IliScreeningCalculation extends AbstractPatientCalculation {
	protected static final Log log = LogFactory.getLog(IliScreeningCalculation.class);
	public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
	public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);
	public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);

	public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);
	public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
	public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);

	Integer MEASURE_FEVER = 140238;
	Integer COUGH_PRESENCE = 143264;
	Integer DURATION = 159368;
	Integer SCREENING_QUESTION = 5219;
	Integer TEMPERATURE = 5088;
	Integer PATIENT_OUTCOME = 160433;
	Integer INPATIENT_ADMISSION = 1654;

	/**
	 * Evaluates the calculation
	 */

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		Set<Integer> alive = Filters.alive(cohort, context);
		PatientService patientService = Context.getPatientService();
		CalculationResultMap ret = new CalculationResultMap();

		for (Integer ptId : alive) {
			System.out.println("Checking ili eligibility ==>"+patientService.getPatient(ptId));
			boolean eligible = false;
			Date currentDate = new Date();
			Double tempValue = 0.0;
			Double duration = 0.0;
			Date dateCreated = null;
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String todayDate = dateFormat.format(currentDate);
			Patient patient = patientService.getPatient(ptId);

			Encounter lastTriageEnc = EmrUtils.lastEncounter(patient, triageEncType, triageScreeningForm);
			Encounter lastFollowUpEncounter = EmrUtils.lastEncounter(patient, greenCardEncType, greenCardForm);   //last greencard followup form
			Encounter lastClinicalEncounter = EmrUtils.lastEncounter(patient, consultationEncType, clinicalEncounterForm);   //last clinical encounter form

			ConceptService cs = Context.getConceptService();
			Concept measureFeverResult = cs.getConcept(MEASURE_FEVER);
			Concept coughPresenceResult = cs.getConcept(COUGH_PRESENCE);
			Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);
			Concept adminQuestion = cs.getConcept(PATIENT_OUTCOME);
			Concept admissionAnswer = cs.getConcept(INPATIENT_ADMISSION);

			CalculationResultMap tempMap = Calculations.lastObs(cs.getConcept(TEMPERATURE), cohort, context);
			boolean patientFeverResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, measureFeverResult) : false;
			boolean patientCoughResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, coughPresenceResult) : false;
			boolean patientFeverResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, measureFeverResult) : false;
			boolean patientCoughResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, coughPresenceResult) : false;
			boolean patientFeverResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, measureFeverResult) : false;
			boolean patientCoughResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, coughPresenceResult) : false;
			//Check admission status : Only found in clinical encounter
			boolean patientAdmissionStatus = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, adminQuestion, admissionAnswer) : false;

			Obs lastTempObs = EmrCalculationUtils.obsResultForPatient(tempMap, ptId);
			if (lastTempObs != null) {
				tempValue = lastTempObs.getValueNumeric();
			}

			if (lastTriageEnc != null) {
				if (patientFeverResult && patientCoughResult) {
					for (Obs obs : lastTriageEnc.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getConceptId().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((duration > 0.0 && duration < 10) && tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
									if (!patientAdmissionStatus) {
										eligible = true;
										break;
									}
								}
							}
						}
					}
				}
			}

			if (lastFollowUpEncounter != null) {
				if (patientFeverResultGreenCard && patientCoughResultGreenCard) {
					for (Obs obs : lastFollowUpEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getConceptId().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((duration > 0.0 && duration < 10) && tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
									if (!patientAdmissionStatus) {
										eligible = true;
										break;
									}
								}
							}
						}
					}
				}
			}
			if (lastClinicalEncounter != null) {
				if (patientFeverResultClinical && patientCoughResultClinical) {
					for (Obs obs : lastClinicalEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getConceptId().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((duration > 0.0 && duration < 10) && tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
									if (!patientAdmissionStatus) {
										eligible = true;
										break;
									}

								}
							}
						}
					}
				}
			}
			System.out.println("Eligibility ili==>"+eligible);
			ret.put(ptId, new BooleanResult(eligible, this));
          
		}

		return ret;
	}
}
