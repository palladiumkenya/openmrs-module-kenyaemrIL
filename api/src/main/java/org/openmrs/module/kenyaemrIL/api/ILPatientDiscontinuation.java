package org.openmrs.module.kenyaemrIL.api;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.HivProgramDiscontinuationMessage;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ILPatientDiscontinuation {

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


        String pattern = "MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        HivProgramDiscontinuationMessage hivProgramDiscontinuationMessage = new HivProgramDiscontinuationMessage();
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                if (ob.getValueCoded().getUuid().equals("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    hivProgramDiscontinuationMessage.setDiscontinuation_reason("Transfer Out");
                    hivProgramDiscontinuationMessage.setService_request(referralInfo(encounter));
                }
                if (ob.getValueCoded().getUuid().equals("160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    hivProgramDiscontinuationMessage.setDiscontinuation_reason("Death");
                }
                if (ob.getValueCoded().getUuid().equals("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    hivProgramDiscontinuationMessage.setDiscontinuation_reason("LTFU");
                }
                if (ob.getValueCoded().getUuid().equals("164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    hivProgramDiscontinuationMessage.setDiscontinuation_reason("Stopped Treatment");
                }
            }
            if (ob.getConcept().getUuid().equals("164384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                hivProgramDiscontinuationMessage.setEffective_discontinuation_date(df.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1543AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_date(df.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_indicator(ob.getValueCoded().getName().getName());
            }
        }
        ilMessage.setPatient_identification(patientIdentification);

        ilMessage.setPatient_hiv_discontinuation_message(hivProgramDiscontinuationMessage);
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
        referralInformation.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        referralInformation.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        referralInformation.setPriority(ServiceRequest.ServiceRequestPriority.ASAP);
        referralInformation.setToFacilityMflCode(facilityMfl);
        referralInformation.setTiFacilityMflCode("");

        return referralInformation;
    }

}
