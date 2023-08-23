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
        String serverUrl = "http://192.168.1.44:8002/api/patients/transfer-in/";
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
                        ExpectedTransferInPatients transferInPatient = transferInPatientTranslator(patientObject.toString());
                        transferInPatient.setPatientSummary(String.valueOf(patientObject.get("DISCONTINUATION_MESSAGE")));
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

    private ExpectedTransferInPatients transferInPatientTranslator(String referralObject) throws java.text.ParseException, ParseException, IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        ExpectedTransferInPatients expectedTransferInPatient = new ExpectedTransferInPatients();
        ObjectMapper mapper = new ObjectMapper();

        ILMessage ilMessage = mapper.readValue(referralObject.toLowerCase(), ILMessage.class);

        if (ilMessage != null) {
            Patient ilPatient = patientHandler(ilMessage.getPatient_identification());
            expectedTransferInPatient.setPatient(ilPatient);
            Program_Discontinuation_Message discontinuation_message = ilMessage.getDiscontinuation_message();
            expectedTransferInPatient.setAppointmentDate(formatter.parse(discontinuation_message.getService_request().getSupporting_info().getAppointment_date()));
            expectedTransferInPatient.setEffectiveDiscontinuationDate(formatter.parse(discontinuation_message.getEffective_discontinuation_date()));
            expectedTransferInPatient.setTransferOutDate(formatter.parse(discontinuation_message.getService_request().getTransfer_out_date()));
            expectedTransferInPatient.setTransferOutFacility(discontinuation_message.getService_request().getSending_facility_mflcode());
            expectedTransferInPatient.setReferralStatus("ACTIVE");
        }
        return expectedTransferInPatient;
    }

    private Patient patientHandler(PATIENT_IDENTIFICATION patient_identification) throws ParseException, java.text.ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Person person = new Person();
        PersonName personName = new PersonName(patient_identification.getPatient_name().getLast_name(),
                patient_identification.getPatient_name().getMiddle_name(), patient_identification.getPatient_name().getFirst_name());
        person.addName(personName);
        PatientIdentifierType cccIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        PatientIdentifierType upiIdType = Context.getPatientService().getPatientIdentifierTypeByUuid("f85081e2-b4be-4e48-b3a4-7994b69bb101");

        person.setBirthdate(formatter.parse(patient_identification.getDate_of_birth()));
        person.setBirthdateEstimated(Boolean.getBoolean(patient_identification.getDate_of_birth_precision()));
        person.setGender(patient_identification.getSex());
        PersonAttribute phoneNumber = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Telephone contact"), patient_identification.getPhone_number());
        PersonAttribute maritalStatus = new PersonAttribute(Context.getPersonService().getPersonAttributeTypeByName("Civil Status"), patient_identification.getMarital_status());
        person.addAttribute(phoneNumber);
        person.addAttribute(maritalStatus);

        PersonAddress personAddress = getPersonAddress(patient_identification);
        person.addAddress(personAddress);
        Context.getPersonService().savePerson(person);
        Patient patient = new Patient(person);
        for (INTERNAL_PATIENT_ID internalPatientId : patient_identification.getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                PatientIdentifier ccc = new PatientIdentifier(internalPatientId.getId(), cccIdType, MessageHeaderSingleton.getDefaultLocation());
                ccc.setPreferred(true);
                patient.addIdentifier(ccc);
            } else if (internalPatientId.getIdentifier_type().equalsIgnoreCase("NUPI")) {
                patient.addIdentifier(new PatientIdentifier(internalPatientId.getId(), upiIdType, MessageHeaderSingleton.getDefaultLocation()));
            }
        }
        // Assign a patient an OpenMRS ID
        String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
        PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);

        String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "Registration");
        PatientIdentifier omrsId = new PatientIdentifier(generated, openmrsIdType, MessageHeaderSingleton.getDefaultLocation());
        omrsId.setPreferred(true);
        patient.addIdentifier(omrsId);


        Patient patient1 = Context.getPatientService().getPatient(5586);

        return patient1;
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
