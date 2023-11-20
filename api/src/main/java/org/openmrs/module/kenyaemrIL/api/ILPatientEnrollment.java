package org.openmrs.module.kenyaemrIL.api;

import com.google.common.base.Strings;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
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
import org.openmrs.module.kenyaemrIL.kenyaemrUtils.Utils;
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
                // Form the default external patient IDs
                epd.setAssigning_authority("MPI");
                epd.setIdentifier_type("GODS_NUMBER");
                patientIdentification.setExternal_patient_id(epd);
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Patient Clinic Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PATIENT_CLINIC_NUMBER");
                internalPatientIds.add(ipd);
            } else if (patientIdentifier.getIdentifierType().getUuid().equalsIgnoreCase("f85081e2-b4be-4e48-b3a4-7994b69bb101")) { // this is NUPI
                ipd.setAssigning_authority("MOH");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NUPI");
                internalPatientIds.add(ipd);
            }
        }

        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);

        //Set the patient name
        PATIENT_NAME patientName = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        if(personName != null) {
            patientName.setFirst_name(personName.getGivenName() != null ? personName.getGivenName() : "");
            patientName.setMiddle_name(personName.getMiddleName() != null ? personName.getMiddleName() : "");
            patientName.setLast_name(personName.getFamilyName() != null ? personName.getFamilyName() : "");
            patientIdentification.setPatient_name(patientName);
        }
        // Set to empty strings unwanted patient details for viral load
        patientIdentification.setSex(patient.getGender());
        patientIdentification.setPhone_number(patient.getAttribute("Telephone contact") != null ? patient.getAttribute("Telephone contact").getValue() : "");
        patientIdentification.setMarital_status(patient.getAttribute("Civil Status") != null ? patient.getAttribute("Civil Status").getValue() : "");
        String iLDob;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        iLDob = formatter.format(patient.getBirthdate());
        patientIdentification.setDate_of_birth(iLDob);
        //set dob precision
        patientIdentification.setDate_of_birth_precision(patient.getBirthdateEstimated() == true ? "ESTIMATED" : "EXACT");
        //set death date and indicator
        if (patient.isDead()) {
            patientIdentification.setDeath_date(String.valueOf(patient.getDeathDate()));
            patientIdentification.setDeath_indicator(String.valueOf(patient.isDead()));
        } else {
            patientIdentification.setDeath_date("");
            patientIdentification.setDeath_indicator("");
        }

        //add patientIdentification to IL message
        PersonAddress personAddress = patient.getPersonAddress();
        if (personAddress != null) {

            PATIENT_ADDRESS pAddress = new PATIENT_ADDRESS();
            PHYSICAL_ADDRESS physicalAddress = new PHYSICAL_ADDRESS();

            physicalAddress.setWard(personAddress.getAddress6() != null ? personAddress.getAddress6() : "");
            physicalAddress.setCounty(personAddress.getCountyDistrict() != null ? personAddress.getCountyDistrict() : "");
            physicalAddress.setNearest_landmark(personAddress.getAddress2() != null ? personAddress.getAddress2() : "");
            physicalAddress.setSub_county(personAddress.getAddress4() != null ? personAddress.getAddress4() : "");
            physicalAddress.setVillage(personAddress.getCityVillage() != null ? personAddress.getCityVillage() : "");
            physicalAddress.setGps_location("");
            pAddress.setPhysical_address(physicalAddress);

            pAddress.setPostal_address("");
            patientIdentification.setPatient_address(pAddress);
        }

        //Set mothers name
        MOTHER_NAME motherName = new MOTHER_NAME();
        if (patient.getAttribute("Mother's Name") != null) {
            motherName.setFirst_name(patient.getAttribute("Mother Name") != null ? patient.getAttribute("Mother Name").getValue() : "");
            patientIdentification.setMother_name(motherName);
        }

        Program_Enrollment_Message hivProgramEnrolmentMessage = new Program_Enrollment_Message();
        PATIENT_REFERRAL_INFORMATION referralInformation = referralInfo(encounter);
        hivProgramEnrolmentMessage.setPatient_type("Transfer In");
        hivProgramEnrolmentMessage.setTarget_program("HIV");
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                hivProgramEnrolmentMessage.setEntry_point(ob.getValueCoded().getName().getName());
            }
           if (ob.getConcept().getUuid().equals("160535AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") && !Strings.isNullOrEmpty(ob.getValueText())) {
               String valueText = ob.getValueText();
               Location location = Context.getLocationService().getLocationByUuid(valueText);
               if (location == null) {
                   referralInformation.setSending_facility_mflcode(ob.getValueText().split("-")[0]);
               } else {
                   referralInformation.setSending_facility_mflcode(Utils.getLocationMflCode(location));
               }
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
