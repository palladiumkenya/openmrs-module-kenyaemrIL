package org.openmrs.module.kenyaemrIL.api.shr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import groovy.util.logging.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
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
        FhirContext fhirContextNew = FhirContext.forR4();
        fhirContextNew.getRestfulClientFactory().setSocketTimeout(200 * 1000);
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(ILUtils.getShrToken());
        IGenericClient client = fhirContextNew.getRestfulClientFactory().newGenericClient(ILUtils.getShrServerUrl());
        client.registerInterceptor(authInterceptor);
        return client;
    }



    /**TODO - Change this to fetch from CR instead*/
    public Bundle fetchPatientAllergies(String identifier) {
        String url = ILUtils.getShrServerUrl() + "AllergyIntolerance?patient=Patient/"
                + identifier;
        System.out.println("Fhir: fetchAllergies ==>");
        try {
            IGenericClient client = getFhirClient();
            if (client != null) {
                System.out.println("Fhir: client is not null ==>");
                Bundle allergies = client.search()
                        .byUrl(url)
                        .returnBundle(Bundle.class).count(10000).execute();
                return allergies;
            }
        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR patient resource %s", e));
        }
        return null;
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

    public Bundle fetchConditions(String patientIdentifier, String categoryString) throws UnsupportedEncodingException, MalformedURLException {
        String encodedParam1 = URLEncoder.encode(patientIdentifier, "UTF-8");
        String encodedParam2 = URLEncoder.encode(categoryString, "UTF-8");

        String url = ILUtils.getShrServerUrl() + "Condition?subject=Patient/"
                + encodedParam1 + "&category="+encodedParam2;
        URL localUrl = new URL(url);

        try {
            IGenericClient client = getFhirClient();
            Bundle conditionsBundle = client.search()
                    .byUrl(localUrl.toString())
                    .count(1000)
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

    public Bundle fetchReferrals(String performer) {
        String url = ILUtils.getShrServerUrl() + "ServiceRequest?performer=Organization/"
                + performer + "&status=active";
        System.out.println("Fhir: fetchReferrals ==>");
        try {
            IGenericClient client = getFhirClient();
            if (client != null) {
                System.out.println("Fhir: client is not null ==>");
                Bundle serviceRequestResource = client.search()
                        .byUrl(url)
                        .returnBundle(Bundle.class).count(10000).execute();
                return serviceRequestResource;
            }
        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR patient resource %s", e));
        }
        return null;
    }

    public void updateReferral(ServiceRequest request) throws Exception {
        System.out.println("Fhir: Update Referral Data ==>");
        IGenericClient client = getFhirClient();
        if(client != null) {
            System.out.println("Fhir: client is not null ==>");
            client.update().resource(request).execute();
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