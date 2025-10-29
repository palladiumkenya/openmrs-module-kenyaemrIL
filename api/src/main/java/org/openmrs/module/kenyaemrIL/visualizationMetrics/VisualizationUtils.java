package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.KenyaEmrInteropDirectPushTask;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VisualizationUtils {
    private static final Logger log = LoggerFactory.getLogger(KenyaEmrInteropDirectPushTask.class);

    public static JsonNodeFactory getJsonNodeFactory() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        return factory;
    }

    public static Boolean sendPOST(String params)   {

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        try {
            String stringResponse = "";           
            GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_VISUALIZATION_SERVER_POST_END_POINT);
            if (globalPostUrl == null || StringUtils.isBlank(globalPostUrl.getPropertyValue())) {
                log.error("Visualization POST endpoint GlobalProperty is not configured.");
                return false;
            }
            String strPostUrl = globalPostUrl.getPropertyValue();
            HttpPost postRequest = new HttpPost(strPostUrl);

            //Set the API media type in http content-type header
            postRequest.addHeader("content-type", "application/json");
            //Set the request post body
            StringEntity userEntity = new StringEntity(params);
            postRequest.setEntity(userEntity);
            HttpResponse response = httpClient.execute(postRequest);

            //verify the valid error code first
            Integer statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Server response: " + statusCode);

            if (statusCode != 200) {
                String errorsString = "";
                try {
                    if (response.getEntity() != null) {
                        String responseString = EntityUtils.toString(response.getEntity());
                        System.out.println("Server detailed error: " + responseString);

                        // Enhanced parsing for server error response in JSON
                        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
                        org.json.simple.JSONObject responseObj = (org.json.simple.JSONObject) parser.parse(responseString);

                        // Try to extract "msg" and "data" (deep error details)
                        String msg = (String) responseObj.get("msg");
                        Object data = responseObj.get("data");
                        errorsString += (msg != null ? msg : "");
                        if (data != null) {
                            errorsString += " [ERROR DETAILS]: " + data.toString();
                        }
                    } else {
                        System.out.println("Response entity is null.");
                    }
                } catch (Exception ex) {
                    log.error("Failed to parse error response: " + ex.getMessage(), ex);
                    System.out.println("Failed to parse error response: " + ex.getMessage());
                    errorsString = "Failed to parse error response";
                }
                if (StringUtils.isNotBlank(errorsString)) {
                    if (errorsString.length() > 200) {
                        errorsString = errorsString.substring(0, 199);
                    }
                } else {
                    errorsString = "No error message";
                }
                System.out.println("Error sending message to interop server! Status code - " + statusCode + ". Msg - " + errorsString);
                log.error("Error sending message to visualization server! Status code - " + statusCode + ". Msg - " + errorsString);
            return false;
            } else {

                log.info("Successfully sent message to visualization server");
                System.out.println("Successfully sent message to visualization server");
            return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            System.out.println(e.getMessage());
            return true;
        }
    }

    /**
     * Utility method to build a breakdown List<SimpleObject> for charts.
     * keyField is the name of the key (e.g. "age", "service"), valueField is "total".
     */
    public static List<SimpleObject> mapToBreakdownList(Map<String, Integer> map, String keyField, String valueField) {
        List<SimpleObject> list = new ArrayList<>(map.size());
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            SimpleObject detail = new SimpleObject();
            detail.put(keyField, entry.getKey());
            detail.put(valueField, entry.getValue().toString());
            list.add(detail);
        }
        return list;
    }
}

