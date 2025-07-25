package org.openmrs.module.kenyaemrIL.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.Months;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;
import static org.openmrs.module.kenyaemrIL.api.ILPatientRegistration.conceptService;

public class CaseSurveillanceUtils {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillanceUtils.class);
    // Cache for token and expiration
    private static String cachedToken = null;
    private static long tokenExpirationTime = 0; // Epoch time in milliseconds
    private static final ReentrantLock tokenLock = new ReentrantLock(); // Thread-safety for token updates

    public static final String BASE_CS_URL = ILMetadata.GP_CS_SERVER_BASE_URL;
    private static final String CS_TOKEN_URL = ILMetadata.GP_CS_SERVER_TOKEN_URL;
    private static final String API_CS_CLIENT_ID = ILMetadata.GP_CS_SERVER_CLIENT_ID;
    private static final String API_CS_CLIENT_SECRET = ILMetadata.GP_CS_SERVER_CLIENT_SECRET;
    private static final String PrEP_NUMBER_IDENTIFIER_TYPE_UUID = "ac64e5cb-e3e2-4efa-9060-0dd715a843a1";
    private static final int PrEP_REGIMEN_CONCEPT_ID = 164515;
    public static SSLConnectionSocketFactory createSslConnectionFactory() throws Exception {
        return new SSLConnectionSocketFactory(
                SSLContexts.createDefault(),
                new String[]{"TLSv1.2"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );
    }

    public static String getBearerToken() {
        long currentTime = System.currentTimeMillis();
        tokenLock.lock();
        try {
            // Check if a valid token exists in the cache
            if (cachedToken != null && currentTime < tokenExpirationTime) {
                log.info("Using cached Bearer token...");
                return cachedToken;
            }

            // Token is expired or not set; fetch a new one
            String username = getGlobalPropertyValue(API_CS_CLIENT_ID).trim();
            String secret = getGlobalPropertyValue(API_CS_CLIENT_SECRET).trim();
            String tokenUrl = getGlobalPropertyValue(CS_TOKEN_URL).trim();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost postRequest = new HttpPost(tokenUrl);
                postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
                postRequest.setHeader("Authorization", createBasicAuthHeader(username, secret));

                // Add form parameters
                List<BasicNameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("client_id", username));
                params.add(new BasicNameValuePair("client_secret", secret));
                params.add(new BasicNameValuePair("grant_type", "client_credentials"));
                postRequest.setEntity(new UrlEncodedFormEntity(params));

                // Execute the request
                try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpURLConnection.HTTP_OK) {
                        String responseString = EntityUtils.toString(response.getEntity()).trim();

                        // Parse the JSON response to extract the token and expiration
                        JsonObject jsonResponse = JsonParser.parseString(responseString).getAsJsonObject();
                        if (jsonResponse.has("access_token") && jsonResponse.has("expires_in")) {
                            cachedToken = jsonResponse.get("access_token").getAsString();
                            int expiresIn = jsonResponse.get("expires_in").getAsInt(); // in seconds

                            // Calculate and store the expiration time (current time + token lifetime)
                            tokenExpirationTime = currentTime + (expiresIn * 1000L); // Convert seconds to milliseconds
                            log.info("Bearer token retrieved and cached successfully.");
                            return cachedToken;
                        } else {
                            log.error("Invalid token response: {}", responseString);
                        }
                    } else {
                        log.error("Failed to fetch Bearer Token. HTTP Status: {}", statusCode);
                    }
                }
            } catch (Exception e) {
                log.error("Error retrieving Bearer Token: {}", e.getMessage(), e);
            }
        } finally {
            tokenLock.unlock(); // Ensure lock is released
        }

        return ""; // Return empty if token retrieval fails
    }

    private static String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public static String createSuccessResponse(HttpResponse response) {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException("Error parsing response", e);
        }
    }

    /**
     * Utility Method to compute age in months
     * @param birtDate
     * @param context
     * @return
     */
    public static Integer getAgeInMonths(Date birtDate, Date context) {
        DateTime d1 = new DateTime(birtDate.getTime());
        DateTime d2 = new DateTime(context.getTime());
        return Months.monthsBetween(d1, d2).getMonths();
    }

    /**
     * Utility method to get HEI number
     * @param patient
     * @return
     */
    public static String getHEINumber(Patient patient){
        PatientIdentifierType heiIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, Metadata.IdentifierType.HEI_UNIQUE_NUMBER);
        PatientIdentifier heiIdentifier = patient.getPatientIdentifier(heiIdentifierType);
        return heiIdentifier != null ? heiIdentifier.getIdentifier() : null;
    }

    /** Fetch PrEP Number for the given patient */
    public static String getPrepNumber(Patient patient) {
        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(
                PatientIdentifierType.class, PrEP_NUMBER_IDENTIFIER_TYPE_UUID
        );
        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
        return prepIdentifier != null ? prepIdentifier.getIdentifier() : null;
    }

    /** Fetch PrEP Regimen from the for patient */
    public static String getPrepRegimen(Patient patient) {
        Concept prepRegimenConcept = conceptService.getConcept(PrEP_REGIMEN_CONCEPT_ID);
        PatientWrapper patientWrapper = new PatientWrapper(patient);
        Obs obs = patientWrapper.lastObs(prepRegimenConcept);
        if (obs != null && obs.getConcept().equals(prepRegimenConcept) && obs.getValueCoded() != null) {
            return obs.getValueCoded().getName().getName();
        }
        return null;
    }
    public static boolean isBetween6And8WeeksOld(Date birthDate, Date referenceDate) {
        LocalDate birth = birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate ref = referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        long weeks = ChronoUnit.WEEKS.between(birth, ref);
        return weeks >= 6 && weeks <= 8;
    }

}
