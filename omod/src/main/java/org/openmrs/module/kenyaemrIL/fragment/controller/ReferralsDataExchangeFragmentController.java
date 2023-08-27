package org.openmrs.module.kenyaemrIL.fragment.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.providers.r4.PatientFhirResourceProvider;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;

/**
 * controller for pivotTableCharts fragment
 */
public class ReferralsDataExchangeFragmentController {
       // Logger
    private static final Logger log = LoggerFactory.getLogger(ReferralsDataExchangeFragmentController.class);
    public static final String OPENMRS_ID = "dfacd928-0370-4315-99d7-6ec1c9f7ae76";
    public static final String NUPI = "f85081e2-b4be-4e48-b3a4-7994b69bb101";
    @Qualifier("fhirR4")
    private FhirContext fhirContext;
    public FhirContext getFhirContext() {
        return fhirContext;
    }
    public IGenericClient getFhirClient() throws Exception {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(ILUtils.getShrServerUrl());
        return fhirClient;
    }

    public void controller(FragmentModel model, @FragmentParam("patient") Patient patient) {

    }
    /**
     * Get community referrals for FHIR server       *
     * @return
     */
    public SimpleObject pullCommunityReferralsFromFhir() {
        System.out.println("Fhir :Start pullCommunityReferralsFromFhir ==>");
        Bundle serviceRequestResourceBundle;
        Bundle patientResourceBundle;
        org.hl7.fhir.r4.model.Resource fhirServiceRequestResource;
        org.hl7.fhir.r4.model.Resource fhirResource;
        org.hl7.fhir.r4.model.Patient fhirPatient = null;
        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = null;
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        serviceRequestResourceBundle = fhirConfig.fetchReferrals();
        //Logging the string value
        System.out.println(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(serviceRequestResourceBundle));
     if (!serviceRequestResourceBundle.getEntry().isEmpty()) {
         for (int i = 0; i < serviceRequestResourceBundle.getEntry().size(); i++) {
             fhirServiceRequestResource = serviceRequestResourceBundle.getEntry().get(0).getResource();
             System.out.println("Fhir : Checking Service request is null ==>");
             if (fhirServiceRequestResource != null) {
                 System.out.println("Fhir : Service request is not null ==>");
                 fhirServiceRequest = (org.hl7.fhir.r4.model.ServiceRequest) fhirServiceRequestResource;
                 // Get UPI,
                 // Persist service request
                 String nupiNumber = fhirServiceRequest.getSubject().getDisplay();
                 System.out.println("NUPI :  ==>"+nupiNumber);
                 // Use UPI to get Patient,
                 patientResourceBundle = fhirConfig.fetchPatientResource(nupiNumber);

                 System.out.println(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(patientResourceBundle));
                 if (patientResourceBundle != null && !patientResourceBundle.getEntry().isEmpty()) {
                     fhirResource = patientResourceBundle.getEntry().get(0).getResource();
                     if (fhirResource.getResourceType().toString().equals("Patient")) {
                         fhirPatient = (org.hl7.fhir.r4.model.Patient) fhirResource;
                     }
                     // Persist the patient,
                     if (fhirPatient != null) {
                         System.out.println("Fhir patient exists:  ==>");
                         PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NUPI);
                         PatientIdentifier fetchedNupi = new PatientIdentifier(nupiNumber, nupiIdType, getDefaultLocation());
                         addPatientObjectToBundle(fhirPatient,fetchedNupi);

                     }


                 System.out.println("Fhir : Looping service request ==>");
                 System.out.println("Fhir service request identifier ==>" + fhirServiceRequest.getSubject().getIdentifier().getValue());
                 //for (Coding c : fhirServiceRequest.getCode().getCoding()) {


//                 if (fhirConfig.vitalConcepts().contains(c.getCode())) {
//                     vitalObs.add(SimpleObject.create(
//                             "display", c.getDisplay(),
//                             "date", ILUtils.getObservationValue(fhirServiceRequest),
//                             "value", new SimpleDateFormat("yyyy-MM-dd").format(fhirServiceRequest.getEffectiveDateTimeType().toCalendar().getTime())));
//                 }
                   }
             }else {
                 System.out.println("Fhir : Service request is null ==>");
             }
         }
     }

                String success = "";



