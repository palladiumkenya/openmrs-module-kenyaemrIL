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
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the eligibility for Mpox screening flag for  patients
 * @should calculate temperature  for >= 38.5same day
 */
public class MpoxScreeningCalculation extends AbstractPatientCalculation {
	protected static final Log log = LogFactory.getLog(MpoxScreeningCalculation.class);
	public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
	public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);
	public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);

	public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);
	public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
	public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);

	Integer MEASURE_FEVER = 140238;
	Integer SCREENING_QUESTION = 5219;
	Integer TEMPERATURE = 5088;
	Integer RASH = 1441;
	Integer HEADACHE = 139084;
	Integer LYMPHADENOPATHY = 135488;
	Integer MYALGIA = 121;
	Integer BACKACHE = 148035;


	/**
	 * Evaluates the calculation
	 */

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		Set<Integer> alive = Filters.alive(cohort, context);
		PatientService patientService = Context.getPatientService();
		CalculationResultMap ret = new CalculationResultMap();

		for (Integer ptId : alive) {			
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
			Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);
			Concept rashResult = cs.getConcept(RASH);
			Concept headacheResult = cs.getConcept(HEADACHE);
			Concept lymphadenopathyResult = cs.getConcept(LYMPHADENOPATHY);
			Concept myalgiaResult = cs.getConcept(MYALGIA);
			Concept backacheResult = cs.getConcept(BACKACHE);


			CalculationResultMap tempMap = Calculations.lastObs(cs.getConcept(TEMPERATURE), cohort, context);
			boolean patientFeverResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, measureFeverResult) : false;
			boolean patientRashResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, rashResult) : false;
			boolean patientHeadacheResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, headacheResult) : false;
			boolean patientLymphadenopathyResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, lymphadenopathyResult) : false;
			boolean patientMyalgiaResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, myalgiaResult) : false;
			boolean patientBackacheResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, backacheResult) : false;
			boolean patientFeverResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, measureFeverResult) : false;
			boolean patientRashResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, rashResult) : false;
			boolean patientHeadacheResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, headacheResult) : false;
			boolean patientLymphadenopathyResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, lymphadenopathyResult) : false;
			boolean patientMyalgiaResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, myalgiaResult) : false;
			boolean patientBackacheResultGreenCard = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, backacheResult) : false;
			boolean patientFeverResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, measureFeverResult) : false;
			boolean patientRashResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, rashResult) : false;
			boolean patientHeadacheResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, headacheResult) : false;
			boolean patientLymphadenopathyResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, lymphadenopathyResult) : false;
			boolean patientMyalgiaResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, myalgiaResult) : false;
			boolean patientBackacheResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, backacheResult) : false;

			Obs lastTempObs = EmrCalculationUtils.obsResultForPatient(tempMap, ptId);
			if (lastTempObs != null) {
				tempValue = lastTempObs.getValueNumeric();
			}

			if (lastTriageEnc != null) {
				if (patientFeverResult && patientRashResult && (patientHeadacheResult || patientLymphadenopathyResult || patientMyalgiaResult ||patientBackacheResult)) {
					for (Obs obs : lastTriageEnc.getObs()) {
						dateCreated = obs.getDateCreated();
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if (tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
										eligible = true;
										break;
								}
							}
						}
					}
				}
			}

			if (lastFollowUpEncounter != null) {
				if (patientFeverResultGreenCard && patientRashResultGreenCard && (patientHeadacheResultGreenCard || patientLymphadenopathyResultGreenCard || patientMyalgiaResultGreenCard || patientBackacheResultGreenCard)) {
					for (Obs obs : lastFollowUpEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if (tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
									eligible = true;
									break;
								}
							}
						}
					}
				}
			}
			if (lastClinicalEncounter != null) {
				if (patientFeverResultClinical && patientRashResultClinical && (patientHeadacheResultClinical || patientLymphadenopathyResultClinical || patientMyalgiaResultClinical || patientBackacheResultClinical)) {
					for (Obs obs : lastClinicalEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if (tempValue != null && tempValue >= 38.0) {
								if (createdDate.equals(todayDate)) {
									eligible = true;
									break;
								}
							}
						}
					}
				}
			}		
			ret.put(ptId, new BooleanResult(eligible, this));
          
		}

		return ret;
	}
}
