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
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PullTransferInsTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(PullTransferInsTask.class);

    @Override
    public void execute() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        /**Fetch the last date of sync*/
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("transferInFetchTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String serverUrl = "http://192.168.1.44:8002/api/patients/transfer-in/";
        String mflParam = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());


        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ILUtils.sslConnectionSocketFactoryWithDisabledSSLVerification()).build();

        HttpGet httpGet = new HttpGet(serverUrl + mflParam + "/" + fetchDate);
        httpGet.addHeader("content-type", "application/json");
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(httpResponse.getEntity());
                System.out.println("RESPONSE " + res);
                JSONParser parser = new JSONParser();
                JSONObject responseObj = (JSONObject) parser.parse(res);
                List<JSONObject> message = (List<JSONObject>) responseObj.get("message");
                if (!message.isEmpty()) {
                    for (JSONObject patientObject : message) {
                        Patient patient = patientTranslator(String.valueOf(patientObject.get("PATIENT_IDENTIFICATION")));
                        ExpectedTransferInPatients transferInPatient = transferInPatientTranslator(String.valueOf(patientObject.get("SERVICE_REQUEST")));
                        transferInPatient.setPatient(patient);
                        transferInPatient.setPatientSummary(String.valueOf(patientObject.get("SERVICE_REQUEST")));
                        Context.getPatientService().savePatient(patient);
                        Context.getService(KenyaEMRILService.class).createPatient(transferInPatient);
                    }
                }
            }
            Date nextProcessingDate = new Date();
            globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
            Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
        } catch (IOException | ParseException | java.text.ParseException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private ExpectedTransferInPatients transferInPatientTranslator(String referralObject) throws java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        ExpectedTransferInPatients patient = new ExpectedTransferInPatients();
        ObjectMapper mapper = new ObjectMapper();
        PATIENT_REFERRAL_INFORMATION patientReferralInformation;
        try {
            patientReferralInformation = mapper.readValue(referralObject.toLowerCase(), PATIENT_REFERRAL_INFORMATION.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        patient.setAppointmentDate(formatter.parse(patientReferralInformation.getSupporting_info().getAppointment_date()));
        patient.setEffectiveDiscontinuationDate(formatter.parse(patientReferralInformation.getSupporting_info().getAppointment_date()));
        patient.setTransferOutDate(formatter.parse(patientReferralInformation.getTransfer_out_date()));
        patient.setTransferOutFacility(Integer.valueOf(patientReferralInformation.getSending_facility_mflCode()));
        patient.setReferralStatus("ACTIVE");
        return patient;
    }

    private Patient patientTranslator(String patientIdentification) throws ParseException, java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        ObjectMapper mapper = new ObjectMapper();
        PATIENT_IDENTIFICATION patient_identification;
        try {
            patient_identification = mapper.readValue(patientIdentification.toLowerCase(), PATIENT_IDENTIFICATION.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Patient patient = new Patient();
        PersonName personName = new PersonName(patient_identification.getPatient_name().getLast_name(),
                patient_identification.getPatient_name().getMiddle_name(),patient_identification.getPatient_name().getFirst_name());
        patient.setNames(Collections.singleton(personName));
        PatientIdentifierType cccIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        PatientIdentifierType upiIdType = Context.getPatientService().getPatientIdentifierTypeByUuid("f85081e2-b4be-4e48-b3a4-7994b69bb101");
        patient_identification.getInternal_patient_id().forEach(internalPatientId -> {
            if (internalPatientId.getIdentifier_type().equals("CCC_NUMBER")) {
                patient.addIdentifier(new PatientIdentifier(internalPatientId.getId(), cccIdType, MessageHeaderSingleton.getDefaultLocation()));
            } else if (internalPatientId.getIdentifier_type().equals("NUPI")) {
                patient.addIdentifier(new PatientIdentifier(internalPatientId.getId(), upiIdType, MessageHeaderSingleton.getDefaultLocation()));
            }
        });

        patient.setBirthdate(formatter.parse(patient_identification.getDate_of_birth()));
        patient.setBirthdateEstimated(Boolean.getBoolean(patient_identification.getDate_of_birth_precision()));
        patient.setGender(patient_identification.getSex());
        PersonAttribute phoneNumber = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Telephone contact"), patient_identification.getPhone_number());
        PersonAttribute maritalStatus = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Civil Status"), patient_identification.getMarital_status());
        patient.addAttribute(phoneNumber);
        patient.addAttribute(maritalStatus);

        PersonAddress personAddress = getPersonAddress(patient_identification);
        patient.setAddresses(Collections.singleton(personAddress));
        return patient;
    }

    private static PersonAddress getPersonAddress(PATIENT_IDENTIFICATION patient_identification) {
        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddress6(patient_identification.getPatient_address().getPhysical_address().getWard());
        personAddress.setCountyDistrict(patient_identification.getPatient_address().getPhysical_address().getCounty());
        personAddress.setAddress2(patient_identification.getPatient_address().getPhysical_address().getNearest_landmark());
        personAddress.setAddress4(patient_identification.getPatient_address().getPhysical_address().getSub_county());
        personAddress.setCityVillage(patient_identification.getPatient_address().getPhysical_address().getVillage());
        return personAddress;
    }
}
