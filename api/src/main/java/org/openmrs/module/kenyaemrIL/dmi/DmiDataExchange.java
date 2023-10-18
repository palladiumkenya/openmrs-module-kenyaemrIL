package org.openmrs.module.kenyaemrIL.dmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.common.Age;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER;
import static org.openmrs.module.kenyaemr.metadata.CommonMetadata._PatientIdentifierType.OPENMRS_ID;

public class DmiDataExchange {

    private Log log = LogFactory.getLog(DmiDataExchange.class);

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Generates the payload used to post to DMI server
     *
     * @param encounter
     * @return
     */
    public static JSONArray generateDMIpostPayload(Encounter encounter, Date fetchDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONArray payload = new JSONArray();
        JSONObject payloadObj = new JSONObject();
        List<SimpleObject> labs = new ArrayList<SimpleObject>();
        List<SimpleObject> complaints = new ArrayList<SimpleObject>();
        List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
        //Considering triage and greencard forms
        Patient patient = encounter.getPatient();

        Form hivGreencardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);
        Form traigeForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);
        List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null,
                fetchDate, null, Arrays.asList(hivGreencardForm, traigeForm), null, null, null, null, false);
        System.out.println("Count of encounters  ==> " + encounters.size());

        //Unique id
        PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
        PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
        //CaseUniqueId : Use visit ID to link complaints and diagnosis in DMI server, labs do not have visit id
        Integer caseUniqueId = encounter.getVisit() != null ? encounter.getVisit().getId() : encounter.getEncounterId();

        //Nupi id
        PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
        PatientIdentifier nupiId = patient.getPatientIdentifier(nupiIdType);
        //Patient address
        String county = "";
        String subcounty = "";
        Person person = Context.getPersonService().getPerson(patient.getPatientId());
        if(person.getPersonAddress() != null){
            county = person.getPersonAddress().getCountyDistrict();
            subcounty = person.getPersonAddress().getStateProvince();
        }

        String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());
        String dob = patient.getBirthdate() != null ? dmiUtils.getSimpleDateFormat("yyyy-MM-dd").format(patient.getBirthdate()) : "";
        Age age = new Age(patient.getBirthdate());
        Integer ageInYears = age.getFullYears();
        String gender = patient.getGender();
        String encounterDate = null;
        String outPatientDate = null;
        String complaint = "";
        Integer complaintId = null;
        String onsetDate = "";
        Double duration = null;
        String diagnosisName = "";
        Integer diagnosisId = null;
        Integer orderId = null;
        String testName = "";
        String testResult = "";
        String temperature = "";
        for (Obs obs : encounter.getObs()) {

            encounterDate = sd.format(encounter.getEncounterDatetime());
            outPatientDate = formatter.format(encounter.getEncounterDatetime());
               //Complaints
            if (obs.getConcept().getConceptId().equals(5219)) {
                complaint = obs.getValueCoded().getName().getName();
                complaintId = obs.getValueCoded().getConceptId();
            }
            if (obs.getConcept().getConceptId().equals(159948)) {
                onsetDate = formatter.format(obs.getValueDate());
            }
            if (obs.getConcept().getConceptId().equals(159368)) {
                duration = obs.getValueNumeric();
            }
            if (obs.getConcept().getConceptId().equals(5088)) {
                temperature = obs.getValueNumeric().toString();
            }
               //Diagnosis
            if (obs.getConcept().getConceptId().equals(6042)) {
                diagnosisName = obs.getValueCoded().getName().getName();
                diagnosisId = obs.getValueCoded().getConceptId();
            }
            //labs
            if(obs.getOrder() != null) {
                if (obs.getOrder().getOrderId() != null) {
                    orderId = obs.getOrder().getOrderId();
                    testName = obs.getConcept().getName().getName();
                    if (obs.getValueCoded() != null) {
                        testResult = obs.getValueCoded().getName().getName();
                    } else if (obs.getValueNumeric() != null) {
                        testResult = obs.getValueNumeric().toString();
                    } else if (obs.getValueText() != null) {
                        testResult = obs.getValueText();
                    }
                }
            }
        }
        //add to list
        payloadObj.put("patientUniqueId", openmrsId != null ? openmrsId.getIdentifier() : "");
        payloadObj.put("nupi", nupiId != null ? nupiId.getIdentifier() : "");
        payloadObj.put("caseUniqueId",caseUniqueId );
        payloadObj.put("hospitalIdNumber", facilityMfl);
        payloadObj.put("interviewDate", encounterDate);
        payloadObj.put("verbalConsentDone", true);
        payloadObj.put("dateOfBirth", dob);
        payloadObj.put("age", ageInYears);
        payloadObj.put("sex", gender != null ? dmiUtils.formatGender(gender) : "");
        payloadObj.put("address", "");
        payloadObj.put("county", county);
        payloadObj.put("subCounty", subcounty);
        payloadObj.put("admissionDate", null);
        payloadObj.put("outpatientDate", outPatientDate);
        payloadObj.put("temperature", temperature);
        payloadObj.put("voided", false);
        payloadObj.put("createdAt", encounterDate);
        payloadObj.put("updatedAt", null);
        if (orderId != null && testName != "") {
            SimpleObject labsObject = new SimpleObject();
            labsObject.put("orderId", orderId);
            labsObject.put("testName", testName);
            labsObject.put("testResult", testResult);
            labsObject.put("labDate", encounterDate);
            labs.add(labsObject);
            payloadObj.put("labDtoList", labs);
        }else{
            payloadObj.put("labDtoList", labs);
        }
        if (complaintId != null && complaint != "") {
            SimpleObject complaintObject = new SimpleObject();
            complaintObject.put("complaintId", complaintId);
            complaintObject.put("complaint", complaint);
            complaintObject.put("onsetDate", onsetDate);
            complaintObject.put("duration", duration);
            complaintObject.put("voided", false);
            complaints.add(complaintObject);
            payloadObj.put("complaintDtoList", complaints);
        }else {
            payloadObj.put("complaintDtoList", complaints);
        }
        if (diagnosisId != null && diagnosisName != "") {
            SimpleObject diagnosisObject = new SimpleObject();
            diagnosisObject.put("diagnosisId", diagnosisId);
            diagnosisObject.put("diagnosisDate", encounterDate);
            diagnosisObject.put("diagnosis", diagnosisName);
            diagnosisObject.put("voided", false);
            diagnosis.add(diagnosisObject);
            payloadObj.put("diagnosis", diagnosis);
        }else{
            payloadObj.put("diagnosis", diagnosis);
        }

        payload.add(payloadObj);

        Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        System.out.println("Payload generated: " + payload);

        return payload;
    }
}


