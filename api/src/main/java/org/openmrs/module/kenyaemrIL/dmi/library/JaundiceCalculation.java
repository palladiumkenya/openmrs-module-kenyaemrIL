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
import java.util.*;

public class JaundiceCalculation extends AbstractPatientCalculation {
    protected static final Log log = LogFactory.getLog(JaundiceCalculation.class);

    public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
    public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);
    public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);
    public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);
    public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
    public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);
    Integer JAUNDICE = 136443;
    Integer SCREENING_QUESTION = 162737;

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
        Set<Integer> alive = Filters.alive(cohort, context);
        PatientService patientService = Context.getPatientService();
        CalculationResultMap ret = new CalculationResultMap();
        for (Integer ptId : alive) {
            boolean result = false;
            Date dateCreated = null;
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayDate = dateFormat.format(currentDate);
            Patient patient = patientService.getPatient(ptId);

            Encounter lastTriageEnc = EmrUtils.lastEncounter(patient, triageEncType, triageScreeningForm);
            Encounter lastClinicalEncounter = EmrUtils.lastEncounter(patient, consultationEncType, clinicalEncounterForm); //last clinical encounter form
            Encounter lastGreenCardEnc = EmrUtils.lastEncounter(patient, greenCardEncType, greenCardForm);   //last greencard followup form
            ConceptService cs = Context.getConceptService();
            Concept jaundiceResult = cs.getConcept(JAUNDICE);
            Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);

            boolean patientJaundiceTriageEncResult = lastTriageEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastTriageEnc, screeningQuestion, jaundiceResult) : false;
            boolean patientJaundiceClinicalEncResult = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, jaundiceResult) : false;
            boolean patientJaundiceGreenCardResult = lastGreenCardEnc != null ? EmrUtils.encounterThatPassCodedAnswer(lastGreenCardEnc, screeningQuestion, jaundiceResult) : false;

            if (lastClinicalEncounter != null) {
                for (Obs obs : lastClinicalEncounter.getObs()) {
                    if (patientJaundiceClinicalEncResult) {
                        dateCreated = obs.getDateCreated();
                        if (dateCreated != null) {
                            String createdDate = dateFormat.format(dateCreated);
                            if (createdDate.equals(todayDate)) {
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (lastGreenCardEnc != null) {
                for (Obs obs : lastGreenCardEnc.getObs()) {
                    if (patientJaundiceGreenCardResult) {
                        dateCreated = obs.getDateCreated();
                        if (dateCreated != null) {
                            String createdDate = dateFormat.format(dateCreated);
                            if (createdDate.equals(todayDate)) {
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (lastTriageEnc != null) {
                for (Obs obs : lastTriageEnc.getObs()) {
                    if (patientJaundiceTriageEncResult) {
                        dateCreated = obs.getDateCreated();
                        if (dateCreated != null) {
                            String createdDate = dateFormat.format(dateCreated);
                            if (createdDate.equals(todayDate)) {
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }

            ret.put(ptId, new BooleanResult(result, this));
        }
        return ret;
    }
}
