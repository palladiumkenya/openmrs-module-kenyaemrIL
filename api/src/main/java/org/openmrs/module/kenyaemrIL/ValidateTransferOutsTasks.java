package org.openmrs.module.kenyaemrIL;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ValidateTransferOutsTasks extends AbstractTask {

    private String url = "http://www.google.com:80/index.html";

    @Override
    public void execute() {
        System.out.println("PROCESSING ValidateTransferOutPatients =============================================");

        /*Collect CCC numbers for transfer out patients*/
        List<String> trfCccNumbers = new ArrayList<>();
        for (Patient patient : this.fetchTransferOutPatients()) {
            List<PatientIdentifier> patientIdentifiers = patient.getActiveIdentifiers();
            for (PatientIdentifier id : patientIdentifiers) {
                if (id.getIdentifierType().getUuid().equals("05ee9cf4-7242-4a17-b4d4-00f707265c8a")) {
                    trfCccNumbers.add(id.getIdentifier());
                }
            }
        }

        /*Fetch transfer statuses from Art Directory*/
        GlobalProperty artDirectoryServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_USHAURI_PUSH_SERVER_URL);
        if (artDirectoryServerUrl == null) {
            System.out.println("There is no global property for art directory server URL!");
            return;
        }

        if (StringUtils.isBlank(artDirectoryServerUrl.getPropertyValue())) {
            System.out.println("ART Directory server URL has not been set!");
            return;
        }
        String serverUrl = "http://prod.kenyahmis.org:8002/api/patients/referral-status/";
        String cccParam = String.join(", ", trfCccNumbers);
        String mflParam = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());


        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        HttpGet httpGet = new HttpGet(serverUrl + mflParam + "/" + new String(Base64.encodeBase64(cccParam.getBytes())));
        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                System.out.println("RESPONSE " + res);
                System.out.println();
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
                List<JSONObject> message = (List<JSONObject>) responseObj.get("message");
                if (!message.isEmpty()) {
                    for (JSONObject patientStatus : message) {
                        String ccc = String.valueOf(patientStatus.get("ccc_no"));
                        String referralStatus = String.valueOf(patientStatus.get("transfer_status"));
                        if (referralStatus.equals("COMPLETED")) {
                            PatientIdentifierType cccIdType = Context.getPatientService().getPatientIdentifierTypeByUuid("05ee9cf4-7242-4a17-b4d4-00f707265c8a");
                            List<Patient> patient = Context.getPatientService().getPatients(null, ccc, Arrays.asList(cccIdType), true);
                            if (!patient.isEmpty()) {
                            Patient patient1 = Context.getPatientService().getPatient(patient.get(0).getPatientId());
                                verifyTransferredPatient(patient1);
                            }
                        }
                    }
                }
            }
        } catch (IOException | ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<Patient> fetchTransferOutPatients() {
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String effectiveToday = today.format(dateTimeFormatter);
        String effectivePastDate = today.minusDays(90).format(dateTimeFormatter);
        StringBuilder q = new StringBuilder();
        q.append("select patient_id  ");
        q.append("from kenyaemr_etl.etl_patient_program_discontinuation  where program_name='HIV' and discontinuation_reason = 159492 and (trf_out_verified = 1066 or trf_out_verified is null)  " +
                "and ((date(transfer_date) between '" + effectivePastDate + "' and '" + effectiveToday + "') or (date(visit_date) between '" + effectivePastDate + "' and '" + effectiveToday + "'))");
        System.out.println("Pending TO Queries " + new String(q));
        List<Patient> patients = new ArrayList<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);

        for (List<Object> row : queryData) {
            Integer patientId = (Integer) row.get(0);
            Patient patient = Context.getPatientService().getPatient(patientId);
            patients.add(patient);
        }

        return patients;
    }

    /*For complete transfers, update transfer verification status*/
    private void verifyTransferredPatient(Patient patient) {
        String lastToEncounterSql = "select max(encounter_id) from encounter e " +
                "inner join encounter_type et on e.encounter_type = et.encounter_type_id " +
                "where et.uuid = '2bdada65-4c72-4a48-8730-859890e25cee' and patient_id = " + patient.getPatientId() + " and voided = 0;";
        List<List<Object>> resultSet = Context.getAdministrationService().executeSQL(lastToEncounterSql, true);

        if (!resultSet.isEmpty()) {
            Integer lastToEncounterId = (Integer) resultSet.get(0).get(0);
            Encounter toEncounterToUpdate = Context.getEncounterService().getEncounter(lastToEncounterId);
            Obs trfVerification = new Obs();
            trfVerification.setConcept(Context.getConceptService().getConceptByUuid("1285AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            trfVerification.setValueCoded(Context.getConceptService().getConceptByUuid("1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            Obs trfVerificationDate = new Obs();
            trfVerificationDate.setConcept(Context.getConceptService().getConceptByUuid("164133AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            trfVerificationDate.setValueDatetime(new Date());

            toEncounterToUpdate.addObs(trfVerification);
            toEncounterToUpdate.addObs(trfVerificationDate);
            Context.getEncounterService().saveEncounter(toEncounterToUpdate);
        }
    }
}
