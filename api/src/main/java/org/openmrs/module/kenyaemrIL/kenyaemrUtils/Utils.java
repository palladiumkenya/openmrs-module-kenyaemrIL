package org.openmrs.module.kenyaemrIL.kenyaemrUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.util.PrivilegeConstants;

import java.util.*;

public class Utils {

    /**
     * Get the last n observations for a given concept
     * @param concept
     * @param patient
     * @param nLast
     * @return
     * @throws Exception
     */
    public static List<Obs> getNLastObs(Concept concept, Patient patient, Integer nLast) throws Exception {
        List<Obs> obs = Context.getObsService().getObservations(
                Arrays.asList(Context.getPersonService().getPerson(patient.getPersonId())),
                null,
                Arrays.asList(concept),
                null,
                null,
                null,
                null,
                nLast,
                null,
                null,
                null,
                false);
        return obs;
    }

    /**
     * Get the first obs for a given concept
     * @param concept
     * @param patient
     * @return
     * @throws Exception
     */

    public static Obs getFirstObs(Concept concept, Patient patient) throws Exception {
        List<Obs> obs = Context.getObsService().getObservations(
                Arrays.asList(Context.getPersonService().getPerson(patient.getPersonId())),
                null,
                Arrays.asList(concept),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
        return obs.size() > 0 ? obs.get(0) : null;
    }

    public static Obs getLatestObs(Patient patient, String conceptIdentifier) {
        Concept concept = Context.getConceptService().getConceptByUuid(conceptIdentifier);
        List<Obs> obs = Context.getObsService().getObservationsByPersonAndConcept(patient, concept);
        if (obs.size() > 0) {
            // these are in reverse chronological order
            return obs.get(obs.size() - 1);
        }
        return null;
    }

    /**
     * Finds the last encounter during the program enrollment with the given encounter type
     *
     * @param type the encounter type
     *
     * @return the encounter
     */
    public static Encounter lastEncounter(Patient patient, EncounterType type) {
        List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null, Collections.singleton(type), null, null, null, false);
        return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
    }

    /**
     * getEncounters(Patient who, Location loc, Date fromDate, Date toDate,
     Collection<Form> enteredViaForms, Collection<EncounterType> encounterTypes, Collection<Provider> providers,
     Collection<VisitType> visitTypes, Collection<Visit> visits, boolean includeVoided);
     * @return
     */


    public static List<Encounter> getEncounters (Patient patient, List<Form> forms) {

        return Context.getEncounterService().getEncounters(patient, null, null, null, forms, null, null, null, null, false);

    }

    public static List<Obs> getEncounterObservationsForQuestions(Person patient, Encounter encounter, List<Concept> questions) {
        /**
         * getObservations(List<Person> whom, List<Encounter> encounters, List<Concept> questions,
         List<Concept> answers, List<PERSON_TYPE> personTypes, List<Location> locations, List<String> sort,
         Integer mostRecentN, Integer obsGroupId, Date fromDate, Date toDate, boolean includeVoidedObs)
         */
        return Context.getObsService().getObservations(Arrays.asList(patient), Arrays.asList(encounter), questions, null, null, null, null, null, null, null, null, false);
    }

