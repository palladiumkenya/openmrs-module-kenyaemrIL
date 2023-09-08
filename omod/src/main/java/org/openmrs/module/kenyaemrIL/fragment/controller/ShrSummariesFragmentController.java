package org.openmrs.module.kenyaemrIL.fragment.controller;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
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

        List<PatientIdentifier> identifiers = Context.getPatientService().getPatientByUuid(patientUuid).getActiveIdentifiers();
        List<PatientIdentifier> upi = identifiers.stream().filter(i -> i.getIdentifierType().getUuid().equals("")).collect(Collectors.toList());
        if (upi.isEmpty()) {
            new SimpleObject();
        }
//        String upiId = upi.get(0).getIdentifier();
        Bundle diagnosisBundle = fhirConfig.fetchConditions("828a95d6-a0a2-470f-a9f5-36479b2b64fd",
                "http://hl7.org/fhir/ValueSet/condition-category|encounter-diagnosis");

        Bundle conditionsBundle = fhirConfig.fetchConditions("828a95d6-a0a2-470f-a9f5-36479b2b64fd",
                "http://hl7.org/fhir/ValueSet/condition-category|conditions");

        Bundle allergyBundle = fhirConfig.fetchPatientAllergies("MOHQ7X8WM31K3");

        if (!conditionsBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : conditionsBundle.getEntry()) {
                Condition condition = (Condition) resource.getResource();
                if (condition.getCode().hasCoding()) {
                    SimpleObject local = new SimpleObject();
                    local.put("name", condition.getCode().getCoding().get(0).getDisplay());
                    local.put("date_recorded", new SimpleDateFormat("yyyy-MM-dd").format(condition.getOnsetDateTimeType().toCalendar().getTime()));
                    local.put("value", condition.getCode().getCoding().get(0).getDisplay());
                    conditions.add(local);
                }
            }
        }

        if (!diagnosisBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : diagnosisBundle.getEntry()) {
                Condition condition = (Condition) resource.getResource();
                if (!condition.getCode().getCoding().isEmpty()) {
                    SimpleObject local = new SimpleObject();
                    local.put("name", condition.getCode().getCoding().get(0).getDisplay());
                    local.put("date_recorded", new SimpleDateFormat("yyyy-MM-dd").format(condition.getOnsetDateTimeType().toCalendar().getTime()));
                    local.put("value", condition.getCode().getCoding().get(0).getDisplay());
                    diagnosis.add(local);
                }
            }
        }

        if (!allergyBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : allergyBundle.getEntry()) {
                AllergyIntolerance allergyIntolerance = (AllergyIntolerance) resource.getResource();
                if (!allergyIntolerance.getCode().getCoding().isEmpty()) {
                    SimpleObject local = new SimpleObject();
                    local.put("name", allergyIntolerance.getCode().getCoding().get(0).getDisplay());
                    local.put("date_recorded", new SimpleDateFormat("yyyy-MM-dd").format(allergyIntolerance.getRecordedDate()));
                    local.put("value", allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getDisplay());
                    allergies.add(local);
                }
            }
        }

        finalResult.put("conditions", conditions);
        finalResult.put("diagnosis", diagnosis);
        finalResult.put("allergies", allergies);
        return finalResult;
    }

}
