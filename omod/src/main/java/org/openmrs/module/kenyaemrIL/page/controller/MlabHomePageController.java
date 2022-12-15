package org.openmrs.module.kenyaemrIL.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.il.viralload.ViralLoadMessage;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@AppPage("kenyaemr.mlab.home")
public class MlabHomePageController {
	
	public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException {
		
		KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
		List<KenyaEMRILMessage> queueDataList = ilService.fetchAllViralLoadResults(false);
		List<KenyaEMRILMessageErrorQueue> errorQueueList = ilService.fetchAllViralLoadErrors();

		List<SimpleObject> queueList = new ArrayList<SimpleObject>();
		List<SimpleObject> generalErrorList = new ArrayList<SimpleObject>();
		SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

		ObjectMapper objectMapper = new ObjectMapper();
		for (KenyaEMRILMessage kenyaEMRILMessage : queueDataList) { // get records in queue

			String cccNumber = "";
			ILMessage ilMessage = objectMapper.readValue(kenyaEMRILMessage.getMessage(), ILMessage.class);
			// 1. Fetch the person to update using the CCC number
			for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
				if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
					cccNumber = internalPatientId.getId().replaceAll("\\D", "");
					break;
				}
			}

			ViralLoadMessage viralLoadMessage = ilMessage.extractViralLoadMessage();
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
			SimpleObject queueObject = SimpleObject.create(
					"id", kenyaEMRILMessage.getId(),
					"uuid", kenyaEMRILMessage.getUuid(),
					"cccNumber", cccNumber,
					"sampleCollectionDate", dateSampleCollected,
					"testDate", dateSampleTested,
					"dateCreated", yyyyMMdd.format(kenyaEMRILMessage.getDateCreated()),
			    	"sampleType", sampleTypeCode == 0 ? "EID" : "Viral Load",
					"result", vlResult);
			queueList.add(queueObject);
		}

		for (KenyaEMRILMessageErrorQueue errorItem : errorQueueList) { // get records in error

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
		}

		model.put("queueList", ui.toJson(queueList));
		model.put("queueListSize", queueList.size());
		
		model.put("generalErrorListSize", generalErrorList.size());
		model.put("generalErrorList", ui.toJson(generalErrorList));
	}
}
