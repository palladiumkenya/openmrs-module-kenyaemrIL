package org.openmrs.module.kenyaemrIL.fragment.controller;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShrSummariesFragmentController {

    public static SimpleObject constructSHrSummary(String patientUuid) throws MalformedURLException, UnsupportedEncodingException {
        SimpleObject finalResult = new SimpleObject();
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);


        List<SimpleObject> diagnosis = new ArrayList<>();
        List<SimpleObject> conditions = new ArrayList<>();
        List<SimpleObject> allergies = new ArrayList<>();
        List<SimpleObject> vitals = new ArrayList<>();
        List<SimpleObject> labResults = new ArrayList<>();
        List<SimpleObject> complaints = new ArrayList<>();

        List<PatientIdentifier> identifiers = Context.getPatientService().getPatientByUuid(patientUuid).getActiveIdentifiers();
        List<PatientIdentifier> upi = identifiers.stream().filter(i -> i.getIdentifierType().getUuid().equals("f85081e2-b4be-4e48-b3a4-7994b69bb101")).collect(Collectors.toList());
        if (upi.isEmpty()) {
            new SimpleObject();
        }

        String patientUniqueNumber = upi.get(0).getIdentifier();

        Bundle diagnosisBundle = fhirConfig.fetchConditions(patientUniqueNumber,
                "http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis");

        Bundle conditionsBundle = fhirConfig.fetchConditions(patientUniqueNumber,
                "http://terminology.hl7.org/CodeSystem/condition-category|conditions");


        Bundle allergyBundle = fhirConfig.fetchPatientAllergies(patientUniqueNumber);

        Bundle allObs = fhirConfig.fetchObservationResource(patientUniqueNumber);

        if (!allObs.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : allObs.getEntry()) {
                Observation observation = (Observation) resource.getResource();

                if (observation.hasCode() && observation.getCode().hasCoding() && observation.hasCategory() &&
                        observation.getCategoryFirstRep().hasCoding() &&
                        observation.getCategoryFirstRep().getCodingFirstRep().getCode().equals("vital-signs")) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", observation.getId());
                    local.put("name", observation.getCode().getCodingFirstRep().getDisplay());
                    local.put("date_recorded", observation.getIssued() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(observation.getIssued()) : "");
                    local.put("value", fhirValueProcessor(observation.getValue()));
                    vitals.add(local);
                }

                if (observation.hasCode() && observation.getCode().hasCoding() && observation.hasCategory()
                        && observation.getCategoryFirstRep().hasCoding() &&
                        observation.getCategoryFirstRep().getCodingFirstRep().getCode().equals("laboratory")) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", observation.getId());
                    local.put("name", observation.getCode().getCodingFirstRep().getDisplay());
                    local.put("date_recorded", observation.getIssued() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(observation.getIssued()) : "");
                    local.put("value", fhirValueProcessor(observation.getValue()));
                    labResults.add(local);
                }

                if (observation.hasCode() && observation.getCode().hasCoding() && observation.hasCategory() &&
                        observation.getCategoryFirstRep().hasCoding() &&
                        observation.getCategoryFirstRep().getCodingFirstRep().getCode().equals("exam")) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", observation.getId());
                    local.put("name", observation.getCode().getCodingFirstRep().getDisplay());
                    local.put("date_recorded", observation.getIssued() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(observation.getIssued()) : "");
                    local.put("onset_date", "");
                    local.put("value", fhirValueProcessor(observation.getValue()));
                    complaints.add(local);
                }

            }
        }

        if (!conditionsBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : conditionsBundle.getEntry()) {
                Condition condition = (Condition) resource.getResource();
                if (condition.hasCode() && condition.getCode().hasCoding()) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", condition.getId());
                    local.put("name", condition.getCode().getCodingFirstRep().getDisplay());
                    //add onset date of the condition
                    local.put("onset_date", condition.getOnsetDateTimeType().isEmpty() ? "" :
                            fhirValueProcessor(condition.getOnsetDateTimeType()));
                    local.put("date_recorded", condition.getRecordedDate() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(condition.getRecordedDate()) : "");
                    local.put("status", condition.getClinicalStatus().isEmpty() ? "" : condition.getClinicalStatus().getCodingFirstRep().getDisplay());
                    local.put("value", condition.getCode().getCodingFirstRep().getDisplay());
                    conditions.add(local);
                }
            }
        }

        if (!diagnosisBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : diagnosisBundle.getEntry()) {
                Condition condition = (Condition) resource.getResource();
                if (condition.hasCode() && condition.getCode().hasCoding()) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", condition.getId());
                    local.put("name", condition.getCode().getCodingFirstRep().getDisplay());
                    //add date the diagnosis was made
                    local.put("date_recorded", condition.getRecordedDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(condition.getRecordedDate()) : "");
                    local.put("value", condition.getCode().getCodingFirstRep().getDisplay());
                    diagnosis.add(local);
                }
            }
        }

        if (!allergyBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : allergyBundle.getEntry()) {
                AllergyIntolerance allergyIntolerance = (AllergyIntolerance) resource.getResource();
                if (allergyIntolerance.hasCode() && allergyIntolerance.getCode().hasCoding() && allergyIntolerance.hasReaction()
                        && allergyIntolerance.getReactionFirstRep().hasSubstance() && allergyIntolerance.getReactionFirstRep().hasManifestation()) {
                    SimpleObject local = new SimpleObject();
                    local.put("uuid", allergyIntolerance.getId());
                    local.put("allergen", allergyIntolerance.getReactionFirstRep().getSubstance().getCodingFirstRep().getDisplay());
                    local.put("reaction", allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getDisplay());
                    local.put("severity", allergyIntolerance.getReactionFirstRep().getSeverity().toString());
                    local.put("onset_date", allergyIntolerance.getOnsetDateTimeType().isEmpty() ? "" :
                            fhirValueProcessor(allergyIntolerance.getOnsetDateTimeType()));
                    local.put("date_recorded", allergyIntolerance.getRecordedDate() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(allergyIntolerance.getRecordedDate()) : "");
                    allergies.add(local);
                }
            }
        }

        finalResult.put("conditions", conditions);
        finalResult.put("diagnosis", diagnosis);
        finalResult.put("allergies", allergies);
        finalResult.put("vitals", vitals);
        finalResult.put("labResults", labResults);
        finalResult.put("complaints", complaints);
        return finalResult;
    }

    private static String fhirValueProcessor(Type resource) {
        String value = "";
        if (resource instanceof CodeableConcept) {
            value = ((CodeableConcept) resource).getText();
        } else if (resource instanceof DateTimeType) {
            value = new SimpleDateFormat("yyyy-MM-dd").format(((DateTimeType) resource).toCalendar().getTime());
        } else if (resource instanceof IntegerType) {
            value = ((IntegerType) resource).getValue().toString();
        } else if (resource instanceof Quantity) {
            value = Double.toString(((Quantity) resource).getValue().doubleValue());
        } else if (resource instanceof BooleanType) {
            value = Boolean.toString(((BooleanType) resource).getValue());
        } else if (resource instanceof StringType) {
            value = ((StringType) resource).getValue();
        }
        return value;
    }

}
