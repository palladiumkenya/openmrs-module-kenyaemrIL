package org.openmrs.module.kenyaemrIL.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_VISIT;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaemrMhealthOutboxMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@AppPage("kenyaemr.ushauri.home")
public class UshauriHomePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException {
		
		KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
		List<KenyaemrMhealthOutboxMessage> queueDataList = ilService.getAllMhealthOutboxMessages(false); // fetch for direct route
		List<KenyaEMRILMessageErrorQueue> errorQueueList = ilService.fetchAllViralLoadErrors();

		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		List<SimpleObject> generalErrorList = new ArrayList<SimpleObject>();

		ObjectMapper objectMapper = new ObjectMapper();
		for (KenyaemrMhealthOutboxMessage kenyaEMRILMessage : queueDataList) { // get records in queue

			String cccNumber = "";
			String messageType = "";

			if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_APPOINTMENT_MESSAGE)) {
				messageType = "Appointment";
			} else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_MESSAGE)) {
				messageType = "Registration";
			} else if (kenyaEMRILMessage.getHl7_type().equalsIgnoreCase(ILUtils.HL7_REGISTRATION_UPDATE_MESSAGE)) {
				messageType = "Registration update";
			}
			SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");
			ILMessage ilMessage = objectMapper.readValue(kenyaEMRILMessage.getMessage().toLowerCase(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
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

			PATIENT_VISIT visitInfo = ilMessage.getPatient_visit();


			SimpleObject queueObject = SimpleObject.create(
					"id", kenyaEMRILMessage.getId(),
					"uuid", kenyaEMRILMessage.getUuid(),
					"cccNumber", cccNumber,
					"patientName", fullName,
					"messageType", messageType,
					"visitDate", visitInfo.getVisit_date(),
			    	"dateCreated", yyyyMMdd.format(kenyaEMRILMessage.getDateCreated()));
			queueList.add(queueObject);
		}

		/*for (KenyaEMRILMessageErrorQueue errorItem : errorQueueList) { // get records in error

			String cccNumber = "";
			ILMessage ilErrorMessage = objectMapper.readValue(errorItem.getMessage(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilErrorMessage.getPatient_identification().getInternal_patient_id()) {
				if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
					cccNumber = internalPatientId.getId().replaceAll("\\D", "");
					break;
				}
			}

			ViralLoadMessage viralLoadMessage = ilErrorMessage.extractViralLoadMessage();
			VIRAL_LOAD_RESULT[] viralLoadResult = viralLoadMessage.getViral_load_result();
			String dateSampleCollected = "";
			String dateSampleTested= "";
			String vlResult = "";
			String sampleType = "";
			for (VIRAL_LOAD_RESULT labInfo : viralLoadResult) { // there is only one result in the payload
				dateSampleCollected = labInfo.getDate_sample_collected();
				dateSampleTested = labInfo.getDate_sample_tested();
				vlResult = labInfo.getVl_result();
				sampleType = labInfo.getSample_type();

			}

			int sampleTypeCode = Integer.valueOf(sampleType);
			SimpleObject errorObject = SimpleObject.create(
					"id", errorItem.getId(),
					"uuid", errorItem.getUuid(),
					"cccNumber", cccNumber,
					"sampleCollectionDate", dateSampleCollected,
					"testDate", dateSampleTested,
					"sampleType", sampleTypeCode == 0 ? "EID" : "Viral Load",
					"error", errorItem.getStatus(),
					"result", vlResult);
			generalErrorList.add(errorObject);
		}*/

		model.put("queueList", ui.toJson(queueList));
		model.put("queueListSize", queueList.size());
		
		model.put("generalErrorListSize", generalErrorList.size());
		model.put("generalErrorList", generalErrorList);
	}
}
