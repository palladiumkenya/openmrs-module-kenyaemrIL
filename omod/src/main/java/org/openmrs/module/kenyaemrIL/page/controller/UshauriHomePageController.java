package org.openmrs.module.kenyaemrIL.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_VISIT;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;
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
import java.util.Date;
import java.util.List;

@AppPage("kenyaemr.ushauri.home")
public class UshauriHomePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException {
		
		KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
		List<KenyaEMRInteropMessage> queueDataList = ilService.getAllMhealthOutboxMessagesByHl7Type(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE, ILUtils.HL7_REGISTRATION_MESSAGE, ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE), false); // fetch for direct route
		List<KenyaEMRILMessageErrorQueue> errorQueueList = ilService.fetchAllMhealthErrors(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE, ILUtils.HL7_REGISTRATION_MESSAGE, ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE));
		List<KenyaEMRILMessageArchive> archiveRecordList = ilService.fetchRecentArchives(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE, ILUtils.HL7_REGISTRATION_MESSAGE, ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE));

		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		List<SimpleObject> archiveList = new ArrayList<SimpleObject>();
		List<SimpleObject> generalErrorList = new ArrayList<SimpleObject>();

		SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat yyyyMMddWithoutHiphen = new SimpleDateFormat("yyyyMMdd");

		ObjectMapper objectMapper = new ObjectMapper();
		for (KenyaEMRInteropMessage kenyaEMRILMessage : queueDataList) { // get records in queue

			String cccNumber = "";
			String messageType = "";

			if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_APPOINTMENT_MESSAGE)) {
				messageType = "Appointment";
			} else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_MESSAGE)) {
				messageType = "Registration";
			} else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE)) {
				messageType = "Registration update";
			}

			ILMessage ilMessage = objectMapper.readValue(kenyaEMRILMessage.getMessage().toLowerCase(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
				if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
					cccNumber = internalPatientId.getId().replaceAll("\\D", "");
				}

				if (Strings.isNullOrEmpty(cccNumber) && internalPatientId.getIdentifier_type().equalsIgnoreCase("PREP Unique Number")){
					cccNumber = internalPatientId.getId();
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

			String dataValue = null;
			Date dateValue = null;
			if (StringUtils.isNotBlank(messageType) && messageType.equalsIgnoreCase("Appointment")) {
				APPOINTMENT_INFORMATION[] appointmentList = ilMessage.getAppointment_information();
				if (appointmentList.length > 0) {
					APPOINTMENT_INFORMATION appointmentInformation = appointmentList[0];
					dataValue = appointmentInformation.getAppointment_date();
				}


			} else {
				PATIENT_VISIT visitInfo = ilMessage.getPatient_visit();
				dataValue = visitInfo.getVisit_date();

			}

			if (dataValue != null) {
				try {
					dateValue = yyyyMMddWithoutHiphen.parse(dataValue);
				} catch (ParseException e) {
					//throw new RuntimeException(e);
				}
			}

			SimpleObject queueObject = SimpleObject.create(
					"id", kenyaEMRILMessage.getId(),
					"uuid", kenyaEMRILMessage.getUuid(),
					"cccNumber", cccNumber,
					"patientName", fullName,
					"messageType", messageType,
					"visitDate", dateValue != null ? yyyyMMdd.format(dateValue) : "",
			    	"dateCreated", yyyyMMdd.format(kenyaEMRILMessage.getDateCreated()));
			queueList.add(queueObject);
		}

		for (KenyaEMRILMessageArchive archiveMessage : archiveRecordList) { // get records in queue

			String cccNumber = "";
			String messageType = "";

			if (archiveMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_APPOINTMENT_MESSAGE)) {
				messageType = "Appointment";
			} else if (archiveMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_MESSAGE)) {
				messageType = "Registration";
			} else if (archiveMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE)) {
				messageType = "Registration update";
			}

			ILMessage ilMessage = objectMapper.readValue(archiveMessage.getMessage().toLowerCase(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
				if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
					cccNumber = internalPatientId.getId().replaceAll("\\D", "");
				}

				if (Strings.isNullOrEmpty(cccNumber) && internalPatientId.getIdentifier_type().equalsIgnoreCase("PREP Unique Number")){
					cccNumber = internalPatientId.getId();
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

			String dataValue = null;
			Date dateValue = null;
			if (StringUtils.isNotBlank(messageType) && messageType.equalsIgnoreCase("Appointment")) {
				APPOINTMENT_INFORMATION[] appointmentList = ilMessage.getAppointment_information();
				if (appointmentList.length > 0) {
					APPOINTMENT_INFORMATION appointmentInformation = appointmentList[0];
					dataValue = appointmentInformation.getAppointment_date();
				}


			} else {
				PATIENT_VISIT visitInfo = ilMessage.getPatient_visit();
				dataValue = visitInfo.getVisit_date();

			}

			if (dataValue != null) {
				try {
					dateValue = yyyyMMddWithoutHiphen.parse(dataValue);
				} catch (ParseException e) {
					//throw new RuntimeException(e);
				}
			}

			SimpleObject archiveObject = SimpleObject.create(
					"id", archiveMessage.getId(),
					"uuid", archiveMessage.getUuid(),
					"cccNumber", cccNumber,
					"patientName", fullName,
					"messageType", messageType,
					"visitDate", dateValue != null ? yyyyMMdd.format(dateValue) : "",
					"dateCreated", yyyyMMdd.format(archiveMessage.getDateCreated()));
			archiveList.add(archiveObject);
		}


		for (KenyaEMRILMessageErrorQueue errorItem : errorQueueList) { // get records in error

			String cccNumber = "";

			String messageType = "";

			if (errorItem.getHl7_type().equalsIgnoreCase(ILUtils.HL7_APPOINTMENT_MESSAGE)) {
				messageType = "Appointment";
			} else if (errorItem.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_MESSAGE)) {
				messageType = "Registration";
			} else if (errorItem.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE)) {
				messageType = "Registration update";
			}

			ILMessage ilErrorMessage = objectMapper.readValue(errorItem.getMessage(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilErrorMessage.getPatient_identification().getInternal_patient_id()) {
				if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
					cccNumber = internalPatientId.getId().replaceAll("\\D", "");
				}

				if (Strings.isNullOrEmpty(cccNumber) && internalPatientId.getIdentifier_type().equalsIgnoreCase("PREP Unique Number")){
					cccNumber = internalPatientId.getId();
				}
			}

			// get patient name
			PATIENT_IDENTIFICATION patientIdentification = ilErrorMessage.getPatient_identification();

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

			String dataValue = null;
			Date dateValue = null;
			if (StringUtils.isNotBlank(messageType) && messageType.equalsIgnoreCase("Appointment")) {
				APPOINTMENT_INFORMATION[] appointmentList = ilErrorMessage.getAppointment_information();
				if (appointmentList.length > 0) {
					APPOINTMENT_INFORMATION appointmentInformation = appointmentList[0];
					dataValue = appointmentInformation.getAppointment_date();
				}


			} else {
				PATIENT_VISIT visitInfo = ilErrorMessage.getPatient_visit();
				dataValue = visitInfo.getVisit_date();

			}

			if (dataValue != null) {
				try {
					dateValue = yyyyMMddWithoutHiphen.parse(dataValue);
				} catch (ParseException e) {
					//throw new RuntimeException(e);
				}
			}

			SimpleObject errorObject = SimpleObject.create(
					"id", errorItem.getId(),
					"uuid", errorItem.getUuid(),
					"cccNumber", cccNumber,
					"patientName", fullName,
					"visitDate", dateValue != null ? yyyyMMdd.format(dateValue) : "",
					"messageType", messageType,
					"error", errorItem.getStatus(),
					"dateCreated", yyyyMMdd.format(errorItem.getDateCreated())
			);
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
