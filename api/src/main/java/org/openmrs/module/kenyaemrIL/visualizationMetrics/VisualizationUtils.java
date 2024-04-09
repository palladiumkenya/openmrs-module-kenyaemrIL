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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(EntityUtils.toString(response.getEntity()));
                JSONObject errorObj = (JSONObject) responseObj.get("response");
                System.out.println("Error object" + errorObj.toJSONString());

                if (errorObj != null) {
                    errorsString = (String) errorObj.get("msg");
                }
                System.out.println("Error sending message to interop server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
                log.error("Error sending message to visualization server! " + "Status code - " + statusCode + ". Msg - " + errorsString);
                System.out.println("Error object" + errorObj.toJSONString());

                if (StringUtils.isNotBlank(errorsString)) {
                    if (errorsString.length() > 200) {
                        errorsString = errorsString.substring(0, 199);
                    }
                } else {
                    errorsString = "No error message";
                }

            } else {

                log.info("Successfully sent message to visualization server");
                System.out.println("Successfully sent message to visualization server");

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return true;
    }

    //return responseObj;
}

