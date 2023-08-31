package org.openmrs.module.kenyaemrIL.fragment.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.nupi.UpiUtilsDataExchange;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestParam;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * controller for pivotTableCharts fragment
 */
public class ReferralsDataExchangeFragmentController {
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ReferralsDataExchangeFragmentController.class);
        @Qualifier("fhirR4")
    private FhirContext fhirContext;

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public IGenericClient getFhirClient() throws Exception {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(ILUtils.getShrServerUrl());
        return fhirClient;
    }

    public void controller(FragmentModel model, @FragmentParam("patient") Patient patient) {

    }

    /**
     * Get community referrals for FHIR server       *
     *
     * @return
     */
    public SimpleObject pullCommunityReferralsFromFhir() throws Exception {
        System.out.println("Fhir :Start pullCommunityReferralsFromFhir ==>");
        org.hl7.fhir.r4.model.Resource fhirServiceRequestResource;
        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = null;
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        Bundle serviceRequestResourceBundle = fhirConfig.fetchReferrals();
        System.out.println(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(serviceRequestResourceBundle));
        if (serviceRequestResourceBundle != null && !serviceRequestResourceBundle.getEntry().isEmpty()) {
            for (int i = 0; i < serviceRequestResourceBundle.getEntry().size(); i++) {
                fhirServiceRequestResource = serviceRequestResourceBundle.getEntry().get(i).getResource();
                System.out.println("Fhir : Checking Service request is null ==>");
                if (fhirServiceRequestResource != null) {
                    System.out.println("Fhir : Service request is not null ==>");
                    fhirServiceRequest = (org.hl7.fhir.r4.model.ServiceRequest) fhirServiceRequestResource;
                    String nupiNumber = fhirServiceRequest.getSubject().getDisplay();
                    System.out.println("NUPI :  ==>" + nupiNumber);
                    if (nupiNumber != null) {
                        String serverUrl = "https://afyakenyaapi.health.go.ke/partners/registry/search/upi/" + nupiNumber;
                        persistReferralData(getCRPatient(serverUrl), fhirConfig, fhirServiceRequest);
                    }
                } else {
                    System.out.println("Fhir : Service request is null ==>");
                }
            }
        }
        return SimpleObject.create("success", "");
    }

    public void updateShrReferral(Patient patient, FhirConfig fhirConfig) throws Exception {
        List<ExpectedTransferInPatients> patientReferrals = Context.getService(KenyaEMRILService.class).getTransferInPatient(patient);
        List<ExpectedTransferInPatients> activeReferral = patientReferrals.stream().filter(p -> p.getReferralStatus().equalsIgnoreCase("ACTIVE")).collect(Collectors.toList());
        IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);

        ServiceRequest serviceRequest;
        if (!activeReferral.isEmpty()) {
            serviceRequest = parser.parseResource(ServiceRequest.class, activeReferral.get(0).getPatientSummary());
            System.out.println(serviceRequest.getStatus());
            System.out.println(serviceRequest.getCategory().get(0).getCoding().get(0).getDisplay());
            serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
            fhirConfig.updateReferral(serviceRequest);
        }
    }

    /**
     * Fetch client from CR
     */
    private JSONObject getCRPatient(String serverUrl) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String stringResponse = "";
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        URL url = new URL(serverUrl);

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        UpiUtilsDataExchange upiUtils = new UpiUtilsDataExchange();
        String authToken = upiUtils.getToken();

        System.out.println("TOKEN " + authToken);

        con.setRequestProperty("Authorization", "Bearer " + authToken);
        con.setRequestProperty("Accept", "application/json");
        con.setConnectTimeout(10000); // set timeout to 10 seconds

        con.setDoOutput(true);

        if (con.getResponseCode() == HttpsURLConnection.HTTP_OK) { //success
            BufferedReader in = null;
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            stringResponse = response.toString();

            try {
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(stringResponse);
                JSONObject client = (JSONObject) responseObj.get("client");

                if (client != null) {
                    return client;
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }
        return null;
    }

    // Create client referral data
    public void persistReferralData(JSONObject crClient, FhirConfig fhirConfig, org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest) {
        ExpectedTransferInPatients expectedTransferInPatients = new ExpectedTransferInPatients();
        expectedTransferInPatients.setClientFirstName(String.valueOf(crClient.get("firstName")));
        expectedTransferInPatients.setClientMiddleName(String.valueOf(crClient.get("middleName")));
        expectedTransferInPatients.setClientLastName(String.valueOf(crClient.get("lastName")));
        expectedTransferInPatients.setNupiNumber(String.valueOf(crClient.get("clientNumber")));
        expectedTransferInPatients.setClientBirthDate(new Date());
        String gender = String.valueOf(crClient.get("gender"));
        if (gender.equalsIgnoreCase("male")) {
            gender = "M";
        } else if (gender.equalsIgnoreCase("female")) {
            gender = "F";
        }
        expectedTransferInPatients.setClientGender(gender);
        expectedTransferInPatients.setReferralStatus("ACTIVE");
        expectedTransferInPatients.setPatientSummary(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(fhirServiceRequest));
        expectedTransferInPatients.setServiceType("COMMUNITY");
        Context.getService(KenyaEMRILService.class).createPatient(expectedTransferInPatients);
        System.out.println("Successfully persisted in expected referrals model ==>");
    }

    public void completeClientReferral(@RequestParam("patientId") Integer patientId) throws Exception {

        //Update  referral_status attribute to completed status
        System.out.println("Patient ID ==>" + patientId);
        System.out.println("Here ==>");
        PatientService patientService = Context.getPatientService();
        Patient patient = patientService.getPatient(patientId);
        PersonAttribute referralStatusAttribute = new PersonAttribute();
        PersonAttributeType referralStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("df7e9996-23b5-4f66-a799-97498d19850d");
        if (referralStatusAttributeType != null) {
            referralStatusAttribute.setAttributeType(referralStatusAttributeType);
            referralStatusAttribute.setValue("Completed");
            patient.addAttribute(referralStatusAttribute);
            patientService.savePatient(patient);
            //Update fhir message status to complete
            FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
//            updateShrReferral(patient, fhirConfig);
        }

    }

    public Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    /** Todo Create patient from CR data */
    private ExpectedTransferInPatients fillClientName(org.hl7.fhir.r4.model.Patient fhirPatient, ExpectedTransferInPatients expectedTransferInPatients) {
        String familyName = "";
        String givenName = "";
        String middleName = "";
        if (!fhirPatient.getName().get(0).isEmpty()) {
            if (fhirPatient.getName().get(0).getFamily() != null) {
                familyName = fhirPatient.getName().get(0).getFamily();
                System.out.println("Fhir patient family name here ==>" + familyName);
            }
            if (fhirPatient.getName().get(0).getGiven() != null) {
                //  String fullGivenName = fhirPatient.getName().get(0).getGiven().toString().replaceAll("[^a-zA-Z0-9]", " ");
                givenName = fhirPatient.getName().get(0).getGiven().get(0).toString();
                middleName = fhirPatient.getName().get(0).getGiven().get(1).toString();
                System.out.println("Fhir patient given name here ==>" + givenName);
                System.out.println("Fhir patient middle name here ==>" + middleName);

            }
            expectedTransferInPatients.setClientFirstName(familyName);
            expectedTransferInPatients.setClientMiddleName(middleName);
            expectedTransferInPatients.setClientLastName(givenName);
            expectedTransferInPatients.setClientFirstName(familyName);

            System.out.println("Fhir patient full name here ==>" + expectedTransferInPatients);

            return expectedTransferInPatients;
        }
        return null;
    }

    public SimpleObject addReferralCategoryAndReasons(@RequestParam("clientId") Integer clientId) throws Exception {

        //Update  referral category and reasons
        System.out.println("Client ID ==>" + clientId);
        System.out.println("Here ==>");
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

        ExpectedTransferInPatients patientReferral = Context.getService(KenyaEMRILService.class).getCommunityReferralsById(clientId);
        IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);

        ServiceRequest serviceRequest;
        SimpleObject referralsDetailsObject = null;
        if (patientReferral != null) {

            serviceRequest = parser.parseResource(ServiceRequest.class, patientReferral.getPatientSummary());
            System.out.println(serviceRequest.getCategory());
            System.out.println(serviceRequest.getCategory().get(0).getCoding().get(0).getDisplay());
            System.out.println(serviceRequest.getReasonCode().get(0).getCoding().get(0).getDisplay());
            System.out.println(serviceRequest.getReasonCode().get(0).getCoding().get(1).getDisplay());
            System.out.println(serviceRequest.getReasonCode().get(0).getCoding().get(2).getDisplay());
            System.out.println(serviceRequest.getReasonCode().get(0).getCoding().get(3).getDisplay());

            String category = "";
            if (!serviceRequest.getCategory().isEmpty()) {
                if (!serviceRequest.getCategory().get(0).getCoding().isEmpty()) {
                    category = serviceRequest.getCategory().get(0).getCoding().get(0).getDisplay();
                }
            }

        List<String> reasons = new ArrayList<>();

        for (CodeableConcept codeableConcept : serviceRequest.getReasonCode()) {
            if (!codeableConcept.getCoding().isEmpty()) {
                for (Coding code : codeableConcept.getCoding()) {
                    reasons.add(code.getDisplay());
                }
            }
        }

             referralsDetailsObject = SimpleObject.create("category", category,
                "reasonCode", String.join(", ", reasons)
        );
    }
        return referralsDetailsObject;
  }
}