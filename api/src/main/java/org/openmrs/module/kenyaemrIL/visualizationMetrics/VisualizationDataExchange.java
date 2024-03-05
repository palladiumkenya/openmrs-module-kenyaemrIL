package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class VisualizationDataExchange {

    private static Log log = LogFactory.getLog(VisualizationDataExchange.class);
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * Generates the payload used to post to visualization server     *
     * @param
     * @return
     */
    public static JSONObject generateVisualizationPayload(Date fetchDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        JSONObject payloadObj = new JSONObject();
        List<SimpleObject> bedManagement = new ArrayList<SimpleObject>();
        List<SimpleObject> visits = new ArrayList<>();
        Map<String, Integer> visitsMap;
        List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
        List<SimpleObject> workload = new ArrayList<SimpleObject>();
        List<SimpleObject> billing = new ArrayList<SimpleObject>();
        List<SimpleObject> payments = new ArrayList<SimpleObject>();
        List<SimpleObject> inventory = new ArrayList<SimpleObject>();
        List<SimpleObject> mortality = new ArrayList<SimpleObject>();
        String timestamp = formatter.format(fetchDate);
        //Data extraction
        String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        //add to list
        payloadObj.put("mfl_code",facilityMfl);
        payloadObj.put("timestamp", timestamp);


        if (bedManagement.size() > 0) {
            SimpleObject bedManagementObject = new SimpleObject();
            bedManagementObject.put("ward", "");
            bedManagementObject.put("capacity", "");
            bedManagementObject.put("occupancy", "");
            bedManagementObject.put("new_admissions", "");
            bedManagement.add(bedManagementObject);
            payloadObj.put("bed_management", bedManagement);
        }else{
            payloadObj.put("bed_management", bedManagement);
        }

        visitsMap = allVisits(fetchDate);
        if (!visitsMap.isEmpty()) {
            for (Map.Entry<String, Integer> visitEntry : visitsMap.entrySet()) {
                SimpleObject visitsObject = new SimpleObject();
                visitsObject.put("visit_type", visitEntry.getKey());
                visitsObject.put("total", visitEntry.getValue());
                visits.add(visitsObject);
            }
        } else {
            SimpleObject visitsObject = new SimpleObject();
            visitsObject.put("visit_type", "");
            visitsObject.put("total", "");
            visits.add(visitsObject);
        }
        payloadObj.put("visits", visits);

        if (diagnosis.size() > 0) {
            SimpleObject diagnosisObject = new SimpleObject();
            diagnosisObject.put("diagnosis_name", "");
            diagnosisObject.put("total", "");
            diagnosis.add(diagnosisObject);
            payloadObj.put("diagnosis", diagnosis);
        }else{
            payloadObj.put("diagnosis", diagnosis);
        }
        if (workload.size() > 0) {
            SimpleObject workloadObject = new SimpleObject();
            workloadObject.put("department", "");
            workloadObject.put("total", "");
            workload.add(workloadObject);
            payloadObj.put("workload", workload);
        }else{
            payloadObj.put("workload", workload);
        }
        if (workload.size() > 0) {
            SimpleObject workloadObject = new SimpleObject();
            workloadObject.put("department", "");
            workloadObject.put("total", "");
            workload.add(workloadObject);
            payloadObj.put("workload", workload);
        }else{
            payloadObj.put("workload", workload);
        }
        if (billing.size() > 0) {
            SimpleObject billingObject = new SimpleObject();
            billingObject.put("service_type", "");
            billingObject.put("invoices", "");
            billingObject.put("amount_due", "");
            billingObject.put("amount_paid", "");
            billingObject.put("balance_due", "");
            billing.add(billingObject);
            payloadObj.put("billing", billing);
        }else{
            payloadObj.put("billing", billing);
        }
        if (payments.size() > 0) {
            SimpleObject paymentsObject = new SimpleObject();
            paymentsObject.put("payment_mode", "");
            paymentsObject.put("no_of_patients", "");
            paymentsObject.put("amount_paid", "");
            payments.add(paymentsObject);
            payloadObj.put("payments", payments);
        }else{
            payloadObj.put("payments", payments);
        }
        if (inventory.size() > 0) {
            SimpleObject inventoryObject = new SimpleObject();
            inventoryObject.put("item_name", "");
            inventoryObject.put("item_type", "");
            inventoryObject.put("unit_of_measure", "");
            inventoryObject.put("quantity_at_hand", "");
            inventoryObject.put("quantity_consumed", "");
            inventory.add(inventoryObject);
            payloadObj.put("inventory", inventory);
        }else{
            payloadObj.put("inventory", inventory);
        }
        if (mortality.size() > 0) {
            SimpleObject mortalityObject = new SimpleObject();
            mortalityObject.put("cause_of_death", "");
            mortalityObject.put("total", "");
            mortality.add(mortalityObject);
            payloadObj.put("mortality", mortality);
        }else{
            payloadObj.put("mortality", mortality);
        }

        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        System.out.println("Payload generated: " + payloadObj);

        return payloadObj;
    }

    public static Map<String, Integer> allVisits(Date fetchDate) {

        Map<String, Integer> visitMap = new HashMap<>();
        VisitService visitService = Context.getVisitService();
        List<Visit> allVisits = visitService.getVisits(null, null, null, null, fetchDate, null, null, null, null, true, false);

        if (!allVisits.isEmpty()) {
            for (Visit visit : allVisits) {
                String visitType = visit.getVisitType().getName();
                visitMap.put(visitType, visitMap.getOrDefault(visitType, 0) + 1);
            }
        }
        return visitMap;
    }
}


