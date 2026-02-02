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
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    private static final String CR_NUMBER_ID_TYPE = Metadata.IdentifierType.SHA_UNIQUE_IDENTIFICATION_NUMBER;
    public static final String DATE_INITIATED_PREP = "160555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String DATE_INITIATED_PREP_TRANSFER = "159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

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
     *
     * @param birthDate
     * @param contextDate
     * @return
     */
    public static Integer getAgeInMonths(Date birthDate, Date contextDate) {
        if (birthDate == null || contextDate == null) {
            return null;
        }
        // Convert Dates to YearMonth (ignoring day-of-month for month diff)
        YearMonth birthYM = YearMonth.from(birthDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        YearMonth contextYM = YearMonth.from(contextDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        // Count months between like MySQL TIMESTAMPDIFF(MONTH, dob, context)
        return (int) birthYM.until(contextYM, ChronoUnit.MONTHS);
    }

    /**
     * Utility method to get HEI number
     *
     * @param patient
     * @return
     */
    public static String getHEINumber(Patient patient) {
        PatientIdentifierType heiIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, Metadata.IdentifierType.HEI_UNIQUE_NUMBER);
        PatientIdentifier heiIdentifier = patient.getPatientIdentifier(heiIdentifierType);
        return heiIdentifier != null ? heiIdentifier.getIdentifier() : null;
    }

    /**
     * Fetch PrEP Number for the given patient
     */
    public static String getPrepNumber(Patient patient) {
        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(
                PatientIdentifierType.class, PrEP_NUMBER_IDENTIFIER_TYPE_UUID
        );
        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
        return prepIdentifier != null ? prepIdentifier.getIdentifier() : null;
    }

    /**
     * Fetch PrEP Regimen from the for patient
     */
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

    public static Concept getConceptByConceptId(Integer conceptId) {
        if (conceptId == null) {
            return null;
        }
        Concept concept = conceptService.getConcept(conceptId);
        if (concept == null) {
            log.warn("Concept with ID {} not found", conceptId);
        }
        return concept;
    }
    // Helper: safely handle Strings and nulls
    public static String safeToString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    // Helper: format Date or SQLDate into a String (yyyy-MM-dd)
    public static String formatDate(Object obj, DateFormat df) {
        if (obj instanceof java.sql.Date || obj instanceof java.util.Date) {
            return df.format((Date) obj);
        }
        return obj != null ? obj.toString() : null;
    }
    public static Encounter latestOnOrBefore(List<Encounter> encounters, Date cutoff) {
        if (encounters == null || encounters.isEmpty() || cutoff == null) {
            return null;
        }
        Encounter latestEnc = null;
        for (Encounter e : encounters) {
            if (e == null || e.getEncounterDatetime() == null) {
                continue;
            }
            if (e.getEncounterDatetime().after(cutoff)) {
                continue;
            }
            if (latestEnc == null || e.getEncounterDatetime().after(latestEnc.getEncounterDatetime())) {
                latestEnc = e;
            }
        }
        return latestEnc;
    }
    public static Encounter latestOnSameDay(List<Encounter> encounters, Date dayAnchor) {
        if (encounters == null || encounters.isEmpty() || dayAnchor == null) {
            return null;
        }
        Encounter best = null;
        for (Encounter e : encounters) {
            if (e == null || e.getEncounterDatetime() == null) {
                continue;
            }
            if (!isSameDay(e.getEncounterDatetime(), dayAnchor)) {
                continue;
            }
            if (best == null || e.getEncounterDatetime().after(best.getEncounterDatetime())) {
                best = e;
            }
        }
        return best;
    }
    public static boolean isSameDay(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        LocalDate da = a.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate db = b.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return da.equals(db);
    }
    public static String resolveShaNumber(Integer patientId) {
        if (patientId == null) {
            return null;
        }

        Patient patient = Context.getPatientService().getPatient(patientId);
        if (patient == null) {
            return null;
        }

        PatientIdentifierType shaIdType =
                Context.getPatientService().getPatientIdentifierTypeByUuid(CR_NUMBER_ID_TYPE);
        if (shaIdType == null) {
            return null;
        }

        PatientIdentifier shaId = patient.getPatientIdentifier(shaIdType);
        return shaId != null ? shaId.getIdentifier() : null;
    }
    // Helper for linked-to-PrEP flow: latest encounter within [from, to] inclusive
    public static Encounter latestWithin(List<Encounter> encounters, Date from, Date to) {
        if (encounters == null || encounters.isEmpty()) {
            return null;
        }
        Encounter latestEnc = null;
        for (Encounter e : encounters) {
            if (e == null || e.getEncounterDatetime() == null) {
                continue;
            }
            Date d = e.getEncounterDatetime();
            if (from != null && d.before(from)) {
                continue;
            }
            if (to != null && d.after(to)) {
                continue;
            }
            if (latestEnc == null || d.after(latestEnc.getEncounterDatetime())) {
                latestEnc = e;
            }
        }
        return latestEnc;
    }
    public static Date getPrepStartDateFromEnrollment(Encounter enrollment) {
        Date initiated = getDateValue(enrollment, DATE_INITIATED_PREP);
        Date transferred = getDateValue(enrollment, DATE_INITIATED_PREP_TRANSFER);

        if (initiated == null) return transferred;
        if (transferred == null) return initiated;

        return initiated.before(transferred) ? initiated : transferred;
    }
    public static Encounter getLatestEncounter(List<Encounter> encounters) {
        if (encounters == null || encounters.isEmpty()) {
            return null;
        }

        return encounters.stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                // Deterministic: break ties with encounterId
                .max(Comparator
                        .comparing(Encounter::getEncounterDatetime)
                        .thenComparing(e -> e.getEncounterId() == null ? Integer.MIN_VALUE : e.getEncounterId()))
                .orElse(null);
    }
    private static Obs getObs(Encounter encounter, String conceptUuid) {
        if (encounter == null) return null;
        Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
        return encounter.getObs().stream()
                .filter(o -> o.getConcept().equals(concept))
                .findFirst()
                .orElse(null);
    }
    private static final Map<Integer, String> CODED_CONCEPT_ID_TO_LABEL;
    static {
        Map<Integer, String> m = new HashMap<>();
        m.put(165203, "Start");
        m.put(1257, "Continue");
        m.put(162904, "Restart");
        m.put(1256, "Switch");
        m.put(1260, "Discontinue");
        CODED_CONCEPT_ID_TO_LABEL = java.util.Collections.unmodifiableMap(m);
    }
    public static String getCodedValue(Encounter encounter, String conceptUuid) {
        Obs obs = getObs(encounter, conceptUuid);

        if (obs == null || obs.getValueCoded() == null) {
            return null;
        }

        Integer codedConceptId = obs.getValueCoded().getConceptId();
        if (codedConceptId != null) {
            String mapped = CODED_CONCEPT_ID_TO_LABEL.get(codedConceptId);
            if (mapped != null) {
                return mapped; // "Start", "Continue", "Restart", "Switch", "Discontinue"
            }
        }
        // Fallback to the concept name if not in the mapping
        return obs.getValueCoded().getName() != null
                ? obs.getValueCoded().getName().getName()
                : null;
    }
    public static Date getDateValue(Encounter encounter, String conceptUuid) {
        Obs obs = getObs(encounter, conceptUuid);
        return obs != null ? obs.getValueDate() : null;
    }
    public static Date aMomentBefore(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() - 1000L); // 1 second earlier (DBs often store to seconds)
    }

    public static Encounter latestOf(Encounter a, Encounter b) {
        if (a == null) return b;
        if (b == null) return a;
        Date ad = a.getEncounterDatetime();
        Date bd = b.getEncounterDatetime();
        if (ad == null) return b;
        if (bd == null) return a;

        int cmp = ad.compareTo(bd);
        if (cmp > 0) return a;
        if (cmp < 0) return b;

        Integer aid = a.getEncounterId();
        Integer bid = b.getEncounterId();
        int ac = (aid == null) ? Integer.MIN_VALUE : aid;
        int bc = (bid == null) ? Integer.MIN_VALUE : bid;
        return (ac >= bc) ? a : b;
    }

    public static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
    public static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
    public static Encounter latestFromMap(Map<Integer, Encounter> map, Integer patientId) {
        return (map == null || patientId == null) ? null : map.get(patientId);
    }

    public static void mergeLatest(Map<Integer, Encounter> latestByPatientId, Encounter enc) {
        if (latestByPatientId == null || enc == null || enc.getPatient() == null || enc.getPatient().getPatientId() == null) {
            return;
        }
        if (enc.getEncounterDatetime() == null) {
            return;
        }
        Integer pid = enc.getPatient().getPatientId();
        Encounter existing = latestByPatientId.get(pid);
        latestByPatientId.put(pid, latestOf(existing, enc));
    }

    public static Date computePrepStartDateFromEnrollmentEncounters(List<Encounter> enrollmentEncounters) {
        if (enrollmentEncounters == null || enrollmentEncounters.isEmpty()) {
            return null;
        }

        List<Encounter> sorted = enrollmentEncounters.stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .sorted((a, b) -> {
                    Encounter best = latestOf(a, b); // latestOf already has datetime+encounterId tie-breaker
                    return (best == a) ? -1 : 1;     // sort desc: a before b if a is later
                })
                .collect(Collectors.toList());

        for (Encounter e : sorted) {
            Date d = CaseSurveillanceUtils.getPrepStartDateFromEnrollment(e);
            if (d != null) {
                return d;
            }
        }
        return null;
    }
}
