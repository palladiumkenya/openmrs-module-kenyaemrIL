package org.openmrs.module.kenyaemrIL.api;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.LastWhoStageCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.LastCd4CountDateCalculation;
import org.openmrs.module.kenyaemr.metadata.IPTMetadata;
import org.openmrs.module.kenyaemr.metadata.TbMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.util.EncounterBasedRegimenUtils;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.Program_Discontinuation_Message;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_NCD;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.SERVICE_REQUEST_SUPPORTING_INFO;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.MOTHER_NAME;
import org.openmrs.module.kenyaemrIL.il.PATIENT_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.PHYSICAL_ADDRESS;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ILPatientDiscontinuation {
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

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
        //set dob precision
        patientIdentification.setDate_of_birth_precision(patient.getBirthdateEstimated() == true ? "ESTIMATED" : "EXACT");
        //set death date and indicator
        if (patient.isDead()) {
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


        Program_Discontinuation_Message programDiscontinuationMessage = new Program_Discontinuation_Message();
        programDiscontinuationMessage.setTarget_program("HIV");
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                if (ob.getValueCoded().getUuid().equals("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Transfer Out");
                    programDiscontinuationMessage.setService_request(referralInfo(encounter));
                }else if (ob.getValueCoded().getUuid().equals("160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Death");
                } else if (ob.getValueCoded().getUuid().equals("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("LTFU");
                }else if (ob.getValueCoded().getUuid().equals("164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Stopped Treatment");
                }
            }
            if (ob.getConcept().getUuid().equals("164384AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                programDiscontinuationMessage.setEffective_discontinuation_date(formatter.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1543AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_date(formatter.format(ob.getValueDatetime()));
            }
            if (ob.getConcept().getUuid().equals("1599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                patientIdentification.setDeath_indicator(ob.getValueCoded().getName().getName());
            }
        }
        ilMessage.setPatient_identification(patientIdentification);

        ilMessage.setDiscontinuation_message(programDiscontinuationMessage);
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
        SERVICE_REQUEST_SUPPORTING_INFO serviceRequestSupportingInfo = new SERVICE_REQUEST_SUPPORTING_INFO();
        referralInformation.setTransfer_status("active");
        referralInformation.setTransfer_intent("order");
        referralInformation.setTransfer_priority("asap");
        referralInformation.setSending_facility_mflcode(facilityMfl);
        for (Obs obs : encounter.getObs()) {
            if (obs.getConcept().getUuid().equals("159495AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setReceiving_facility_mflcode(obs.getValueText().split("-")[0]);
            }
            if (obs.getConcept().getUuid().equals("160649AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setTransfer_out_date(formatter.format(obs.getValueDatetime()));
            }
        }

        //Set patient's last vl and current regimen
        Encounter lastLabResultsEncounter = ILUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
        List<Encounter> followUpEncounters = Context.getEncounterService().getEncounters(encounter.getPatient(), null, null, null, null, Arrays.asList(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e")), null, null, null, false);
        List<Encounter> enrolmentEncounters = Context.getEncounterService().getEncounters(encounter.getPatient(), null, null, null, null, Arrays.asList(Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc")), null, null, null, false);
        Encounter latestFollowUpEncounter = null;
        if (!followUpEncounters.isEmpty()) {
            latestFollowUpEncounter = followUpEncounters.get(followUpEncounters.size() - 1);
        }

        StringBuilder drugAllergies = new StringBuilder();
        StringBuilder otherAllergies = new StringBuilder();
        List<PATIENT_NCD> patientNcds = new ArrayList<>();
        if (latestFollowUpEncounter != null) {
            for (Obs obs : latestFollowUpEncounter.getObs()) {
                if (obs.getConcept().getConceptId() == 5096) {
                    serviceRequestSupportingInfo.setAppointment_date(formatter.format(obs.getValueDatetime()));
                    long difference_In_Time = obs.getValueDatetime().getTime() - latestFollowUpEncounter.getEncounterDatetime().getTime();
                    serviceRequestSupportingInfo.setDrug_days(String.valueOf(TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365));
                }
                if (obs.getConcept().getUuid().equals("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    serviceRequestSupportingInfo.setWeight(Double.toString(obs.getValueNumeric()));
                }
                if (obs.getConcept().getUuid().equals("5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    serviceRequestSupportingInfo.setHeight(Double.toString(obs.getValueNumeric()));
                }
                if (obs.getConcept().getUuid().equals("1658AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    serviceRequestSupportingInfo.setArv_adherence_outcome(obs.getValueCoded().getName().getName());
                }
                if (obs.getConcept().getUuid().equals("1193AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    drugAllergies.append(" " + obs.getValueCoded().getName().getName());
                }
                if (obs.getConcept().getUuid().equals("160643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    otherAllergies.append(" " + obs.getValueCoded().getName().getName());
                }
                if (obs.getConcept().getUuid().equals("1284AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    List<Obs> onsetDate = latestFollowUpEncounter.getObs()
                            .stream()
                            .filter(c -> c.getConcept().getUuid().equals("159948AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") && c.getObsGroup().getUuid().equals(obs.getObsGroup().getUuid()))
                            .collect(Collectors.toList());
                    if (!onsetDate.isEmpty()) {
                        patientNcds.add(new PATIENT_NCD(obs.getValueCoded().getName().getName(), formatter.format(onsetDate.get(0).getValueDatetime()), ""));
                    }
                }
            }

        }
        serviceRequestSupportingInfo.setDrug_allergies(drugAllergies.toString());
        serviceRequestSupportingInfo.setOther_allergies(otherAllergies.toString());
        serviceRequestSupportingInfo.setPatient_ncds(patientNcds);


        PatientCalculationContext context = Context.getService(PatientCalculationService.class).createCalculationContext();
        context.setNow(new Date());

        // current who staging
        CalculationResult currentWhoStaging = EmrCalculationUtils.evaluateForPatient(LastWhoStageCalculation.class, null, encounter.getPatient());
        if (currentWhoStaging != null) {
            serviceRequestSupportingInfo.setWho_stage(((Obs) currentWhoStaging.getValue()).getValueCoded().getName().getName());
        } else {
            serviceRequestSupportingInfo.setWho_stage("");
        }

        // Current regimen
        Encounter lastDrugRegimenEditorEncounter = EncounterBasedRegimenUtils.getLastEncounterForCategory(encounter.getPatient(), "ARV");
        if (lastDrugRegimenEditorEncounter != null) {
            SimpleObject simpleObject = EncounterBasedRegimenUtils.buildRegimenChangeObject(lastDrugRegimenEditorEncounter.getAllObs(), lastDrugRegimenEditorEncounter);
            serviceRequestSupportingInfo.setCurrent_regimen(simpleObject.get("regimenShortDisplay").toString());
        }

        // current cd4
        CalculationResult cd4Results = EmrCalculationUtils.evaluateForPatient(LastCd4CountDateCalculation.class, null, encounter.getPatient());
        if (cd4Results != null && cd4Results.getValue() != null) {
            serviceRequestSupportingInfo.setCd4_value(((Obs) cd4Results.getValue()).getValueNumeric().toString());
            serviceRequestSupportingInfo.setCd4_date(formatter.format(((Obs) cd4Results.getValue()).getObsDatetime()));
        }

        // regimen change history
        List<SimpleObject> regimenChangeHistory = EncounterBasedRegimenUtils.getRegimenHistoryFromObservations(encounter.getPatient(), "ARV");
        serviceRequestSupportingInfo.setRegimen_change_history(regimenChangeHistory);
        
        Integer latestVLConcept = 856;
        Integer LDLQuestionConcept = 1305;
        if (lastLabResultsEncounter != null) {
            for (Obs obs : lastLabResultsEncounter.getObs()) {
                //set vl sample collection date
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {
                    serviceRequestSupportingInfo.setLast_vl_date(String.valueOf(obs.getObsDatetime()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                    serviceRequestSupportingInfo.setLast_vl_date(String.valueOf(obs.getObsDatetime()));
                }

                //set vl result
                if (obs.getConcept().getConceptId().equals(latestVLConcept)) {
                    serviceRequestSupportingInfo.setViral_load(String.valueOf(obs.getValueNumeric()));
                } else if (obs.getConcept().getConceptId().equals(LDLQuestionConcept)) {
                    serviceRequestSupportingInfo.setViral_load("LDL");
                }
            }
        }

        if (!enrolmentEncounters.isEmpty()) {
            for (Encounter enrolment : enrolmentEncounters) {
                //Filter patient type obs record
                List<Obs> patientType = enrolment.getObs()
                        .stream()
                        .filter(c -> c.getConcept().getConceptId() == 164932)
                        .collect(Collectors.toList());
                if (!patientType.isEmpty()) {
                    if (!Arrays.asList(new Integer[]{164931, 159833}).contains(patientType.get(0).getValueCoded().getConceptId())) {
                        for (Obs obs : enrolment.getObs()) {
                            if (obs.getConcept().getUuid().equals("160554AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                serviceRequestSupportingInfo.setDate_confirmed_positive(formatter.format(obs.getValueDatetime()));
                            }
                            if (obs.getConcept().getUuid().equals("160555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                serviceRequestSupportingInfo.setDate_first_enrolled(formatter.format(obs.getValueDatetime()));
                            }
                            if (obs.getConcept().getUuid().equals("159599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                serviceRequestSupportingInfo.setDate_started_art_at_transferring_facility(formatter.format(enrolment.getEncounterDatetime()));
                            }
                            if (obs.getConcept().getUuid().equals("160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                serviceRequestSupportingInfo.setEntry_point(obs.getValueCoded().getName().getName());
                            }
                        }
                    }
                }
            }
        }
        //IPT Data
        Program iptProgram = MetadataUtils.existing(Program.class, IPTMetadata._Program.IPT);
        Encounter lastIptOutcomeEncounter = EmrUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid(IPTMetadata._EncounterType.IPT_OUTCOME));
        List<PatientProgram> patientPrograms = Context.getProgramWorkflowService().getPatientPrograms(encounter.getPatient(), iptProgram, null, null, null, null, false);
        List<PatientProgram> patientIptProgram = patientPrograms.stream()
                .filter(pp -> pp.getProgram().getUuid().equals(IPTMetadata._Program.IPT))
                .collect(Collectors.toList());
        if (!patientIptProgram.isEmpty()) {
            serviceRequestSupportingInfo.setTpt_start_date(formatter.format(patientIptProgram.get(0).getDateEnrolled()));
            if (lastIptOutcomeEncounter != null) {
                serviceRequestSupportingInfo.setTpt_end_date(formatter.format(patientIptProgram.get(0).getDateCompleted()));
                if (lastIptOutcomeEncounter != null) {
                    List<Obs> stopReasonList = lastIptOutcomeEncounter.getObs().stream().filter(ob -> ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).collect(Collectors.toList());
                    serviceRequestSupportingInfo.setTpt_end_reason(stopReasonList.get(0).getValueCoded().getName().getName());
                }
            }
        }

        //TB Data
        Program tbProgram = MetadataUtils.existing(Program.class, TbMetadata._Program.TB);
        Encounter lastTbOutcomeEncounter = EmrUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid(TbMetadata._EncounterType.TB_DISCONTINUATION));
        List<PatientProgram> patientTbPrograms = Context.getProgramWorkflowService().getPatientPrograms(encounter.getPatient(), tbProgram, null, null, null, null, false);
        List<PatientProgram> patientTbProgram = patientTbPrograms.stream()
                .filter(pp -> pp.getProgram().getUuid().equals(TbMetadata._Program.TB))
                .collect(Collectors.toList());
        if (!patientTbProgram.isEmpty()) {
            serviceRequestSupportingInfo.setTb_start_date(formatter.format(patientTbProgram.get(0).getDateEnrolled()));
            if (lastTbOutcomeEncounter != null) {
                serviceRequestSupportingInfo.setTb_end_date(formatter.format(patientTbProgram.get(0).getDateCompleted()));
                List<Obs> stopReasonList = lastTbOutcomeEncounter.getObs().stream().filter(ob -> ob.getConcept().getUuid().equals("159786AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).collect(Collectors.toList());
                serviceRequestSupportingInfo.setTb_end_reason(stopReasonList.get(0).getValueCoded().getName().getName());
            }
        }


        referralInformation.setSupporting_info(serviceRequestSupportingInfo);

        return referralInformation;
    }
}
