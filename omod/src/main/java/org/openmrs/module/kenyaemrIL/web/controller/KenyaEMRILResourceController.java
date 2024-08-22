/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrIL.web.controller;


import ca.uhn.fhir.parser.IParser;
import com.google.common.base.Strings;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.fragment.controller.ReferralsDataExchangeFragmentController;
import org.openmrs.module.kenyaemrIL.fragment.controller.ShrSummariesFragmentController;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.SERVICE_REQUEST_SUPPORTING_INFO;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The web service controller.
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE)
public class KenyaEMRILResourceController extends MainResourceController {

    public static final String KENYAEMR_IL__NAMESPACE = "/kenyaemril";

    /**
     * @see org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController#getNamespace()
     */
    @Override
    public String getNamespace() {
        return RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/expected-ti-patient")
    @ResponseBody
    public SimpleObject getTransferInSummary(@RequestParam("patientUuid") Integer patientId) throws ParseException {
        Patient patient = Context.getPatientService().getPatient(patientId);
        List<PatientIdentifier> ccc = patient.getActiveIdentifiers().stream()
                .filter(id -> id.getIdentifierType().getUuid().equals("05ee9cf4-7242-4a17-b4d4-00f707265c8a"))
                .collect(Collectors.toList());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        SimpleObject object = new SimpleObject();
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        if (!ccc.isEmpty()) {
            List<ExpectedTransferInPatients> transferInPatient = service.getTransferInPatient(ccc.get(0).getIdentifier());
            ObjectMapper mapper = new ObjectMapper();
            if (!transferInPatient.isEmpty()) {
                Location locationByMflCode = Context.getService(KenyaEmrService.class).getLocationByMflCode(transferInPatient.get(0).getTransferOutFacility());
                object.put("transfer_out_facility", transferInPatient.get(0).getTransferOutFacility() + "-" + locationByMflCode.getName());
                object.put("upi_number", ccc.get(0).getIdentifier());
                try {
                    ILMessage ilMessage = mapper.readValue(transferInPatient.get(0).getPatientSummary().toLowerCase(), ILMessage.class);

                    Program_Discontinuation_Message discontinuation_message = ilMessage.getDiscontinuation_message();
                    SERVICE_REQUEST_SUPPORTING_INFO supporting_info = discontinuation_message.getService_request().getSupporting_info();
                    object.put("transfer_out_date", discontinuation_message.getService_request().getTransfer_out_date());
                    object.put("appointment_date", !Strings.isNullOrEmpty(supporting_info.getAppointment_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getAppointment_date()), "yyyy-MM-dd") : "");
                    object.put("hiv_enrollment_date", !Strings.isNullOrEmpty(supporting_info.getDate_first_enrolled()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_first_enrolled()), "yyyy-MM-dd") : "");
                    object.put("art_start_date", !Strings.isNullOrEmpty(supporting_info.getDate_started_art_at_transferring_facility()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_started_art_at_transferring_facility()), "yyyy-MM-dd") : "");
                    object.put("date_confirmed_positive", !Strings.isNullOrEmpty(supporting_info.getDate_confirmed_positive()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_confirmed_positive()), "yyyy-MM-dd") : "");
                    object.put("hiv_confirmation_facility", transferInPatient.get(0).getTransferOutFacility() + " - " + ILUtils.getLocationByMflCode(transferInPatient.get(0).getTransferOutFacility()));
                    object.put("who_stage", !Strings.isNullOrEmpty(supporting_info.getWho_stage()) ? supporting_info.getWho_stage() : "");
                    object.put("current_regimen", supporting_info.getCurrent_regimen());
                    object.put("on_art", "");
                    List<org.openmrs.ui.framework.SimpleObject> currentRegimen = supporting_info.getRegimen_change_history().stream()
                            .filter(simpleObject -> simpleObject.get("current").equals(true)).collect(Collectors.toList());
                    if (!currentRegimen.isEmpty()) {
                        object.put("on_art", "yes");
                        // object.put("current_regimen", currentRegimen.get(0).get("regimenShortDisplay"));
                    }

                    object.put("cd4_count", supporting_info.getCd4_value());
                    object.put("cd4_date", !Strings.isNullOrEmpty(supporting_info.getCd4_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getCd4_date()), "yyyy-MM-dd") : "");
                    if (!Strings.isNullOrEmpty(supporting_info.getViral_load())) {
                        if (supporting_info.getViral_load().equals("ldl")) {
                            object.put("vl_result", "200");
                        } else {
                            object.put("vl_result", supporting_info.getViral_load());
                        }
                    }
                    object.put("last_vl_date", !Strings.isNullOrEmpty(supporting_info.getLast_vl_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getLast_vl_date()), "yyyy-MM-dd") : "");
                    object.put("hbv_infected", "");
                    object.put("tb_infected", "");
                    object.put("arv_adherence_outcome", !Strings.isNullOrEmpty(supporting_info.getArv_adherence_outcome()) ? supporting_info.getArv_adherence_outcome() : "");
                    object.put("drug_allergies", !Strings.isNullOrEmpty(supporting_info.getDrug_allergies()) ? supporting_info.getDrug_allergies() : "");
                    if (!Strings.isNullOrEmpty(supporting_info.getTb_start_date()) && Strings.isNullOrEmpty(supporting_info.getTb_start_date())) {
                        object.put("tb_start_date", supporting_info.getTb_start_date());
                        object.put("tb_infected", "Yes");
                    } else {
                        object.put("tb_infected", "No");
                    }
                    if (!Strings.isNullOrEmpty(supporting_info.getTpt_start_date()) && Strings.isNullOrEmpty(supporting_info.getTpt_end_date())) {
                        object.put("tpt_start_date", supporting_info.getTpt_start_date());
                        object.put("on_tpt", "Yes");
                    } else {
                        object.put("on_tpt", "No");
                    }

                    object.put("is_pregnant", "");
                    object.put("is_breastfeeding", "");
                    object.put("weight", supporting_info.getWeight());
                    object.put("height", supporting_info.getHeight());

                    if (!supporting_info.getRegimen_change_history().isEmpty()) {
                        object.put("regimen_change", supporting_info.getRegimen_change_history());
                    } else {
                        object.put("regimen_change", new ArrayList<SimpleObject>());
                    }
                    if (!supporting_info.getPatient_ncds().isEmpty()) {
                        object.put("patient_ncds", supporting_info.getPatient_ncds());
                    } else {
                        object.put("patient_ncds", new ArrayList<SimpleObject>());
                    }
                    return object;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/shrPatientSummary")
    @ResponseBody
    public SimpleObject shrSummary(@RequestParam("patientUuid") String patientUuid) {
        SimpleObject result = null;
        try {
            result = ShrSummariesFragmentController.constructSHrSummary(patientUuid);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/communityReferrals")
    @ResponseBody
    public List<SimpleObject> fetchCommunityReferrals(@RequestParam("status") String status) {
        SimpleObject result = null;

        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

        List<SimpleObject> activeReferrals = new ArrayList<>();
        List<SimpleObject> completedReferrals = new ArrayList<>();

        if (status.equals("active")) {
            // Filter all community referrals with active referral status
            List<ExpectedTransferInPatients> activeCommunityReferralsList = ilService.getCommunityReferrals("COMMUNITY", "ACTIVE");
            for (ExpectedTransferInPatients expectedTransferInPatients : activeCommunityReferralsList) {
                IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
                ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, expectedTransferInPatients.getPatientSummary());
                String requester = "";
                if (serviceRequest.hasRequester()) {
                    if (serviceRequest.getRequester().getDisplay() != null) {
                        Location location = ILUtils.getLocationByMflCode(serviceRequest.getRequester().getDisplay());
                        if (location != null) {
                            requester = location.getName();
                        } else {
                            requester = "Community";
                        }

                    } else if (serviceRequest.getRequester().getIdentifier() != null && serviceRequest.getRequester().getIdentifier().getValue() != null) {
                        Location location = ILUtils.getLocationByMflCode(serviceRequest.getRequester().getIdentifier().getValue());
                        if (location != null) {
                            requester = location.getName();
                        } else {
                            requester = "Community";
                        }
                    }
                }

                SimpleObject activeReferralsObject = new SimpleObject();
                activeReferralsObject.put("id", expectedTransferInPatients.getId());
                activeReferralsObject.put("uuid", expectedTransferInPatients.getUuid());
                activeReferralsObject.put("nupi", expectedTransferInPatients.getNupiNumber());
                activeReferralsObject.put("dateReferred", serviceRequest.getAuthoredOn() != null ? new SimpleDateFormat("yyyy-MM-dd").format(serviceRequest.getAuthoredOn()) : "");
                activeReferralsObject.put("referredFrom", requester);
                activeReferralsObject.put("givenName", expectedTransferInPatients.getClientFirstName() != null ? expectedTransferInPatients.getClientFirstName() : "");
                activeReferralsObject.put("middleName", expectedTransferInPatients.getClientMiddleName() != null ? expectedTransferInPatients.getClientMiddleName() : "");
                activeReferralsObject.put("familyName", expectedTransferInPatients.getClientLastName() != null ? expectedTransferInPatients.getClientLastName() : "");
                activeReferralsObject.put("birthdate", formatDate(expectedTransferInPatients.getClientBirthDate()));
                activeReferralsObject.put("gender", expectedTransferInPatients.getClientGender());
                activeReferralsObject.put("status", expectedTransferInPatients.getReferralStatus());
                activeReferralsObject.add("referralReasons", extractReferralReasons(serviceRequest));
                activeReferrals.add(activeReferralsObject);
            }
            return activeReferrals;

        } else if (status.equals("completed")) {

            // Filter all community referrals with active referral status
            List<ExpectedTransferInPatients> completedCommunityReferralsList = ilService.getCommunityReferrals("COMMUNITY", "COMPLETED");

            for (ExpectedTransferInPatients expectedTransferInPatients : completedCommunityReferralsList) {
                IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
                ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, expectedTransferInPatients.getPatientSummary());
                String requester = "";
                if (serviceRequest.hasRequester()) {
                    if (serviceRequest.getRequester().getDisplay() != null) {
                        requester = serviceRequest.getRequester().getDisplay();
                    }
                }


                SimpleObject completedReferralsObject = new SimpleObject();
                completedReferralsObject.put("id", expectedTransferInPatients.getId());
                completedReferralsObject.put("uuid", expectedTransferInPatients.getUuid());
                completedReferralsObject.put("nupi", expectedTransferInPatients.getNupiNumber());
                completedReferralsObject.put("dateReferred", serviceRequest.getAuthoredOn() != null ? new SimpleDateFormat("yyyy-MM-dd").format(serviceRequest.getAuthoredOn()) : "");
                completedReferralsObject.put("referredFrom", requester);
                completedReferralsObject.put("givenName", expectedTransferInPatients.getClientFirstName() != null ? expectedTransferInPatients.getClientFirstName() : "");
                completedReferralsObject.put("middleName", expectedTransferInPatients.getClientMiddleName() != null ? expectedTransferInPatients.getClientMiddleName() : "");
                completedReferralsObject.put("familyName", expectedTransferInPatients.getClientLastName() != null ? expectedTransferInPatients.getClientLastName() : "");
                completedReferralsObject.put("birthdate", formatDate(expectedTransferInPatients.getClientBirthDate()));
                completedReferralsObject.put("gender", expectedTransferInPatients.getClientGender());
                completedReferralsObject.put("status", expectedTransferInPatients.getReferralStatus());
                completedReferralsObject.add("referralReasons", extractReferralReasons(serviceRequest));
                completedReferrals.add(completedReferralsObject);
            }
            return completedReferrals;
        }

        return new ArrayList<>();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/serveReferredClient")
    @ResponseBody
    public ResponseEntity<SimpleObject> processReferredPatient(@RequestBody String referralMessage) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject responseObj = (JSONObject) parser.parse(referralMessage);
        Long messageId = (Long) responseObj.get("referralMessageId");
        Patient patient = processReferral(messageId.intValue());
        return ResponseEntity.ok()
                .body(formattedResponse(patient));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/pullShrReferrals")
    @ResponseBody
    public SimpleObject pullShrReferrals() throws Exception {
        ReferralsDataExchangeFragmentController component =
                Context.getRegisteredComponent("referralsDataExchangeFragmentController", ReferralsDataExchangeFragmentController.class);

        org.openmrs.ui.framework.SimpleObject result = component.pullCommunityReferralsFromFhir();
        SimpleObject formattedResponse = new SimpleObject();
        formattedResponse.put("result", result.get("status"));
        return formattedResponse;
    }

    public String formatDate(Date date) {
        DateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy");
        return date == null ? "" : dateFormatter.format(date);
    }

    public SimpleObject extractReferralReasons(ServiceRequest serviceRequest) {
        SimpleObject referralReasons = new SimpleObject();

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


        String note = "";
        if (!serviceRequest.getNote().isEmpty()) {
            note = serviceRequest.getNoteFirstRep().getText();
        }

        referralReasons.put("category", String.join(",  ", category));
        referralReasons.put("reasonCode", String.join(", ", reasons));
        referralReasons.put("referralDate", referralDate);
        referralReasons.put("clinicalNote", note);
        return referralReasons;
    }

    public Patient processReferral(Integer referralMessageId) throws Exception {
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        ExpectedTransferInPatients referred = service.getCommunityReferralsById(referralMessageId);
        ReferralsDataExchangeFragmentController component =
                Context.getRegisteredComponent("referralsDataExchangeFragmentController", ReferralsDataExchangeFragmentController.class);
        Patient patient = component.registerReferredPatient(referred);
        if (patient != null) {
            referred.setReferralStatus("COMPLETED");
            referred.setPatient(patient);
            service.createPatient(referred);
        }

        return patient;
    }

    public SimpleObject formattedResponse(Patient patient) {
        SimpleObject response = new SimpleObject();
        response.put("uuid", patient.getUuid());
        response.put("givenName", patient.getGivenName());
        return response;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/communityReferralByNupi")
    @ResponseBody
    public SimpleObject fetchCommunityReferralByNupi(@RequestParam("nupi") String nupi) {

        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        SimpleObject referralsObject = new SimpleObject();
        ExpectedTransferInPatients expectedTransferInPatients = ilService.getCommunityReferralByNupi(nupi);
        if (expectedTransferInPatients != null) {
            IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
            ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class,
                    expectedTransferInPatients.getPatientSummary());
            String requester = "";
            if (serviceRequest.hasRequester()) {
                if (serviceRequest.getRequester().getDisplay() != null) {
                    Location location = ILUtils.getLocationByMflCode(serviceRequest.getRequester().getDisplay());
                    if (location != null) {
                        requester = location.getName();
                    } else {
                        requester = "Community";
                    }

                } else if (serviceRequest.getRequester().getIdentifier() != null
                        && serviceRequest.getRequester().getIdentifier().getValue() != null) {
                    Location location = ILUtils
                            .getLocationByMflCode(serviceRequest.getRequester().getIdentifier().getValue());
                    if (location != null) {
                        requester = location.getName();
                    } else {
                        requester = "Community";
                    }
                }
            }

            referralsObject.put("status", expectedTransferInPatients.getReferralStatus());
            referralsObject.add("referralReasons", extractReferralReasons(serviceRequest));
            referralsObject.put("referredFrom", requester);
        } else {
            referralsObject.put("status", new ArrayList<>());
            referralsObject.add("referralReasons", new ArrayList<>());
            referralsObject.put("referredFrom", new ArrayList<>());
        }

        return referralsObject;
    }

}

