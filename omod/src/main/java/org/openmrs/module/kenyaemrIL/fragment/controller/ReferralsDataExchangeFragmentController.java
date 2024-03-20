package org.openmrs.module.kenyaemrIL.fragment.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.SecurityMetadata;
import org.openmrs.module.kenyaemr.nupi.UpiUtilsDataExchange;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * controller for pivotTableCharts fragment
 */
@Component
public class ReferralsDataExchangeFragmentController {
    // Logger
    private static final Logger log = LoggerFactory.getLogger(ReferralsDataExchangeFragmentController.class);
    public static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
    public static final String NUPI = "f85081e2-b4be-4e48-b3a4-7994b69bb101";

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
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        if (Strings.isNullOrEmpty(getDefaultLocationMflCode())) { // let us know early if this is not configured
            return SimpleObject.create("status", "Fail",
                    "message", "Facility mfl cannot be empty");
        }
        org.hl7.fhir.r4.model.Resource fhirServiceRequestResource;
        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = null;
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

        Bundle serviceRequestResourceBundle = fhirConfig.fetchAllReferralsByFacility(getDefaultLocationMflCode());

        if (serviceRequestResourceBundle == null) {
            return SimpleObject.create("status", "Fail",
                    "message", "There was an error pulling referrals. Please verify all configurations");
        } else if (serviceRequestResourceBundle.getEntry().isEmpty()) { // entry is empty if the bundle has no data
            return SimpleObject.create("status", "Success",
                    "message", "Got no referrals to pull");
        } else if (!serviceRequestResourceBundle.getEntry().isEmpty()) {

            int validMessages = 0;
            for (int i = 0; i < serviceRequestResourceBundle.getEntry().size(); i++) {
                fhirServiceRequestResource = serviceRequestResourceBundle.getEntry().get(i).getResource();

                if (fhirServiceRequestResource != null) {
                    fhirServiceRequest = (org.hl7.fhir.r4.model.ServiceRequest) fhirServiceRequestResource;

                    if (fhirServiceRequest.hasPerformer() && fhirServiceRequest.getPerformerFirstRep().getIdentifier() != null && fhirServiceRequest.getPerformerFirstRep().getIdentifier().getValue() != null) {
                        if (fhirServiceRequest.getPerformerFirstRep().getIdentifier().getValue().equals(getDefaultLocationMflCode())) {
                            String nupiNumber = fhirServiceRequest.getSubject() != null && fhirServiceRequest.getSubject().getIdentifier() != null ? fhirServiceRequest.getSubject().getIdentifier().getValue() : "";
                            if (Strings.isNullOrEmpty(nupiNumber)) {
                                continue;
                            }
                            System.out.println("NUPI :  ==>" + nupiNumber);
                            if (nupiNumber != null && service.getCommunityReferralByNupi(nupiNumber) == null) {
                                GlobalProperty globalTokenUrl = Context.getAdministrationService().getGlobalPropertyObject(CommonMetadata.GP_CLIENT_VERIFICATION_QUERY_UPI_END_POINT);
                                if (globalTokenUrl != null && !Strings.isNullOrEmpty(globalTokenUrl.getPropertyValue())) {
                                    String serverUrl = globalTokenUrl.getPropertyValue() + "/" + nupiNumber;
                                    persistReferralData(getCRPatient(serverUrl), fhirConfig, fhirServiceRequest, "COMMUNITY");
                                    validMessages++;
                                }
                            }
                        }
                    } else if (fhirServiceRequest.hasPerformer() && fhirServiceRequest.getPerformerFirstRep().getDisplay() != null) {
                        if (fhirServiceRequest.getPerformerFirstRep().getDisplay().equals(getDefaultLocationMflCode())) {
                            String nupiNumber = fhirServiceRequest.getSubject() != null && fhirServiceRequest.getSubject().getIdentifier() != null ? fhirServiceRequest.getSubject().getIdentifier().getValue() : "";
                            if (Strings.isNullOrEmpty(nupiNumber)) {
                                continue;
                            }
                            if (nupiNumber != null && service.getCommunityReferralByNupi(nupiNumber) == null) {
                                GlobalProperty globalTokenUrl = Context.getAdministrationService().getGlobalPropertyObject(CommonMetadata.GP_CLIENT_VERIFICATION_QUERY_UPI_END_POINT);
                                if (globalTokenUrl != null && !Strings.isNullOrEmpty(globalTokenUrl.getPropertyValue())) {
                                    String serverUrl = globalTokenUrl.getPropertyValue() + "/" + nupiNumber;
                                    persistReferralData(getCRPatient(serverUrl), fhirConfig, fhirServiceRequest, "COMMUNITY");
                                    validMessages++;
                                }
                            }
                        }
                    }
                }
            }

            return SimpleObject.create("status", "Success",
                    "message", "Successfully pulled " + validMessages + " referral(s)");

        } else {
            return SimpleObject.create("status", "Success",
                    "message", "Could not understand response from the server!");
        }
    }

    /**
     * Updates a referral message once a patient is served by a provider
     *
     * @param referral
     * @return
     * @throws Exception
     */
    public SimpleObject updateShrReferral(@RequestParam("patientId") Integer referral) {
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        ExpectedTransferInPatients referred = service.getCommunityReferralsById(referral);
        PersonAttribute referralStatusAttribute = new PersonAttribute();
        PersonAttributeType referralStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("df7e9996-23b5-4f66-a799-97498d19850d");
        if (referralStatusAttributeType != null) {
            referralStatusAttribute.setAttributeType(referralStatusAttributeType);
            referralStatusAttribute.setValue("Completed");
            referred.getPatient().addAttribute(referralStatusAttribute);
            Context.getPatientService().savePatient(referred.getPatient());
        }

        IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
        ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, referred.getPatientSummary());
        System.out.println(serviceRequest.getStatus());
        System.out.println(serviceRequest.getCategory().get(0).getCoding().get(0).getDisplay());
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
        try {
            fhirConfig.postReferralResourceToOpenHim(serviceRequest);
            return SimpleObject.create("Success");
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    /**
     * Create client referral data from CR data
     */
    public void persistReferralData(JSONObject crClient, FhirConfig fhirConfig, org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest, String serviceType) {
        if (crClient == null) {
            System.out.println("Patient not found in CR");
            return;
        }
        ExpectedTransferInPatients expectedTransferInPatients = new ExpectedTransferInPatients();
        expectedTransferInPatients.setClientFirstName(String.valueOf(crClient.get("firstName")));
        expectedTransferInPatients.setClientMiddleName(String.valueOf(crClient.get("middleName")));
        expectedTransferInPatients.setClientLastName(String.valueOf(crClient.get("lastName")));
        expectedTransferInPatients.setNupiNumber(String.valueOf(crClient.get("clientNumber")));
        expectedTransferInPatients.setClientBirthDate(getDateFromString((String) crClient.get("dateOfBirth"), "yyyy-mm-dd"));
        String gender = String.valueOf(crClient.get("gender"));
        if (gender.equalsIgnoreCase("male")) {
            gender = "M";
        } else if (gender.equalsIgnoreCase("female")) {
            gender = "F";
        }
        expectedTransferInPatients.setClientGender(gender);
        expectedTransferInPatients.setReferralStatus("ACTIVE");
        expectedTransferInPatients.setPatientSummary(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(fhirServiceRequest));
        expectedTransferInPatients.setServiceType(serviceType);
        Context.getService(KenyaEMRILService.class).createPatient(expectedTransferInPatients);
        System.out.println("Successfully persisted in expected referrals model ==>");
    }

    public SimpleObject completeClientReferral(@RequestParam("patientId") Integer referral) throws Exception {
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        ExpectedTransferInPatients referred = service.getCommunityReferralsById(referral);
        Patient patient = registerReferredPatient(referred);
        if (patient != null) {
            referred.setReferralStatus("COMPLETED");
            referred.setPatient(patient);
            service.createPatient(referred);

            return SimpleObject.create("patientId", patient.getPatientId());
        }

        return SimpleObject.create("patientId", "");
    }

    public SimpleObject artReferralsHandler(@RequestParam("patientId") Integer referral) throws Exception {
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        ExpectedTransferInPatients referred = service.getCommunityReferralsById(referral);
        ObjectMapper objectMapper = new ObjectMapper();
        ILMessage ilMessage = objectMapper.readValue(referred.getPatientSummary().toLowerCase(), ILMessage.class);
        Patient patient = registerArtReferralPatient(ilMessage.getPatient_identification());
        if (patient != null) {
            referred.setReferralStatus("COMPLETED");
            referred.setPatient(patient);
            service.createPatient(referred);
            if(Context.getAuthenticatedUser().containsRole(SecurityMetadata._Role.CLINICIAN)) {
                return SimpleObject.create("patientId", patient.getPatientId(), "isClinician",true);
            } else {
                return SimpleObject.create("patientId", patient.getPatientId(), "isClinician",false);
            }

        }

        return SimpleObject.create("patientId", "");
    }

    public Location loggedInUserFacility() {
        List<Location> facility = ILUtils.getFacilityByLoggedInUser();
        Location defaultFacility = null;
        if (!facility.isEmpty()) {
            defaultFacility = facility.get(0);
        } else {
            defaultFacility = getDefaultLocation();
        }
        return getDefaultLocation();
    }

    public String getDefaultLocationMflCode() {
        String mflCodeAttribute = "8a845a89-6aa5-4111-81d3-0af31c45c002";
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATION_ATTRIBUTE_TYPES);
            Location location = loggedInUserFacility();
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

    /**
     * Create referred patient
     */
    public Patient registerReferredPatient(ExpectedTransferInPatients referredPatient) {
        List<Patient> results = Context.getPatientService().getPatients(referredPatient.getNupiNumber());
        //Assign  active referral_status attribute
        PersonAttribute referralStatusAttribute = new PersonAttribute();
        PersonAttributeType referralStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("df7e9996-23b5-4f66-a799-97498d19850d");

        if (!results.isEmpty()) {
            if (referralStatusAttributeType != null) {
                referralStatusAttribute.setAttributeType(referralStatusAttributeType);
                referralStatusAttribute.setValue("Active");
                results.get(0).addAttribute(referralStatusAttribute);
            }
            return Context.getPatientService().savePatient(results.get(0));
        } else {
            Patient patient = new Patient();
            PersonName pn = new PersonName();
            pn.setGivenName(referredPatient.getClientFirstName());
            pn.setMiddleName(referredPatient.getClientMiddleName());
            pn.setFamilyName(referredPatient.getClientLastName());
            patient.addName(pn);
            patient.setBirthdate(new Date());
            patient.setGender(referredPatient.getClientGender());
            PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NUPI);
            PatientIdentifier fetchedNupi = new PatientIdentifier(referredPatient.getNupiNumber(), nupiIdType, getDefaultLocation());
            patient.addIdentifier(fetchedNupi);

            PatientIdentifierType openMrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
            String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openMrsIdType, "Registration");
            PatientIdentifier openMrsId = new PatientIdentifier(generated, openMrsIdType, getDefaultLocation());
            patient.addIdentifier(openMrsId);
            if (!patient.getPatientIdentifier().isPreferred()) {
                openMrsId.setPreferred(true);
            }

            if (referralStatusAttributeType != null) {
                referralStatusAttribute.setAttributeType(referralStatusAttributeType);
                referralStatusAttribute.setValue("Active");
                patient.addAttribute(referralStatusAttribute);
            }
            return Context.getPatientService().savePatient(patient);
        }
    }

    private Patient registerArtReferralPatient(PATIENT_IDENTIFICATION patient_identification) throws ParseException, java.text.ParseException {
        for (INTERNAL_PATIENT_ID internalPatientId : patient_identification.getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                List<Patient> results = Context.getPatientService().getPatients(internalPatientId.getId());
                if (!results.isEmpty()) return results.get(0);
            } else if (internalPatientId.getIdentifier_type().equalsIgnoreCase("NUPI")) {
                List<Patient> results = Context.getPatientService().getPatients(internalPatientId.getId().toUpperCase());
                if (!results.isEmpty()) return results.get(0);
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Person person = new Person();
        PersonName personName = new PersonName(patient_identification.getPatient_name().getLast_name(),
                patient_identification.getPatient_name().getMiddle_name(), patient_identification.getPatient_name().getFirst_name());
        person.addName(personName);
        PatientIdentifierType cccIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        PatientIdentifierType upiIdType = Context.getPatientService().getPatientIdentifierTypeByUuid("f85081e2-b4be-4e48-b3a4-7994b69bb101");
        if (patient_identification.getDate_of_birth().equals("")) {
            System.out.println("NullPointerException ================> Age cannot be null");
            return null;
        }
        person.setBirthdate(getDateFromString(patient_identification.getDate_of_birth(),"yyyyMMdd" ));
        person.setBirthdateEstimated(Boolean.getBoolean(patient_identification.getDate_of_birth_precision()));
        if (patient_identification.getSex() == null) {
            System.out.println("NullPointerException ================> Gender cannot be null");
            person.setGender("F");
        } else {
            person.setGender(patient_identification.getSex().toUpperCase());
        }
        PersonAttribute phoneNumber = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Telephone contact"), patient_identification.getPhone_number());
        PersonAttribute maritalStatus = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Civil Status"), patient_identification.getMarital_status());
        person.addAttribute(phoneNumber);
        person.addAttribute(maritalStatus);

        PersonAddress personAddress = getPersonAddress(patient_identification);
        person.addAddress(personAddress);
        Context.getPersonService().savePerson(person);
        Patient patient = new Patient(person);
        for (INTERNAL_PATIENT_ID internalPatientId : patient_identification.getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                PatientIdentifier ccc = new PatientIdentifier(internalPatientId.getId(), cccIdType, MessageHeaderSingleton.getDefaultLocation());
                ccc.setPreferred(true);
                patient.addIdentifier(ccc);
            } else if (internalPatientId.getIdentifier_type().equalsIgnoreCase("NUPI")) {
                patient.addIdentifier(new PatientIdentifier(internalPatientId.getId().toUpperCase(), upiIdType, MessageHeaderSingleton.getDefaultLocation()));
            }
        }
        // Assign a patient an OpenMRS ID
        String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
        PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);

        String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "Registration");
        PatientIdentifier omrsId = new PatientIdentifier(generated, openmrsIdType, MessageHeaderSingleton.getDefaultLocation());
        omrsId.setPreferred(true);
        patient.addIdentifier(omrsId);

        return Context.getPatientService().savePatient(patient);
    }

    private static PersonAddress getPersonAddress(PATIENT_IDENTIFICATION patient_identification) {
        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddress6(patient_identification.getPatient_address().getPhysical_address().getWard());
        personAddress.setCountyDistrict(patient_identification.getPatient_address().getPhysical_address().getCounty());
        personAddress.setAddress2(patient_identification.getPatient_address().getPhysical_address().getNearest_landmark());
        personAddress.setAddress4(patient_identification.getPatient_address().getPhysical_address().getSub_county());
        personAddress.setCityVillage(patient_identification.getPatient_address().getPhysical_address().getVillage());
        return personAddress;
    }


    public SimpleObject addReferralCategoryAndReasons(@RequestParam("clientId") Integer clientId) throws Exception {

        //Update  referral category and reasons
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

        ExpectedTransferInPatients patientReferral = Context.getService(KenyaEMRILService.class).getCommunityReferralsById(clientId);
        IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);

        ServiceRequest serviceRequest;
        SimpleObject referralsDetailsObject = null;
        List<SimpleObject> list = new ArrayList<>();
        if (patientReferral != null) {

            serviceRequest = parser.parseResource(ServiceRequest.class, patientReferral.getPatientSummary());

            Set<String> category = new HashSet<>();
            String referralDate = "";
            if (!serviceRequest.getCategory().isEmpty()) {
                for (CodeableConcept c : serviceRequest.getCategory()) {
                    for (Coding code : c.getCoding()) {
                        category.add(code.getDisplay());
                    }
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
            if (serviceRequest.getAuthoredOn() != null) {
                referralDate = new SimpleDateFormat("yyyy-MM-dd").format(serviceRequest.getAuthoredOn());
            }

            if (!serviceRequest.getSupportingInfo().isEmpty()) {
                for (Reference r : serviceRequest.getSupportingInfo()) {
                    SimpleObject object = new SimpleObject();
                    String obsId = r.getReference();
                    System.out.println("OBS ID " + obsId);

                    Resource resource = fhirConfig.fetchFhirResource("Observation", obsId);
                    if (resource == null) {
                        break;
                    }
                    Observation observation = (Observation) resource;

                    List<String> theTest = new ArrayList<>();
                    List<String> theFindings = new ArrayList<>();
                    String theTxPlan = "";

                    if (observation.getCode() != null && !observation.getCode().getCoding().isEmpty()) {
                        for (Coding c : observation.getCode().getCoding()) {
                            String display = !Strings.isNullOrEmpty(c.getDisplay()) ? c.getDisplay() : c.getCode();
                            theTest.add(display);
                        }
                    }

                    if (observation.getValue() != null) {
                        CodeableConcept codeableConcept = (CodeableConcept) observation.getValue();
                        if (!codeableConcept.getCoding().isEmpty()) {
                            for (Coding c : codeableConcept.getCoding()) {
                                String display = !Strings.isNullOrEmpty(c.getDisplay()) ? c.getDisplay() : c.getCode();
                                theFindings.add(display);
                            }
                        }
                    }


                    if (!observation.getNote().isEmpty() && !Strings.isNullOrEmpty(observation.getNoteFirstRep().getText())) {
                        System.out.println("OBS NOTE" + observation.getCode().getCodingFirstRep().getCode());
                        theTxPlan = observation.getNoteFirstRep().getText();
                    }

                    object.put("theTests", String.join(" ,", theTest));
                    object.put("theFindings", String.join(" ,", theFindings));
                    object.put("theTxPlan", theTxPlan);
                    list.add(object);
                }
            }

            String note = "";
            if (!serviceRequest.getNote().isEmpty()) {
               note = serviceRequest.getNoteFirstRep().getText();
            }

            referralsDetailsObject = SimpleObject.create("category", String.join(",  ", category),
                    "reasonCode", String.join(", ", reasons),
                    "referralDate", referralDate,
                    "clinicalNote", note,
                    "cancerReferral", list
            );

        }
        return referralsDetailsObject;
    }

    public Date getDateFromString(String date, String pattern) {
        Date result = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            result =  sdf.parse(date);
        }
        catch (Exception e) {
            System.out.println("Error: "+e.toString());
        }

        return result;
    }
}