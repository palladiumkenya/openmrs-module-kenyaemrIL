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


import com.google.common.base.Strings;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.SERVICE_REQUEST_SUPPORTING_INFO;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.fragment.controller.ShrSummariesFragmentController;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;


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
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        SimpleObject object = new SimpleObject();
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        List<ExpectedTransferInPatients> transferInPatient = service.getTransferInPatient(Context.getPatientService().getPatient(patientId));
        ObjectMapper mapper = new ObjectMapper();
        if (!transferInPatient.isEmpty()) {
            Location locationByMflCode = Context.getService(KenyaEmrService.class).getLocationByMflCode(transferInPatient.get(0).getTransferOutFacility());
            object.put("transfer_out_facility", transferInPatient.get(0).getTransferOutFacility() + "-" + locationByMflCode.getName());
            List<PatientIdentifier> patientIdentifier = Context.getPatientService().getPatient(patientId).getActiveIdentifiers().stream()
                    .filter(pid -> pid.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")).collect(Collectors.toList());
            object.put("upi_number", !patientIdentifier.isEmpty() ? patientIdentifier.get(0).getIdentifier() : "");
            try {
                ILMessage ilMessage = mapper.readValue(transferInPatient.get(0).getPatientSummary().toLowerCase(), ILMessage.class);

                Program_Discontinuation_Message discontinuation_message = ilMessage.getDiscontinuation_message();
                SERVICE_REQUEST_SUPPORTING_INFO supporting_info = discontinuation_message.getService_request().getSupporting_info();
                object.put("transfer_out_date", discontinuation_message.getService_request().getTransfer_out_date());
                object.put("appointment_date", !Strings.isNullOrEmpty(supporting_info.getAppointment_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getAppointment_date()) , "yyyy-MM-dd"): "");
                object.put("hiv_enrollment_date", !Strings.isNullOrEmpty(supporting_info.getDate_first_enrolled()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_first_enrolled()),"yyyy-MM-dd") : "");
                object.put("art_start_date", !Strings.isNullOrEmpty(supporting_info.getDate_started_art_at_transferring_facility()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_started_art_at_transferring_facility()),"yyyy-MM-dd") : "");
                object.put("date_confirmed_positive", !Strings.isNullOrEmpty(supporting_info.getDate_confirmed_positive()) ? DateFormatUtils.format(formatter.parse(supporting_info.getDate_confirmed_positive()),"yyyy-MM-dd") : "");
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
                object.put("cd4_date", !Strings.isNullOrEmpty(supporting_info.getCd4_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getCd4_date()),"yyyy-MM-dd") : "");
                object.put("vl_result", !Strings.isNullOrEmpty(supporting_info.getViral_load()) ? supporting_info.getViral_load() : "");
                object.put("last_vl_date", !Strings.isNullOrEmpty(supporting_info.getLast_vl_date()) ? DateFormatUtils.format(formatter.parse(supporting_info.getLast_vl_date()),"yyyy-MM-dd") : "");
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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return object;
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
}
