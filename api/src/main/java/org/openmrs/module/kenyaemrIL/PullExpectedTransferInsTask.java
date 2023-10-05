package org.openmrs.module.kenyaemrIL;

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
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PullExpectedTransferInsTask extends AbstractTask {

    @Override
    public void execute() {
        System.out.println("Executing  PullExpectedTransferInsTask .................");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        /**Fetch the last date of sync*/
        String fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("transferInsFetchTask.lastFetchDateAndTime");

        try {
            fetchDate = globalPropertyObject.getValue().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        String serverUrl = "http://192.168.1.44:8002/api/patients/transfer-in/";
        String serverUrl = "http://prod.kenyahmis.org:8002/api/patients/transfer-in/";
        String mflParam = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        HttpGet httpGet = new HttpGet(serverUrl + mflParam + "/" + fetchDate);
        System.out.println("LAST FETCH DATE "+serverUrl + mflParam + "/" + fetchDate);

        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
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
            }
            Date nextProcessingDate = new Date();
//            globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
//            Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
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

        if (ilMessage != null) {
            Program_Discontinuation_Message discontinuation_message = ilMessage.getDiscontinuation_message();
            expectedTransferInPatient.setAppointmentDate(formatter.parse(discontinuation_message.getService_request().getSupporting_info().getAppointment_date()));
            expectedTransferInPatient.setEffectiveDiscontinuationDate(formatter.parse(discontinuation_message.getEffective_discontinuation_date()));
            expectedTransferInPatient.setTransferOutDate(formatter.parse(discontinuation_message.getService_request().getTransfer_out_date()));
            expectedTransferInPatient.setTransferOutFacility(discontinuation_message.getService_request().getSending_facility_mflcode());
            expectedTransferInPatient.setReferralStatus("ACTIVE");
            expectedTransferInPatient.setServiceType("HIV");
        }
        return expectedTransferInPatient;
    }
}
