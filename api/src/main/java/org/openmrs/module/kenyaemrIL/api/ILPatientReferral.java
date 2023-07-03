package org.openmrs.module.kenyaemrIL.api;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.module.kenyaemrIL.artReferral.PATIENT_REFERRAL_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ILPatientReferral {

    public static ILMessage iLPatientWrapper(Patient patient, Encounter encounter) {
        ILMessage ilMessage = new ILMessage();
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;
//set external identifier if available

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

        ilMessage.setPatient_referral(referralInfo(encounter));
        return ilMessage;
    }

    public static PATIENT_REFERRAL_INFORMATION referralInfo(Encounter encounter) {
        //Service Request Message
        ServiceRequest referralRequest = new ServiceRequest();
        CodeableConcept codeableConcept = new CodeableConcept().addCoding(new Coding("https://hl7.org/fhir/r4/","OR23","TestCode"));
        referralRequest.setId(encounter.getUuid());
        referralRequest.setCategory(Arrays.asList(codeableConcept));
        referralRequest.setCode(codeableConcept);
        String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

        PATIENT_REFERRAL_INFORMATION referralInformation = new PATIENT_REFERRAL_INFORMATION();
        referralInformation.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        referralInformation.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        referralInformation.setPriority(ServiceRequest.ServiceRequestPriority.ASAP);
        referralInformation.setToFacilityName("");
        referralInformation.setToFacilityMflCode(facilityMfl);
        referralInformation.setTiFacilityPhone("");
        referralInformation.setTiFacilityName("");
        referralInformation.setTiFacilityMflCode("");
        referralInformation.setTiFacilityMflCode("");

        /**
         * Improve TO form
         * 1. Transfer to facility is value text - it'll be hard to know which facility this is hence prefer using mfl code instead
         * 2. Make transfer to facility required
         * 3. Use mfl codes for both requester and transfer to facility
         * 4. Concept for patient referral category and code - use discontinuation reason - align with nhdd
         */

        return referralInformation;
    }

}
