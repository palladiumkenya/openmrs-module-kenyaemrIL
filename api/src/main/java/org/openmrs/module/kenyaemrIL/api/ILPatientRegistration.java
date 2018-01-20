package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.util.*;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for an IL Registration
 */
public class ILPatientRegistration {

    private final Log log = LogFactory.getLog(this.getClass());
    static ConceptService conceptService = Context.getConceptService();


    public static ILPerson iLPatientWrapper(Patient patient) {
        ILPerson ilPerson = new ILPerson();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();

        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        patientIdentification.setDate_of_birth(patient.getBirthdate().toString());
        patientIdentification.setDate_of_birth_precision(patient.getBirthdateEstimated() == true ? "ESTIMATED" : "EXACT");
        //set death date and indicator
        if(patient.isDead())     {
            patientIdentification.setDeath_date(String.valueOf(patient.getDeathDate()));
            patientIdentification.setDeath_indicator(String.valueOf(patient.isDead()));
        }

        //set patient address
//        TODO - confirm address mappings
        PersonAddress personAddress = patient.getPersonAddress();
        PATIENT_ADDRESS pAddress = new PATIENT_ADDRESS();
        PHYSICAL_ADDRESS physicalAddress = new PHYSICAL_ADDRESS();
        physicalAddress.setWard(personAddress.getAddress6());
        physicalAddress.setCounty(personAddress.getCountyDistrict());
        physicalAddress.setNearest_landmark(personAddress.getAddress2());
        physicalAddress.setSub_county(personAddress.getAddress4());
        physicalAddress.setVillage(personAddress.getCityVillage());
        pAddress.setPhysical_address(physicalAddress);
        pAddress.setPostal_address(personAddress.getAddress1());
        patientIdentification.setPatient_address(pAddress);

//set patient visit
        PATIENT_VISIT  patientVisit = new PATIENT_VISIT();
        // get enrollment Date

        Encounter lastEnrollment = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
        Date lastEnrollmentDate = lastEnrollment.getEncounterDatetime();
        patientVisit.setVisit_date(String.valueOf(lastEnrollmentDate));
        patientVisit.setHiv_care_enrollment_date(String.valueOf(lastEnrollmentDate));

        Integer patientEnrollmentTypeConcept = 164932;
        Integer patientEnrollmentSourceConcept = 160540;

        for (Obs obs : lastEnrollment.getObs()) {
            //set patient type
            if (obs.getConcept().getConceptId().equals(patientEnrollmentTypeConcept)) {    //get patient type
                patientVisit.setPatient_type(patientTypeConverter(obs.getValueCoded()));
            }
            if (obs.getConcept().getConceptId().equals(patientEnrollmentSourceConcept)) {    //get patient source
                patientVisit.setPatient_type(patientSourceConverter(obs.getValueCoded()));
            }
        }
//set external identifier if available
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;
//        Form the internal patient IDs
        for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
            ipd = new INTERNAL_PATIENT_ID();
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("OpenMRS ID")) {
                ipd.setAssigning_authority("SOURCE_SYSTEM");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("SOURCE_SYSTEM_ID");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("TB Treatment Number")) {
                ipd.setAssigning_authority("TB");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("TB_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("National ID")) {
                ipd.setAssigning_authority("GOK");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NATIONAL_ID");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HTS Number")) {
                ipd.setAssigning_authority("HTS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HTS_NUMBER");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HDSS ID")) {
                ipd.setAssigning_authority("HDSS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HDSS_ID");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("ANC NUMBER")) {
                ipd.setAssigning_authority("ANC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("ANC_NUMBER");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("OPD NUMBER")) {
                ipd.setAssigning_authority("OPD");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("OPD_NUMBER");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("PMTCT NUMBER")) {
                ipd.setAssigning_authority("PMTCT");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PMTCT_NUMBER");
            }else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("NHIF NUMBER")) {
                ipd.setAssigning_authority("NHIF");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NHIF");
            }
            else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("MPI GODS NUMBER")) {
                if(patientIdentifier.getIdentifierType().getName() !=null){
                    epd.setAssigning_authority("MPI");
                    epd.setId(patientIdentifier.getIdentifier());
                    epd.setIdentifier_type("GODS_NUMBER");
                    patientIdentification.setExternal_patient_id(epd);
                }
                continue;
            }
            internalPatientIds.add(ipd);
        }

        patientIdentification.setInternal_patient_id(internalPatientIds);

        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName());
        patientname.setMiddle_name(personName.getMiddleName());
        patientname.setLast_name(personName.getFamilyName());
        patientIdentification.setPatient_name(patientname);
        //Set the patient mothers name
        if (patient.getAttribute("Mother's Name") != null) {
            MOTHER_NAME motherName = new MOTHER_NAME();
            motherName.setFirst_name(patient.getAttribute("Mother Name").getValue());
            patientIdentification.setMother_name(motherName);
        }
//        Set the Gender
        patientIdentification.setSex(patient.getGender());

//        Set the phone number
        if (patient.getAttribute("Telephone contact") != null) {
            patientIdentification.setPhone_number(patient.getAttribute("Telephone contact").getValue());
        }

//        Get the marital status
        if (patient.getAttribute(" Civil Status") != null) {
            patientIdentification.setMarital_status(patient.getAttribute("Civil Status").getValue());
        }
        ilPerson.setPatient_identification(patientIdentification);

//    Next of KIN

        List<NEXT_OF_KIN> patientKins = new ArrayList<NEXT_OF_KIN>();
        NEXT_OF_KIN nok = new NEXT_OF_KIN();
        if (patient.getAttribute("Next of kin name") != null) {
            NOK_NAME fnok = new NOK_NAME();
            fnok.setFirst_name(patient.getAttribute("Next of kin name").getValue());
            nok.setNok_name(fnok);
        }  if (patient.getAttribute("Next of kin contact") != null) {
            nok.setPhone_number(patient.getAttribute("Next of kin contact").getValue());

        }   if (patient.getAttribute("Next of kin relationship") != null) {
            nok.setRelationship(patient.getAttribute("Next of kin relationship").getValue());

        } if (patient.getAttribute("Next of kin address") != null) {
            nok.setAddress(patient.getAttribute("Next of kin address").getValue());

        }
        patientKins.add(nok);

        return ilPerson;
    }
    static String patientTypeConverter(Concept key) {
        Map<Concept, String> patientTypeList = new HashMap<Concept, String>();
        patientTypeList.put(conceptService.getConcept(164144), "New");
        patientTypeList.put(conceptService.getConcept(160563), "Transfer In");
        patientTypeList.put(conceptService.getConcept(164931), "Transit");
        return patientTypeList.get(key);
    }
    static String patientSourceConverter(Concept key) {
        Map<Concept, String> patientSourceList = new HashMap<Concept, String>();
        patientSourceList.put(conceptService.getConcept(159938), "hbtc");
        patientSourceList.put(conceptService.getConcept(160539), "vct_site");
        patientSourceList.put(conceptService.getConcept(159937), "mch");
        patientSourceList.put(conceptService.getConcept(160536), "ipd_adult");
        patientSourceList.put(conceptService.getConcept(160537), "ipd_child");
        patientSourceList.put(conceptService.getConcept(160541), "tb");
        patientSourceList.put(conceptService.getConcept(160542), "opd");
        patientSourceList.put(conceptService.getConcept(162050), "ccc");
        patientSourceList.put(conceptService.getConcept(160551), "self");
        patientSourceList.put(conceptService.getConcept(5622), "other");
        return patientSourceList.get(key);
    }

}
