package org.openmrs.module.kenyaemrIL.api.shr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import groovy.util.logging.Slf4j;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Obs;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hibernate.search.util.AnalyzerUtils.log;

@Slf4j
@Component
public class FhirConfig {
    @Autowired
    @Qualifier("fhirR4")
    private FhirContext fhirContext;

    public FhirContext getFhirContext() {
        return fhirContext;
    }
    public IGenericClient getFhirClient() throws Exception {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(ILUtils.getShrServerUrl());
        if (!ILUtils.getShrUserName().isEmpty()) {
            BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(ILUtils.getShrUserName(),
                    ILUtils.getShrPassword());
            fhirClient.registerInterceptor(authInterceptor);
        }
        return fhirClient;
    }

    public Bundle fetchPatientResource(String identifier) {
        try {
            IGenericClient client = getFhirClient();
            Bundle patientResource = client.search().forResource("Patient").where(Patient.IDENTIFIER.exactly().code(identifier))
                    .returnBundle(Bundle.class).execute();
            return patientResource;
        }
        catch (Exception e) {
            log.error(String.format("Failed fetching FHIR patient resource %s", e));
            return null;
        }
    }

    public Bundle fetchEncounterResource(Patient patient) {
        try {
            IGenericClient client = getFhirClient();
            Bundle encounterResource = client.search()
                    .forResource(Encounter.class)
                    .where(Encounter.PATIENT.hasId(patient.getIdElement().getIdPart()))
                    .include(Observation.INCLUDE_ALL)
                    .returnBundle(Bundle.class).execute();
            return encounterResource;
        }
        catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchObservationResource(Patient patient) {
        try {
            IGenericClient client = getFhirClient();
            Bundle observationResource = client.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patient.getIdElement().getIdPart()))
                    .returnBundle(Bundle.class).execute();

            //System.out.println("Observation bundle: " + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(observationResource));
            return observationResource;
        }
        catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    /**
     * Gets a patient's observations after a date provided
     * @param patient
     * @param fromDate
     * @return
     */
    public Bundle fetchObservationResource(Patient patient, Date fromDate) {
        try {
            IGenericClient client = getFhirClient();
            Bundle observationResource = client.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(patient.getIdElement().getIdPart()))
                    .count(200)
                    //.where(Observation.DATE.after().day(new SimpleDateFormat("yyyy-MM-dd").format(fromDate))) // same as encounter date
                    .returnBundle(Bundle.class).execute();
            return observationResource;
        }
        catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchConditions(Patient patient) {
        try {
            IGenericClient client = getFhirClient();
            Bundle conditionsBundle = client.search()
                    .forResource(Condition.class)
                    .where(Condition.PATIENT.hasId(patient.getIdElement().getIdPart()))
                    .returnBundle(Bundle.class).execute();
            return conditionsBundle;

        }catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchAppointments(Patient patient) {
        try {
            IGenericClient client = getFhirClient();
            Bundle conditionsBundle = client.search()
                    .forResource(Appointment.class)
                    .where(Condition.PATIENT.hasId(patient.getIdElement().getIdPart()))
                    .returnBundle(Bundle.class).execute();
            return conditionsBundle;

        }catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public List<String> vitalConcepts() {
        return Arrays.asList("5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","163300AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "1343AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> labConcepts() {
        return Arrays.asList("12AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","162202AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "1659AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","307AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> presentingComplaints() {
        return Arrays.asList("5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> diagnosisObs() {
        return Arrays.asList("6042AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }
}
