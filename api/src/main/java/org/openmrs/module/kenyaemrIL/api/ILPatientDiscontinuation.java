package org.openmrs.module.kenyaemrIL.api;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.MOTHER_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PHYSICAL_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ILPatientDiscontinuation {
    static DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    public static ILMessage iLPatientWrapper(Patient patient, Encounter encounter) {
        ILMessage ilMessage = new ILMessage();
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;

//        Form the internal patient IDs
        for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
            ipd = new INTERNAL_PATIENT_ID();
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
                internalPatientIds.add(ipd);
//        Form the default external patient IDs
                epd.setAssigning_authority("MPI");
                epd.setIdentifier_type("GODS_NUMBER");
                patientIdentification.setExternal_patient_id(epd);
            }
        }
        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName() != null ? personName.getGivenName() : "");
        patientname.setMiddle_name(personName.getMiddleName() != null ? personName.getMiddleName() : "");
        patientname.setLast_name(personName.getFamilyName() != null ? personName.getFamilyName() : "");
        patientIdentification.setPatient_name(patientname);

        // Set to empty string unwanted patient details for viral load
        patientIdentification.setSex("");   //        Set the Gender, phone number and marital status
        patientIdentification.setPhone_number("");
        patientIdentification.setMarital_status("");
        patientIdentification.setDate_of_birth("");
        patientIdentification.setDate_of_birth_precision("");
        patientIdentification.setDeath_date("");
        patientIdentification.setDeath_indicator("");

        PATIENT_ADDRESS patientAddress = new PATIENT_ADDRESS();
        patientAddress.setPostal_address("");
        patientAddress.setPhysical_address(new PHYSICAL_ADDRESS());
        patientIdentification.setPatient_address(patientAddress);

        //Set mothers name
        patientIdentification.setMother_name(new MOTHER_NAME());

        patientIdentification.setPatient_name(patientname);
        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);



        Program_Discontinuation_Message programDiscontinuationMessage = new Program_Discontinuation_Message();
        programDiscontinuationMessage.setTarget_program("HIV");
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                if (ob.getValueCoded().getUuid().equals("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Transfer Out");
                    programDiscontinuationMessage.setService_request(referralInfo(encounter));
                }
                if (ob.getValueCoded().getUuid().equals("160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Death");
                }
                if (ob.getValueCoded().getUuid().equals("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("LTFU");
                }
                if (ob.getValueCoded().getUuid().equals("164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Stopped Treatment");
                }
            }
            if (ob.getConcept().getUuid().equals("164384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                programDiscontinuationMessage.setEffective_discontinuation_date(df.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1543AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_date(df.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_indicator(ob.getValueCoded().getName().getName());
            }
        }
        ilMessage.setPatient_identification(patientIdentification);

        ilMessage.setDiscontinuation_message(programDiscontinuationMessage);
        return ilMessage;
    }

    public static PATIENT_REFERRAL_INFORMATION referralInfo(Encounter encounter) {
        //Service Request Message
        ServiceRequest referralRequest = new ServiceRequest();
        CodeableConcept codeableConcept = new CodeableConcept().addCoding(new Coding("https://hl7.org/fhir/r4/", "", ""));
        referralRequest.setId(encounter.getUuid());
        referralRequest.setCategory(Arrays.asList(codeableConcept));
        referralRequest.setCode(codeableConcept);
        String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        PATIENT_REFERRAL_INFORMATION referralInformation = new PATIENT_REFERRAL_INFORMATION();
        referralInformation.setTransfer_status(ServiceRequest.ServiceRequestStatus.ACTIVE);
        referralInformation.setTransfer_intent(ServiceRequest.ServiceRequestIntent.ORDER);
        referralInformation.setTransfer_priority(ServiceRequest.ServiceRequestPriority.ASAP);
        referralInformation.setSending_facility_mflCode(facilityMfl);
        referralInformation.setReceiving_facility_mflCode("");
        for (Obs obs: encounter.getObs()) {
            if (obs.getConcept().getUuid().equals("159495AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setReceiving_facility_mflCode(obs.getValueText().split("-")[0]);
            }
            if (obs.getConcept().getUuid().equals("160649AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setTransfer_out_date(df.format(obs.getValueDatetime()));
            }
        }

        //Set patient's last vl and current regimen
        Encounter lastLabResultsEncounter = ILUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
        Encounter lastDrugOrderEncounter = ILUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3"));
        List<Encounter> followUpEncounters = Context.getEncounterService().getEncounters(encounter.getPatient(), null, null, null, null, Arrays.asList(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e")), null, null, null, false);
        Encounter latestFollowUpEncounter = followUpEncounters.get(followUpEncounters.size() - 1);

        for (Obs obs: latestFollowUpEncounter.getObs()) {
            if (obs.getConcept().getConceptId() == 5096) {
                referralInformation.setAppointment_date(String.valueOf(obs.getValueDatetime()));
                long difference_In_Time = obs.getValueDatetime().getTime() - latestFollowUpEncounter.getEncounterDatetime().getTime();
                referralInformation.setDrug_days(String.valueOf(TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365));
            }
        }

        Integer latestVLConcept = 856;
        Integer LDLQuestionConcept = 1305;
        Integer ARVConcept = 1085;
        if(lastLabResultsEncounter != null) {
            for (Obs obs : lastLabResultsEncounter.getObs()) {
                //set vl sample collection date
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {
                    referralInformation.setLast_vl_date(String.valueOf(obs.getObsDatetime()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                    referralInformation.setLast_vl_date(String.valueOf(obs.getObsDatetime()));
                }

                //set vl result
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {
                    referralInformation.setViral_load(String.valueOf(obs.getValueNumeric()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                    referralInformation.setViral_load("LDL");
                }
            }
        }
        if (lastDrugOrderEncounter != null) {
            for (Obs obs : lastDrugOrderEncounter.getObs()) {
                if (obs != null) {
                    if (obs.getConcept().getConceptId().equals(ARVConcept)) {    //set current regimen
                        referralInformation.setCurrent_regimen(obs.getValueText());
                    }
                }
            }
        }

        return referralInformation;
    }

}
