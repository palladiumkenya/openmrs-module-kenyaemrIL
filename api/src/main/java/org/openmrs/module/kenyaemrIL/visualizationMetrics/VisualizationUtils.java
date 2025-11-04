package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.KenyaEmrInteropDirectPushTask;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class VisualizationUtils {
    private static final Logger log = LoggerFactory.getLogger(KenyaEmrInteropDirectPushTask.class);

    public static Boolean sendPOST(String params)   {

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        try {
            GlobalProperty globalPostUrl = Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_VISUALIZATION_SERVER_POST_END_POINT);
            if (globalPostUrl == null || StringUtils.isBlank(globalPostUrl.getPropertyValue())) {
                System.err.println("Visualization POST endpoint GlobalProperty is not configured.");
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

            if (statusCode != 200) {
                String errorsString = "";
                try {
                    if (response.getEntity() != null) {
                        String responseString = EntityUtils.toString(response.getEntity());
                        System.err.println("Server detailed error: " + responseString);

                        // Enhanced parsing for server error response in JSON
                        org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
                        org.json.simple.JSONObject responseObj = (org.json.simple.JSONObject) parser.parse(responseString);

                        // Try to extract "msg" and "data" (deep error details)
                        String msg = (String) responseObj.get("msg");
                        Object data = responseObj.get("data");
                        errorsString += (msg != null ? msg : "");
                        if (data != null) {
                            errorsString += " [ERROR DETAILS]: " + data;
                        }
                    } else {
                        System.out.println("Response entity is null.");
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse error response: " + ex.getMessage()+":"+ex);
                    errorsString = "Failed to parse error response";
                }
                if (StringUtils.isNotBlank(errorsString)) {
                    if (errorsString.length() > 200) {
                        errorsString = errorsString.substring(0, 199);
                    }
                } else {
                    errorsString = "No error message";
                }
                System.err.println("Error sending message to interop server! Status code - " + statusCode + ". Msg - " + errorsString);
            return false;
            } else {
                System.out.println("Message sent successfully: Visualization server responded with code "+statusCode);
            return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Sys err: "+e.getMessage());
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

    /**
     * Checks Internet connectivity using a test URL.
     * @param testUrl URL to test
     * @return true if connected, false otherwise
     */
    public static boolean hasInternetConnectivity(String testUrl) {
        try {
            URLConnection connection = new URL(testUrl).openConnection();
            connection.setConnectTimeout(4000);
            connection.connect();
            return true;
        } catch (Exception ex) {
            log.warn("KenyaEMR IL: Unable to connect to {}", testUrl, ex);
            return false;
        }
    }

}