        return SimpleObject.create("success", success );

    }

    public void addPatientObjectToBundle(org.hl7.fhir.r4.model.Patient fhirPatient, PatientIdentifier fetchedNupi) {

        PatientFhirResourceProvider patientResourceProvider = Context.getRegisteredComponent(
                "patientFhirR4ResourceProvider", PatientFhirResourceProvider.class);
        System.out.println("Saving fhir client");
        //Using openmrs service to create and persist person
        Patient patient = new Patient();
        //Add name
        String patientName = null;
        fillPatientName(patientName, fhirPatient, patient);
       //Add birthdate
        patient.setBirthdate(fhirPatient.getBirthDate());
        //Set Gender
        String gender =fhirPatient.getGender().getDisplay();
        System.out.println("Fhir client gender here ==>"+gender);
        if(gender.equalsIgnoreCase("male")){
            gender="M";
        }else if(gender.equalsIgnoreCase("female")){
            gender="F";
        }
        patient.setGender(gender);
        // Add the NUPI Number fetched from the serviceRequest
        patient.addIdentifier(fetchedNupi);
        // Make sure everyone gets an OpenMRS ID
        PatientIdentifierType openmrsIdType = MetadataUtils.existing(PatientIdentifierType.class, OPENMRS_ID);
        PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType);
        boolean errorOccured = false;
        if (openmrsId == null) {
            String generated = Context.getService(IdentifierSourceService.class).generateIdentifier(openmrsIdType, "Registration");
            openmrsId = new PatientIdentifier(generated, openmrsIdType, getDefaultLocation());
            patient.addIdentifier(openmrsId);
            if (!patient.getPatientIdentifier().isPreferred()) {
                openmrsId.setPreferred(true);
            }
        }
        try {
            // Check to see a patient with similar nupi number exists
            PatientIdentifierType nupiIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
            PatientService patientService = Context.getPatientService();
            List<Patient> patients = patientService.getPatients(null, fetchedNupi.getIdentifier().trim(), Arrays.asList(nupiIdentifierType), true);
            if (patients.size() < 1) {
                //Register patient
                Patient savePatient = patientService.savePatient(patient);
                if (savePatient != null) {
                    //Assign  community referral attribute
                    PersonAttribute referralSourceAttribute = new PersonAttribute();
                    PersonAttributeType referralSourceAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("c4281b3c-6c01-4213-bd3c-a52f8f6fe223");
                    if (referralSourceAttributeType != null) {
                        referralSourceAttribute.setAttributeType(referralSourceAttributeType);
                        referralSourceAttribute.setValue("Community");
                        patient.addAttribute(referralSourceAttribute);
                        patientService.savePatient(patient);
                    }
                }
            }else {
                log.error("Cannot register: Patient with similar NUPI exists:");
            }


        } catch (Exception e) {
            e.printStackTrace();
            errorOccured = true;
        }
         //TODO:Use FHIR 2 method to persist patient
        // MethodOutcome results = patientResourceProvider.createPatient(fhirPatient);
        System.out.println("Error occured ==>"+errorOccured);

    }

    public Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    private Patient fillPatientName(String fullName, org.hl7.fhir.r4.model.Patient fhirPatient, Patient patient ) {
        PersonName pn = new PersonName();
        String familyName = "";
        String givenName = "";
        String middleName = "";
        if(!fhirPatient.getName().get(0).isEmpty()) {
            if (fhirPatient.getName().get(0).getFamily() != null) {
                familyName = fhirPatient.getName().get(0).getFamily();
                System.out.println("Fhir patient family name here ==>" + familyName);
            }
            if (fhirPatient.getName().get(0).getGiven() != null) {
              //  String fullGivenName = fhirPatient.getName().get(0).getGiven().toString().replaceAll("[^a-zA-Z0-9]", " ");
                givenName = fhirPatient.getName().get(0).getGiven().get(0).toString();
                middleName = fhirPatient.getName().get(0).getGiven().get(1).toString();
                System.out.println("Fhir patient given name here ==>" + givenName);
                System.out.println("Fhir patient middle name here ==>" + middleName);

            }
            pn.setGivenName(familyName);
            pn.setMiddleName(givenName);
            pn.setFamilyName(middleName);

            System.out.println("Fhir patient full name here ==>" + pn);
            patient.addName(pn);
            return patient;
        }
        return null;
    }
}