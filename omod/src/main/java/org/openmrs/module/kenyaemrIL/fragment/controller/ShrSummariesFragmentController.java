package org.openmrs.module.kenyaemrIL.fragment.controller;

import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.webservices.rest.SimpleObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ShrSummariesFragmentController {

    public static SimpleObject constructSHrSummary(String patientUniqueNumber) throws MalformedURLException, UnsupportedEncodingException {
        SimpleObject finalResult = new SimpleObject();
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);


        List<SimpleObject> diagnosis = new ArrayList<>();
        List<SimpleObject> conditions = new ArrayList<>();
        List<SimpleObject> allergies = new ArrayList<>();

        if (Strings.isNullOrEmpty(patientUniqueNumber)) {
            new SimpleObject();
        }
        Bundle diagnosisBundle = fhirConfig.fetchConditions(patientUniqueNumber,
                "http://hl7.org/fhir/ValueSet/condition-category|encounter-diagnosis");

        Bundle conditionsBundle = fhirConfig.fetchConditions(patientUniqueNumber,
                "http://hl7.org/fhir/ValueSet/condition-category|conditions");

        Bundle allergyBundle = fhirConfig.fetchPatientAllergies(patientUniqueNumber);

        if (!conditionsBundle.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent resource : conditionsBundle.getEntry()) {
                Condition condition = (Condition) resource.getResource();
                if (condition.hasCode() && condition.getCode().hasCoding()) {
                    SimpleObject local = new SimpleObject();
                    local.put("name", condition.getCode().getCodingFirstRep().getDisplay());
                    //add onset date of the condition
                    local.put("date_recorded", condition.getOnsetDateTimeType() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(condition.getOnsetDateTimeType().toCalendar().getTime()) : "");
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
                if (allergyIntolerance.hasCode() && allergyIntolerance.getCode().hasCoding()) {
                    SimpleObject local = new SimpleObject();
                    local.put("name", allergyIntolerance.getCode().getCodingFirstRep().getDisplay());
                    local.put("date_recorded", allergyIntolerance.getRecordedDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(allergyIntolerance.getRecordedDate()) : "");
                    local.put("value", allergyIntolerance.hasReaction() && allergyIntolerance.getReactionFirstRep()
                            .hasManifestation() ? allergyIntolerance.getReactionFirstRep().getManifestationFirstRep().getCodingFirstRep().getDisplay() : "");
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
