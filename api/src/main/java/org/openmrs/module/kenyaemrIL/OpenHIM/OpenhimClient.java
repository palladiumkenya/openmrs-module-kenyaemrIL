package org.openmrs.module.kenyaemrIL.OpenHIM;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.openmrs.api.context.Context;

public class OpenhimClient {
    public static String getOpenhimServerUrl(String channel) {
        String url = Context.getAdministrationService().getGlobalProperty(OpenhimConstants.GP_OPENHIM_BASE_URL);
        if (url == null) {
            System.out.println("OpenHIM configuration is missing!");
            return "";
        }
        return url + channel;
    }

    public static HttpResponse postMessage(String message, String channel) throws Exception {
        String openHIMUrl = getOpenhimServerUrl(channel);
        if (openHIMUrl.isEmpty()) {
            return null;
        }

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(openHIMUrl);

        StringEntity fhirResourceEntity = new StringEntity(message);
        httpPost.setEntity(fhirResourceEntity);
        httpPost.setHeader("Content-type", "application/json");

        HttpResponse response = httpClient.execute(httpPost);
        return response;
    }
}
