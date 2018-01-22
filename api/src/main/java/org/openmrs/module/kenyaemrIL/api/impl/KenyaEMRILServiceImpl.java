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

import com.thoughtworks.xstream.core.util.PresortedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.NotYetImplementedException;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.il.appointment.AppointmentMessage;
import org.openmrs.module.kenyaemrIL.il.observation.ObservationMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * It is a default implementation of {@link KenyaEMRILService}.
 */
public class KenyaEMRILServiceImpl extends BaseOpenmrsService implements KenyaEMRILService {

    protected final Log log = LogFactory.getLog(this.getClass());

    private List<PatientIdentifierType> allPatientIdentifierTypes;

    private Map<String, PatientIdentifierType> identifiersMap = new HashMap<>();

    private KenyaEMRILDAO dao;

    @Autowired
    PatientService patientService;

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

//    public boolean sendAddPersonRequest(ILPerson ilPerson) {
//        boolean isSuccessful;
//        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance();
//        messageHeader.setMessage_type("ADT^A04");
//        messageHeader.setMessage_datetime(String.valueOf(new Date()));
//        ilPerson.setMessage_header(messageHeader);
//        ObjectMapper mapper = new ObjectMapper();
//        KenyaEMRILMessage ilMessage = new KenyaEMRILMessage();
//        try {
//            String messageString = mapper.writeValueAsString(ilPerson);
//            ilMessage.setHl7Type("ADT^A04");
//            ilMessage.setMessage(messageString);
//            ilMessage.setDescription("");
//            ilMessage.setName("");
//            ilMessage.setMessageType(ILMessageType.OUTBOUND.getValue());
//            KenyaEMRILMessage savedInstance = saveKenyaEMRILMessage(ilMessage);
//            if (savedInstance != null) {
//                isSuccessful = true;
//            } else {
//                isSuccessful = false;
//            }
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            isSuccessful = false;
//        }
//        return isSuccessful;
//    }


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
//        TODO - process an appointment
        AppointmentMessage message = (AppointmentMessage) ilMessage;


