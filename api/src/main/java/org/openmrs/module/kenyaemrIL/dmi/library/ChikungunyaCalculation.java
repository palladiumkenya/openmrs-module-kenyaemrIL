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
 * Calculates the eligibility for Chikungunya screening flag for  patients
 * @should calculate Active visit
 * @should calculate Fever
 * @should calculate Joint pains
 * @should calculate Temperature  >37.5C
 * @should calculate Duration  > 2 days
 */
public class ChikungunyaCalculation extends AbstractPatientCalculation {
	protected static final Log log = LogFactory.getLog(ChikungunyaCalculation.class);
	public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);
	public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);
	public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
	public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);
	Integer JOINT_PAIN = 116558;
	Integer TEMPERATURE = 5088;
	Integer FEVER = 140238;
	Integer DURATION = 159368;
	Integer SCREENING_QUESTION = 5219;

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

			Encounter lastHivFollowUpEncounter = EmrUtils.lastEncounter(patient, greenCardEncType, greenCardForm);   //last greencard followup form
			Encounter lastClinicalEncounter = EmrUtils.lastEncounter(patient, consultationEncType, clinicalEncounterForm);   //last clinical encounter form

			ConceptService cs = Context.getConceptService();
			Concept feverResult = cs.getConcept(FEVER);
			Concept jointPainResult = cs.getConcept(JOINT_PAIN);
			Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);

			CalculationResultMap tempMap = Calculations.lastObs(cs.getConcept(TEMPERATURE), cohort, context);

			boolean patientJointPainResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, jointPainResult) : false;
			boolean patientFeverResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, feverResult) : false;
			boolean patientJointPainResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, jointPainResult) : false;
			boolean patientFeverResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, feverResult) : false;

			Obs lastTempObs = EmrCalculationUtils.obsResultForPatient(tempMap, ptId);
			if (lastTempObs != null) {
				tempValue = lastTempObs.getValueNumeric();
			}

			if (lastHivFollowUpEncounter != null) {
				if (patientJointPainResultGreenCard && patientFeverResultGreenCard) {
					for (Obs obs : lastHivFollowUpEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getConceptId().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if (duration > 2 && tempValue != null && tempValue > 38.5) {
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
				if (patientJointPainResultClinical && patientFeverResultClinical) {
					for (Obs obs : lastClinicalEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getConceptId().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if (duration > 2 && tempValue != null && tempValue > 38.5) {
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
