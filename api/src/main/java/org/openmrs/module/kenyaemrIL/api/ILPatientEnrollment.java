package org.openmrs.module.kenyaemrIL.api;

import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
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
import org.openmrs.module.kenyaemrIL.programEnrollment.Program_Enrollment_Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class ILPatientEnrollment {

    static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
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

        Program_Enrollment_Message hivProgramEnrolmentMessage = new Program_Enrollment_Message();
        PATIENT_REFERRAL_INFORMATION referralInformation = referralInfo(encounter);
        hivProgramEnrolmentMessage.setPatient_type("Transfer In");
        hivProgramEnrolmentMessage.setTarget_program("HIV");
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                hivProgramEnrolmentMessage.setEntry_point(ob.getValueCoded().getName().getName());
            }
           if (ob.getConcept().getUuid().equals("160535AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
               referralInformation.setSending_facility_mflcode(ob.getValueText().split("-")[0]);

           }
        }
        hivProgramEnrolmentMessage.setService_request(referralInformation);
        ilMessage.setPatient_identification(patientIdentification);

        ilMessage.setProgram_enrollment_message(hivProgramEnrolmentMessage);
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
        referralInformation.setTransfer_status("completed");
        referralInformation.setTransfer_intent("order");
        referralInformation.setTransfer_priority("asap");
        referralInformation.setTo_acceptance_date(formatter.format(encounter.getEncounterDatetime()));
        referralInformation.setTransfer_out_date("");
        referralInformation.setReceiving_facility_mflcode(facilityMfl);

        return referralInformation;
    }
}
