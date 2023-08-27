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

@AppPage("kenyaemrilladmin.home")
public class CommunityReferralsHomePageController {

    public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException, ParseException {

        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        List<KenyaEMRInteropMessage> queueDataList = ilService.getAllMhealthOutboxMessagesByHl7Type(Arrays.asList(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE, ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE), false);
        List<KenyaEMRILMessageErrorQueue> errorQueueList = ilService.fetchAllMhealthErrors(Arrays.asList(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE, ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE));
        List<KenyaEMRILMessageArchive> archiveRecordList = ilService.fetchRecentArchives(Arrays.asList(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE, ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE));

        List<SimpleObject> queueList = new ArrayList<SimpleObject>();
        List<SimpleObject> archiveList = new ArrayList<SimpleObject>();
        List<SimpleObject> generalErrorList = new ArrayList<SimpleObject>();

        SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat yyyyMMddWithoutHiphen = new SimpleDateFormat("yyyyMMdd");

        ObjectMapper objectMapper = new ObjectMapper();
        for (KenyaEMRInteropMessage kenyaEMRILMessage : queueDataList) { // get records in queue

            String cccNumber = "";
            String messageType = "";
            String transferOutDate = "";
            String appointmentDate = "";
            String to_acceptance_date = "";
            String discontinuationReason = "";

            ILMessage ilMessage = objectMapper.readValue(kenyaEMRILMessage.getMessage(), ILMessage.class);

            if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE)) {
                messageType = "Discontinuation";

                Patient_Program_Discontinuation_Message patientProgramDiscontinuationMessage = ilMessage.extractHivDiscontinuationMessage();
                discontinuationReason = patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason();
                if (patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason().equals("Transfer Out")) {
                    transferOutDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getTransfer_out_date();
                    appointmentDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getSupporting_info().getAppointment_date();
                }
            } else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE)) {
                messageType = "Enrollment";
                Program_Enrollment_Message programEnrollmentMessage = ilMessage.getProgram_enrollment_message();
                to_acceptance_date = programEnrollmentMessage.getService_request().getTo_acceptance_date();
            }

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
                    "id", kenyaEMRILMessage.getId(),
                    "uuid", kenyaEMRILMessage.getUuid(),
                    "cccNumber", cccNumber,
                    "patientName", fullName,
                    "messageType", messageType,
                    "discontinuationReason", discontinuationReason,
                    "transferOutDate", Strings.isNullOrEmpty(transferOutDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(transferOutDate)),
                    "appointmentDate", Strings.isNullOrEmpty(appointmentDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(appointmentDate)),
                    "toAcceptanceDate", Strings.isNullOrEmpty(to_acceptance_date) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(to_acceptance_date)),
                    "dateCreated", yyyyMMdd.format(kenyaEMRILMessage.getDateCreated()));
            queueList.add(queueObject);
        }

        for (KenyaEMRILMessageArchive kenyaEMRILMessage : archiveRecordList) {

            String cccNumber = "";
            String messageType = "";
            String transferOutDate = "";
            String appointmentDate = "";
            String to_acceptance_date = "";
            String discontinuationReason = "";

            ILMessage ilMessage = objectMapper.readValue(kenyaEMRILMessage.getMessage(), ILMessage.class);

            if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE)) {
                messageType = "Discontinuation";

                Patient_Program_Discontinuation_Message patientProgramDiscontinuationMessage = ilMessage.extractHivDiscontinuationMessage();
                discontinuationReason = patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason();
                if (patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason().equals("Transfer Out")) {
                    transferOutDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getTransfer_out_date();
                    appointmentDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getSupporting_info().getAppointment_date();
                }
            } else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE)) {
                messageType = "Enrollment";
                Program_Enrollment_Message programEnrollmentMessage = ilMessage.getProgram_enrollment_message();
                to_acceptance_date = programEnrollmentMessage.getService_request().getTo_acceptance_date();
            }

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
                    "id", kenyaEMRILMessage.getId(),
                    "uuid", kenyaEMRILMessage.getUuid(),
                    "cccNumber", cccNumber,
                    "patientName", fullName,
                    "messageType", messageType,
                    "discontinuationReason", discontinuationReason,
                    "transferOutDate", Strings.isNullOrEmpty(transferOutDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(transferOutDate)),
                    "appointmentDate", Strings.isNullOrEmpty(appointmentDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(appointmentDate)),
                    "toAcceptanceDate", Strings.isNullOrEmpty(to_acceptance_date) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(to_acceptance_date)),
                    "dateCreated", yyyyMMdd.format(kenyaEMRILMessage.getDateCreated()));
            archiveList.add(queueObject);
        }

        for (KenyaEMRILMessageErrorQueue errorItem : errorQueueList) { // get records in error

            String cccNumber = "";
            String messageType = "";
            String transferOutDate = "";
            String appointmentDate = "";
            String to_acceptance_date = "";
            String discontinuationReason = "";

            ILMessage ilMessage = objectMapper.readValue(errorItem.getMessage(), ILMessage.class);

            if (errorItem.getHl7_type().equalsIgnoreCase(ILUtils.HL7_ACTIVE_REFERRAL_MESSAGE)) {
                messageType = "Discontinuation";

                Patient_Program_Discontinuation_Message patientProgramDiscontinuationMessage = ilMessage.extractHivDiscontinuationMessage();
                discontinuationReason = patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason();
                if (patientProgramDiscontinuationMessage.getDiscontinuation_message().getDiscontinuation_reason().equals("Transfer Out")) {
                    transferOutDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getTransfer_out_date();
                    appointmentDate = patientProgramDiscontinuationMessage.getDiscontinuation_message().getService_request().getSupporting_info().getAppointment_date();
                }
            } else if (errorItem.getHl7_type().equalsIgnoreCase(ILUtils.HL7_COMPLETE_REFERRAL_MESSAGE)) {
                messageType = "Enrollment";
                Program_Enrollment_Message programEnrollmentMessage = ilMessage.getProgram_enrollment_message();
                to_acceptance_date = programEnrollmentMessage.getService_request().getTo_acceptance_date();
            }

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

            SimpleObject errorObject = SimpleObject.create(
                    "id", errorItem.getId(),
                    "uuid", errorItem.getUuid(),
                    "cccNumber", cccNumber,
                    "patientName", fullName,
                    "messageType", messageType,
                    "discontinuationReason", discontinuationReason,
                    "transferOutDate", Strings.isNullOrEmpty(transferOutDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(transferOutDate)),
                    "appointmentDate", Strings.isNullOrEmpty(appointmentDate) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(appointmentDate)),
                    "toAcceptanceDate", Strings.isNullOrEmpty(to_acceptance_date) ? "" : yyyyMMdd.format(yyyyMMddWithoutHiphen.parse(to_acceptance_date)),
                    "error", errorItem.getStatus(),
                    "dateCreated", yyyyMMdd.format(errorItem.getDateCreated()));
            generalErrorList.add(errorObject);
        }

        model.put("queueList", ui.toJson(queueList));
        model.put("queueListSize", queueList.size());

        model.put("archiveList", ui.toJson(archiveList));
        model.put("archiveListSize", archiveList.size());

        model.put("generalErrorListSize", generalErrorList.size());
        model.put("generalErrorList", ui.toJson(generalErrorList));
    }
}
