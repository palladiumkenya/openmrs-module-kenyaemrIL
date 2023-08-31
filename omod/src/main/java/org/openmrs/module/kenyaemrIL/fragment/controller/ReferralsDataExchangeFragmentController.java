package org.openmrs.module.kenyaemrIL.fragment.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

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
    public SimpleObject pullCommunityReferralsFromFhir() throws Exception {
        String res = "";
        System.out.println("Fhir :Start pullCommunityReferralsFromFhir ==>");
        Bundle serviceRequestResourceBundle;
        Bundle patientResourceBundle;
        org.hl7.fhir.r4.model.Resource fhirServiceRequestResource;
        org.hl7.fhir.r4.model.Resource fhirResource;
        org.hl7.fhir.r4.model.Patient fhirPatient = null;
        org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest = null;
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        serviceRequestResourceBundle = fhirConfig.fetchReferrals();
        System.out.println(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(serviceRequestResourceBundle));
        if (!serviceRequestResourceBundle.getEntry().isEmpty()) {
            for (int i = 0; i < serviceRequestResourceBundle.getEntry().size(); i++) {
                fhirServiceRequestResource = serviceRequestResourceBundle.getEntry().get(i).getResource();
                System.out.println("Fhir : Checking Service request is null ==>");
                if (fhirServiceRequestResource != null) {
                    System.out.println("Fhir : Service request is not null ==>");
                    fhirServiceRequest = (org.hl7.fhir.r4.model.ServiceRequest) fhirServiceRequestResource;
                    // Get UPI,
                    // Persist service request
                    String nupiNumber = fhirServiceRequest.getSubject().getDisplay();
                    System.out.println("NUPI :  ==>" + nupiNumber);
                    // Use UPI to get Patient, /**TODO - Change this to fetch from CR instead*/
                    patientResourceBundle = fhirConfig.fetchPatientResource(nupiNumber);

                    System.out.println(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(patientResourceBundle));
                    if (patientResourceBundle != null && !patientResourceBundle.getEntry().isEmpty()) {
                        fhirResource = patientResourceBundle.getEntry().get(0).getResource();
                        if (fhirResource.getResourceType().toString().equals("Patient")) {
                            fhirPatient = (org.hl7.fhir.r4.model.Patient) fhirResource;
                        }
                        // Persist the patient
                        if (fhirPatient != null) {
                            System.out.println("Fhir patient exists:  ==>");
                            //Persist client in expected referrals model
                            try{
                                addReferredClientObjectToBundle(fhirPatient,fhirConfig, fhirServiceRequest);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Fhir : Looping service request ==>");
                        System.out.println("Fhir service request identifier ==>" + fhirServiceRequest.getSubject().getIdentifier().getValue());
                    }
                } else {
                    System.out.println("Fhir : Service request is null ==>");
                }
            }
        }

        String success = "";
        return SimpleObject.create("success", success);

    }

    public void updateShrReferral(Patient patient, FhirConfig fhirConfig) throws Exception {
        List<ExpectedTransferInPatients> patientReferrals = Context.getService(KenyaEMRILService.class).getTransferInPatient(patient);
        List<ExpectedTransferInPatients> activeReferral = patientReferrals.stream().filter(p ->
                p.getReferralStatus().equalsIgnoreCase("ACTIVE")).collect(Collectors.toList());
        IParser parser=fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);

        ServiceRequest serviceRequest;
        if (!activeReferral.isEmpty()) {
            serviceRequest = parser.parseResource(ServiceRequest.class, activeReferral.get(0).getPatientSummary());
            System.out.println(serviceRequest.getStatus());
            System.out.println(serviceRequest.getCategory().get(0).getCoding().get(0).getDisplay());
            serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
            fhirConfig.updateReferral(serviceRequest);
        }
    }

    public void addReferredClientObjectToBundle(org.hl7.fhir.r4.model.Patient fhirPatient, FhirConfig fhirConfig, org.hl7.fhir.r4.model.ServiceRequest fhirServiceRequest) {

           System.out.println("Saving fhir client in expected referrals model");
        //Persist client in expected referrals model
        ExpectedTransferInPatients expectedTransferInPatients = new ExpectedTransferInPatients();
        //Add name
        fillClientName(fhirPatient, expectedTransferInPatients);
        //Add birthdate
        expectedTransferInPatients.setClientBirthDate(fhirPatient.getBirthDate().toString());
        //Set Gender
        String gender = fhirPatient.getGender().getDisplay();
        System.out.println("Fhir client gender here ==>" + gender);
        if (gender.equalsIgnoreCase("male")) {
            gender = "M";
        } else if (gender.equalsIgnoreCase("female")) {
            gender = "F";
        }
        expectedTransferInPatients.setClientGender(gender);
        // Add the NUPI Number
        String nupiNumber = fhirServiceRequest.getSubject().getDisplay();
        expectedTransferInPatients.setNupiNumber(nupiNumber);

        expectedTransferInPatients.setReferralStatus("ACTIVE");
        expectedTransferInPatients.setPatientSummary(fhirConfig.getFhirContext().newJsonParser().encodeResourceToString(fhirServiceRequest));
        expectedTransferInPatients.setServiceType("COMMUNITY");
        Context.getService(KenyaEMRILService.class).createPatient(expectedTransferInPatients);

        //TODO:Use FHIR 2 method to persist patient
        // MethodOutcome results = patientResourceProvider.createPatient(fhirPatient);
        System.out.println("Successfully persisted in expected referrals model ==>");


    }

    public void completeClientReferral(@RequestParam("patientId") Integer patientId) throws Exception {

        //Update  referral_status attribute to completed status
        System.out.println("Patient ID ==>" + patientId);
        System.out.println("Here ==>");
        PatientService patientService = Context.getPatientService();
        Patient patient = patientService.getPatient(patientId);
        PersonAttribute referralStatusAttribute = new PersonAttribute();
        PersonAttributeType referralStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("df7e9996-23b5-4f66-a799-97498d19850d");
        if (referralStatusAttributeType != null) {
            referralStatusAttribute.setAttributeType(referralStatusAttributeType);
            referralStatusAttribute.setValue("Completed");
            patient.addAttribute(referralStatusAttribute);
            patientService.savePatient(patient);
            //Update fhir message status to complete
            FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
            updateShrReferral(patient,fhirConfig);
        }

    }

    public Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    private ExpectedTransferInPatients fillClientName(org.hl7.fhir.r4.model.Patient fhirPatient, ExpectedTransferInPatients expectedTransferInPatients) {
        String familyName = "";
        String givenName = "";
        String middleName = "";
        if (!fhirPatient.getName().get(0).isEmpty()) {
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
            expectedTransferInPatients.setClientFirstName(familyName);
            expectedTransferInPatients.setClientMiddleName(middleName);
            expectedTransferInPatients.setClientLastName(givenName);
            expectedTransferInPatients.setClientFirstName(familyName);

            System.out.println("Fhir patient full name here ==>" + expectedTransferInPatients);

            return expectedTransferInPatients;
        }
        return null;
    }
}