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
import org.openmrs.module.kenyacore.calculation.Filters;
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
 * Calculates the eligibility for Acute Watery Diarrhoeal Disease screening flag for  patients
 */
public class AcuteWateryDiarrhoealDiseaseScreeningCalculation extends AbstractPatientCalculation {
	protected static final Log log = LogFactory.getLog(AcuteWateryDiarrhoealDiseaseScreeningCalculation.class);
	public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
	public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);
	public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);
	public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);
	public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
	public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);
	Integer DURATION = 159368;
	Integer SCREENING_QUESTION = 5219;
	Integer RASH = 512;
	Integer MEASURE_FEVER = 140238;
	Integer WATERY_DIARRHEA = 161887;
	String NUMBER_OF_MOTIONS = "164456AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
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
			Double duration = 0.0;
			Double motions = 0.0;
			Date dateCreated = null;
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String todayDate = dateFormat.format(currentDate);
			Patient patient = patientService.getPatient(ptId);

			Encounter lastTriageEnc = EmrUtils.lastEncounter(patient, triageEncType, triageScreeningForm);
			Encounter lastFollowUpEncounter = EmrUtils.lastEncounter(patient, greenCardEncType, greenCardForm);   //last greencard followup form
			Encounter lastClinicalEncounter = EmrUtils.lastEncounter(patient, consultationEncType, clinicalEncounterForm);   //last clinical encounter form

			ConceptService cs = Context.getConceptService();
			Concept wateryDiarrheaResult = cs.getConcept(WATERY_DIARRHEA);
			Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);

			boolean triageEncounterHasWateryDiarrhea = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, wateryDiarrheaResult) : false;
			boolean hivFollowupEncounterHasWateryDiarrhea = lastFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastFollowUpEncounter, screeningQuestion, wateryDiarrheaResult) : false;
			boolean clinicalEncounterHasWateryDiarrhea = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, wateryDiarrheaResult) : false;

			if (lastTriageEnc != null) {
				if (triageEncounterHasWateryDiarrhea) {
					for (Obs obs : lastTriageEnc.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getUuid().equals(NUMBER_OF_MOTIONS)) {
							motions = obs.getValueNumeric();
						}
						if (obs.getConcept().getUuid().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((motions < 3)) {
								if ((duration > 0.0 && duration < 14)) {
									if (createdDate.equals(todayDate)) {
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
				if (hivFollowupEncounterHasWateryDiarrhea) {
					for (Obs obs : lastFollowUpEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getUuid().equals(NUMBER_OF_MOTIONS)) {
							motions = obs.getValueNumeric();
						}
						if (obs.getConcept().getUuid().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((motions < 3)) {
								if ((duration > 0.0 && duration < 14)) {
									if (createdDate.equals(todayDate)) {
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
				if (clinicalEncounterHasWateryDiarrhea) {
					for (Obs obs : lastClinicalEncounter.getObs()) {
						dateCreated = obs.getDateCreated();
						if (obs.getConcept().getUuid().equals(NUMBER_OF_MOTIONS)) {
							motions = obs.getValueNumeric();
						}
						if (obs.getConcept().getUuid().equals(DURATION)) {
							duration = obs.getValueNumeric();
						}
						if (dateCreated != null) {
							String createdDate = dateFormat.format(dateCreated);
							if ((motions < 3)) {
								if ((duration > 0.0 && duration < 14)) {
									if (createdDate.equals(todayDate)) {
										eligible = true;
										break;
									}
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
