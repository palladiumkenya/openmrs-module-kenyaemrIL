package org.openmrs.module.kenyaemrIL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.il.utils.ViralLoadProcessorUtil;
import org.openmrs.module.kenyaemrIL.kenyaemrUtils.Utils;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Pulls viral load results from Mlab
 */
public class MLabViralLoadResultsPullTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(MLabViralLoadResultsPullTask.class);
    private String url = "http://www.google.com:80/index.html";
    private static final String RESULTS_NOT_FOUND = "No results found";
    private static final String FACILITY_NOT_CONFIGURED_FOR_MLAB_RESULTS = "No Facility with given MFL code registered for IL";
    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        Context.openSession();
        System.out.println("IL Module: MLAB Pull results - starting the scheduler...");

        // check first if there is internet connectivity before pushing

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.connect();
            try {

                GlobalProperty gpServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MLAB_SERVER_REQUEST_URL);
                //GlobalProperty gpApiToken = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MLAB_SERVER_API_TOKEN);

                String serverUrl = gpServerUrl.getPropertyValue();
                //String API_KEY = gpApiToken.getPropertyValue();

                if (StringUtils.isBlank(serverUrl)/* || StringUtils.isBlank(API_KEY)*/) {
                    System.out.println("IL Module: MLAB Pull results - Please set server URL for MLAB");
                    return;
                }

                /*SSLConnectionSocketFactory sslsf = null;
                GlobalProperty gpSslVerification = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_SSL_VERIFICATION_ENABLED);

                if (gpSslVerification != null) {
                    String sslVerificationEnabled = gpSslVerification.getPropertyValue();
                    if (StringUtils.isNotBlank(sslVerificationEnabled)) {
                        if (sslVerificationEnabled.equals("true")) {
                            sslsf = ILUtils.sslConnectionSocketFactoryDefault();
                        } else {
                            sslsf = ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification();
                        }
                    }
                }

                CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();*/

                CloseableHttpClient httpClient = HttpClients.createDefault();

                try {

                    //Define a postRequest request
                    HttpPost postRequest = new HttpPost(serverUrl);

                    //Set the API media type in http content-type header
                    postRequest.addHeader("content-type", "application/json");
                    postRequest.addHeader("cache-control", "no-cache");
                    ObjectNode data = ViralLoadProcessorUtil.getJsonNodeFactory().objectNode();

                    String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());
                    data.put("mfl_code", facilityMfl);

                    //Set the request post body
                    StringEntity userEntity = new StringEntity(data.toString());
                    postRequest.setEntity(userEntity);

                    //Send the request; It will immediately return the response in HttpResponse object if any
                    HttpResponse response = httpClient.execute(postRequest);

                    //verify the valid error code first
                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        System.out.println("Get MLAB Results: the server responded with an error");
                        String message = EntityUtils.toString(response.getEntity(), "UTF-8");

                        if (statusCode == 429) { // too many requests. just terminate
                            System.out.println("MLAB Results PULL: 429 The pull lab scheduler has been configured to run at very short intervals. Please change this to at least 30min");
                            return;
                        }
                        throw new RuntimeException("Get MLab Results Failed with HTTP error code : " + statusCode + ", message: " + message);
                    }

                    System.out.println("Get MLAB Results: server responded with results");


                    String jsonString = null;
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));

                        try {
                            jsonString = rd.lines().collect(Collectors.joining()).toString();

                        } finally {
                            rd.close();
                        }
                    } else {

                        System.out.println("Get MLAB Results: There are no results to update. " + EntityUtils.toString(response.getEntity(), "UTF-8"));
                        return;
                    }

                    /*jsonString = "[\"IL\n" +
                            "SUQ6IDQwMjkzNTIsIFBJRDoxMzkyOTIwMjIwMDI2LCBBZ2U6MiwgU2V4Ok1hbGUsIERDOjIwMjItMDctMTEsIExPRDogMjAyMi0wNy0yOSwgQ1NSOiAsIENTVDogMCwgQ0o6IDAsIFJlc3VsdDogOk5lZ2F0aXZlICwgTUZMOiAxMzkyOSwgTGFiOiAz\",\"IL\n" +
                            "SUQ6IDQwMjYzMDQsIFBJRDoxMzkyOTIwMjEwMDYwLCBBZ2U6NiwgU2V4Ok1hbGUsIERDOjIwMjItMDYtMjgsIExPRDogMjAyMi0wNy0yNiwgQ1NSOiAsIENTVDogMCwgQ0o6IDAsIFJlc3VsdDogOk5lZ2F0aXZlICwgTUZMOiAxMzkyOSwgTGFiOiAz\"]";
*/
                    System.out.println("MLAB: server result: " + jsonString);

                    if (StringUtils.isBlank(jsonString) || jsonString == null || RESULTS_NOT_FOUND.equals(jsonString)) {
                        System.out.println("MLAB server: Results not found");
                        return;
                    }

                    if (FACILITY_NOT_CONFIGURED_FOR_MLAB_RESULTS.equals(jsonString)) {
                        System.out.println("MLAB server: facility has not been configured to pull MLAB results");
                        return;
                    }

                    System.out.println("Received lab results ----: " + jsonString);

                    // decode results from the server

                    JSONParser parser = new JSONParser();
                    JSONArray responseArray = (JSONArray) parser.parse(jsonString);
                    Base64 base64 = new Base64();

                    for (int i = 0; i < responseArray.size(); i++) {
                        String encodedString = (String) responseArray.get(i);
                        if (encodedString.startsWith("IL")) {
                            encodedString = encodedString.substring(3);

                            String decodedString = new String(base64.decode(encodedString.getBytes()));

                            if (StringUtils.isNotBlank(decodedString)) {
                                String [] payload = decodedString.split(",");

                                String labResultId = payload[0].split(":")[1].trim();
                                String CCCNumber = payload[1].split(":")[1].trim();
                                String age = payload[2].split(":")[1].trim();
                                String sex = payload[3].split(":")[1].trim();
                                String dateSampleCollected = payload[4].split(":")[1].trim();
                                String orderDate = payload[5].split(":")[1].trim();
                                String sampleRejection = payload[6].split(":")[1].trim();
                                String sampleType = payload[7].split(":")[1].trim();
                                String justification = payload[8].split(":")[1].trim();
                                String vLResult = payload[9].substring(payload[9].lastIndexOf(":") +1 ).trim(); // Result: :Negative is sample result
                                String mflCode = payload[10].split(":")[1].trim();
                                String lab = payload[11].split(":")[1].trim();


                                MESSAGE_HEADER headerEntity = new MESSAGE_HEADER();
                                headerEntity.setSending_application("MLAB SMS APP");
                                headerEntity.setSending_facility(mflCode);
                                headerEntity.setReceiving_application("IL");
                                headerEntity.setReceiving_facility(mflCode);
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
                                headerEntity.setMessage_datetime(formatter.format(new Date()));
                                headerEntity.setMessage_type("ORU^VL");
                                headerEntity.setProcessing_id("P");
                                headerEntity.setSecurity("");

                                PATIENT_IDENTIFICATION patientIdentificationEntity = new PATIENT_IDENTIFICATION();
                                INTERNAL_PATIENT_ID internalPatientId = new INTERNAL_PATIENT_ID();
                                internalPatientId.setId(CCCNumber);
                                internalPatientId.setAssigning_authority("CCC");
                                internalPatientId.setIdentifier_type("CCC_NUMBER");
                                patientIdentificationEntity.setInternal_patient_id(Arrays.asList(internalPatientId));

                                VIRAL_LOAD_RESULT vlTestResults[] = new VIRAL_LOAD_RESULT[1];
                                VIRAL_LOAD_RESULT viralLoadResult = new VIRAL_LOAD_RESULT();
                                viralLoadResult.setDate_sample_collected(dateSampleCollected);
                                viralLoadResult.setDate_sample_tested(orderDate);
                                viralLoadResult.setVl_result(vLResult);
                                viralLoadResult.setSample_type(sampleType);
                                viralLoadResult.setSample_rejection(sampleRejection);
                                viralLoadResult.setJustification(justification);
                                viralLoadResult.setRegimen("");
                                viralLoadResult.setLab_tested_in(lab);

                                vlTestResults[0] = viralLoadResult;
                                ILMessage ilMessage = new ILMessage();
                                ilMessage.setMessage_header(headerEntity);
                                ilMessage.setPatient_identification(patientIdentificationEntity);
                                ilMessage.setViral_load_result(vlTestResults);

                                KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
                                ObjectMapper mapper = new ObjectMapper();

                                String messageString = mapper.writeValueAsString(ilMessage);
                                KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();

                                kenyaEMRILMessage.setHl7_type("ORU^VL");
                                kenyaEMRILMessage.setSource("MLAB SMS APP");
                                kenyaEMRILMessage.setMessage(messageString);
                                kenyaEMRILMessage.setDescription("");
                                kenyaEMRILMessage.setName("");
                                kenyaEMRILMessage.setMessage_type(ILMessageType.INBOUND.getValue());
                                service.saveKenyaEMRILMessage(kenyaEMRILMessage);
                                System.out.println("IL Message: " + messageString);

                                /*//ID: 4029352, PID:1392920220026, Age:2, Sex:Male, DC:2022-07-11, LOD: 2022-07-29, CSR: , CST: 0, CJ: 0, Result: :Negative , MFL: 13929, Lab: 3

                                const payload = decodedVL.split(',')


                                labResult.labResultId = payload[0].split(':')[1].trim() - lab result id
                                labResult.CCCNumber = payload[1].split(':')[1].trim() -
                                labResult.age = payload[2].split(':')[1].trim()
                                labResult.sex = payload[3].split(':')[1].trim()
                                labResult.dateSampleCollected = payload[4].split(':')[1].trim()
                                labResult.orderDate = payload[5].split(':')[1].trim()
                                labResult.sampleRejection = payload[6].split(':')[1].trim()
                                labResult.sampleType = payload[7].split(':')[1].trim()
                                labResult.justification = payload[8].split(':')[1].trim()
                                labResult.VLResult = payload[9].replace(/Result:/g, '').trim()
                                labResult.mflCode = payload[10].split(':')[1].trim()
                                labResult.lab = payload[11].split(':')[1].trim()*/
                            }

                            /*JSONParser parser = new JSONParser();
                            JSONArray responseObject = (JSONArray) parser.parse(decodedString);*/

                            System.out.println("MLAB lab results: decoded string " + decodedString.trim());

                           // String resultString = responseObject.toString();

                            //Utils.processMlabResponse(resultString);
                            //Context.flushSession();
                        } else {
                            System.out.println("MLAB: Results cannot be understood");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Could not pull requests from MLAB! " + e.getCause());
                    log.error("Could not pull requests from MLAB! " + e.getCause());
                    e.printStackTrace();
                } finally {
                    httpClient.close();
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to execute task that pulls viral load results from MLAB", e);
            } finally {
                Context.closeSession();

            }
        } catch (IOException ioe) {

            try {
                String text = "At " + new Date() + " there was an error reported connecting to the internet. Will not attempt pushing viral load manifest ";
                log.warn(text);
            } catch (Exception e) {
                log.error("Failed to check internet connectivity", e);
            }
        }
    }
}
