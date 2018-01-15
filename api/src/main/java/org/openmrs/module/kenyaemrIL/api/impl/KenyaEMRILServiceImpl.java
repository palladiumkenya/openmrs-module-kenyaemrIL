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

import org.hibernate.cfg.NotYetImplementedException;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemrIL.il.*;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.observation.ILObservation;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * It is a default implementation of {@link KenyaEMRILService}.
 */
public class KenyaEMRILServiceImpl extends BaseOpenmrsService implements KenyaEMRILService {

    protected final Log log = LogFactory.getLog(this.getClass());

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

    @Override
    public List<ILPerson> getPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPerson> getAddPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");

    }

    @Override
    public List<ILPerson> getUpdatePersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendUpdateRequest(ILPerson ilPerson) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendAddPersonRequest(ILPerson ilPerson) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILAppointment> fetchAllAppointments() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILAppointment> fetchAppointments(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deleteAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILAppointment createAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILAppointment updateAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");
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
    public List<ILObservation> fetchAllObservations() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILObservation> fetchObservations(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILObservation createObservation(ILObservation ilObservation) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILObservation updateObservation(ILObservation ilObservation) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deleteObservation(ILObservation ilObservation) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public KenyaEMRILMessage getKenyaEMRILMessageByUuid(String uniqueId) {
        KenyaEMRILMessage kenyaEMRILMessage = this.dao.getKenyaEMRILMessageByUuid(uniqueId);
        System.out.println("What is it htat was returned: " + kenyaEMRILMessage);
        return kenyaEMRILMessage;
    }

    @Override
    public KenyaEMRILMessage saveKenyaEMRILMessage(KenyaEMRILMessage delegate) {
        return this.dao.createKenyaEMRILMessage(delegate);
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILInboxes(Boolean includeRetired) {
        return this.dao.getKenyaEMRILInboxes(includeRetired);
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired) {
        return this.dao.getKenyaEMRILInboxes(includeRetired);
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
    public boolean processCreatePatientRequest(ILPerson ilPerson) {
        boolean successful = false;
        Patient patient = patientService.savePatient(wrapIlPerson(ilPerson));
        if (patient != null) {
            successful = true;
        }
        return successful;
    }

    private Patient wrapIlPerson(ILPerson ilPerson) {
        Patient patient = new Patient();


//        Process Patient Identification details
        PATIENT_IDENTIFICATION patientIdentification = ilPerson.getPatient_identification();

//        Process the general stuff here
        String dateOfBirth = patientIdentification.getDate_of_birth();
//        TODO - re-work to form an appropriate date
//        patient.setBirthdate(new Date(dateOfBirth));

        String maritalStatus = patientIdentification.getMarital_status();
//        TODO - Process the marital status thing

        String phoneNumber = patientIdentification.getPhone_number();
//        TODO - process phone number as an attribute

        patient.setGender(patientIdentification.getSex());

//        Patient name processing
        PATIENT_NAME patientName = patientIdentification.getPatient_name();
        Set<PersonName> names = new HashSet<>();
        PersonName personName = new PersonName();
        personName.setGivenName(patientName.getFirst_name());
        personName.setMiddleName(patientName.getMiddle_name());
        personName.setFamilyName(patientName.getLast_name());
        names.add(personName);
        patient.setNames(names);

//        Process external patient id if exists
        EXTERNAL_PATIENT_ID externalPatientId = patientIdentification.getExternal_patient_id();
//        TODO - Process external ID

//        Process internal patient IDs
        List<INTERNAL_PATIENT_ID> internalPatientIds = patientIdentification.getInternal_patient_id();
        Set<PatientIdentifier> patientIdentifiers = new HashSet<>();

//        Must set a preferred Identifier
        for (INTERNAL_PATIENT_ID internalPatientId : internalPatientIds) {
            PatientIdentifier patientIdentifier = new PatientIdentifier();
            PatientIdentifierType idType = processIdentifierType(internalPatientId.getIdentifier_type());
            if (idType != null) {
                patientIdentifier.setIdentifierType(idType);
            } else {
                continue;
            }
            patientIdentifier.setIdentifier(internalPatientId.getId());
//            internalPatientId.getAssigning_authority();
            patientIdentifiers.add(patientIdentifier);
        }
        patient.setIdentifiers(patientIdentifiers);


//        Process mother name
        MOTHER_NAME motherName = patientIdentification.getMother_name();
//        TODO - Process as an attribute


//        Process patient address if exists
        PATIENT_ADDRESS patientAddress = patientIdentification.getPatient_address();
        Set<PersonAddress> addresses = new HashSet<>();
        PersonAddress personAddress = new PersonAddress();
        personAddress.setPreferred(true);
        personAddress.setCityVillage(patientAddress.getPhysical_address().getVillage());
//        personAddress.setCountry();
        personAddress.setCountyDistrict(patientAddress.getPhysical_address().getCounty());
        personAddress.setAddress1(patientAddress.getPostal_address());
        addresses.add(personAddress);
        patient.setAddresses(addresses);


//        Process Next of kin details
        NEXT_OF_KIN[] next_of_kin = ilPerson.getNext_of_kin();


        return patient;
    }

    private PatientIdentifierType processIdentifierType(String identifierType) {
        PatientIdentifierType patientIdentifierType = null;
//        TODO - Process the appropriate openmrs identifier types
        switch (identifierType) {
            case "CCC_NUMBER": {

                break;
            }
            case "HTS_NUMBER": {

                break;
            }
            case "TB_NUMBER": {

                break;
            }
            case "ANC_NUMBER": {

                break;
            }
            case "PMTCT_NUMBER": {

                break;
            }
            case "OPD_NUMBER": {

                break;
            }
            case "NATIONAL_ID": {

                break;
            }
            case "NHIF": {

                break;
            }
            case "HDSS_ID": {

                break;
            }
            default: {
//                nothing to process, set to null
                patientIdentifierType = null;
                break;
            }
        }
        return patientIdentifierType;
    }

}