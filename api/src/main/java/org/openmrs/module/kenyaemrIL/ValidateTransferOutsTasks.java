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

    @Override
    public void execute() {
        System.out.println("Executing ValidateTransferOutPatients Task .................");

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
        GlobalProperty artDirectoryServerUrl = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_ART_DIRECTORY_SERVER_URL);
        if (artDirectoryServerUrl == null) {
            System.out.println("There is no global property for art directory server URL!");
            return;
        }

        if (StringUtils.isBlank(artDirectoryServerUrl.getPropertyValue())) {
            System.out.println("ART Directory server URL has not been set!");
            return;
        }
        String serverUrl =   artDirectoryServerUrl.getPropertyValue() + "/patients/referral-status/";
        String cccParam = String.join(", ", trfCccNumbers);
        String mflParam = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        System.out.println("Validate TOs URL "+serverUrl + mflParam + "/" + new String(Base64.encodeBase64(cccParam.getBytes())));
        HttpGet httpGet = new HttpGet(serverUrl + mflParam + "/" + new String(Base64.encodeBase64(cccParam.getBytes())));
        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
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
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String effectiveToday = today.format(dateTimeFormatter);
        String effectivePastDate = "2023-07-15";
        StringBuilder q = new StringBuilder();
        q.append("select  e.patient_id from encounter e inner join (\n" +
                "    select encounter_type_id, uuid, name from encounter_type where uuid ='2bdada65-4c72-4a48-8730-859890e25cee')\n" +
                "    et on (et.encounter_type_id = e.encounter_type and e.voided = 0 )\n" +
                "    left join obs o on (e.encounter_id = o.encounter_id and o.voided = 0 and ( o.concept_id = 1285 or o.concept_id is null) )\n" +
                "    and (o.value_coded = 1066 or o.value_coded is null)  ");
        q.append("where e.date_created >=  '" + effectivePastDate + "' or e.date_changed >=  '" + effectivePastDate + "' group by e.patient_id");
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
