/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrIL.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.core.util.PresortedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.NotYetImplementedException;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.appointment.AppointmentMessage;
import org.openmrs.module.kenyaemrIL.il.observation.ObservationMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.il.viralload.ViralLoadMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openmrs.module.kenyaemrIL.api.ILPatientRegistration.conceptService;

/**
 * It is a default implementation of {@link KenyaEMRILService}.
 */
public class KenyaEMRILServiceImpl extends BaseOpenmrsService implements KenyaEMRILService {

    protected final Log log = LogFactory.getLog(this.getClass());

    private List<PatientIdentifierType> allPatientIdentifierTypes;

    private Map<String, PatientIdentifierType> identifiersMap = new HashMap<>();

    private KenyaEMRILDAO dao;
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    PatientService patientService;

    @Autowired
    private KenyaEmrService kenyaEmrService;

    @Autowired
    private IdentifierSourceService idgenService;

    /**
     * @param dao the dao to set
     */
    public void setDao(KenyaEMRILDAO dao) {
        this.dao = dao;
    }

    /**
     * @return the dao
     */
    public KenyaEMRILDAO getDao() {
        return dao;
    }

    @Override
    public List<ILMessage> getPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILMessage> getAddPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");

    }

    @Override
    public List<ILMessage> getUpdatePersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendUpdateRequest(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendAddPersonRequest(ILMessage ilMessage) {
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("ADT^A04");
        ilMessage.setMessage_header(messageHeader);
        ILPerson ilPerson = ilMessage.extractILRegistration();
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            String messageString = mapper.writeValueAsString(ilPerson);
            kenyaEMRILMessage.setHl7Type("ADT^A04");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessageType(ILMessageType.OUTBOUND.getValue());
            KenyaEMRILMessage savedInstance = saveKenyaEMRILMessage(kenyaEMRILMessage);
            if (savedInstance != null) {
                isSuccessful = true;
            } else {
                isSuccessful = false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            isSuccessful = false;
        }
        return isSuccessful;
    }


    @Override
    public List<ILPharmacyOrder> fetchAllPharmacyOrders() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPharmacyOrder> fetchPharmacyOrders(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyOrder createPharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyOrder updatePharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deletePharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPharmacyDispense> fetchAllPharmacyDispenses() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPharmacyDispense> fetchPharmacyDispenses(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyDispense createPharmacyDispense(ILPharmacyDispense ilPharmacyDispense) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyDispense updatePharmacyDispense(ILPharmacyDispense ilPharmacyDispense) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deletePharmacyDispense(ILPharmacyDispense ilPharmacyDispense) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public KenyaEMRILMessage getKenyaEMRILMessageByUuid(String uniqueId) {
        KenyaEMRILMessage kenyaEMRILMessage = this.dao.getKenyaEMRILMessageByUuid(uniqueId);
        System.out.println("What is it htat was returned: " + kenyaEMRILMessage);
        return kenyaEMRILMessage;
    }

    @Override
    public KenyaEMRILMessage saveKenyaEMRILMessage(KenyaEMRILMessage ilMessage) {
        return this.dao.createKenyaEMRILMessage(ilMessage);
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILInboxes(Boolean includeRetired) {
        return this.dao.getKenyaEMRILInboxes(includeRetired);
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired) {
        return this.dao.getKenyaEMRILOutboxes(includeRetired);
    }

    @Override
    public void deleteKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage) {
        this.dao.deleteKenyaEMRILMessage(kenyaEMRILMessage);
    }

    @Override
    public List<KenyaEMRILMessage> getAllKenyaEMRILMessages(Boolean includeAll) {
        return this.dao.getAllKenyaEMRILMessages(includeAll);
    }

    @Override
    public boolean processCreatePatientRequest(ILMessage ilMessage) {
        boolean successful = false;
        Patient patient = patientService.savePatient(wrapIlPerson(ilMessage));
        if (patient != null) {
            successful = true;
        }
        return successful;
    }

    @Override
    public boolean processUpdatePatientRequest(ILMessage ilMessage) {
        Patient patient = null;
        String cccNumber = null;
//        1. Fetch the person to update using the CCC number
        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                cccNumber = internalPatientId.getId();
                break;
            }
        }
        if (cccNumber != null) {
//            no patient with the given ccc number, proceed to create a new patient with the received details
            return processCreatePatientRequest(ilMessage);
        } else {
//            fetch the patient
            List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
            if (patients.size() > 0) {
                patient = patients.get(0);
                patient = updatePatientDetails(patient, wrapIlPerson(ilMessage));
            }

            Patient cPatient = patientService.savePatient(patient);
            if (cPatient != null) {
                return true;
            } else {
                return false;
            }
        }
    }

    private Patient updatePatientDetails(Patient patientToUpdate, Patient newPatientDetails) {
//        TODO - Do the over writing of details right here
        ILMessage ilMessage = new ILMessage();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();

        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        //Set date of birth
        String iLDob = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        iLDob = formatter.format(patientToUpdate.getBirthdate());
        patientIdentification.setDate_of_birth(iLDob);
        //set dob precision
        patientIdentification.setDate_of_birth_precision(patientToUpdate.getBirthdateEstimated() == true ? "ESTIMATED" : "EXACT");
        //set death date and indicator
        if (patientToUpdate.isDead()) {
            patientIdentification.setDeath_date(String.valueOf(patientToUpdate.getDeathDate()));
            patientIdentification.setDeath_indicator(String.valueOf(patientToUpdate.isDead()));
        } else {
            patientIdentification.setDeath_date("");
            patientIdentification.setDeath_indicator("");
        }

        //set patient address
//        TODO - confirm address mappings
        PersonAddress personAddress = patientToUpdate.getPersonAddress();
        PATIENT_ADDRESS pAddress = new PATIENT_ADDRESS();
        PHYSICAL_ADDRESS physicalAddress = new PHYSICAL_ADDRESS();
        physicalAddress.setWard(personAddress.getAddress6());
        physicalAddress.setCounty(personAddress.getCountyDistrict());
        physicalAddress.setNearest_landmark(personAddress.getAddress2());
        physicalAddress.setSub_county(personAddress.getAddress4());
        physicalAddress.setVillage(personAddress.getCityVillage());
        physicalAddress.setGps_location("");
        pAddress.setPhysical_address(physicalAddress);

        pAddress.setPostal_address(personAddress.getAddress1());
        patientIdentification.setPatient_address(pAddress);

//set patient visit
        PATIENT_VISIT patientVisit = new PATIENT_VISIT();
        // get enrollment Date

        Encounter lastEnrollment = ILUtils.lastEncounter(patientToUpdate, Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));
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
        for (PatientIdentifier patientIdentifier : patientToUpdate.getIdentifiers()) {
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
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HTS Number")) {
                ipd.setAssigning_authority("HTS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HTS_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("HDSS ID")) {
                ipd.setAssigning_authority("HDSS");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("HDSS_ID");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("ANC NUMBER")) {
                ipd.setAssigning_authority("ANC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("ANC_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("OPD NUMBER")) {
                ipd.setAssigning_authority("OPD");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("OPD_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("PMTCT NUMBER")) {
                ipd.setAssigning_authority("PMTCT");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PMTCT_NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("NHIF NUMBER")) {
                ipd.setAssigning_authority("NHIF");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("NHIF");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Patient Clinic Number")) {
                ipd.setAssigning_authority("CLINIC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PATIENT CLINIC NUMBER");
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("MPI GODS NUMBER")) {
                if (patientIdentifier.getIdentifierType().getName() != null) {
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
        patientIdentification.setExternal_patient_id(epd);

        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patientToUpdate.getPersonName();
        patientname.setFirst_name(personName.getGivenName());
        patientname.setMiddle_name(personName.getMiddleName());
        patientname.setLast_name(personName.getFamilyName());
        patientIdentification.setPatient_name(patientname);
        //Set the patient mothers name
        MOTHER_NAME motherName = new MOTHER_NAME();
        if (patientToUpdate.getAttribute("Mother's Name") != null) {
            motherName.setFirst_name(patientToUpdate.getAttribute("Mother Name") != null ? patientToUpdate.getAttribute("Mother Name").getValue() : "");
            patientIdentification.setMother_name(motherName);
        }
//        Set the Gender
        patientIdentification.setSex(patientToUpdate.getGender());


//        Set the phone number
        patientIdentification.setPhone_number(patientToUpdate.getAttribute("Telephone contact") != null ? patientToUpdate.getAttribute("Telephone contact").getValue() : "");


//        Get the marital status
        patientIdentification.setMarital_status(patientToUpdate.getAttribute("Civil Status") != null ? patientToUpdate.getAttribute("Civil Status").getValue() : "");
        ilMessage.setPatient_identification(patientIdentification);

//    Next of KIN

        NEXT_OF_KIN patientKins[] = new NEXT_OF_KIN[1];
        NEXT_OF_KIN nok = new NEXT_OF_KIN();
        if (patientToUpdate.getAttribute("Next of kin name") != null) {
            NOK_NAME fnok = new NOK_NAME();
            String nextOfKinName = patientToUpdate.getAttribute("Next of kin name").getValue();
            String[] split = nextOfKinName.split(" ");
            switch (split.length) {
                case 1: {
                    fnok.setFirst_name(split[0]);
                    break;
                }
                case 2: {
                    fnok.setFirst_name(split[0]);
                    fnok.setMiddle_name(split[1]);
                    break;
                }
                case 3: {
                    fnok.setFirst_name(split[0]);
                    fnok.setMiddle_name(split[1]);
                    fnok.setLast_name(split[2]);
                    break;
                }
            }

            nok.setNok_name(fnok);
        } else {

        }
        nok.setPhone_number(patientToUpdate.getAttribute("Next of kin contact") != null ? patientToUpdate.getAttribute("Next of kin contact").getValue() : "");
        nok.setRelationship(patientToUpdate.getAttribute("Next of kin relationship") != null ? patientToUpdate.getAttribute("Next of kin relationship").getValue() : "");
        nok.setAddress(patientToUpdate.getAttribute("Next of kin address") != null ? patientToUpdate.getAttribute("Next of kin address").getValue() : "");
        nok.setSex("");
        nok.setDate_of_birth("");
        nok.setContact_role("");
        patientKins[0] = nok;
        ilMessage.setNext_of_kin(patientKins);

        return patientToUpdate;
    }

    @Override
    public boolean processPharmacyOrder(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean processPharmacyDispense(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean processAppointmentSchedule(ILMessage ilMessage) {
        boolean success = false;
//        TODO - process an incoming appointment
        return success;
    }

    @Override
    public boolean processLabOrder(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean processObservationResult(ILMessage ilMessage) {
        boolean success = false;
//        TODO - Process incoming ORUs
        return success;
    }

    @Override
    public boolean processViralLoad(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean process731Adx(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean logAppointmentSchedule(ILMessage ilMessage) {
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("SIU^S12");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            AppointmentMessage appointmentMessage = ilMessage.extractAppointmentMessage();
            String messageString = mapper.writeValueAsString(appointmentMessage);
            kenyaEMRILMessage.setHl7Type("SIU^S12");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessageType(ILMessageType.OUTBOUND.getValue());
            KenyaEMRILMessage savedInstance = saveKenyaEMRILMessage(kenyaEMRILMessage);
            if (savedInstance != null) {
                isSuccessful = true;
            } else {
                isSuccessful = false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            isSuccessful = false;
        }
        return isSuccessful;
    }

    @Override
    public boolean logViralLoad(ILMessage ilMessage) {
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("ORU^VL");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            ViralLoadMessage viralLoadMessage =  ilMessage.extractViralLoadMessage();
            String messageString = mapper.writeValueAsString(viralLoadMessage);
            kenyaEMRILMessage.setHl7Type("ORU^VL");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessageType(ILMessageType.OUTBOUND.getValue());
            KenyaEMRILMessage savedInstance = saveKenyaEMRILMessage(kenyaEMRILMessage);
            if (savedInstance != null) {
                isSuccessful = true;
            } else {
                isSuccessful = false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            isSuccessful = false;
        }
        return isSuccessful;
    }

    @Override
    public boolean logORUs(ILMessage ilMessage) {
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("ORU^R01");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            ObservationMessage observationMessage = ilMessage.extractORUMessage();
            String messageString = mapper.writeValueAsString(observationMessage);
            kenyaEMRILMessage.setHl7Type("ORU^R01");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessageType(ILMessageType.OUTBOUND.getValue());
            KenyaEMRILMessage savedInstance = saveKenyaEMRILMessage(kenyaEMRILMessage);
            if (savedInstance != null) {
                isSuccessful = true;
            } else {
                isSuccessful = false;
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            isSuccessful = false;
        }
        return isSuccessful;
    }

    private Patient wrapIlPerson(ILMessage ilPerson) {

        Patient patient = new Patient();
        Location defaultLocation = kenyaEmrService.getDefaultLocation();

        allPatientIdentifierTypes = Context.getPatientService().getAllPatientIdentifierTypes();
        for (PatientIdentifierType identiferType : allPatientIdentifierTypes) {
            identifiersMap.put(identiferType.getName(), identiferType);
        }
//        Process Patient Identification details
        PATIENT_IDENTIFICATION patientIdentification = ilPerson.getPatient_identification();


//        Process the general stuff here


//        Set the patient date of birth
        String stringDateOfBirth = patientIdentification.getDate_of_birth();
        if (stringDateOfBirth != null) {
            Date dateOfBirth = null;
            DateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            try {
                dateOfBirth = formatter.parse(stringDateOfBirth);
                patient.setBirthdate(dateOfBirth);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

//        Set the date of birth precision
        String dateOfBirthPrecision = patientIdentification.getDate_of_birth_precision();
        if (dateOfBirthPrecision != null) {
            patient.setBirthdateEstimated(dateOfBirthPrecision.equalsIgnoreCase("estimated") ? true : false);
        }


//        Process the marital status
        String maritalStatus = patientIdentification.getMarital_status();
        PersonAttribute maritalAttribute = new PersonAttribute();
        PersonAttributeType maritalAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("8d871f2a-c2cc-11de-8d13-0010c6dffd0f");
        if (maritalAttributeType != null) {
            maritalAttribute.setAttributeType(maritalAttributeType);
            maritalAttribute.setValue(maritalStatus);
            patient.addAttribute(maritalAttribute);
        }

//        Process phone number as an attribute
        String phoneNumber = patientIdentification.getPhone_number();
        if (phoneNumber != null) {
            PersonAttribute phoneAttribute = new PersonAttribute();
            PersonAttributeType phoneAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("b2c38640-2603-4629-aebd-3b54f33f1e3a");
            phoneAttribute.setAttributeType(phoneAttributeType);
            phoneAttribute.setValue(phoneNumber);
            patient.addAttribute(phoneAttribute);
        }


//        Set the gender
        if (patientIdentification.getSex() != null) {
            patient.setGender(patientIdentification.getSex().toUpperCase());
        }

//        Patient name processing
        PATIENT_NAME patientName = patientIdentification.getPatient_name();
//        Set<PersonName> names = new HashSet<>();
        if (patientName != null) {
            SortedSet<PersonName> names = new PresortedSet();
            PersonName personName = new PersonName();
            personName.setGivenName(patientName.getFirst_name() != null ? patientName.getFirst_name() : "");
            personName.setMiddleName(patientName.getMiddle_name() != null ? patientName.getMiddle_name() : "");
            personName.setFamilyName(patientName.getLast_name() != null ? patientName.getLast_name() : "");
            names.add(personName);
            patient.setNames(names);
        }

        SortedSet<PatientIdentifier> patientIdentifiers = new PresortedSet();
//        Process external patient id if it exists
        EXTERNAL_PATIENT_ID externalPatientId = patientIdentification.getExternal_patient_id();
        if (externalPatientId != null) {
            PatientIdentifier patientIdentifier = new PatientIdentifier();
            PatientIdentifierType idType = processIdentifierType(externalPatientId.getIdentifier_type());
            if (idType != null && patientIdentifier != null) {
                patientIdentifier.setIdentifierType(idType);
                patientIdentifier.setIdentifier(externalPatientId.getId());
                patientIdentifiers.add(patientIdentifier);
            }
        }

//        Process internal patient IDs
        List<INTERNAL_PATIENT_ID> internalPatientIds = patientIdentification.getInternal_patient_id();

//        Must set a preferred Identifier
        if (internalPatientIds.size() > 0) {

            for (INTERNAL_PATIENT_ID internalPatientId : internalPatientIds) {
                PatientIdentifier patientIdentifier = new PatientIdentifier();
                PatientIdentifierType idType = processIdentifierType(internalPatientId.getIdentifier_type());
                int missingOpenMRSId = 0;
                if (idType != null) {
                    patientIdentifier.setIdentifierType(idType);

                    if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                        patientIdentifier.setPreferred(true);
                        String ccc = internalPatientId.getId();
                        ccc = deleteCharacter(ccc, "-");
                        patientIdentifier.setIdentifier(ccc);
                        patientIdentifiers.add(patientIdentifier);
                        continue;
                    }
                } else {
                    continue;
                }
                patientIdentifier.setIdentifier(internalPatientId.getId());
//            internalPatientId.getAssigning_authority();
            }
//            //Generate openmrsID
            PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.OPENMRS_ID);
            PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
            String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "Registration");
            openmrsId = new PatientIdentifier(generated, openmrsIdType, defaultLocation);
            patientIdentifiers.add(openmrsId);
        }

        patient.setIdentifiers(patientIdentifiers);


////        Process mother name
        MOTHER_NAME motherName = patientIdentification.getMother_name();
        if (motherName != null) {
            PersonAttribute motherNameAttribute = new PersonAttribute();
            PersonAttributeType motherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("8d871d18-c2cc-11de-8d13-0010c6dffd0f");
            if (motherNameAttributeType != null) {
                motherNameAttribute.setAttributeType(motherNameAttributeType);
                patient.addAttribute(motherNameAttribute);

                String motherNameString = motherName.getFirst_name() + motherName.getMiddle_name() + motherName.getLast_name();
                motherNameAttribute.setValue(motherNameString);
            }
        }
//        Process patient address if exists
        PATIENT_ADDRESS patientAddress = patientIdentification.getPatient_address();
        if (patientAddress != null) {
//        Set<PersonAddress> addresses = new HashSet<>();
            SortedSet<PersonAddress> addresses = new PresortedSet();
            PersonAddress personAddress = new PersonAddress();
            personAddress.setPreferred(true);
            personAddress.setAddress6(patientAddress.getPhysical_address() != null ? patientAddress.getPhysical_address().getWard() : "");
            personAddress.setAddress2(patientAddress.getPhysical_address() != null ? patientAddress.getPhysical_address().getNearest_landmark() : "");
            personAddress.setAddress4(patientAddress.getPhysical_address() != null ? patientAddress.getPhysical_address().getSub_county() : "");
            personAddress.setCityVillage(patientAddress.getPhysical_address() != null ? patientAddress.getPhysical_address().getVillage() : "");
//        personAddress.setCountry();
            personAddress.setCountyDistrict(patientAddress.getPhysical_address() != null ? patientAddress.getPhysical_address().getCounty() : "");
            personAddress.setAddress1(patientAddress.getPostal_address() != null ? patientAddress.getPostal_address() : "");
            addresses.add(personAddress);
            patient.setAddresses(addresses);
        }

//        Process Next of kin details
        NEXT_OF_KIN[] next_of_kin = ilPerson.getNext_of_kin();
        if (next_of_kin.length > 0 && next_of_kin != null) {
//            There is a next of kin thus process it
            NEXT_OF_KIN nok = next_of_kin[0];
//            Process the nok name
            if (nok.getNok_name() != null) {
                PersonAttribute nokNameAttribute = new PersonAttribute();
                PersonAttributeType nokNameAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("830bef6d-b01f-449d-9f8d-ac0fede8dbd3");
                nokNameAttribute.setAttributeType(nokNameAttributeType);
                nokNameAttribute.setValue(nok.getNok_name().toString());
                patient.addAttribute(nokNameAttribute);
            }

//            Process the nok contact
            if (nok.getPhone_number() != null) {
                PersonAttribute nokContactAttribute = new PersonAttribute();
                PersonAttributeType nokContactAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("342a1d39-c541-4b29-8818-930916f4c2dc");
                nokContactAttribute.setAttributeType(nokContactAttributeType);
                nokContactAttribute.setValue(nok.getPhone_number());
                patient.addAttribute(nokContactAttribute);
            }

//            Process the nok address
            if (nok.getAddress() != null) {
                PersonAttribute nokAddressAttribute = new PersonAttribute();
                PersonAttributeType nokAddressAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5");
                nokAddressAttribute.setAttributeType(nokAddressAttributeType);
                nokAddressAttribute.setValue(nok.getAddress());
                patient.addAttribute(nokAddressAttribute);
            }

//            Process the nok relationship
            if (nok.getRelationship() != null) {
                PersonAttribute nokRealtionshipAttribute = new PersonAttribute();
                PersonAttributeType nokRelationshipAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5");
                nokRealtionshipAttribute.setAttributeType(nokRelationshipAttributeType);
                nokRealtionshipAttribute.setValue(nok.getRelationship());
                patient.addAttribute(nokRealtionshipAttribute);
            }
        }


        return patient;
    }

    private PatientIdentifierType processIdentifierType(String identifierType) {
        PatientIdentifierType patientIdentifierType = null;
//        TODO - Process the appropriate openmrs identifier types
//        OpenMRS Identification Number
//        Old Identification Number
        if (identifierType != null) {

            switch (identifierType.toUpperCase()) {
                case "CCC_NUMBER": {
                    patientIdentifierType = identifiersMap.get("Unique Patient Number");
                    break;
                }
                case "HTS_NUMBER": {
                    patientIdentifierType = identifiersMap.get("HTS NUMBER");
                    break;
                }
                case "TB_NUMBER": {
                    patientIdentifierType = identifiersMap.get("TB Treatment Number");
                    break;
                }
                case "ANC_NUMBER": {
                    patientIdentifierType = identifiersMap.get("ANC NUMBER");
                    break;
                }
                case "PMTCT_NUMBER": {
                    patientIdentifierType = identifiersMap.get("PMTCT NUMBER");
                    break;
                }
                case "OPD_NUMBER": {
                    patientIdentifierType = identifiersMap.get("OPD NUMBER");
                    break;
                }
                case "NATIONAL_ID": {
                    patientIdentifierType = identifiersMap.get("National ID");
                    break;
                }
                case "NHIF": {
                    patientIdentifierType = identifiersMap.get("NHIF");
                    break;
                }
                case "HDSS_ID": {
                    patientIdentifierType = identifiersMap.get("HDSS ID");
                    break;
                }
                case "GODS_NUMBER": {
                    patientIdentifierType = identifiersMap.get("MPI GODS NUMBER");
                    break;
                }
                default: {
//                nothing to process, set to null
                    patientIdentifierType = null;
                    break;
                }
            }
        }
        return patientIdentifierType;
    }


    //    TODO - review after discussion on the standard format
    private static String deleteCharacter(String string, String index) {
        return string.replaceAll(index, "");
    }

    public String deleteCharacter(String string, Object... arguments) {
        for (Object argument : arguments) {
            string = string.replaceAll(String.valueOf(argument), "");
        }
        return string;
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
