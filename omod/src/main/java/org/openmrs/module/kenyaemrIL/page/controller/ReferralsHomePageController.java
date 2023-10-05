package org.openmrs.module.kenyaemrIL.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Patient_Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.programEnrollment.Program_Enrollment_Message;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AppPage("kenyaemr.referral.home")
public class ReferralsHomePageController {

    public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException, ParseException {

        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        List<ExpectedTransferInPatients> activeReferralsDataList = ilService.getCommunityReferrals("HIV","ACTIVE");
        List<ExpectedTransferInPatients> completedReferralsDataList = ilService.getCommunityReferrals("HIV","COMPLETED");

        List<SimpleObject> queueList = new ArrayList<SimpleObject>();
        List<SimpleObject> archiveList = new ArrayList<SimpleObject>();
        List<SimpleObject> generalErrorList = new ArrayList<SimpleObject>();

        SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat yyyyMMddWithoutHiphen = new SimpleDateFormat("yyyyMMdd");

        ObjectMapper objectMapper = new ObjectMapper();
        for (ExpectedTransferInPatients expectedTransferInPatient : activeReferralsDataList) { // get records in queue

            String cccNumber = "";
            String messageType = "";
            String transferOutDate = "";
            String appointmentDate = "";
            String to_acceptance_date = "";
            String discontinuationReason = "";

            ILMessage ilMessage = objectMapper.readValue(expectedTransferInPatient.getPatientSummary().toLowerCase(), ILMessage.class);

            // 1. Fetch the person to update using the CCC number
            for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification()
                    .getInternal_patient_id()) {
                if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                    cccNumber = internalPatientId.getId().replaceAll("\\D", "");
                    break;
                }
            }

            // get patient name
            PATIENT_IDENTIFICATION patientIdentification = ilMessage.getPatient_identification();

            PATIENT_NAME patientName = patientIdentification.getPatient_name();
            String fullName = "";
            if (patientName != null) {
                if (patientName.getFirst_name() != null) {
                    fullName = patientName.getFirst_name() + " ";
                }
                if (patientName.getMiddle_name() != null) {
                    fullName = fullName + patientName.getMiddle_name() + " ";
                }
                if (patientName.getLast_name() != null) {
                    fullName = fullName + patientName.getLast_name();
                }
            }

            SimpleObject queueObject = SimpleObject.create(
                    "id", expectedTransferInPatient.getId(),
                    "uuid", expectedTransferInPatient.getUuid(),
                    "cccNumber", cccNumber,
                    "patientName", fullName,
                    "messageType", messageType,
                    "discontinuationReason", discontinuationReason,
                    "transferOutDate", Strings.isNullOrEmpty(transferOutDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(transferOutDate)),
                    "appointmentDate", Strings.isNullOrEmpty(appointmentDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(appointmentDate)),
                    "toAcceptanceDate", Strings.isNullOrEmpty(to_acceptance_date) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(to_acceptance_date)),
                    "dateCreated", yyyyMMdd.format(expectedTransferInPatient.getDateCreated()));
            queueList.add(queueObject);
        }

        for (ExpectedTransferInPatients expectedTransferInPatient : completedReferralsDataList) {

            String cccNumber = "";
            String messageType = "";
            String transferOutDate = "";
            String appointmentDate = "";
            String to_acceptance_date = "";
            String discontinuationReason = "";

            ILMessage ilMessage = objectMapper.readValue(expectedTransferInPatient.getPatientSummary().toLowerCase(), ILMessage.class);

            // 1. Fetch the person to update using the CCC number
            for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification()
                    .getInternal_patient_id()) {
                if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                    cccNumber = internalPatientId.getId().replaceAll("\\D", "");
                    break;
                }
            }

            // get patient name
            PATIENT_IDENTIFICATION patientIdentification = ilMessage.getPatient_identification();

            PATIENT_NAME patientName = patientIdentification.getPatient_name();
            String fullName = "";
            if (patientName != null) {
                if (patientName.getFirst_name() != null) {
                    fullName = patientName.getFirst_name() + " ";
                }
                if (patientName.getMiddle_name() != null) {
                    fullName = fullName + patientName.getMiddle_name() + " ";
                }
                if (patientName.getLast_name() != null) {
                    fullName = fullName + patientName.getLast_name();
                }
            }

            SimpleObject queueObject = SimpleObject.create(
                    "id", expectedTransferInPatient.getId(),
                    "uuid", expectedTransferInPatient.getUuid(),
                    "cccNumber", cccNumber,
                    "patientName", fullName,
                    "messageType", messageType,
                    "discontinuationReason", discontinuationReason,
                    "transferOutDate", Strings.isNullOrEmpty(transferOutDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(transferOutDate)),
                    "appointmentDate", Strings.isNullOrEmpty(appointmentDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(appointmentDate)),
                    "toAcceptanceDate", Strings.isNullOrEmpty(to_acceptance_date) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(to_acceptance_date)),
                    "dateCreated", yyyyMMdd.format(expectedTransferInPatient.getDateCreated()));
            archiveList.add(queueObject);
        }

        model.put("queueList", ui.toJson(queueList));
        model.put("queueListSize", queueList.size());

        model.put("archiveList", ui.toJson(archiveList));
        model.put("archiveListSize", archiveList.size());

        model.put("generalErrorListSize", generalErrorList.size());
        model.put("generalErrorList", ui.toJson(generalErrorList));
    }
}
