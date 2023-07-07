package org.openmrs.module.kenyaemrIL;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        String serverUrl = artDirectoryServerUrl.getPropertyValue();
        String cccParam = String.join(", ", trfCccNumbers);
        String mflParam =  MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());


        CloseableHttpClient httpClient  = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        HttpGet httpGet = new HttpGet(serverUrl+"?mflCode"+mflParam+"&ccc="+cccParam);
        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        /*For complete transfers, update transfer verification status*/
        for (Patient patient : this.fetchTransferOutPatients()) {
            System.out.println("THE PATIENT ID " + patient.getPatientId());

            String lastToEncounterSql = "select max(encounter_id) from encounter e " +
                                        "inner join encounter_type et on e.encounter_type = et.encounter_type_id " +
                                        "where et.uuid = '2bdada65-4c72-4a48-8730-859890e25cee' and patient_id = " + patient.getPatientId() + " and voided = 0;";
            List<List<Object>> resultSet = Context.getAdministrationService().executeSQL(lastToEncounterSql, true);

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

    private List<Patient> fetchTransferOutPatients() {
        System.out.println("PROCESSING ValidateTransferOutPatients TENA TENA=============================================");
        LocalDateTime today = LocalDateTime.now();
        String effectiveToday = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String effectivePastDate = today.minusDays(90).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        StringBuilder q = new StringBuilder();
        q.append("select patient_id  ");
        q.append("from kenyaemr_etl.etl_patient_program_discontinuation  where program_name='HIV' and discontinuation_reason = 159492 and (trf_out_verified = 1066 or trf_out_verified is null)  " +
                "and ((date(transfer_date) between '" + effectivePastDate + "' and '" + effectiveToday + "') or (date(visit_date) between '" + effectivePastDate + "' and '" + effectiveToday + "'))");
        List<Patient> patients = new ArrayList<>();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);

        for (List<Object> row : queryData) {
            Integer patientId = (Integer) row.get(0);
            Patient patient = Context.getPatientService().getPatient(patientId);
            patients.add(patient);
        }

        return patients;
    }
}