        return success;
    }

    @Override
    public boolean processLabOrder(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean processObservationResult(ILMessage ilMessage) {
        boolean success = false;
        ObservationMessage observationMessage = (ObservationMessage) ilMessage;
//        TODO - Process ORUs
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

    private Patient wrapIlPerson(ILMessage ilPerson) {

        Patient patient = new Patient();

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
            } catch (ParseException e) {
                e.printStackTrace();
            }
            patient.setBirthdate(dateOfBirth);
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
        if(maritalAttributeType !=null ) {
            maritalAttribute.setAttributeType(maritalAttributeType);
            maritalAttribute.setValue(maritalStatus);
            patient.addAttribute(maritalAttribute);
        }

//        Process phone number as an attribute
        String phoneNumber = patientIdentification.getPhone_number();
        PersonAttribute phoneAttribute = new PersonAttribute();
        PersonAttributeType phoneAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("b2c38640-2603-4629-aebd-3b54f33f1e3a");
        phoneAttribute.setAttributeType(phoneAttributeType);
        phoneAttribute.setValue(phoneNumber);
        patient.addAttribute(phoneAttribute);


//        Set the gender
        patient.setGender(patientIdentification.getSex().toUpperCase());

//        Patient name processing
        PATIENT_NAME patientName = patientIdentification.getPatient_name();
//        Set<PersonName> names = new HashSet<>();
        SortedSet<PersonName> names = new PresortedSet();
        PersonName personName = new PersonName();
        personName.setGivenName(patientName.getFirst_name());
        personName.setMiddleName(patientName.getMiddle_name());
        personName.setFamilyName(patientName.getLast_name());
        names.add(personName);
        patient.setNames(names);

        SortedSet<PatientIdentifier> patientIdentifiers = new PresortedSet();
//        Process external patient id if it exists
        EXTERNAL_PATIENT_ID externalPatientId = patientIdentification.getExternal_patient_id();
        if (externalPatientId != null && externalPatientId != null) {
            PatientIdentifier patientIdentifier = new PatientIdentifier();
            PatientIdentifierType idType = processIdentifierType(externalPatientId.getIdentifier_type());
            if (idType != null) {
                patientIdentifier.setIdentifierType(idType);
                patientIdentifier.setIdentifier(externalPatientId.getId());
            }
            patientIdentifiers.add(patientIdentifier);
        }

//        Process internal patient IDs
        List<INTERNAL_PATIENT_ID> internalPatientIds = patientIdentification.getInternal_patient_id();

//        Must set a preferred Identifier
        for (INTERNAL_PATIENT_ID internalPatientId : internalPatientIds) {
            PatientIdentifier patientIdentifier = new PatientIdentifier();
            PatientIdentifierType idType = processIdentifierType(internalPatientId.getIdentifier_type());
            if (idType != null) {
                patientIdentifier.setIdentifierType(idType);
                if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                    patientIdentifier.setPreferred(true);
                }
            } else {
                continue;
            }
            patientIdentifier.setIdentifier(internalPatientId.getId());
//            internalPatientId.getAssigning_authority();
            patientIdentifiers.add(patientIdentifier);
        }
        patient.setIdentifiers(patientIdentifiers);


////        Process mother name
//        MOTHER_NAME motherName = patientIdentification.getMother_name();
//        PersonAttribute motherNameAttribute = new PersonAttribute();
//        PersonAttributeType motherNameAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("8d871d18-c2cc-11de-8d13-0010c6dffd0f");
//
//        if(motherNameAttributeType != null) {
//            motherNameAttribute.setAttributeType(motherNameAttributeType);
//            patient.addAttribute(motherNameAttribute);
//
//            String motherNameString = motherName.getFirst_name() + motherName.getMiddle_name() + motherName.getLast_name();
//            motherNameAttribute.setValue(motherNameString);
//        }
////        Process patient address if exists
//        PATIENT_ADDRESS patientAddress = patientIdentification.getPatient_address();
//
//
////        Set<PersonAddress> addresses = new HashSet<>();
//        SortedSet<PersonAddress> addresses = new PresortedSet();
//        PersonAddress personAddress = new PersonAddress();
//        personAddress.setPreferred(true);
//        personAddress.setAddress6(patientAddress.getPhysical_address().getWard());
//        personAddress.setAddress2(patientAddress.getPhysical_address().getNearest_landmark());
//        personAddress.setAddress4(patientAddress.getPhysical_address().getSub_county());
//        personAddress.setCityVillage(patientAddress.getPhysical_address().getVillage());
////        personAddress.setCountry();
//        personAddress.setCountyDistrict(patientAddress.getPhysical_address().getCounty());
//        personAddress.setAddress1(patientAddress.getPostal_address());
//        addresses.add(personAddress);
//        patient.setAddresses(addresses);


//        Process Next of kin details
        NEXT_OF_KIN[] next_of_kin = ilPerson.getNext_of_kin();
        if (next_of_kin.length > 0) {
//            There is a next of kin thus process it
            NEXT_OF_KIN nok = next_of_kin[0];
//            Process the nok name
            PersonAttribute nokNameAttribute = new PersonAttribute();
            PersonAttributeType nokNameAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("830bef6d-b01f-449d-9f8d-ac0fede8dbd3");
            nokNameAttribute.setAttributeType(nokNameAttributeType);
            nokNameAttribute.setValue(nok.getNok_name().toString());
            patient.addAttribute(nokNameAttribute);


//            Process the nok contact
            PersonAttribute nokContactAttribute = new PersonAttribute();
            PersonAttributeType nokContactAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("342a1d39-c541-4b29-8818-930916f4c2dc");
            nokContactAttribute.setAttributeType(nokContactAttributeType);
            nokContactAttribute.setValue(nok.getPhone_number());
            patient.addAttribute(nokContactAttribute);

//            Process the nok address
            PersonAttribute nokAddressAttribute = new PersonAttribute();
            PersonAttributeType nokAddressAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5");
            nokAddressAttribute.setAttributeType(nokAddressAttributeType);
            nokAddressAttribute.setValue(nok.getAddress());
            patient.addAttribute(nokAddressAttribute);

//            Process the nok relationship
            PersonAttribute nokRealtionshipAttribute = new PersonAttribute();
            PersonAttributeType nokRelationshipAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("d0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5");
            nokRealtionshipAttribute.setAttributeType(nokRelationshipAttributeType);
            nokRealtionshipAttribute.setValue(nok.getRelationship());
            patient.addAttribute(nokRealtionshipAttribute);

        }


        return patient;
    }

    private PatientIdentifierType processIdentifierType(String identifierType) {
        PatientIdentifierType patientIdentifierType = null;
//        TODO - Process the appropriate openmrs identifier types
//        OpenMRS Identification Number
//        Old Identification Number
        if(patientIdentifierType != null) {

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

}