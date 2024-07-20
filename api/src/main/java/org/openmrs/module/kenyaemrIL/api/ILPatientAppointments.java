package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.LastWhoStageCalculation;
import org.openmrs.module.kenyaemr.util.EncounterBasedRegimenUtils;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.MOTHER_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PHYSICAL_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.appointment.APPOINTMENT_INFORMATION;
import org.openmrs.module.kenyaemrIL.il.appointment.PLACER_APPOINTMENT_NUMBER;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.kenyaemrUtils.Utils;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for an IL Appointment Scheduling
 */
public class ILPatientAppointments {
public static String HIV_FOLLOWUP_APPOINTMENT_UUID = "885b4ad3-fd4c-4a16-8ed3-08813e6b01fa";
public static String HIV_DRUG_REFILL_APPOINTMENT_UUID = "a96921a1-b89e-4dd2-b6b4-7310f13bbabe";
public static String PREP_FOLLOWUP_APPOINTMENT_UUID = "6f9b19f6-ac25-41f9-a75c-b8b125dec3da";
public static String PREP_REFILL_APPOINTMENT_UUID = "b8c3efd9-e106-4409-ae0e-b9c651484a20";


    private final Log log = LogFactory.getLog(this.getClass());
    public static ILMessage iLPatientWrapper(Patient patient, Appointment appointment, boolean consentForReminder) {
        ILMessage ilMessage = new ILMessage();
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        INTERNAL_PATIENT_ID ipd;

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
            } else if (patientIdentifier.getIdentifierType().getUuid().equals("ac64e5cb-e3e2-4efa-9060-0dd715a843a1")) {
                ipd.setAssigning_authority("PREP");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("PREP Unique Number");
                internalPatientIds.add(ipd);
            }
        }

        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);

        //Set the patient name
        PATIENT_NAME patientName = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientName.setFirst_name(personName.getGivenName() != null ? personName.getGivenName() : "");
        patientName.setMiddle_name(personName.getMiddleName() != null ? personName.getMiddleName() : "");
        patientName.setLast_name(personName.getFamilyName() != null ? personName.getFamilyName() : "");
        patientIdentification.setPatient_name(patientName);

        // Set to empty strings unwanted patient details for viral load
        patientIdentification.setSex(patient.getGender());
        patientIdentification.setPhone_number(patient.getAttribute("Telephone contact") != null ? patient.getAttribute("Telephone contact").getValue() : "");
        patientIdentification.setMarital_status(patient.getAttribute("Civil Status") != null ? patient.getAttribute("Civil Status").getValue() : "");
        String iLDob;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        iLDob = formatter.format(patient.getBirthdate());
        patientIdentification.setDate_of_birth(iLDob);
        patientIdentification.setDate_of_birth_precision(patient.getBirthdateEstimated() == true ? "ESTIMATED" : "EXACT");
        if (patient.getDead()) {
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

        ilMessage.setPatient_identification(patientIdentification);
        //set appointment information
        APPOINTMENT_INFORMATION appointments[] = new APPOINTMENT_INFORMATION[1];
        APPOINTMENT_INFORMATION appointmentInformation = new APPOINTMENT_INFORMATION();

        setCommonAppointmentVariables(appointmentInformation, appointment, consentForReminder);
        appointments[0] = appointmentInformation;
        ilMessage.setAppointment_information(appointments);

        Encounter lastLabResultsEncounter = ILUtils.lastEncounter(appointment.getPatient(), Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
        List<OBSERVATION_RESULT> observationResults = new ArrayList<>();
        Integer heightConcept = 5090;
        Integer weightConcept = 5089;
        Integer latestVLConcept = 856;
        Integer LDLQuestionConcept = 1305;

        // extract triage information
        try {
            List<Obs> latestWeightObs = Utils.getNLastObs(Context.getConceptService().getConcept(weightConcept), patient, 1);

            if (latestWeightObs.size() > 0) {
                Obs weightObs = latestWeightObs.get(0);

                // compose observation object
                OBSERVATION_RESULT weightObservationResult = new OBSERVATION_RESULT();
                weightObservationResult.setObservation_identifier("WEIGHT");
                weightObservationResult.setSet_id("");
                weightObservationResult.setCoding_system("");
                weightObservationResult.setValue_type("NM");
                weightObservationResult.setObservation_value(String.valueOf(weightObs.getValueNumeric()));
                weightObservationResult.setUnits("KG");
                weightObservationResult.setObservation_result_status("F");
                String ts = formatter.format(weightObs.getObsDatetime());
                weightObservationResult.setObservation_datetime(ts);
                weightObservationResult.setAbnormal_flags("N");
                observationResults.add(weightObservationResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // latest height
        try {
            List<Obs> latestHeightObs = Utils.getNLastObs(Context.getConceptService().getConcept(heightConcept), patient, 1);
            if (latestHeightObs.size() > 0) {
                Obs heightObs = latestHeightObs.get(0);

                // compose observation object
                OBSERVATION_RESULT heightObservationResult = new OBSERVATION_RESULT();
                heightObservationResult.setObservation_identifier("HEIGHT");

                heightObservationResult.setSet_id("");
                heightObservationResult.setCoding_system("");
                heightObservationResult.setValue_type("NM");
                heightObservationResult.setObservation_value(String.valueOf(heightObs.getValueNumeric().intValue()));
                heightObservationResult.setUnits("CM");
                heightObservationResult.setObservation_result_status("F");
                String ts = formatter.format(heightObs.getObsDatetime());
                heightObservationResult.setObservation_datetime(ts);
                heightObservationResult.setAbnormal_flags("N");
                observationResults.add(heightObservationResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Current regimen
        Encounter lastDrugRegimenEditorEncounter = EncounterBasedRegimenUtils.getLastEncounterForCategory(appointment.getPatient(), "ARV");
        if (lastDrugRegimenEditorEncounter != null) {
            SimpleObject o = EncounterBasedRegimenUtils.buildRegimenChangeObject(lastDrugRegimenEditorEncounter.getAllObs(), lastDrugRegimenEditorEncounter);
            OBSERVATION_RESULT currentRegimen = new OBSERVATION_RESULT();
            currentRegimen.setObservation_identifier("CURRENT REGIMEN");
            currentRegimen.setSet_id("");
            currentRegimen.setCoding_system("");
            currentRegimen.setValue_type("NM");
            currentRegimen.setObservation_value(o.get("regimenShortDisplay").toString());
            currentRegimen.setUnits("STR");
            currentRegimen.setObservation_result_status("F");
            currentRegimen.setObservation_datetime("");
            currentRegimen.setAbnormal_flags("N");
            observationResults.add(currentRegimen);
        }

        // current who staging
        CalculationResult currentWhoStaging = EmrCalculationUtils.evaluateForPatient(LastWhoStageCalculation.class, null, appointment.getPatient());
        if (currentWhoStaging != null) {
            OBSERVATION_RESULT whoStage = new OBSERVATION_RESULT();
            whoStage.setObservation_identifier("WHO STAGE");
            whoStage.setSet_id("");
            whoStage.setCoding_system("");
            whoStage.setValue_type("NM");
            whoStage.setObservation_value(((Obs) currentWhoStaging.getValue()).getValueCoded().getName().getName());
            whoStage.setUnits("STR");
            whoStage.setObservation_result_status("F");
            whoStage.setObservation_datetime("");
            whoStage.setAbnormal_flags("N");
            observationResults.add(whoStage);
        }

        // VL values
        if (lastLabResultsEncounter != null) {
            for (Obs obs : lastLabResultsEncounter.getObs()) {
                //set vl sample collection date
                if (obs.getConcept().getConceptId().equals(latestVLConcept) || obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                    OBSERVATION_RESULT lastVlDate = new OBSERVATION_RESULT();
                    lastVlDate.setObservation_identifier("LAST LV DATE");
                    lastVlDate.setSet_id("");
                    lastVlDate.setCoding_system("");
                    lastVlDate.setValue_type("NM");
                    lastVlDate.setObservation_value(String.valueOf(obs.getObsDatetime()));
                    lastVlDate.setUnits("DATE");
                    lastVlDate.setObservation_result_status("F");
                    lastVlDate.setObservation_datetime(formatter.format(obs.getObsDatetime()));
                    lastVlDate.setAbnormal_flags("N");
                    observationResults.add(lastVlDate);

                    // Vl result
                    OBSERVATION_RESULT lastVlResult = new OBSERVATION_RESULT();
                    lastVlResult.setObservation_identifier("LAST LV RESULT");
                    lastVlResult.setSet_id("");
                    lastVlResult.setCoding_system("");
                    lastVlResult.setValue_type("ML");
                    if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                        lastVlResult.setObservation_value("LDL");
                    } else {
                        lastVlResult.setObservation_value(String.valueOf(obs.getValueNumeric()));
                    }
                    lastVlResult.setUnits("ML");
                    lastVlResult.setObservation_result_status("F");
                    lastVlResult.setObservation_datetime(formatter.format(obs.getObsDatetime()));
                    lastVlResult.setAbnormal_flags("N");
                    observationResults.add(lastVlResult);
                }
            }
        }
        ilMessage.setObservation_result(observationResults.toArray(new OBSERVATION_RESULT[observationResults.size()]));
        return ilMessage;
    }

    private static void setCommonAppointmentVariables(APPOINTMENT_INFORMATION appointmentInformation, Appointment appointment, boolean consentForReminder) {
        PLACER_APPOINTMENT_NUMBER placerAppointmentNumber = new PLACER_APPOINTMENT_NUMBER();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        if (appointment != null) {
            placerAppointmentNumber.setNumber(String.valueOf(appointment.getAppointmentId().intValue()));
            appointmentInformation.setPlacer_appointment_number(placerAppointmentNumber);
            appointmentInformation.setVisit_date(formatter.format(appointment.getDateAppointmentScheduled()));
            appointmentInformation.setAppointment_date(formatter.format((appointment.getDateFromStartDateTime())));
            appointmentInformation.setAction_code("A");
            appointmentInformation.setAppointment_note("N/A");
            appointmentInformation.setAppointment_status("PENDING");
            appointmentInformation.setAppointment_placing_entity("KENYAEMR");
            String serviceTypeUuid = appointment.getService().getUuid();

            if (HIV_FOLLOWUP_APPOINTMENT_UUID.equals(serviceTypeUuid)) {
                appointmentInformation.setAppointment_type("FOLLOWUP");
            } else if (HIV_DRUG_REFILL_APPOINTMENT_UUID.equals(serviceTypeUuid)) {
                appointmentInformation.setAppointment_type("PHARMACY_REFILL");
            } else if (PREP_FOLLOWUP_APPOINTMENT_UUID.equals(serviceTypeUuid)) {
                appointmentInformation.setAppointment_type("PREP_FOLLOWUP");
            } else if (PREP_REFILL_APPOINTMENT_UUID.equals(serviceTypeUuid)) {
                appointmentInformation.setAppointment_type("PREP_REFILL");
            }
            appointmentInformation.setAppointment_reason("CLINICAL");
            appointmentInformation.setAppointment_location("CLINIC");
            appointmentInformation.setConsent_for_reminder(consentForReminder? "Y" : "N");

        }
    }
}
