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
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderGroup;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.ILPerson;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.MOTHER_NAME;
import org.openmrs.module.kenyaemrIL.il.NEXT_OF_KIN;
import org.openmrs.module.kenyaemrIL.il.PATIENT_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.AppointmentMessage;
import org.openmrs.module.kenyaemrIL.il.appointment.PLACER_APPOINTMENT_NUMBER;
import org.openmrs.module.kenyaemrIL.il.observation.ObservationMessage;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;
import org.openmrs.module.kenyaemrIL.il.pharmacy.DispenseMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.OrderMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_DISPENSE;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_ENCODED_ORDER;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PLACER_ORDER_NUMBER;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.il.viralload.ViralLoadMessage;
import org.openmrs.module.kenyaemrIL.kenyaemrUtils.Utils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            kenyaEMRILMessage.setHl7_type("ADT^A04");
            kenyaEMRILMessage.setSource("KENYAEMR");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessage_type(ILMessageType.OUTBOUND.getValue());
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
        System.out.println("What is it that was returned: " + kenyaEMRILMessage);
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
    public List<KenyaEMRILMessage> getKenyaEMRILStatus(String status) {
        return this.dao.getKenyaEMRILStatus(status);
    }

    @Override
    public boolean processCreatePatientRequest(ILMessage ilMessage, String messsageUUID) {
        boolean successful = false;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
        try {
            Patient ilPerson = wrapIlPerson(ilMessage);
            PatientIdentifier uniquePatientNumber = ilPerson.getPatientIdentifier("Unique Patient Number");
            Pattern p = Pattern.compile("^[0-9]{10,11}$");
            String cccNumber = uniquePatientNumber.getIdentifier();
            if (cccNumber == null) {
                kenyaEMRILMessage.setStatus("Missing CCC Number");
                successful = false;
            } else {
                Matcher m = p.matcher(cccNumber);
                if (m.find()) {
                    // Check to see a patient with similar upn number exists
                    List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
                    if (patients.size() < 1) {
                        //Register patient
                        Patient patient = patientService.savePatient(ilPerson);
                        if (patient != null) {
                            //TODO: Patient Enrollment - Suspend entill further discussions - this is critical as some systems like ADT hard code entry point to NEW distorting report
                            kenyaEMRILMessage.setStatus("Success");
                            successful = true;
                        }
                    } else {
                        log.error("Cannot register: Patient with similar UPN exists:");
                        kenyaEMRILMessage.setStatus("Duplicate CCC Number");
                        successful = false;
                    }
                } else {
                    log.error("Cannot register: CCC number format does not match:");
                    kenyaEMRILMessage.setStatus("CCC Number in wrong format");
                    successful = false;
                }
            }

        } catch (Exception e) {
            log.error("Cannot register reason :" + e.getMessage());
            kenyaEMRILMessage.setStatus("Unknown Error");
            successful = false;
        }
        return successful;
    }

    @Override
    public boolean processUpdatePatientRequest(ILMessage ilMessage, String messsageUUID) {
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
//        Patient patient = null;
//        String cccNumber = null;
//        boolean success = false;
//        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
////        1. Fetch the person to update using the CCC number
//        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
//            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
//                cccNumber = internalPatientId.getId().replaceAll("\\D", "");
//                break;
//            }
//        }
//        if (cccNumber == null) {
////           patient without CCC Number - discard
//            kenyaEMRILMessage.setStatus("Missing CCC Number");
//            return false;
//            //return processCreatePatientRequest(ilMessage);
//        } else {
////            fetch the patient
//            List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
//            if (patients.size() > 0) {
//                patient = patients.get(0);
//                patient = updatePatientDetails(patient, wrapIlPerson(ilMessage));
//            }
//
//            Patient cPatient = patientService.savePatient(patient);
//            if (cPatient != null) {
//                kenyaEMRILMessage.setStatus("Success");
//                success = true;
//
//            } else {
//                kenyaEMRILMessage.setStatus("Unknown Error");
//                success = false;
//            }
//        }
        kenyaEMRILMessage.setStatus("Success");
        return true;
    }

    private Patient updatePatientDetails(Patient oldPatientToUpdate, Patient newPatientDetails) {
        //Update patient identification - names and identifiers
        PersonAddress nPersonAddress = newPatientDetails.getPersonAddress();
        String postaladdress = "";
        String cOUNTY = nPersonAddress.getCountry();
        String sUBCOUNTY = nPersonAddress.getStateProvince();
        String vILLAGE = nPersonAddress.getCityVillage();
        String nEAREST_LANDMARK = nPersonAddress.getAddress2();
        String wARD = nPersonAddress.getAddress4();

        Set<PersonAddress> patientAddress = oldPatientToUpdate.getAddresses();
        if (patientAddress.size() > 0) {
            for (PersonAddress address : patientAddress) {
                if (cOUNTY != null) {
                    address.setCountry(cOUNTY);
                }
                if (sUBCOUNTY != null) {
                    address.setStateProvince(sUBCOUNTY);
                }
                if (wARD != null) {
                    address.setAddress4(wARD);
                }
                if (nEAREST_LANDMARK != null) {
                    address.setAddress2(nEAREST_LANDMARK);
                }
                if (vILLAGE != null) {
                    address.setAddress2(vILLAGE);
                }
                if (postaladdress != null) {
                    address.setAddress1(postaladdress);
                }
                oldPatientToUpdate.addAddress(address);
            }
        } else {
            PersonAddress pa = new PersonAddress();
            if (cOUNTY != null) {
                pa.setCountry(cOUNTY);
            }
            if (sUBCOUNTY != null) {
                pa.setStateProvince(sUBCOUNTY);
            }
            if (wARD != null) {
                pa.setAddress4(wARD);
            }
            if (nEAREST_LANDMARK != null) {
                pa.setAddress2(nEAREST_LANDMARK);
            }
            if (vILLAGE != null) {
                pa.setAddress2(vILLAGE);
            }
            if (postaladdress != null) {
                pa.setAddress1(postaladdress);
            }
            oldPatientToUpdate.addAddress(pa);
        }


        //Update person name
        PersonName nPersonName = newPatientDetails.getPersonName();
        PersonName oPersonName = oldPatientToUpdate.getPersonName();

        if (nPersonName != null) {

            if (nPersonName.getGivenName() != null) {
                oPersonName.setGivenName(nPersonName.getGivenName());
            }
            if (nPersonName.getMiddleName() != null) {
                oPersonName.setMiddleName(nPersonName.getMiddleName());
            }
            if (nPersonName.getFamilyName() != null) {
                oPersonName.setFamilyName(nPersonName.getFamilyName());
            }
        }
        //Update identifiers
        Set<PatientIdentifier> oldIds = oldPatientToUpdate.getIdentifiers();
        for (PatientIdentifier patientIdentifier : newPatientDetails.getIdentifiers()) {
            PatientIdentifierType identifierType = patientIdentifier.getIdentifierType();
            PatientIdentifier fetchedId = oldPatientToUpdate.getPatientIdentifier(identifierType);
            if (fetchedId != null) {
                fetchedId.setIdentifier(patientIdentifier.getIdentifier());
                fetchedId.setPreferred(patientIdentifier.getPreferred());
            } else {
                fetchedId = new PatientIdentifier();
                fetchedId.setPreferred(patientIdentifier.getPreferred());
                fetchedId.setIdentifier(patientIdentifier.getIdentifier());
                fetchedId.setIdentifierType(identifierType);
            }
            oldIds.add(fetchedId);
        }
        oldPatientToUpdate.setIdentifiers(oldIds);
//Update person attributes

        Set<PersonAttribute> personAttributes = newPatientDetails.getAttributes();
        for (PersonAttribute personAttribute : personAttributes) {
            if (personAttribute.getValue() != null) {
                PersonAttributeType attributeType = personAttribute.getAttributeType();
                PersonAttribute attribute = oldPatientToUpdate.getAttribute(attributeType);
                if (attribute != null) {
                    attribute.setValue(personAttribute.getValue());
                } else {
                    attribute = new PersonAttribute();
                    attribute.setValue(personAttribute.getValue());
                    attribute.setAttributeType(attributeType);
                }
                oldPatientToUpdate.addAttribute(attribute);
            }
        }

        //Update gender
        oldPatientToUpdate.setGender(newPatientDetails.getGender() != null ? newPatientDetails.getGender() : oldPatientToUpdate.getGender());
        // Update birthdate
        oldPatientToUpdate.setBirthdate(newPatientDetails.getBirthdate() != null ? newPatientDetails.getBirthdate() : oldPatientToUpdate.getBirthdate());
        // Update birthdate Estimated
        oldPatientToUpdate.setBirthdateEstimated(newPatientDetails.getBirthdateEstimated() != null ? newPatientDetails.getBirthdateEstimated() : oldPatientToUpdate.getBirthdateEstimated());
        // Update if dead
        oldPatientToUpdate.setDead(newPatientDetails.getDead() != null ? newPatientDetails.getDead() : oldPatientToUpdate.getDead());
        // Update if death date
        oldPatientToUpdate.setDeathDate(newPatientDetails.getDeathDate() != null ? newPatientDetails.getDeathDate() : oldPatientToUpdate.getDeathDate());
// Update patient attributes
        patientService.savePatient(oldPatientToUpdate);
        return oldPatientToUpdate;
    }

    @Override
    public boolean processPharmacyOrder(ILMessage ilMessage, String messsageUUID) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean processPharmacyDispense(ILMessage ilMessage, String messsageUUID) {
        boolean success = false;
        String cccNumber = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
//        1. Fetch the person to update using the CCC number
        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                cccNumber = internalPatientId.getId().replaceAll("\\D", "");
                ;
                break;
            }
        }
        if (cccNumber == null) {
            // no patient with the given ccc number, proceed to create a new patient with the received details
            //TODO:this is wrong we should discard this message  so do nothing
            kenyaEMRILMessage.setStatus("Missing CCC Number");
            success = false;
            log.info("Pharmacy Dispense message without CCC Number discarded " + new Date());
        } else {
            //            fetch the patient
            List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
            Patient patient;
            if (patients.size() > 0) {
                patient = patients.get(0);
                //Save the dispense

                 DispenseMessage dispenceMessage = ilMessage.extractPharmacyDispenseMessage();
                 PHARMACY_DISPENSE[] dispenseInformation = dispenceMessage.getDispense_information();
                 PLACER_ORDER_NUMBER placer_order_number = dispenceMessage.getCommon_Order_Details().getPlacer_order_number();

                Encounter appEncounter;
                OrderService orderService = Context.getOrderService();
                EncounterService encounterService = Context.getEncounterService();
                EncounterType encounterTypeDrugOrder = Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3");   //  Drug Order encounter
                //Fetch all encounters
                List<EncounterType> encounterTypes = new ArrayList<>();
                encounterTypes.add(encounterTypeDrugOrder);
                ArrayList<Order> orderList=new ArrayList<Order>();

                ProviderService providerService = Context.getProviderService();
                Provider provider = providerService.getProvider(1);        // Added default provider admin

                for (PHARMACY_DISPENSE dispenseInfo : dispenseInformation) {

                    String dispenseNotes = dispenseInfo.getDispensing_notes();
                    String frequency = dispenseInfo.getFrequency();
                    String prescription_number = dispenseInfo.getPrescription_number();
                    String quantityDispensed = dispenseInfo.getQuantity_dispensed();
                    String dosage = dispenseInfo.getDosage();
                    String codingSystem = dispenseInfo.getCoding_system();
                    String strength = dispenseInfo.getStrength();
                    String duration = dispenseInfo.getDuration();
                    String actualDrugs = dispenseInfo.getActual_drugs();
                    String drugName = dispenseInfo.getDrug_name();
                    String dispenseMsgDatetime = dispenceMessage.getMessage_header().getMessage_datetime();
                    Date ilMsgDate = null;
                    //Validate
                    if (drugName != null) {
                        try {
                            ilMsgDate = formatter.parse(dispenseMsgDatetime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                            Encounter encounter = new Encounter();
                            EncounterType encounterType=encounterService.getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3");
                            encounter.setEncounterType(encounterType);
                            encounter.setPatient(patient);
                            encounter.setEncounterDatetime(new Date());
                            encounter.setDateCreated(new Date());
                            encounterService.saveEncounter(encounter);
                            DrugOrder orderToDiscontinue = null;

                        DrugOrder drugOrder = (DrugOrder) orderService.getOrder(Integer.parseInt(prescription_number));

                        // check if order is single or group
                        if (drugOrder.getOrderGroup() != null) { // this is a group
                            // get order group
                            OrderGroup og = Context.getOrderService().getOrderGroup(drugOrder.getOrderGroup().getOrderGroupId());
                            // get group members
                            List<Order> groupMembers = og.getOrders();
                            for (int i = 0; i < groupMembers.size(); i++) {

                                Order order = groupMembers.get(i);
                                drugOrder = (DrugOrder) orderService.getOrder(Integer.valueOf(order.getId()));
                                try {
                                    orderToDiscontinue = (DrugOrder) orderService.discontinueOrder(drugOrder, "order fulfilled", null, provider, encounter);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                orderList.add(orderToDiscontinue);
                            }
                        } else { // it is single

                             try {
                                 orderService.discontinueOrder(drugOrder, "order fulfilled", null, provider, encounter);
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                        }
                            kenyaEMRILMessage.setStatus("Success");
                            success = true;
                        }
                }

            } else {
                log.error("Cannot save drug dispense: CCC number format does not match:");
                kenyaEMRILMessage.setStatus("Could not find a match");
                success = false;
            }

        }
        return success;
    }

    @Override
    public boolean processAppointmentSchedule(ILMessage ilMessage, String messsageUUID) {
        boolean success = false;
        String cccNumber = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
//        1. Fetch the person to update using the CCC number
        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                cccNumber = internalPatientId.getId().replaceAll("\\D", "");
                ;
                break;
            }
        }
        if (cccNumber == null) {
            // no patient with the given ccc number, proceed to create a new patient with the received details
            //TODO:this is wrong we should discard this message  so do nothing
            kenyaEMRILMessage.setStatus("Missing CCC Number");
            success = false;
            log.info("Appointment message without CCC Number discarded " + new Date());
        } else {
            //            fetch the patient
            List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
            Patient patient;
            if (patients.size() > 0) {
                patient = patients.get(0);
                //Save the appointment
                AppointmentMessage appointmentMessage = ilMessage.extractAppointmentMessage();
                APPOINTMENT_INFORMATION[] appointmentInformation = appointmentMessage.getAppointment_information();
                Encounter appEncounter;
                //  Encounter lastFollowUpEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("e87aa2ad-6886-422e-9dfd-064e3bfe3aad"));   //last greencard followup form
                EncounterType encounterTypeGreencard = Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e");   //  HIV consultation/followup encounter
                //Fetch all encounters
                List<EncounterType> encounterTypes = new ArrayList<>();
                encounterTypes.add(encounterTypeGreencard);

                Integer patientTCAConcept = 5096;
                Integer patientTCAReasonConcept = 160288;
                for (APPOINTMENT_INFORMATION appInfo : appointmentInformation) {
                    PLACER_APPOINTMENT_NUMBER placerAppointmentNumber = appInfo.getPlacer_appointment_number();
                    String appointmentStatus = appInfo.getAppointment_status();
                    String appointmentReason = appInfo.getAppointment_reason();
                    String action_code = appInfo.getAction_code();
                    String appointment_note = appInfo.getAppointment_note();
                    String appointmentDate = appInfo.getAppointment_date();
                    String appointmentMsgDatetime = appointmentMessage.getMessage_header().getMessage_datetime();
                    String placerAppointmentNumberNumber = placerAppointmentNumber.getNumber();
                    String entity = placerAppointmentNumber.getEntity();
                    Date ilMsgDate = null;
                    //Validate
                    if (appointmentDate != null) {
                        try {
                            ilMsgDate = formatter.parse(appointmentMsgDatetime);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        List<Encounter> followUpEncounters = Context.getEncounterService().getEncounters(patient, null, ilMsgDate, null, null, encounterTypes, null, null, null, false);
                        if (followUpEncounters.size() > 0) {
                            appEncounter = followUpEncounters.get(0);
                            Obs o = new Obs();
                            o.setComment(placerAppointmentNumberNumber + "" + appointment_note + "" + appointmentStatus + "" + appointmentReason);
                            o.setConcept(Context.getConceptService().getConcept(patientTCAConcept));
                            try {
                                o.setValueDatetime(formatter.parse(appointmentDate));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            o.setObsDatetime(new Date());
                            o.setDateCreated(new Date());
                            o.setCreator(Context.getUserService().getUser(1));
                            o.setLocation(appEncounter.getLocation());
                            o.setObsDatetime(appEncounter.getEncounterDatetime());
                            o.setPerson(patient);
                            appEncounter.addObs(o);
                            Context.getEncounterService().saveEncounter(appEncounter);
                            kenyaEMRILMessage.setStatus("Success");
                            success = true;

                        } else {
                            //Define encounter
                            Encounter enc = new Encounter();
                            Location location = Utils.getDefaultLocation();
                            enc.setLocation(location);
                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));     //  HIV consultation/followup encounter
                            enc.setEncounterDatetime(new Date());
                            enc.setPatient(patient);
                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
                            enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));   //TODO: HIV greencard form  to be substituted with Fast track form

                            Obs o = new Obs();
                            o.setComment(placerAppointmentNumberNumber + "" + appointment_note + "" + appointmentStatus + "" + appointmentReason);
                            o.setConcept(Context.getConceptService().getConcept(patientTCAConcept));
                            try {
                                o.setValueDatetime(formatter.parse(appointmentDate));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            o.setDateCreated(new Date());
                            o.setCreator(Context.getUserService().getUser(1));
                            o.setLocation(enc.getLocation());
                            o.setObsDatetime(enc.getEncounterDatetime());
                            o.setPerson(patient);

                            enc.addObs(o);
                            Context.getEncounterService().saveEncounter(enc);
                            kenyaEMRILMessage.setStatus("Success");
                            success = true;
                        }

                    }
                }

            } else {
                log.error("Cannot schedule appnmt: CCC number format does not match:");
                kenyaEMRILMessage.setStatus("Could not find a match");
                success = false;
            }

        }
        return success;
    }

    @Override
    public boolean processLabOrder(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    //TODO: Lab orders not yet implemented
    @Override
    public boolean processObservationResult(ILMessage ilMessage, String messsageUUID) {
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
       // boolean success = false;
//        String cccNumber = null;
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
////        1. Fetch the person to update using the CCC number
//        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
//            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
//                cccNumber = internalPatientId.getId().replaceAll("\\D", "");
//                ;
//                break;
//            }
//        }
//        if (cccNumber == null) {
////            no patient with the given ccc number, proceed to create a new patient with the received details
//            success = false;
//        } else {
////            fetch the patient
//            List<Patient> patients = Context.getPatientService().getPatients(null, cccNumber, allPatientIdentifierTypes, true);
//            Patient patient;
//            if (patients.size() > 0) {
//                patient = patients.get(0);
//
//                //Save the observation
//                ObservationMessage observationMessage = ilMessage.extractORUMessage();
//                OBSERVATION_RESULT[] observationInformation = observationMessage.getObservation_result();
//
//                //Obs for consideration   - not complete list
//                Integer HeightConcept = 5090;
//                Integer WeightConcept = 5089;
//                Integer HivDiagnosisDateConcept = 160554;
//                Integer HivCareInitiationDateConcept = 160555;
//                Integer ARTInitiationDateConcept = 159599;
//                Integer IspregnantConcept = 5272;
//                Integer EDDConcept = 5596;
//                Integer DateOfDeliveryConcept = 5599;
//                Integer ARVConcept = 1085;    //TODO: Implement incoming regimen
//                Integer SmokerConcept = 155600;
//                Integer AlcoholUseConcept = 159449;
//                Integer CTXStartConcept = 162229;
//                Integer TestsOrderedConcept = 1271;
//                Integer CD4Concept = 5497;
//                Integer CD4PercentConcept = 730;
//                Integer TBdiagnosisDateConcept = 1662;
//                Integer TBTreatmentStartDateConcept = 1113;
//                Integer TBTreatmentCompleteDateConcept = 164384;
//                Integer WhoStageConcept = 5356;
//                Integer PedWhoStage1Concept = 1220;
//                Integer PedWhoStage2Concept = 1221;
//                Integer PedWhoStage3Concept = 1222;
//                Integer PedWhoStage4Concept = 1223;
//                Integer AdultWhoStage1Concept = 1204;
//                Integer AdultWhoStage2Concept = 1205;
//                Integer AdultWhoStage3Concept = 1206;
//                Integer AdultWhoStage4Concept = 1207;
//                Integer YesConcept = 1065;
//                Integer NoConcept = 1066;
//
//                for (OBSERVATION_RESULT obsInfo : observationInformation) {
//                    String observationSetId = obsInfo.getSet_id();
//                    String observationCodingSystem = obsInfo.getCoding_system();
//                    String observationValueType = obsInfo.getValue_type();
//                    String observationUnits = obsInfo.getUnits();
//                    String observationDateTime = obsInfo.getObservation_datetime();
//                    String observationResultStatus = obsInfo.getObservation_result_status();
//                    String observationAbnormalFlags = obsInfo.getAbnormal_flags();
//                    String observationIdentifier = obsInfo.getObservation_identifier();
//
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("START_HEIGHT")) {             //Start Height
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("d1059fb9-a079-4feb-a749-eedd709ae542"));   // enc = Triage
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("37f6bd8d-586a-4169-95fa-5781f987fe62"));  //form= Triage
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setValueNumeric(Double.parseDouble(obsInfo.getObservation_value()));
//                            o.setConcept(Context.getConceptService().getConcept(HeightConcept));
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("START_WEIGHT")) {             //Start Weight
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("d1059fb9-a079-4feb-a749-eedd709ae542"));    // enc = Triage
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("37f6bd8d-586a-4169-95fa-5781f987fe62"));           //form= Triage
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setValueNumeric(Double.parseDouble(obsInfo.getObservation_value()));
//                            o.setConcept(Context.getConceptService().getConcept(WeightConcept));
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("HIV_DIAGNOSIS")) {             //HIV Diagnosis date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));     // enc = HIV enrollment
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));    //form= HIV enrollment
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(HivDiagnosisDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("HIV_CARE_INITIATION")) {             // hiv care initiation date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));     // enc = HIV enrollment
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));    //form= HIV enrollment
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(HivCareInitiationDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("COTRIMOXAZOLE_START")) {             // CTX start date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(CTXStartConcept));
//                            o.setValueCoded(Context.getConceptService().getConcept(YesConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("TB_DIAGNOSIS_DATE")) {             // TB diagnosis  date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(TBdiagnosisDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("TB_TREATMENT_START_DATE")) {             // TB treatment start  date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(TBTreatmentStartDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("TB_TREATMENT_COMPLETE_DATE")) {             // TB treatment complete  date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(TBTreatmentCompleteDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("ART_START")) {             // art start date
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"));     // enc = HIV enrollment
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("e4b506c1-7379-42b6-a374-284469cba8da"));    //form= HIV enrollment
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(ARTInitiationDateConcept));
//                            try {
//                                o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("CD4_COUNT")) {             // CD4 Count
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));     // enc = Lab results
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("7e603909-9ed5-4d0c-a688-26ecb05d8b6e"));    //form= Lab results
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(CD4Concept));
//                            o.setValueNumeric(Double.parseDouble(obsInfo.getObservation_value()));
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//                    if (obsInfo.getObservation_identifier().equalsIgnoreCase("CD4_PERCENT")) {             // CD4 Percent
//                        if (obsInfo.getObservation_value() != null) {
//                            //Define encounter
//                            Encounter enc = new Encounter();
//                            Location location = Utils.getDefaultLocation();
//                            enc.setLocation(location);
//                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));     // enc = Lab results
//                            enc.setEncounterDatetime(new Date());
//                            enc.setPatient(patient);
//                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                            enc.setForm(Context.getFormService().getFormByUuid("7e603909-9ed5-4d0c-a688-26ecb05d8b6e"));    //form= Lab results
//                            //Set observations
//                            Obs o = new Obs();
//                            o.setConcept(Context.getConceptService().getConcept(CD4Concept));
//                            o.setValueNumeric(Double.parseDouble(obsInfo.getObservation_value()));
//                            o.setDateCreated(new Date());
//                            o.setCreator(Context.getUserService().getUser(1));
//                            o.setLocation(enc.getLocation());
//                            o.setObsDatetime(enc.getEncounterDatetime());
//                            o.setPerson(patient);
//
//                            enc.addObs(o);
//                            Context.getEncounterService().saveEncounter(enc);
//                            success = true;
//                        }
//                    }
//               /*    Commented WHO stage, pregnancy and EDD
//                         if (obsInfo.getObservation_identifier().equalsIgnoreCase( "WHO_STAGE")) {             // WHO stage
//                           if (obsInfo.getObservation_value() != null ) {
//                               String whoStage = obsInfo.getObservation_value();
//                               if(whoStage.equals("1") || whoStage.equals("2") || whoStage.equals("3") || whoStage.equals("4")) {
//                                   //Define encounter
//                                   Encounter enc = new Encounter();
//                                   Location location = Utils.getDefaultLocation();
//                                   enc.setLocation(location);
//                                   enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                                   enc.setEncounterDatetime(new Date());
//                                   enc.setPatient(patient);
//                                   enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                                   enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                                   //Set observations
//                                   Obs o = new Obs();
//                                   o.setConcept(Context.getConceptService().getConcept(WhoStageConcept));
//                                   Date dob = patient.getBirthdate();
//                                   Integer age = calculateAge(dob);
//
//                                   o.setValueCoded(Context.getConceptService().getConcept(PedWhoStage1Concept));
//                                   if (age >= 14) {
//                                       if (obsInfo.getObservation_value() == "1") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(AdultWhoStage1Concept));
//                                       } else if (obsInfo.getObservation_value() == "2") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(AdultWhoStage2Concept));
//                                       } else if (obsInfo.getObservation_value() == "3") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(AdultWhoStage3Concept));
//                                       } else if (obsInfo.getObservation_value() == "4") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(AdultWhoStage4Concept));
//                                       }
//                                   } else {
//                                       if (obsInfo.getObservation_value() == "1") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(PedWhoStage1Concept));
//                                       } else if (obsInfo.getObservation_value() == "2") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(PedWhoStage2Concept));
//                                       } else if (obsInfo.getObservation_value() == "3") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(PedWhoStage3Concept));
//                                       } else if (obsInfo.getObservation_value() == "4") {
//                                           o.setValueCoded(Context.getConceptService().getConcept(PedWhoStage4Concept));
//                                       }
//                                   }
//                                   o.setDateCreated(new Date());
//                                   o.setCreator(Context.getUserService().getUser(1));
//                                   o.setLocation(enc.getLocation());
//                                   o.setObsDatetime(enc.getEncounterDatetime());
//                                   o.setPerson(patient);
//
//                                   Concept whoConcept = Context.getConceptService().getConcept(AdultWhoStage1Concept);
//
//                                   System.out.println("WHO stage ==>"+o.getValueCoded().getConceptId());
//                                   System.out.println("WHO concept ==>"+whoConcept.getConceptId());
//
//                                   enc.addObs(o);
//                                   Context.getEncounterService().saveEncounter(enc);
//                                   success = true;
//                               }
//                           }
//                       }
//
//                       if (obsInfo.getObservation_identifier().equalsIgnoreCase("IS_PREGNANT")) {             // Is pregnant
//                           if (obsInfo.getObservation_value() != null && obsInfo.getObservation_value() == "Y") {
//                               //Define encounter
//                               Encounter enc = new Encounter();
//                               Location location = Utils.getDefaultLocation();
//                               enc.setLocation(location);
//                               enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                               enc.setEncounterDatetime(new Date ());
//                               enc.setPatient(patient);
//                               enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                               enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                               //Set observations
//                               Obs o = new Obs();
//                               o.setConcept(Context.getConceptService().getConcept(IspregnantConcept));
//                               o.setValueCoded(Context.getConceptService().getConcept(YesConcept));
//                               o.setDateCreated(new Date());
//                               o.setCreator(Context.getUserService().getUser(1));
//                               o.setLocation(enc.getLocation());
//                               o.setObsDatetime(enc.getEncounterDatetime());
//                               o.setPerson(patient);
//
//                               enc.addObs(o);
//                               Context.getEncounterService().saveEncounter(enc);
//                               success = true;
//                           }
//                       }
//                       if (obsInfo.getObservation_identifier().equalsIgnoreCase( "PREGNANT_EDD")) {             // EDD
//                           if (obsInfo.getObservation_value() != null) {
//                               //Define encounter
//                               Encounter enc = new Encounter();
//                               Location location = Utils.getDefaultLocation();
//                               enc.setLocation(location);
//                               enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e"));    // enc = HIV Greencard
//                               enc.setEncounterDatetime(new Date ());
//                               enc.setPatient(patient);
//                               enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
//                               enc.setForm(Context.getFormService().getFormByUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259"));           //form= HIV Greencard
//                               //Set observations
//                               Obs o = new Obs();
//                               o.setConcept(Context.getConceptService().getConcept(EDDConcept));
//                               try {
//                                   o.setValueDatetime(formatter.parse(obsInfo.getObservation_value()));
//                               } catch (ParseException e) {
//                                   e.printStackTrace();
//                               }
//                               o.setDateCreated(new Date());
//                               o.setCreator(Context.getUserService().getUser(1));
//                               o.setLocation(enc.getLocation());
//                               o.setObsDatetime(enc.getEncounterDatetime());
//                               o.setPerson(patient);
//
//                               enc.addObs(o);
//                               Context.getEncounterService().saveEncounter(enc);
//                               success = true;
//                           }
//                       }
//           */
//                }
//
//            } else {
//                kenyaEMRILMessage.setStatus("Could not find a match");
//                success = false;
//            }
//
//
//        }

        kenyaEMRILMessage.setStatus("Success ORU");
        return true;
    }

    @Override
    public boolean processViralLoad(ILMessage ilMessage, String messsageUUID) {
        boolean success = false;
        String cccNumber = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
        KenyaEMRILMessage kenyaEMRILMessage = getKenyaEMRILMessageByUuid(messsageUUID);
        ConceptService conceptService = Context.getConceptService();
        PatientService patientService = Context.getPatientService();
        EncounterService encounterService = Context.getEncounterService();
//        1. Fetch the person to update using the CCC number
        for (INTERNAL_PATIENT_ID internalPatientId : ilMessage.getPatient_identification().getInternal_patient_id()) {
            if (internalPatientId.getIdentifier_type().equalsIgnoreCase("CCC_NUMBER")) {
                cccNumber = internalPatientId.getId().replaceAll("\\D", "");
                break;
            }
        }
        if (cccNumber == null) {
//            no patient with the given ccc number, proceed to create a new patient with the received details
            kenyaEMRILMessage.setStatus("Missing CCC Number");
            success = false;
        } else {

//            fetch the patient
            List<Patient> patients = patientService.getPatients(null, cccNumber, allPatientIdentifierTypes, true);
            Patient patient;
            if (patients.size() > 0) {
                patient = patients.get(0);

                //Save the viral load results
                ViralLoadMessage viralLoadMessage = ilMessage.extractViralLoadMessage();
                VIRAL_LOAD_RESULT[] viralLoadResult = viralLoadMessage.getViral_load_result();
                //Encounter lastLabResultsEncounter = ILUtils.lastEncounter(patient, Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));   //last lab results encounter type
                EncounterType encounterTypeLabResults = encounterService.getEncounterTypeByUuid("e1406e88-e9a9-11e8-9f32-f2801f1b9fd1");
                //Fetch all lab encounters
                List<EncounterType> encounterTypes = new ArrayList<>();
                encounterTypes.add(encounterTypeLabResults);

                Integer vLConcept = 856;
                Integer LDLQuestionConcept = 1305;
                Integer LDLAnswerConcept = 1302;
                Integer ARVConcept = 1085;

                for (VIRAL_LOAD_RESULT labInfo : viralLoadResult) {
                    String dateSampleCollected = labInfo.getDate_sample_collected();
                    String dateSampleTested = labInfo.getDate_sample_tested();
                    String vlResult = labInfo.getVl_result();
                    String sampleType = labInfo.getSample_type();
                    String sampleRejection = labInfo.getSample_rejection();
                    String justification = labInfo.getJustification();
                    String regimen = labInfo.getRegimen();
                    String labTested = labInfo.getLab_tested_in();
                    String vlResultMsgDatetime = viralLoadMessage.getMessage_header().getMessage_datetime();
                    boolean isLDL = false;
                    Date ilSampleCollectedDate = null;
                    Integer patient_id;
                    //Validate
                    if (vlResult != null) {
                        try {
                            // ilMsgDate = dateFormatter.parse(vlResultMsgDatetime);
                            ilSampleCollectedDate = dateFormatter.parse(dateSampleCollected);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //Select lab encounters on the specific sample collected date
                        patient_id = patient.getPatientId();
                        StringBuilder q = new StringBuilder();
                        q.append("select e.encounter_id, coalesce(o.value_coded, o.value_numeric) as hasResult ");
                        q.append("from encounter e inner join " +
                                "( " +
                                " select encounter_type_id, uuid, name from encounter_type " +
                                " where uuid ='17a381d1-7e29-406a-b782-aa903b963c28' " +
                                " ) et on et.encounter_type_id=e.encounter_type " +
                                " left join obs o on o.encounter_id=e.encounter_id and o.voided=0 and o.concept_id in (856, 1305) ");
                        q.append("where date(e.encounter_datetime) = date('" + ilSampleCollectedDate + "')  and patient_id = " + patient_id.intValue() );
                        q.append(" and e.voided = 0 group by e.patient_id  ");

                        List<Encounter> encounters = new ArrayList<>();
                        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
                        System.out.println("Sample datetime ==> "+ilSampleCollectedDate);
                        if (queryData.size() > 0) {
                            System.out.println("Duplicate in query Data ==> "+queryData.size());
                            for (List<Object> row : queryData) {
                                Integer encounterId = (Integer) row.get(0);
                                Integer vlAnswerConcept = (Integer) row.get(1);
                                if (vlAnswerConcept != null) {
                                    kenyaEMRILMessage.setStatus("Duplicate vl exists");
                                    System.out.println("Duplicate vl exists ");
                                    success = false;
                                    continue;
                                } else {

                                    Encounter labEncounter = encounterService.getEncounter(encounterId);
                                    Obs o = new Obs();
                                    o.setComment(dateSampleCollected + "" + dateSampleTested + "" + sampleType + "" + sampleRejection + "" + justification + "" + regimen + "" + labTested);
                                    isLDL = vlResult.toLowerCase().contains("ldl");
                                    if (!isLDL && !vlResult.trim().equals("")) {
                                        o.setConcept(conceptService.getConcept(vLConcept));       //add viral load concept
                                        String vlStr = vlResult.replaceAll("\\D", "");
                                        double viralResultNumeric = Double.parseDouble(vlStr.equals("") ? "0" : vlStr);
                                        o.setValueNumeric(viralResultNumeric);
                                    } else {
                                        o.setConcept(Context.getConceptService().getConcept(LDLQuestionConcept));       //add ldl concept
                                        o.setValueCoded(Context.getConceptService().getConcept(LDLAnswerConcept));
                                    }
                                    o.setDateCreated(new Date());
                                    o.setCreator(Context.getUserService().getUser(1));
                                    o.setLocation(labEncounter.getLocation());
                                    try {
                                        o.setObsDatetime(dateFormatter.parse(labInfo.getDate_sample_collected()));
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    o.setPerson(patient);
                                    labEncounter.addObs(o);
                                    encounterService.saveEncounter(labEncounter);
                                    kenyaEMRILMessage.setStatus("Success");
                                    success = true;

                                }
                            }

                        } else {

                            //Define new encountetr
                            Encounter enc = new Encounter();
                            Location location = Utils.getDefaultLocation();
                            enc.setLocation(location);
                            enc.setEncounterType(Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
                            enc.setEncounterDatetime(new Date());
                            enc.setPatient(patient);
                            enc.addProvider(Context.getEncounterService().getEncounterRole(1), Context.getProviderService().getProvider(1));
                            enc.setForm(Context.getFormService().getFormByUuid("7e603909-9ed5-4d0c-a688-26ecb05d8b6e"));   //lab results form

                            Obs o = new Obs();
                            o.setComment(dateSampleCollected + "" + dateSampleTested + "" + sampleType + "" + sampleRejection + "" + justification + "" + regimen + "" + labTested);

                            isLDL = vlResult.toLowerCase().contains("ldl");
                            if (!isLDL && !vlResult.trim().equals("")) {
                                o.setConcept(Context.getConceptService().getConcept(vLConcept));       //add viral load concept
                                String vlStr = vlResult.replaceAll("\\D", "");
                                System.out.println("vlStr: " + vlStr);
                                double viralResultNumeric = Double.parseDouble(vlStr.equals("") ? "0" : vlStr);
                                o.setValueNumeric(viralResultNumeric);
                            } else {
                                o.setConcept(Context.getConceptService().getConcept(LDLQuestionConcept));       //add ldl concept
                                o.setValueCoded(Context.getConceptService().getConcept(LDLAnswerConcept));
                            }

                            o.setDateCreated(new Date());
                            o.setCreator(Context.getUserService().getUser(1));
                            o.setLocation(enc.getLocation());
                            try {
                                o.setObsDatetime(dateFormatter.parse(labInfo.getDate_sample_collected()));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            o.setPerson(patient);

                            enc.addObs(o);
                            Context.getEncounterService().saveEncounter(enc);
                            kenyaEMRILMessage.setStatus("Success");
                            success = true;
                        }
                    }
                }
            } else {
                kenyaEMRILMessage.setStatus("Could not find a match");
                success = false;
            }

        }

        return success;
    }

    @Override
    public boolean process731Adx(ILMessage ilMessage) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean logAppointmentSchedule(ILMessage ilMessage) {
        System.out.println("Outbox appmnt message");
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("SIU^S12");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            AppointmentMessage appointmentMessage = ilMessage.extractAppointmentMessage();
            String messageString = mapper.writeValueAsString(appointmentMessage);
//            System.out.println("Outbox appmnt message==>"+messageString);
            kenyaEMRILMessage.setHl7_type("SIU^S12");
            kenyaEMRILMessage.setSource("KENYAEMR");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessage_type(ILMessageType.OUTBOUND.getValue());
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
        System.out.println("Outbox vl message");
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("ORU^VL");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            ViralLoadMessage viralLoadMessage = ilMessage.extractViralLoadMessage();
            String messageString = mapper.writeValueAsString(viralLoadMessage);
            kenyaEMRILMessage.setHl7_type("ORU^VL");
            kenyaEMRILMessage.setSource("KENYAEMR");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessage_type(ILMessageType.OUTBOUND.getValue());
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
            kenyaEMRILMessage.setHl7_type("ORU^R01");
            kenyaEMRILMessage.setSource("KENYAEMR");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessage_type(ILMessageType.OUTBOUND.getValue());
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
    public boolean logPharmacyOrders(ILMessage ilMessage) {
        boolean isSuccessful;
        //Message Header
        MESSAGE_HEADER messageHeader = MessageHeaderSingleton.getMessageHeaderInstance("RDE^001");
        ilMessage.setMessage_header(messageHeader);
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        try {
            OrderMessage orderMessage = ilMessage.extractPharmacyOrderMessage();
            String messageString = mapper.writeValueAsString(orderMessage);
            kenyaEMRILMessage.setHl7_type("RDE^001");
            kenyaEMRILMessage.setSource("KENYAEMR");
            kenyaEMRILMessage.setMessage(messageString);
            kenyaEMRILMessage.setDescription("");
            kenyaEMRILMessage.setName("");
            kenyaEMRILMessage.setMessage_type(ILMessageType.OUTBOUND.getValue());
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
            String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
            PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
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
            personAddress.setAddress1("");
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

    String whoAdultConverter(Concept key) {
        Map<Concept, String> whoSategeList = new HashMap<Concept, String>();
        whoSategeList.put(conceptService.getConcept(1204), "1");
        whoSategeList.put(conceptService.getConcept(1205), "2");
        whoSategeList.put(conceptService.getConcept(1206), "3");
        whoSategeList.put(conceptService.getConcept(1207), "4");
        return whoSategeList.get(key);
    }

    String whoPedConverter(Concept key) {
        Map<Concept, String> whoSategeList = new HashMap<Concept, String>();
        whoSategeList.put(conceptService.getConcept(1220), "1");
        whoSategeList.put(conceptService.getConcept(1221), "2");
        whoSategeList.put(conceptService.getConcept(1222), "3");
        whoSategeList.put(conceptService.getConcept(1223), "4");
        return whoSategeList.get(key);
    }

    static String drugNameConverter(String key) {
        Map<Concept, String> drugNameList = new HashMap<Concept, String>();
        drugNameList.put(conceptService.getConcept(1652), "af1a");
        drugNameList.put(conceptService.getConcept(1652), "cf1a");
        drugNameList.put(conceptService.getConcept(164505), "tle");
        drugNameList.put(conceptService.getConcept(105281), "ctx");
        return drugNameList.get(key);
    }

    public Integer calculateAge(Date date) {
        if (date == null) {
            return null;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        Calendar cal2 = Calendar.getInstance();
        int i = 0;
        while (cal1.before(cal2)) {
            cal1.add(Calendar.YEAR, 1);
            i += 1;
        }
        return i;
    }
}
