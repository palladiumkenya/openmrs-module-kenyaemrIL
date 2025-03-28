package org.openmrs.module.kenyaemrIL.caseSurveillance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.util.HtsConstants;
import org.openmrs.module.kenyaemrIL.dmi.dmiUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.BASE_CS_URL;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.getBearerToken;

public class CaseSurveillanceDataExchange {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillanceDataExchange.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String PrEP_INITIAl_FUP_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
    private static final String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
    private static final String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
    private static final String PrEP_NUMBER_IDENTIFIER_TYPE_UUID = "ac64e5cb-e3e2-4efa-9060-0dd715a843a1";
    private static final int PrEP_REGIMEN_CONCEPT_ID = 164515;

    // Utility method for null-safe string extraction
    private static String safeGetField(PersonAddress address, Function<PersonAddress, String> mapper) {
        return (address != null) ? mapper.apply(address) : null;
    }

    // Utility method for formatting datetime
    private static String formatDateTime(Date date) {
        return (date != null) ? new SimpleDateFormat(DATE_TIME_FORMAT).format(date) : null;
    }

    private static String formatDate(Date date) {
        return (date != null) ? new SimpleDateFormat(DATE_FORMAT).format(date) : null;
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private static SimpleObject mapToTestedPositiveObject(Encounter encounter, Patient patient) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", formatDate(patient.getBirthdate()),
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "positiveHivTestDate", formatDateTime(encounter.getEncounterDatetime())
        );
    }

    // Utility method for creating structured SimpleObject for patients linked to care
    private SimpleObject mapToLinkageObject(Encounter encounter, Patient patient, String artStartDate) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", formatDate(patient.getBirthdate()),
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "positiveHivTestDate", null,
                "artStartDate", artStartDate
        );
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskObject(Encounter encounter, Patient patient) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", formatDate(patient.getBirthdate()),
                "sex", sex != null ? dmiUtils.formatGender(sex) : null
        );
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(Encounter encounter, Patient patient, String prepNumber, String prepRegimen) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", formatDate(patient.getBirthdate()),
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "prepStartDate", formatDateTime(encounter.getEncounterDatetime()),
                "prepNumber", prepNumber,
                "prepRegimen", prepRegimen
        );
    }

    /**
     * Retrieves a list of patients tested HIV-positive since the last fetch date
     */
    public List<SimpleObject> testedHIVPositive(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();

        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsPositiveResult = conceptService.getConcept(HtsConstants.HTS_POSITIVE_RESULT_CONCEPT_ID);

        if (htsFinalTestQuestion == null || htsPositiveResult == null) {
            log.error("Required HTS concepts are missing");
            return result;
        }

        // Get relevant encounter types
        List<EncounterType> testingEncounterTypes = Arrays.asList(
                MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION),
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS)
        );
        List<Form> testingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANTENATAL_VISIT),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DELIVERY),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT)
        );

        // Build encounter search criteria
        EncounterSearchCriteria searchCriteria = new EncounterSearchCriteria(
                null, null, fetchDate, null, null, testingForms, testingEncounterTypes,
                null, null, null, false
        );

        List<Encounter> encounters = encounterService.getEncounters(searchCriteria);

        for (Encounter encounter : encounters) {
            if (encounter == null) {
                log.warn("Encounter is null, skipping...");
                continue;
            }

            Patient patient = encounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no HIV+ patient, skipping...");
                continue;
            }
            if (EmrUtils.encounterThatPassCodedAnswer(encounter, htsFinalTestQuestion, htsPositiveResult)) {
                result.add(mapToTestedPositiveObject(encounter, patient));
            }
        }
        return result;
    }

    /**
     * Retrieves a list of patients linked to HIV care since the last fetch date
     */
    public List<SimpleObject> getLinkageToHIVCare(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        List<EncounterType> linkageEncounterTypes = Arrays.asList(
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS),
                MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_ENROLLMENT)
        );
        List<Form> linkageForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_LINKAGE),
                MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_ENROLLMENT)
        );

        // Fetch all encounters within the date
        List<Encounter> linkageToCareEncounters = encounterService.getEncounters(new EncounterSearchCriteria(
                null, null, fetchDate, null, null, linkageForms, linkageEncounterTypes,
                null, null, null, false
        ));

        // Process each encounter for linkage
        for (Encounter encounter : linkageToCareEncounters) {
            if (encounter == null) {
                log.warn("Encounter is null, skipping...");
                continue;
            }
            Patient patient = encounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no linked patient, skipping...");
                continue;
            }

            String artStartDate;

            CalculationResult artStartDateResults = EmrCalculationUtils
                    .evaluateForPatient(InitialArtStartDateCalculation.class, null, patient);

            if (artStartDateResults != null && artStartDateResults.getValue() != null) {
                artStartDate = formatDateTime((Date) artStartDateResults.getValue());
            } else {
                log.warn("ART Start Date Calculation returned null for patient: " + patient.getPatientId());
                artStartDate = null;
            }

            result.add(mapToLinkageObject(encounter, patient, artStartDate));

        }

        return result;
    }

    /**
     * Pregnant and postpartum women at high risk of turning HIV+
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> pregnantAndPostpartumAtHighRisk(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();
        Concept htsEntryPointQstn = conceptService.getConcept(160540);
        Concept htsEntryPointANC = conceptService.getConcept(160538);
        Concept htsEntryPointMAT = conceptService.getConcept(160456);
        Concept htsEntryPointPNC = conceptService.getConcept(1623);
        Concept htsScrRiskQstn = conceptService.getConcept(167163);
        Concept htsScrHighRiskResult = conceptService.getConcept(1408);
        Concept htsScrHighestRiskResult = conceptService.getConcept(167164);
        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsNegativeResult = conceptService.getConcept(HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID);

        if (htsFinalTestQuestion == null || htsNegativeResult == null) {
            log.error("Required HTS concepts are missing");
            return result;
        }

        // Get relevant encounter types
        List<EncounterType> htsEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));

        List<Form> testingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );

        // Build HTS encounter search criteria for HTS Eligibility screening
        EncounterSearchCriteria htsEligibilityScrSearchCriteria = new EncounterSearchCriteria(
                null, null, fetchDate, null, null, Collections.singletonList(MetadataUtils.existing(Form.class, HTS_ELIGIBILITY_FORM)), htsEncounterType, null, null, null, false
        );

        List<Encounter> screeningEncounters = encounterService.getEncounters(htsEligibilityScrSearchCriteria);
        for (Encounter htsScreeningEncounter : screeningEncounters) {
            if (htsScreeningEncounter == null) {
                log.warn("Encounter is null, skipping...");
                continue;
            }

            Patient patient = htsScreeningEncounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no HTS screening encounter patient, skipping...");
                continue;
            }
            if ((EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult) || EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult))) {
                // Build HTS encounter search criteria for HTS testing
                EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                        null, null, fetchDate, null, null, testingForms, htsEncounterType,
                        null, null, null, false
                );
                List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
                for (Encounter htsTestEncounter : htsTestEncounters) {
                    if (htsTestEncounter != null && htsTestEncounter.getPatient().equals(patient)) {
                        if (EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsFinalTestQuestion, htsNegativeResult) && (EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointANC) || EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointMAT) || EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointPNC))) {
                            result.add(mapToPregnantAndPostpartumAtHighRiskObject(htsScreeningEncounter, patient));
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        if (fetchDate.after(new Date())) {
            throw new IllegalArgumentException("Fetch date cannot be in the future.");
        }

        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();

        // HTS Concepts
        Concept htsEntryPointQstn = conceptService.getConcept(160540);
        Concept htsEntryPointANC = conceptService.getConcept(160538);
        Concept htsEntryPointMAT = conceptService.getConcept(160456);
        Concept htsEntryPointPNC = conceptService.getConcept(1623);
        Concept htsScrRiskQstn = conceptService.getConcept(167163);
        Concept htsScrHighRiskResult = conceptService.getConcept(1408);
        Concept htsScrHighestRiskResult = conceptService.getConcept(167164);
        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsNegativeResult = conceptService.getConcept(HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID);
        Concept prepRegimenConcept = conceptService.getConcept(PrEP_REGIMEN_CONCEPT_ID);

        if (htsFinalTestQuestion == null || htsNegativeResult == null || prepRegimenConcept == null) {
            log.error("Required HTS or PrEP concepts are missing");
            return result;
        }

        // Encounter Types
        List<EncounterType> htsEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));
        List<EncounterType> prepInitialFUPEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, PrEP_INITIAl_FUP_ENCOUNTER));

        List<Form> testingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );
        // Fetch all HTS test encounters and map them to patients
        List<Encounter> allHTSEncounters = encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null, testingForms, htsEncounterType, null, null, null, false));
        Map<Patient, List<Encounter>> htsTestEncountersMap = allHTSEncounters.stream()
                .collect(Collectors.groupingBy(Encounter::getPatient));

        // Fetch all PrEP encounters and map them to patients
        List<Encounter> allPrepEncounters = encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null, Collections.singletonList(MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM)), prepInitialFUPEncounterType, null, null, null, false));
        Map<Patient, List<Encounter>> prepEncountersMap = allPrepEncounters.stream()
                .collect(Collectors.groupingBy(Encounter::getPatient));

        // Fetch HTS Screening encounters
        for (Encounter htsScreeningEncounter : encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null,
                        Collections.singletonList(MetadataUtils.existing(Form.class, HTS_ELIGIBILITY_FORM)), htsEncounterType, null, null, null, false))) {

            if (htsScreeningEncounter == null || htsScreeningEncounter.getPatient() == null) continue;
            Patient patient = htsScreeningEncounter.getPatient();

            // Check if patient is at high risk
            boolean isHighRisk = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult);
            boolean isHighestRisk = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);
            if (!isHighRisk && !isHighestRisk) continue;

            // Check if patient has an HTS test encounter
            for (Encounter htsEncounter : htsTestEncountersMap.getOrDefault(patient, Collections.emptyList())) {
                boolean testedNegative = EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsFinalTestQuestion, htsNegativeResult);
                boolean hasEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointANC) ||
                        EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointMAT) ||
                        EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointPNC);

                if (testedNegative && hasEntryPoint) {
                    //  Check if the patient has a PrEP initiation encounter
                    List<Encounter> patientPrepEncounters = prepEncountersMap.getOrDefault(patient, Collections.emptyList());
                    if (!patientPrepEncounters.isEmpty()) {
                        // Get PrEP Identifier
                        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, PrEP_NUMBER_IDENTIFIER_TYPE_UUID);
                        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
                        String prepNumber = prepIdentifier != null ? prepIdentifier.getIdentifier() : null;

                        // Get PrEP Regimen
                        String prepRegimen = null;
                        for (Encounter prepEncounter : patientPrepEncounters) {
                            for (Obs obs : prepEncounter.getObs()) {
                                if (obs.getConcept().equals(prepRegimenConcept) && obs.getValueCoded() != null) {
                                    prepRegimen = obs.getValueCoded().getName().getName();
                                    break;
                                }
                            }
                            if (prepRegimen != null) break; // Stop checking other encounters if we found a regimen
                        }
                        // Add patient to the result
                        result.add(mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(htsEncounter, patient, prepNumber, prepRegimen));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generates the case surveillance payload for visualization metrics.
     */
    public List<Map<String, Object>> generateCaseSurveillancePayload(Date fetchDate) {
        List<Map<String, Object>> payload = new ArrayList<>();

        // Tested HIV-positive data as "new_case"
        List<SimpleObject> testedPositive = testedHIVPositive(fetchDate);
        for (SimpleObject tested : testedPositive) {
            payload.add(mapToNewStructure(tested, "new_case"));
        }

        // Linked to HIV care data as "linked_case"
        List<SimpleObject> linkedToHIVCare = getLinkageToHIVCare(fetchDate);
        for (SimpleObject linked : linkedToHIVCare) {
            payload.add(mapToNewStructure(linked, "linked_case"));
        }

        // Pregnant and postpartum at high risk as "at_risk_pbfw"
        List<SimpleObject> pregnantAndPostpartumAtHighRisk = pregnantAndPostpartumAtHighRisk(fetchDate);
        for (SimpleObject highRisk : pregnantAndPostpartumAtHighRisk) {
            payload.add(mapToNewStructure(highRisk, "at_risk_pbfw"));
        }

        // Pregnant and postpartum at high risk as "at_risk_pbfw"
        List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP = pregnantAndPostpartumAtHighRiskLinkedToPrEP(fetchDate);
        for (SimpleObject highRiskLinkedToPrep : pregnantAndPostpartumAtHighRiskLinkedToPrEP) {
            payload.add(mapToNewStructure(highRiskLinkedToPrep, "prep_linked_at_risk_pbfw"));
        }

        return payload;
    }

    private Map<String, Object> mapToNewStructure(SimpleObject source, String eventType) {
        Map<String, Object> client = new HashMap<>();
        Map<String, Object> event = new HashMap<>();

        // Ensure proper value assignment and casting for "client" object
        client.put("county", source.get("county") == null ? null : String.valueOf(source.get("county"))); // Ensure JSON-safe strings
        client.put("subCounty", source.get("subCounty") == null ? null : String.valueOf(source.get("subCounty")));
        client.put("ward", source.get("ward") == null ? null : String.valueOf(source.get("ward")));
        client.put("patientPk", source.get("patientId") == null ? null : Integer.valueOf(source.get("patientId").toString())); // Ensure Integer casting
        client.put("sex", source.get("sex") == null ? null : String.valueOf(source.get("sex")));
        client.put("dob", source.get("dob") == null ? null : String.valueOf(source.get("dob"))); // Format Date as String

        // Ensure proper value assignment and casting for "event" object
        event.put("mflCode", source.get("mflCode") == null ? null : Integer.valueOf(source.get("mflCode").toString())); // Ensure Integer casting
        event.put("createdAt", source.get("createdAt") == null ? null : String.valueOf(source.get("createdAt"))); // Date to String
        event.put("updatedAt", source.get("updatedAt") == null ? null : String.valueOf(source.get("updatedAt")));

        // Handle eventType-specific fields
        if ("new_case".equals(eventType)) {
            event.put("positiveHivTestDate", source.get("positiveHivTestDate") == null ? null : String.valueOf(source.get("positiveHivTestDate")));
        } else if ("linked_case".equals(eventType)) {
            event.put("artStartDate", source.get("artStartDate") == null ? null : String.valueOf(source.get("artStartDate")));
            event.put("positiveHivTestDate", source.get("positiveHivTestDate") == null ? null : String.valueOf(source.get("positiveHivTestDate")));
        } else if ("prep_linked_at_risk_pbfw".equals(eventType)) {
            event.put("prepNumber", source.get("prepNumber") == null ? null : String.valueOf(source.get("prepNumber")));
            event.put("prepRegimen", source.get("prepRegimen") == null ? null : String.valueOf(source.get("prepRegimen")));
            event.put("prepStartDate", source.get("prepStartDate") == null ? null : String.valueOf(source.get("prepStartDate")));
        }
        // Combine client and event with eventType
        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("eventType", eventType);
        result.put("event", event);

        return result;
    }

    public String sendCaseSurveillancePayload(List<Map<String, Object>> payload) {
        if (payload.isEmpty()) {
            log.info("No case surveillance data to transmit");
            return "";
        }
        // Retrieve the Bearer Token
        String bearerToken = getBearerToken();
        if (bearerToken.isEmpty()) {
            log.error("Failed to retrieve Bearer token. Aborting payload transmission.");
            return "Error: Bearer token is empty.";
        }

        // Create a Gson instance configured for null serialization
        Gson gson = new GsonBuilder().serializeNulls().create();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endpointUrl = getGlobalPropertyValue(BASE_CS_URL).trim();
            HttpPut putRequest = new HttpPut(endpointUrl);
            putRequest.setHeader("Content-Type", "application/json");
            putRequest.setHeader("Authorization", "Bearer " + bearerToken);

            // Serialize payload to JSON format
            String payloadJson = gson.toJson(payload);

            // Validate JSON serialization
            try {
                new com.google.gson.JsonParser().parse(payloadJson); // Validate JSON syntax
                log.info("Payload is valid JSON");
            } catch (com.google.gson.JsonSyntaxException e) {
                log.error("Invalid JSON format", e);
                return "Error: Invalid JSON - Payload validation failed";
            }

            putRequest.setEntity(new StringEntity(payloadJson, StandardCharsets.UTF_8));

            // Execute the PUT request
            try (CloseableHttpResponse response = httpClient.execute(putRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseContent = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                // Handle response codes
                switch (statusCode) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_CREATED:
                        log.info("Case surveillance payload sent successfully. Response: {}", responseContent);
                        return "Success: Payload sent. Response: " + responseContent;
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        log.error("Case surveillance Bad Request. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Bad Request. Response: " + responseContent;
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        log.error("Case surveillance Unauthorized. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Unauthorized access. Response: " + responseContent;
                    default:
                        log.error("Case surveillance Unexpected Error. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Unexpected failure. Status Code: " + statusCode + ". Response: " + responseContent;
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while sending case surveillance payload: {}", e.getMessage(), e);
            return "Error: Exception occurred - " + e.getMessage();
        }
    }

    public String processAndSendCaseSurveillancePayload(Date fetchDate) {
        List<Map<String, Object>> payload = generateCaseSurveillancePayload(fetchDate);
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String jsonPayload = gson.toJson(payload); // Serialize to JSON
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            log.warn("No case surveillance data found to send for the given date: {}", fetchDate);
        }
        return sendCaseSurveillancePayload(payload);
    }

}
