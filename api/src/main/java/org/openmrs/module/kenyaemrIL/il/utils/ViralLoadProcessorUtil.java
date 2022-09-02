package org.openmrs.module.kenyaemrIL.il.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.TestOrder;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Util class for processing viral load results from mlab (mhealth)
 * In order to fully discontinue vl lab requests, we are forced to post results to an internal endpoint
 * The endpoint is configured through a global property named kenyaemrIL.viral_load_result_end_point
 * The global property should be set to http://<host>:<port>/openmrs/ws/rest/v1/interop/labresults
 */
public class ViralLoadProcessorUtil {

    public static final String LAB_ORDER_ENCOUNTER_TYPE_UUID = "e1406e88-e9a9-11e8-9f32-f2801f1b9fd1";
    public static Concept vlTestConceptQualitative = Context.getConceptService().getConcept(1305);
    public static Concept LDLConcept = Context.getConceptService().getConcept(1302);
    public static Concept vlTestConceptQuantitative = Context.getConceptService().getConcept(856);
    public static EncounterType labEncounterType = Context.getEncounterService().getEncounterTypeByUuid(LAB_ORDER_ENCOUNTER_TYPE_UUID);
    public static OrderType labType = Context.getOrderService().getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID);

    public static EncounterService encounterService = Context.getEncounterService();
    public static OrderService orderService = Context.getOrderService();

    /**
     * POST end point for processing internal REST requests
     * @param payload
     */
    public static String restEndPointForLabResult(String payload) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode resultsObj = null;
        try {
            JsonNode actualObj = mapper.readTree(payload);
            resultsObj = (ObjectNode) actualObj;

            Integer patientId = resultsObj.get("patientId").intValue();
            String vlResult = resultsObj.get("result").textValue();
            String vlDate = resultsObj.get("orderDate").textValue();
            Date vlRequestDate = null;

            try {
                vlRequestDate = df.parse(vlDate);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (patientId != null && vlResult != null && vlRequestDate != null) {
                updateOrder(Context.getPatientService().getPatient(patientId), vlRequestDate, vlResult);
                return "Lab results posted successfully for updates";
            }

        } catch (JsonProcessingException e) {
            String statusMsg = "The payload could not be understood. An object is expected!";
            e.printStackTrace();
            return statusMsg;
        }
        return "Something may have gone wrong!";
    }
    /**
     * Updates an active order and sets results if provided
     * @param patient
     * @param vlOrderDate
     * @param result
     */
    public static void updateOrder(Patient patient, Date vlOrderDate, String result) {

        Map<String, Order> ordersToProcess = getOrdersToProcess(patient, labType, null, vlOrderDate, vlTestConceptQuantitative);
        Order orderToRetain = ordersToProcess.get("orderToRetain");
        Order orderToVoid = ordersToProcess.get("orderToVoid");

        Date orderDiscontinuationDate = aMomentBefore(new Date());

        if (ordersToProcess != null && ordersToProcess.size() > 0) {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(result)) {

                if (orderToRetain == null || orderToVoid == null) { // just skip if any of the orders is null
                    System.out.println("There is a null order in the package. This may be due to manual update by a provider or data clerk!");
                    return;
                } else if (!orderToRetain.isActive() || !orderToVoid.isActive()) { // just skip if any of the orders is not active
                    System.out.println("There is no order to key in results for. This may have been done manually by a provider or data clerk!");
                    return;
                }

                Concept conceptToRetain = null;
                String lDLResult = "ldl";
                Obs o = new Obs();

                if (result.equals(lDLResult)) {
                    conceptToRetain = vlTestConceptQualitative;
                    o.setValueCoded(LDLConcept);
                } else {
                    conceptToRetain = vlTestConceptQuantitative;
                    o.setValueNumeric(Double.valueOf(result));
                }

                // In order to record results both qualitative (LDL) and quantitative,
                // every vl request saves two orders: one with 856(quantitative) for numeric values and another with 1305(quantitative) for LDL value
                // When recording result, it is therefore prudent to set result for one order and void the other one

                // logic that picks the right concept id for the result obs
                o.setConcept(conceptToRetain);
                o.setDateCreated(orderDiscontinuationDate);
                o.setCreator(Context.getUserService().getUser(1));
                o.setObsDatetime(orderToRetain.getDateActivated());
                o.setPerson(patient);
                o.setOrder(orderToRetain);

                Encounter enc = new Encounter();
                enc.setEncounterType(labEncounterType);
                enc.setEncounterDatetime(orderDiscontinuationDate);
                enc.setPatient(patient);
                enc.setCreator(Context.getUserService().getUser(1));

                enc.addObs(o);
                if (orderToRetain != null && orderToVoid != null && orderToRetain.isActive()) {

                    try {

                        Context.getEncounterService().saveEncounter(enc);
                        Context.getOrderService().discontinueOrder(orderToRetain, "Results received", orderDiscontinuationDate, orderToRetain.getOrderer(),
                                orderToRetain.getEncounter());
                        Context.getOrderService().voidOrder(orderToVoid, "Duplicate VL order");
                        System.out.println("Results updated successfully");

                        // this is really a hack to ensure that order date_stopped is filled, otherwise the order will remain active
                        // the issue here is that even though disc order is created, the original order is not stopped
                        // an alternative is to discontinue this order via REST which works well
                    } catch (Exception e) {
                        System.out.println("An error was encountered while updating orders for viral load");
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // create the order, set result, and discontinue the order

            Encounter orderEnc = new Encounter();
            orderEnc.setEncounterType(labEncounterType);
            orderEnc.setEncounterDatetime(vlOrderDate);
            orderEnc.setPatient(patient);
            orderEnc.setCreator(Context.getUserService().getUser(1));

            Encounter enc = new Encounter();
            enc.setEncounterType(labEncounterType);
            enc.setEncounterDatetime(orderDiscontinuationDate);
            enc.setPatient(patient);
            enc.setCreator(Context.getUserService().getUser(1));

            Concept vlQuestionConcept = null;
            String lDLResult = "< LDL copies/ml";
            String aboveMillionResult = "> 10,000,000 cp/ml";
            Obs o = new Obs();

            if (result.equalsIgnoreCase(lDLResult) || result.contains("LDL")) {
                vlQuestionConcept = vlTestConceptQualitative;
                o.setValueCoded(LDLConcept);
            } else if (result.equalsIgnoreCase(aboveMillionResult)) {
                vlQuestionConcept = vlTestConceptQuantitative;
                o.setValueNumeric(new Double(10000001));
            } else {
                vlQuestionConcept = vlTestConceptQuantitative;
                Double vlVal = NumberUtils.toDouble(result);
                o.setValueNumeric(vlVal);
            }

            CareSetting careSetting = orderService.getCareSetting(1);

            TestOrder order = new TestOrder();
            order.setAction(Order.Action.NEW);
            order.setCareSetting(careSetting);
            order.setConcept(vlQuestionConcept);
            order.setPatient(patient);
            order.setFulfillerComment("MLAB result");
            order.setOrderer(Context.getProviderService().getUnknownProvider());
            order.setUrgency(orderToVoid.getUrgency());
            order.setOrderReason(orderToVoid.getOrderReason());
            order.setOrderReasonNonCoded(orderToVoid.getOrderReasonNonCoded());
            order.setDateActivated(vlOrderDate);
            order.setCreator(Context.getUserService().getUser(1));
            order.setEncounter(orderEnc);
            Order savedOrder = orderService.saveOrder(order, null);

            try {

                encounterService.saveEncounter(enc);
                orderService.discontinueOrder(savedOrder, "Results received", orderDiscontinuationDate, savedOrder.getOrderer(),
                        savedOrder.getEncounter());

                o.setConcept(vlQuestionConcept);
                o.setDateCreated(new Date());
                o.setCreator(Context.getUserService().getUser(1));
                o.setObsDatetime(vlOrderDate);
                o.setPerson(patient);
                o.setOrder(savedOrder);

                orderEnc.addObs(o);
                encounterService.saveEncounter(orderEnc);

            } catch (Exception e) {
                System.out.println("Lab Results Get Results: An error was encountered while updating orders for viral load");
                e.printStackTrace();
            }

        }

    }

    /**
     * Returns an object indicating the order to retain and that to void
     * @param patient
     * @param orderType
     * @param careSetting
     * @param orderDate
     * @param conceptToRetain
     * @return
     */
    private static Map<String, Order> getOrdersToProcess(Patient patient, OrderType orderType, CareSetting careSetting, Date orderDate, Concept conceptToRetain) {

        Map<String, Order> listToProcess = new HashMap<String, Order>();
        Concept conceptToVoid = conceptToRetain.equals(vlTestConceptQualitative) ? vlTestConceptQuantitative : vlTestConceptQualitative;
        List<Order> ordersOnSameDay = Context.getOrderService().getActiveOrders(patient, orderType, careSetting, orderDate);

        for (Order order : ordersOnSameDay) {
            if (order.getConcept().equals(conceptToVoid)) {
                listToProcess.put("orderToVoid", order);
            } else if (order.getConcept().equals(conceptToRetain)) {
                listToProcess.put("orderToRetain", order);
            }
        }
        return listToProcess;
    }

    /**
     * Borrowed from OpenMRS core
     * To support MySQL datetime values (which are only precise to the second) we subtract one
     * second. Eventually we may move this method and enhance it to subtract the smallest moment the
     * underlying database will represent.
     *
     * @param date
     * @return one moment before date
     */
    private static Date aMomentBefore(Date date) {
        return DateUtils.addSeconds(date, -1);
    }

    /**
     * this is a hack for updating active orders with results from m-lab
     * @param payload
     */
    public static void postRestRequest(String payload) {
        Context.openSession();
        try {

            GlobalProperty gpPwd = Context.getAdministrationService().getGlobalPropertyObject("scheduler.password");
            GlobalProperty gpUsername = Context.getAdministrationService().getGlobalPropertyObject("scheduler.username");
            GlobalProperty gpServerUrl = Context.getAdministrationService().getGlobalPropertyObject("kenyaemrIL.viral_load_result_end_point");

            String serverUrl = gpServerUrl.getPropertyValue();
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
                    //System.out.println("Successfully updated VL result"); -- remain mute
                }
            }
            finally {
                //Important: Close the connect
                httpClient.close();
            }

        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to update lab results through REST", e);
        }
    }

    /**
     * Extracts the request body and return it as string
     * @param reader
     * @return
     */
    public static String fetchRequestBody(BufferedReader reader) {
        String requestBodyJsonStr = "";
        try {
            String output = "";
            while ((output = reader.readLine()) != null) {
                requestBodyJsonStr += output;
            }
        } catch (IOException e) {

            System.out.println("IOException: " + e.getMessage());

        }
        return requestBodyJsonStr;
    }

    /**
     * Creates a node factory
     * @return
     */
    public static JsonNodeFactory getJsonNodeFactory() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory;
    }
}
