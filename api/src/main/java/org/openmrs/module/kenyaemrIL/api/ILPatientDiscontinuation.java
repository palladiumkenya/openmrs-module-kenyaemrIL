package org.openmrs.module.kenyaemrIL.api;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientProgram;
import org.openmrs.PersonName;
import org.openmrs.Program;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyacore.CoreConstants;
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
import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.REGIMEN_SWITCH_HISTORY;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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


        Program_Discontinuation_Message programDiscontinuationMessage = new Program_Discontinuation_Message();
        programDiscontinuationMessage.setTarget_program("HIV");
        for (Obs ob : encounter.getObs()) {
            if (ob.getConcept().getUuid().equals("161555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                if (ob.getValueCoded().getUuid().equals("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Transfer Out");
                    programDiscontinuationMessage.setService_request(referralInfo(encounter));
                }
                if (ob.getValueCoded().getUuid().equals("160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("Death");
                }
                if (ob.getValueCoded().getUuid().equals("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                    programDiscontinuationMessage.setDiscontinuation_reason("LTFU");
                }
                if (ob.getValueCoded().getUuid().equals("164349AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
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
        referralInformation.setTransfer_status(ServiceRequest.ServiceRequestStatus.ACTIVE);
        referralInformation.setTransfer_intent(ServiceRequest.ServiceRequestIntent.ORDER);
        referralInformation.setTransfer_priority(ServiceRequest.ServiceRequestPriority.ASAP);
        referralInformation.setSending_facility_mflCode(facilityMfl);
        referralInformation.setReceiving_facility_mflCode("");
        for (Obs obs : encounter.getObs()) {
            if (obs.getConcept().getUuid().equals("159495AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setReceiving_facility_mflCode(obs.getValueText().split("-")[0]);
            }
            if (obs.getConcept().getUuid().equals("160649AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                referralInformation.setTransfer_out_date(formatter.format(obs.getValueDatetime()));
            }
        }
        if (referralInformation.getTransfer_out_date() == null) {
            referralInformation.setTransfer_out_date("");
        }

        if (referralInformation.getTo_acceptance_date() == null) {
            referralInformation.setTo_acceptance_date("");
        }

        //Set patient's last vl and current regimen
        Encounter lastLabResultsEncounter = ILUtils.lastEncounter(encounter.getPatient(), Context.getEncounterService().getEncounterTypeByUuid("17a381d1-7e29-406a-b782-aa903b963c28"));
        List<Encounter> followUpEncounters = Context.getEncounterService().getEncounters(encounter.getPatient(), null, null, null, null, Arrays.asList(Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e")), null, null, null, false);
        List<Encounter> enrolmentEncounters = Context.getEncounterService().getEncounters(encounter.getPatient(), null, null, null, null, Arrays.asList(Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc")), null, null, null, false);
        Encounter latestFollowUpEncounter = followUpEncounters.get(followUpEncounters.size() - 1);

        StringBuilder drugAllergies = new StringBuilder();
        StringBuilder otherAllergies = new StringBuilder();
        List<PATIENT_NCD> patientNcds = new ArrayList<>();
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
            SimpleObject o = EncounterBasedRegimenUtils.buildRegimenChangeObject(lastDrugRegimenEditorEncounter.getAllObs(), lastDrugRegimenEditorEncounter);
            serviceRequestSupportingInfo.setCurrent_regimen(o.get("regimenShortDisplay").toString());
        }

        // current cd4
        CalculationResult cd4Results = EmrCalculationUtils.evaluateForPatient(LastCd4CountDateCalculation.class, null, encounter.getPatient());
        if (cd4Results != null && cd4Results.getValue() != null) {
            serviceRequestSupportingInfo.setCd4_value(((Obs) cd4Results.getValue()).getValueNumeric().toString());
            serviceRequestSupportingInfo.setCd4_date(formatter.format(((Obs) cd4Results.getValue()).getObsDatetime()));
        }

        // regimen change history
        List<REGIMEN_SWITCH_HISTORY> regimenChangeHistory = getRegimenHistoryFromObservations(encounter.getPatient(), "ARV");
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

        messageFormatter(serviceRequestSupportingInfo);

        referralInformation.setSupporting_info(serviceRequestSupportingInfo);

        return referralInformation;
    }

    public static List<REGIMEN_SWITCH_HISTORY> getRegimenHistoryFromObservations(Patient patient, String category) {
        FormService formService = Context.getFormService();
        EncounterService encounterService = Context.getEncounterService();
        String ARV_TREATMENT_PLAN_EVENT_CONCEPT = "1255AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String TB_TREATMENT_PLAN_CONCEPT = "1268AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        List<REGIMEN_SWITCH_HISTORY> history = new ArrayList();
        String categoryConceptUuid = category.equals("ARV") ? ARV_TREATMENT_PLAN_EVENT_CONCEPT : TB_TREATMENT_PLAN_CONCEPT;
        EncounterType et = encounterService.getEncounterTypeByUuid("7dffc392-13e7-11e9-ab14-d663bd873d93");
        Form form = formService.getFormByUuid("da687480-e197-11e8-9f32-f2801f1b9fd1");
        List<Encounter> regimenChangeHistory = EmrUtils.AllEncounters(patient, et, form);
        if (regimenChangeHistory != null && regimenChangeHistory.size() > 0) {
            Iterator var11 = regimenChangeHistory.iterator();

            while (var11.hasNext()) {
                Encounter e = (Encounter) var11.next();
                Set<Obs> obs = e.getObs();
                if (programEncounterMatching(obs, categoryConceptUuid)) {
                    REGIMEN_SWITCH_HISTORY object = buildRegimenChangeObject(obs, e);
                    if (object != null) {
                        history.add(object);
                    }
                }
            }

            return history;
        } else {
            return new ArrayList();
        }
    }

    public static boolean programEncounterMatching(Set<Obs> obs, String conceptUuidToMatch) {
        Iterator var2 = obs.iterator();

        Obs o;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            o = (Obs) var2.next();
        } while (!o.getConcept().getUuid().equals(conceptUuidToMatch));

        return true;
    }

    public static REGIMEN_SWITCH_HISTORY buildRegimenChangeObject(Set<Obs> obsList, Encounter e) {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");
        String CURRENT_DRUGS = "1193AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String REASON_REGIMEN_STOPPED_CODED = "1252AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String REASON_REGIMEN_STOPPED_NON_CODED = "5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String DATE_REGIMEN_STOPPED = "1191AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String CURRENT_DRUG_NON_STANDARD = "1088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String REGIMEN_LINE_CONCEPT = "163104AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        String regimen = null;
        String regimenShort = null;
        String regimenLine = null;
        String regimenUuid = null;
        String endDate = null;
        String startDate = e != null ? DATE_FORMAT.format(e.getEncounterDatetime()) : "";
        Set<String> changeReason = new HashSet();
        StringBuilder nonstandardRegimen = new StringBuilder();
        Iterator var16 = obsList.iterator();

        while (true) {
            while (var16.hasNext()) {
                Obs obs = (Obs) var16.next();
                if (obs.getConcept().getUuid().equals(CURRENT_DRUGS)) {
                    regimen = obs.getValueCoded() != null ? obs.getValueCoded().getFullySpecifiedName(CoreConstants.LOCALE).getName() : "Unresolved Regimen name";

                    try {
                        regimenShort = getRegimenNameFromRegimensXMLString(obs.getValueCoded().getUuid(), getRegimenConceptJson());
                    } catch (IOException var19) {
                        var19.printStackTrace();
                    }

                    regimenUuid = obs.getValueCoded() != null ? obs.getValueCoded().getUuid() : "";
                } else if (obs.getConcept().getUuid().equals(CURRENT_DRUG_NON_STANDARD)) {
                    nonstandardRegimen.append(obs.getValueCoded().getFullySpecifiedName(CoreConstants.LOCALE).getName().toUpperCase() + "/");
                    regimenUuid = obs.getValueCoded() != null ? obs.getValueCoded().getUuid() : "";
                } else {
                    String reason;
                    if (obs.getConcept().getUuid().equals(REASON_REGIMEN_STOPPED_CODED)) {
                        reason = obs.getValueCoded() != null ? obs.getValueCoded().getName().getName() : "";
                        if (reason != null) {
                            changeReason.add(reason);
                        }
                    } else if (obs.getConcept().getUuid().equals(REASON_REGIMEN_STOPPED_NON_CODED)) {
                        reason = obs.getValueText();
                        if (reason != null) {
                            changeReason.add(reason);
                        }
                    } else if (obs.getConcept() != null && obs.getConcept().getUuid().equals(DATE_REGIMEN_STOPPED)) {
                        if (obs.getValueDatetime() != null) {
                            endDate = DATE_FORMAT.format(obs.getValueDatetime());
                        }
                    } else if (obs.getConcept() != null && obs.getConcept().getUuid().equals(REGIMEN_LINE_CONCEPT) && obs.getValueText() != null) {
                        if (obs.getValueText().equals("AF")) {
                            regimenLine = "Adult first line";
                        } else if (obs.getValueText().equals("AS")) {
                            regimenLine = "Adult second line";
                        } else if (obs.getValueText().equals("AT")) {
                            regimenLine = "Adult third line";
                        } else if (obs.getValueText().equals("CF")) {
                            regimenLine = "Child first line";
                        } else if (obs.getValueText().equals("CS")) {
                            regimenLine = "Child second line";
                        } else if (obs.getValueText().equals("CT")) {
                            regimenLine = "Child third line";
                        }
                    }
                }
            }

            if (nonstandardRegimen.length() > 0) {
                return new REGIMEN_SWITCH_HISTORY(startDate, endDate != null ? endDate : "", nonstandardRegimen.toString().substring(0, nonstandardRegimen.length() - 1), regimenLine != null ? regimenLine : "", nonstandardRegimen.toString().substring(0, nonstandardRegimen.length() - 1), changeReason, regimenUuid, endDate == null ? "true" : "false");
            }

            if (regimen != null) {
                return new REGIMEN_SWITCH_HISTORY(startDate, endDate != null ? endDate : "", regimenShort != null ? regimenShort : regimen, regimenLine != null ? regimenLine : "", regimen, changeReason, regimenUuid, endDate == null ? "true" : "false");
            }

            return new REGIMEN_SWITCH_HISTORY("", "", "", "", "", new HashSet<>(), "", "");
        }
    }

    public static String getRegimenNameFromRegimensXMLString(String conceptRef, String regimenJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode conf = (ArrayNode) mapper.readTree(regimenJson);
        Iterator<JsonNode> it = conf.iterator();

        ObjectNode node;
        do {
            if (!it.hasNext()) {
                return "Unknown";
            }

            node = (ObjectNode) it.next();
        } while (!node.get("conceptRef").asText().equals(conceptRef));

        return node.get("name").asText();
    }

    public static String getRegimenConceptJson() {
        String json = "[\n  {\n    \"name\": \"TDF/3TC/NVP\",\n    \"conceptRef\": \"162565AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"TDF/3TC/EFV\",\n    \"conceptRef\": \"164505AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"AZT/3TC/NVP\",\n    \"conceptRef\": \"1652AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"AZT/3TC/EFV\",\n    \"conceptRef\": \"160124AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"D4T/3TC/NVP\",\n    \"conceptRef\": \"792AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"D4T/3TC/EFV\",\n    \"conceptRef\": \"160104AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"TDF/3TC/AZT\",\n    \"conceptRef\": \"98e38a9c-435d-4a94-9b66-5ca524159d0e\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"AZT/3TC/DTG\",\n    \"conceptRef\": \"6dec7d7d-0fda-4e8d-8295-cb6ef426878d\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"TDF/3TC/DTG\",\n    \"conceptRef\": \"9fb85385-b4fb-468c-b7c1-22f75834b4b0\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"ABC/3TC/DTG\",\n    \"conceptRef\": \"4dc0119b-b2a6-4565-8d90-174b97ba31db\",\n    \"regimenLine\": \"adult_first\"\n  },\n  {\n    \"name\": \"AZT/3TC/LPV/r\",\n    \"conceptRef\": \"162561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"AZT/3TC/ATV/r\",\n    \"conceptRef\": \"164511AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"TDF/3TC/LPV/r\",\n    \"conceptRef\": \"162201AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"TDF/3TC/ATV/r\",\n    \"conceptRef\": \"164512AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"D4T/3TC/LPV/r\",\n    \"conceptRef\": \"162560AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"AZT/TDF/3TC/LPV/r\",\n    \"conceptRef\": \"c421d8e7-4f43-43b4-8d2f-c7d4cfb976a4\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"ETR/RAL/DRV/RTV\",\n    \"conceptRef\": \"337b6cfd-9fa7-47dc-82b4-d479c39ef355\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"ETR/TDF/3TC/LPV/r\",\n    \"conceptRef\": \"7a6c51c4-2b68-4d5a-b5a2-7ba420dde203\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"ABC/3TC/LPV/r\",\n    \"conceptRef\": \"162200AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"ABC/3TC/ATV/r\",\n    \"conceptRef\": \"dddd9cf2-2b9c-4c52-84b3-38cfe652529a\",\n    \"regimenLine\": \"adult_second\"\n  },\n  {\n    \"name\": \"ABC/3TC/LPV/r\",\n    \"conceptRef\": \"162200AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"ABC/3TC/NVP\",\n    \"conceptRef\": \"162199AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"ABC/3TC/EFV\",\n    \"conceptRef\": \"162563AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"AZT/3TC/ABC\",\n    \"conceptRef\": \"817AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"D4T/3TC/ABC\",\n    \"conceptRef\": \"b9fea00f-e462-4ea5-8d40-cc10e4be697e\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"TDF/ABC/LPV/r\",\n    \"conceptRef\": \"162562AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"ABC/DDI/LPV/r\",\n    \"conceptRef\": \"162559AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"ABC/TDF/3TC/LPV/r\",\n    \"conceptRef\": \"077966a6-4fbd-40ce-9807-2d5c2e8eb685\",\n    \"regimenLine\": \"child_first\"\n  },\n  {\n    \"name\": \"RHZE\",\n    \"conceptRef\": \"1675AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"RHZ\",\n    \"conceptRef\": \"768AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"SRHZE\",\n    \"conceptRef\": \"1674AAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"RfbHZE\",\n    \"conceptRef\": \"07c72be8-c575-4e26-af09-9a98624bce67\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"RfbHZ\",\n    \"conceptRef\": \"9ba203ec-516f-4493-9b2c-4ded6cc318bc\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"SRfbHZE\",\n    \"conceptRef\": \"fce8ba26-8524-43d1-b0e1-53d8a3c06c00\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"S (1 gm vial)\",\n    \"conceptRef\": \"84360AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"adult_intensive\"\n  },\n  {\n    \"name\": \"E\",\n    \"conceptRef\": \"75948AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_intensive\"\n  },\n  {\n    \"name\": \"RH\",\n    \"conceptRef\": \"1194AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_intensive\"\n  },\n  {\n    \"name\": \"RHE\",\n    \"conceptRef\": \"159851AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_intensive\"\n  },\n  {\n    \"name\": \"EH\",\n    \"conceptRef\": \"1108AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"child_intensive\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV\",\n    \"conceptRef\": \"5b8e4955-897a-423b-ab66-7e202b9c304c\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV/AZT\",\n    \"conceptRef\": \"092604d3-e9cb-4589-824e-9e17e3cb4f5e\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV/TDF\",\n    \"conceptRef\": \"c6372744-9e06-40cf-83e5-c794c985b6bf\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"ETV/3TC/DRV/RTV\",\n    \"conceptRef\": \"1995c4a1-a625-4449-ab28-aae88d0f80e6\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"AZT/3TC/LPV/r\",\n    \"conceptRef\": \"162561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"Child (second line)\"\n  },\n  {\n    \"name\": \"AZT/3TC/ATV/r\",\n    \"conceptRef\": \"164511AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"Child (second line)\"\n  },\n  {\n    \"name\": \"ABC/3TC/ATV/r\",\n    \"conceptRef\": \"dddd9cf2-2b9c-4c52-84b3-38cfe652529a\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV\",\n    \"conceptRef\": \"5b8e4955-897a-423b-ab66-7e202b9c304c\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV/AZT\",\n    \"conceptRef\": \"092604d3-e9cb-4589-824e-9e17e3cb4f5e\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"ETV/3TC/DRV/RTV\",\n    \"conceptRef\": \"1995c4a1-a625-4449-ab28-aae88d0f80e6\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"RAL/3TC/DRV/RTV/ABC\",\n    \"conceptRef\": \"0e74f7aa-85ab-4e92-9f97-79e76e618689\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"AZT/3TC/RAL/DRV/r\",\n    \"conceptRef\": \"a1183b26-8e87-457c-8d7d-00a96b17e046\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"ABC/3TC/RAL/DRV/r\",\n    \"conceptRef\": \"02302ab5-dcb2-4337-a792-d6cf1082fc1d\",\n    \"regimenLine\": \"Child (third line)\"\n  },\n  {\n    \"name\": \"TDF/3TC/DTG/DRV/r\",\n    \"conceptRef\": \"5f429c76-2976-4374-a69e-d2d138dd16bf\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"TDF/3TC/RAL/DRV/r\",\n    \"conceptRef\": \"9b9817dd-4c84-4093-95c3-690d65d24b99\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"TDF/3TC/DTG/ATV/r\",\n    \"conceptRef\": \"64b63993-1479-4714-9389-312072f26704\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"TDF/3TC/DTG/ETV/DRV/r\",\n    \"conceptRef\": \"9de6367e-479b-4d50-a0f9-2a9987c6dce0\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"ABC/3TC/DTG/DRV/r\",\n    \"conceptRef\": \"cc728487-2f54-4d5e-ae0f-22ef617a8cfd\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"TDF/3TC/DTG/EFV/DRV/r\",\n    \"conceptRef\": \"f2acaf9b-3da9-4d71-b0cf-fd6af1073c9e\",\n    \"regimenLine\": \"Adult (third line)\"\n  },\n  {\n    \"name\": \"B/F/TAF\",\n    \"conceptRef\": \"167206AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\n    \"regimenLine\": \"Adult (first line)\"\n  },\n  {\n    \"name\": \"ABC/3TC/RAL\",\n    \"conceptRef\": \"7af7ebbe-99da-4a43-a23a-c3866c5d08db\",\n    \"regimenLine\": \"Child (first line)\"\n  }\n]";
        return json;
    }

    public static void messageFormatter(SERVICE_REQUEST_SUPPORTING_INFO serviceRequestSupportingInfo) {
        if (serviceRequestSupportingInfo.getAppointment_date() == null) {
            serviceRequestSupportingInfo.setAppointment_date("");
        }
        if (serviceRequestSupportingInfo.getWho_stage() == null) {
            serviceRequestSupportingInfo.setWho_stage("");
        }
        if (serviceRequestSupportingInfo.getCd4_date() == null) {
            serviceRequestSupportingInfo.setCd4_date("");
        }
        if (serviceRequestSupportingInfo.getArv_adherence_outcome() == null) {
            serviceRequestSupportingInfo.setArv_adherence_outcome("");
        }
        if (serviceRequestSupportingInfo.getCurrent_regimen() == null) {
            serviceRequestSupportingInfo.setCurrent_regimen("");
        }
        if (serviceRequestSupportingInfo.getCd4_value() == null) {
            serviceRequestSupportingInfo.setCd4_value("");
        }
        if (serviceRequestSupportingInfo.getViral_load() == null) {
            serviceRequestSupportingInfo.setViral_load("");
        }
        if (serviceRequestSupportingInfo.getLast_vl_date() == null) {
            serviceRequestSupportingInfo.setLast_vl_date("");
        }
        if (serviceRequestSupportingInfo.getDate_confirmed_positive() == null) {
            serviceRequestSupportingInfo.setDate_confirmed_positive("");
        }
        if (serviceRequestSupportingInfo.getTpt_end_date() == null) {
            serviceRequestSupportingInfo.setTb_end_date("");
        }

        if (serviceRequestSupportingInfo.getDate_first_enrolled() == null) {
            serviceRequestSupportingInfo.setDate_first_enrolled("");
        }

        if (serviceRequestSupportingInfo.getDrug_days() == null) {
            serviceRequestSupportingInfo.setDrug_days("");
        }
        if (serviceRequestSupportingInfo.getHeight() == null) {
            serviceRequestSupportingInfo.setHeight("");
        }
        if (serviceRequestSupportingInfo.getWeight() == null) {
            serviceRequestSupportingInfo.setWeight("");
        }

        if (serviceRequestSupportingInfo.getEntry_point() == null) {
            serviceRequestSupportingInfo.setEntry_point("");
        }
        if (serviceRequestSupportingInfo.getDate_started_art_at_transferring_facility() == null) {
            serviceRequestSupportingInfo.setDate_started_art_at_transferring_facility("");
        }
        if (serviceRequestSupportingInfo.getTpt_start_date() == null) {
            serviceRequestSupportingInfo.setTb_start_date("");
        }
        if (serviceRequestSupportingInfo.getTpt_end_date() == null) {
            serviceRequestSupportingInfo.setTpt_end_date("");
        }
        if (serviceRequestSupportingInfo.getTpt_end_reason() == null) {
            serviceRequestSupportingInfo.setTb_end_reason("");
        }

        if (serviceRequestSupportingInfo.getTb_start_date() == null) {
            serviceRequestSupportingInfo.setTb_start_date("");
        }
        if (serviceRequestSupportingInfo.getTb_end_date() == null) {
            serviceRequestSupportingInfo.setTb_end_date("");
        }
        if (serviceRequestSupportingInfo.getTb_end_reason() == null) {
            serviceRequestSupportingInfo.setTb_end_reason("");
        }
    }

}