    public static Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    public static String getDefaultLocationMflCode(Location location) {
        String MASTER_FACILITY_CODE = "8a845a89-6aa5-4111-81d3-0af31c45c002";

        if(location == null) {
            location = getDefaultLocation();
        }
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            for (LocationAttribute attr : location.getAttributes()) {
                if (attr.getAttributeType().getUuid().equals(MASTER_FACILITY_CODE) && !attr.isVoided()) {
                    return attr.getValueReference();
                }
            }
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }
        return null;
    }


    public static Location getLocationFromMFLCode(String mflCode) {

        String MASTER_FACILITY_CODE = "8a845a89-6aa5-4111-81d3-0af31c45c002";

        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            LocationAttributeType facilityMflCode = Context.getLocationService().getLocationAttributeTypeByUuid(MASTER_FACILITY_CODE);
            Map<LocationAttributeType, Object> mflCodeMap = new HashMap<LocationAttributeType, Object>();
            mflCodeMap.put(facilityMflCode, mflCode);

            List<Location> locationForMfl = Context.getLocationService().getLocations(null, null, mflCodeMap, false, null,null);

            return locationForMfl.size() > 0 ? locationForMfl.get(0) : getDefaultLocation();
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }
    }


    public static boolean processMlabResponse(String resultPayload) {

        JsonElement rootNode = JsonParser.parseString(resultPayload);
        JsonArray resultsObj = null;
        try {
            if (rootNode.isJsonArray()) {
                resultsObj = rootNode.getAsJsonArray();
            } else {
                System.out.println("MLab Results payload could not be understood. An array is expected!");
                return false;
            }
        } catch (Exception e) {
            System.err.println("MLab Results Get Results: Could not extract results: " + e.getMessage());
            e.printStackTrace();
        }

        if (resultsObj.size() > 0) {
            for (int i = 0; i < resultsObj.size(); i++) {

                JsonObject o = resultsObj.get(i).getAsJsonObject();
                if (o != null) {
                    sendMlabResultToILQueueForProcessing(o.toString());
                }
            }
        }
        return true;
    }


    public static void sendMlabResultToILQueueForProcessing(String payload) {
        Context.openSession();
        try {

            GlobalProperty gpPwd = Context.getAdministrationService().getGlobalPropertyObject("scheduler.password");
            GlobalProperty gpUsername = Context.getAdministrationService().getGlobalPropertyObject("scheduler.username");
            //GlobalProperty gpServerUrl = Context.getAdministrationService().getGlobalPropertyObject("local.viral_load_result_end_point");

            String serverUrl = "http://localhost:8080/openmrs/ws/rest/v1/interop/processhl7il";//gpServerUrl.getPropertyValue();
            String username = gpUsername.getPropertyValue();
            String pwd = gpPwd.getPropertyValue();

            if (StringUtils.isBlank(serverUrl) || StringUtils.isBlank(username) || StringUtils.isBlank(pwd)) {
                System.out.println("Please set credentials for the openmrs scheduler");
                return;
            }

            CloseableHttpClient httpClient = HttpClients.createDefault();

            try {
                //Define a postRequest request
                HttpPost postRequest = new HttpPost(serverUrl);

                //Set the API media type in http content-type header
                postRequest.addHeader("content-type", "application/json");

                String auth = username.trim() + ":" + pwd.trim();
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes("UTF-8"));
                String authHeader = "Basic " + new String(encodedAuth);
                postRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
                //Set the request post body
                StringEntity userEntity = new StringEntity(payload);
                postRequest.setEntity(userEntity);

                //Send the request; It will immediately return the response in HttpResponse object if any
                HttpResponse response = httpClient.execute(postRequest);

                //verify the valid error code first
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200 && statusCode != 201) {
                    JSONParser parser = new JSONParser();
                    JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                    JSONObject errorObj = (JSONObject) responseObj.get("error");
                    if (statusCode == 400) {// bad request
                    }
                    throw new RuntimeException("Failed with HTTP error code : " + statusCode + ". Error msg: " + errorObj.get("message"));
                } else {
                    System.out.println("Successfully queued VL result from Mlab");
                }
            }
            finally {
                //Important: Close the connect
                httpClient.close();
            }

        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to queue Mlab results through REST", e);
        }
    }

    public static String getLocationMflCode(Location location) {
        String mflCodeAttribute = "8a845a89-6aa5-4111-81d3-0af31c45c002";
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATION_ATTRIBUTE_TYPES);
            Iterator var2 = location.getAttributes().iterator();

            while (var2.hasNext()) {
                LocationAttribute attr = (LocationAttribute) var2.next();
                if (attr.getAttributeType().getUuid().equals(mflCodeAttribute) && !attr.isVoided()) {
                    return (String) attr.getValue();
                }
            }
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATION_ATTRIBUTE_TYPES);
        }
        return "";
    }
}
