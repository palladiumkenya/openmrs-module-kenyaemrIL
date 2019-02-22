package org.openmrs.module.kenyaemrIL.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
//import java.util.Base64;

public class PostUtil<T> {
//    private String uri = "http://197.232.1.130:8888/openmrs/ws/rest/v1/kenyaemril/api";
//    private String uri = "http://41.206.32.54:8888/ws/rest/v1/kenyaemril/api";
    private String uri = "http://localhost:8080/openmrs/ws/rest/v1/kenyaemril/api";

    public String makePostRequest(T payload) {
        String response = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            String stringPayload = mapper.writeValueAsString(payload);
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            String encoding = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImF1ZCI6IndlYiIsImV4cCI6MTUyMzYxMjg4OSwiaWF0IjoxNTIzMDA4MDg5fQ.lUOuYNGy4kk1RmRFKm7IwVb3iN78gwSf5KE9thLl9KtDwU1ZGkJ3SNjiPSNembQ4nYneHvapggoj_qvfCl2nBA";
            String userpass = "admin" + ":" + "Admin123";
            String basicAuth = String.format("Basic %s", DatatypeConverter.printBase64Binary(userpass.getBytes()));
//            connection.setRequestProperty("Authorization", "Bearer " + basicAuth);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            System.out.println("About to send to the server!!");
            System.out.println(stringPayload);
            connection.setRequestProperty("Content-Type", "application/json");
            OutputStreamWriter stream = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            stream.write(stringPayload);
            stream.flush();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
//                Everything was o.k
                System.out.println("Successfully Sent to server");
            } else {
                throw new RuntimeException("Request Failed!  - Error Code: " + connection.getResponseCode());
            }
            String r;
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((r = br.readLine()) != null) {
                response += r;
            }
            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
