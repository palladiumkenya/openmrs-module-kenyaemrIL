package org.openmrs.module.kenyaemrIL;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class PullExpectedTransferInsTask extends AbstractTask {

    @Override
    public void execute() {
        System.out.println("Executing  PullExpectedTransferInsTask .................");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        /*Art Directory url*/
        GlobalProperty artDirectoryServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_ART_DIRECTORY_SERVER_URL);
        if (artDirectoryServerUrl == null) {
            System.out.println("There is no global property for art directory server URL!");
            return;
        }

        if (StringUtils.isBlank(artDirectoryServerUrl.getPropertyValue())) {
            System.out.println("ART Directory server URL has not been set!");
            return;
        }
        /**Fetch the last date of sync*/
        String fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("transferInsFetchTask.lastFetchDateAndTime");

        try {
            fetchDate = globalPropertyObject.getValue().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String serverUrl = artDirectoryServerUrl.getPropertyValue() + "/patients/transfer-in/";
        String mflParam = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        HttpGet httpGet = new HttpGet(serverUrl + mflParam + "/" + fetchDate);
        System.out.println("PULL EXPECTED TIs URL " + serverUrl + mflParam + "/" + fetchDate);

        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
                String lastPullDatetime = "";
                if (responseObj.get("pull_timestamp") != null) {
                    lastPullDatetime = (String) responseObj.get("pull_timestamp");
                }
                List<JSONObject> message = (List<JSONObject>) responseObj.get("message");
                if (!message.isEmpty()) {
                    for (JSONObject patientObject : message) {
                        if (patientObject.get("MESSAGE_HEADER") != null) {
                            ExpectedTransferInPatients transferInPatient = transferInPatientTranslator(patientObject.toString());
                            transferInPatient.setPatientSummary(String.valueOf(patientObject));
                            Context.getService(KenyaEMRILService.class).createPatient(transferInPatient);
                        }
                    }
                }
                if (!Strings.isNullOrEmpty(lastPullDatetime)) {
                    globalPropertyObject.setPropertyValue(lastPullDatetime);
                    Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
                }
            }
        } catch (IOException | ParseException | java.text.ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private ExpectedTransferInPatients transferInPatientTranslator(String referralObject) throws java.text.ParseException, ParseException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        ExpectedTransferInPatients expectedTransferInPatient = new ExpectedTransferInPatients();

        ObjectMapper mapper = new ObjectMapper();

        ILMessage ilMessage = mapper.readValue(referralObject.toLowerCase(), ILMessage.class);
        String ccc = "";

		ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getFirst_name();
		
        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                ccc = internalPatientId.getId();
                expectedTransferInPatient.setNupiNumber(ccc);
            }
        }
        if (ilMessage != null) {
           //Patient names
			System.out.println(ilMessage.toString());
			if(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getFirst_name() != null) {
				expectedTransferInPatient.setClientFirstName(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getFirst_name());
			};
			if(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getMiddle_name() != null) {
				expectedTransferInPatient.setClientMiddleName(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getMiddle_name());
			};
			if(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getLast_name() != null) {
				expectedTransferInPatient.setClientLastName(ilMessage.extractILRegistration().getPatient_identification().getPatient_name().getLast_name());
			};
           //Gender
			if(ilMessage.extractILRegistration().getPatient_identification().getSex() != null) {
				expectedTransferInPatient.setClientGender(ilMessage.extractILRegistration().getPatient_identification().getSex());
			};
			//DOB
			if(ilMessage.extractILRegistration().getPatient_identification().getDate_of_birth() != null) {
				expectedTransferInPatient.setClientBirthDate(formatter.parse(ilMessage.extractILRegistration().getPatient_identification().getDate_of_birth()));
			};
           
            Program_Discontinuation_Message discontinuation_message = ilMessage.getDiscontinuation_message();
			if (discontinuation_message.getService_request() != null && discontinuation_message.getService_request().getSupporting_info() != null && !Strings.isNullOrEmpty(discontinuation_message.getService_request().getSupporting_info().getAppointment_date())) {
				expectedTransferInPatient.setAppointmentDate(formatter.parse(discontinuation_message.getService_request().getSupporting_info().getAppointment_date()));
			}
			
            if (discontinuation_message.getService_request() != null && discontinuation_message.getService_request().getSupporting_info() != null && !Strings.isNullOrEmpty(discontinuation_message.getService_request().getSupporting_info().getAppointment_date())) {
                expectedTransferInPatient.setAppointmentDate(formatter.parse(discontinuation_message.getService_request().getSupporting_info().getAppointment_date()));
            }

            if (discontinuation_message.getService_request() != null && discontinuation_message.getEffective_discontinuation_date() != null) {
                expectedTransferInPatient.setEffectiveDiscontinuationDate(formatter.parse(discontinuation_message.getEffective_discontinuation_date()));
            }

            if (discontinuation_message.getService_request() != null && discontinuation_message.getService_request().getTransfer_out_date() != null) {
                expectedTransferInPatient.setTransferOutDate(formatter.parse(discontinuation_message.getService_request().getTransfer_out_date()));

            }
            if (discontinuation_message.getService_request() != null && discontinuation_message.getService_request().getSending_facility_mflcode() != null) {
                expectedTransferInPatient.setTransferOutFacility(discontinuation_message.getService_request().getSending_facility_mflcode());
            }
            expectedTransferInPatient.setReferralStatus("ACTIVE");
            expectedTransferInPatient.setServiceType("HIV");
        }
        return expectedTransferInPatient;
    }
}
