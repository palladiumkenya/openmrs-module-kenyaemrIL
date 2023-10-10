package org.openmrs.module.kenyaemrIL.api.shr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import groovy.util.logging.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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


    /**
     * TODO - Change this to fetch from CR instead
     */
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

    private void makeCall(String params) {
        try {
            String authToken = ILUtils.getShrToken();

            System.out.println("Using NUPI POST URL: " + params);
            URL url = new URL(ILUtils.getShrServerUrl() + "ServiceRequest?_search");

            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            con.setRequestProperty("Authorization", "Bearer " + authToken);

            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("charset", "utf-8");
            con.setConnectTimeout(10000); // set timeout to 10 seconds

            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(params.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader in = null;
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String stringResponse = response.toString();
                System.out.println("Got the Response as: " + stringResponse);

            } else {
                String stringResponse = "";
                if (con != null && con.getErrorStream() != null) {
                    BufferedReader in = null;
                    in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    stringResponse = response.toString();
                } else {
                    System.out.println("Could not get error stream");
                }

                // update the patient with verification error
                if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    System.out.println("BD REQUEST");
                } else {
                    System.out.println("NTW ERROR");
                }

                System.out.println("Error getting NUPI for client: " + responseCode + " : " + stringResponse);
            }
        } catch (Exception ex) {
            System.err.println("Error getting NUPI for client: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public Bundle fetchPatientReferrals(String identifier) {
        String url = ILUtils.getShrServerUrl() + "ServiceRequest?patient=Patient/"
                + identifier;
        System.out.println("Fhir: fetchAllergies ==>");
        try {
            IGenericClient client = getFhirClient();
            if (client != null) {
                System.out.println("Fhir: client is not null ==>");
                Bundle referrals = client.search()
                        .byUrl(url)
                        .returnBundle(Bundle.class).count(10000).execute();
                return referrals;
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
        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchObservationResource(String patientId) {
        try {
            String encodedParam2 = URLEncoder.encode(patientId, "UTF-8");

            String url = ILUtils.getShrServerUrl() + "Observation?subject:Patient=Patient/"
                    + patientId;
            URL localUrl = new URL(url);

            IGenericClient client = getFhirClient();
            Bundle obsBundle = client.search()
                    .byUrl(localUrl.toString())
                    .count(1000)
                    .returnBundle(Bundle.class).execute();
            return obsBundle;

        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    /**
     * Gets a patient's observations after a date provided
     *
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
        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchConditions(String patientIdentifier, String categoryString) throws UnsupportedEncodingException, MalformedURLException {
        String encodedParam1 = URLEncoder.encode(patientIdentifier, "UTF-8");
        String encodedParam2 = URLEncoder.encode(categoryString, "UTF-8");

        String url = ILUtils.getShrServerUrl() + "Condition?subject:Patient=Patient/"
                + encodedParam1 + "&category=" + encodedParam2;
        URL localUrl = new URL(url);

        try {
            IGenericClient client = getFhirClient();
            Bundle conditionsBundle = client.search()
                    .byUrl(localUrl.toString())
                    .count(1000)
                    .returnBundle(Bundle.class).execute();
            return conditionsBundle;

        } catch (Exception e) {
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

        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR encounter resource %s", e));
            return null;
        }
    }

    public Bundle fetchReferrals(String performer) {
        String url = ILUtils.getShrServerUrl() + "ServiceRequest?performer:identifier=" + performer + "&status=active";
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

    public Bundle fetchAllReferralsByFacility(String mflCode) {
        String url = ILUtils.getShrServerUrl() + "ServiceRequest?status=active&performer:identifier="+mflCode;
        System.out.println("Fhir: fetchReferrals ==> "+ url);
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

    public Resource fetchFhirResource(String resourceType, String resourceId) {
        try {
            IGenericClient client = getFhirClient();
            IBaseResource resource = client.read().resource(resourceType).withId(resourceId).execute();
            return (Resource) resource;
        } catch (Exception e) {
            log.error(String.format("Failed fetching FHIR %s resource with Id %s: %s", resourceType, resourceId, e));
            return null;
        }
    }

    public void updateReferral(ServiceRequest request) throws Exception {
        System.out.println("Fhir: Update Referral Data ==>");
        IGenericClient client = getFhirClient();
        if (client != null) {
            System.out.println("Fhir: client is not null ==>");
            MethodOutcome outcome = client.update().resource(request).execute();
            System.out.printf("after UPDATE ========================" +outcome.getOperationOutcome());
        }
    }

    public void postReferralResourceToOpenHim(ServiceRequest fhirResource) throws Exception {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(getOpenhimServerUrl());
        String oauthToken = ILUtils.getShrToken();
        oauthToken = "Bearer " + oauthToken;

        /* Todo: Add Oauth2 logic and append to request headers */
        String message = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(fhirResource);
        StringEntity fhirResourceEntity = new StringEntity(message);
        httpPost.setEntity(fhirResourceEntity);
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setHeader("Authorization", oauthToken);

        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            System.out.println("FHIR resource was successfully posted to the OpenHIM channel");
        } else {
            String responseBody = response.getEntity().toString();
            System.out.println("An error occurred while posting the FHIR resource to the OpenHIM channel. Status code: "
                    + statusCode + " Response body: " + responseBody);
        }
    }

    public String getOpenhimServerUrl() {
        String baseUrl = getOpenhimBaseUrl();
        String suffixUrl = getOpenhimSuffixUrl();
        if (baseUrl == null || suffixUrl == null) {
            throw new IllegalArgumentException("OpenHIM URL is invalid: baseUrl or suffixUrl is null");
        }
        return baseUrl + suffixUrl + "/service-request";
    }

    private String getOpenhimBaseUrl() {
        return Context.getAdministrationService().getGlobalProperty("interop.openhimBaseURL");
    }

    private String getOpenhimSuffixUrl() {
        return Context.getAdministrationService().getGlobalProperty("interop.openhimBaseURLSuffix");
    }

    public List<String> vitalConcepts() {
        return Arrays.asList("5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "163300AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "1343AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> labConcepts() {
        return Arrays.asList("12AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "162202AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "1659AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "307AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> presentingComplaints() {
        return Arrays.asList("5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    public List<String> diagnosisObs() {
        return Arrays.asList("6042AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }
}