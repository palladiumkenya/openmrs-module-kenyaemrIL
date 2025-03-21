package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.json.simple.JSONObject;
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
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

public class CaseSurveillance {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillance.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    // Utility method for null-safe string extraction
    private static String safeGetField(PersonAddress address, Function<PersonAddress, String> mapper) {
        return (address != null) ? mapper.apply(address) : null;
    }

    // Utility method for formatting dates
    private static String formatDate(Date date) {
        return (date != null) ? new SimpleDateFormat(DATE_FORMAT).format(date) : null;
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private static SimpleObject mapToTestedPositiveObject(Encounter encounter, Patient patient, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        return SimpleObject.create(
                "createdAt", formatDate(encounter.getDateCreated()),
                "updatedAt", formatDate(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "geoLocation", null, // Update logic to calculate location if applicable
                "dob", formatDate(patient.getBirthdate()),
                "sex", patient.getGender(),
                "dateTestedHIV", formatDate(encounter.getEncounterDatetime()),
                "timeStamp", formatDate(fetchDate)
        );
    }

    // Utility method for creating structured SimpleObject for patients linked to care
    private static SimpleObject mapToLinkageObject(Encounter encounter, Patient patient, String artStartDate, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        return SimpleObject.create(
                "createdAt", formatDate(encounter.getDateCreated()),
                "updatedAt", formatDate(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "geoLocation", null, // Update logic to calculate location if applicable
                "dob", formatDate(patient.getBirthdate()),
                "sex", patient.getGender(),
                "artStartDate", artStartDate,
                "timeStamp", formatDate(fetchDate)
        );
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private static SimpleObject mapToPregnantAndPostpartumAtHighRiskObject(Encounter encounter, Patient patient, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        return SimpleObject.create(
                "createdAt", formatDate(encounter.getDateCreated()),
                "updatedAt", formatDate(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "geoLocation", null, // Update logic to calculate location if applicable
                "dob", formatDate(patient.getBirthdate()),
                "sex", patient.getGender(),
                "timestamp", formatDate(fetchDate),
                "htsDate", formatDate(encounter.getEncounterDatetime())
        );
    }

    // Utility method for creating structured SimpleObject for VL eligibility variables
    private static SimpleObject mapToVLEligibilityObject(Encounter encounter, Patient patient, String artStartDate, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        return SimpleObject.create(
                "createdAt", formatDate(encounter.getDateCreated()),
                "updatedAt", formatDate(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "geoLocation", null, // Update logic to calculate location if applicable
                "dob", formatDate(patient.getBirthdate()),
                "sex", patient.getGender(),
                "artStartDate", artStartDate,
                "timeStamp", formatDate(fetchDate)
        );
    }

    /**
     * Retrieves a list of patients tested HIV-positive within the specified date range.
     */
    public static List<SimpleObject> testedHIVPositive(Date fetchDate) {
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
            System.out.println("::::::::::::Fetch date: " + fetchDate);
            System.out.println("---Hiv+ encounters: " + encounters.size() + ":  Encounters-> " + encounters);
            Patient patient = encounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no HIV+ patient, skipping...");
                continue;
            }
            if (EmrUtils.encounterThatPassCodedAnswer(encounter, htsFinalTestQuestion, htsPositiveResult)) {
                result.add(mapToTestedPositiveObject(encounter, patient, fetchDate));
            }
        }
        System.out.println(":::::::result size at HIV+::::::: " + result.size());
        return result;
    }

    /**
     * Retrieves a list of patients linked to HIV care within the specified date range.
     */
    public static List<SimpleObject> getLinkageToHIVCare(Date fetchDate) {
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
        List<Encounter> encounters = encounterService.getEncounters(new EncounterSearchCriteria(
                null, null, fetchDate, null, null, linkageForms, linkageEncounterTypes,
                null, null, null, false
        ));

        // Process each encounter for linkage
        for (Encounter encounter : encounters) {
            if (encounter == null) {
                log.warn("Encounter is null, skipping...");
                continue;
            }
            System.out.println("---Linkage encounters: " + encounters.size() + ":  Encounters-> " + encounters);
            Patient patient = encounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no linked patient, skipping...");
                continue;
            }

            String artStartDate;

            CalculationResult artStartDateResults = EmrCalculationUtils
                    .evaluateForPatient(InitialArtStartDateCalculation.class, null, patient);

            if (artStartDateResults != null && artStartDateResults.getValue() != null) {
                artStartDate = formatDate((Date) artStartDateResults.getValue());
            } else {
                log.warn("ART Start Date Calculation returned null for patient: " + patient.getPatientId());
                artStartDate = null; // Or set a default value if needed
            }

            result.add(mapToLinkageObject(encounter, patient, artStartDate, fetchDate));

        }

        return result;
    }

    /**
     * Pregnant and postpartum women at high risk of turning HIV+
     *
     * @param fetchDate
     * @return
     */
    public static List<SimpleObject> pregnantAndPostpartumAtHighRisk(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();
// Currently in Prep qstn: 165203 ans : != 1065
        //risk qstn : 167163AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA : ans 1408AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA or  167164AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
// hts entry point: 160540 ans: 160538 or 160456 or 1623

        Concept htsEntryPointQstn = conceptService.getConcept(160540);
        Concept htsEntryPointANC = conceptService.getConcept(160538);
        Concept htsEntryPointMAT = conceptService.getConcept(160456);
        Concept htsEntryPointPNC = conceptService.getConcept(1623);
        Concept htsScrPrEPQstn = conceptService.getConcept(165203);
        Concept htsScrPrEPResult = conceptService.getConcept(1065);
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
            System.out.println("Pregnant and postpartum High risk: " + screeningEncounters.size() + ":  Encounters-> " + screeningEncounters);
            Patient patient = htsScreeningEncounter.getPatient();
            if (patient == null) {
                log.warn("Encounter has no HTS screening encounter patient, skipping...");
                continue;
            }
            //todo check whether we need to check prep status (from HTS screening and PrEP enrollment)

            if ((EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult) || EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult))) {
                // Build HTS encounter search criteria for HTS testing
                EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                        null, null, fetchDate, null, null, testingForms, htsEncounterType,
                        null, null, null, false
                );
                List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
                for (Encounter htsTestEncounter : htsTestEncounters) {
                    if (htsTestEncounter != null && htsTestEncounter.getPatient().equals(patient)) {
                        System.out.println("--screened and has test" + patient.getPatientId());
                        if (EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsFinalTestQuestion, htsNegativeResult) && (EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointANC) || EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointMAT) || EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointPNC))) {
                            System.out.println("---Pregnant and postpartum High risk encounters: " + htsTestEncounters.size() + ":  Encounters-> " + htsTestEncounters);
                            result.add(mapToPregnantAndPostpartumAtHighRiskObject(htsScreeningEncounter, patient, fetchDate));
                            break;
                        }
                    }
                }
                //result.add(mapToPregnantAndPostpartumAtHighRiskObject(encounter, patient));
            }
        }

        return result;
    }

    /**
     * Generates the case surveillance payload for visualization metrics.
     */
    public static List<Map<String, Object>> generateCaseSurveillancePayload(Date fetchDate) {
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

        // Pregnant and postpartum at high risk as "high_risk_case"
        List<SimpleObject> pregnantAndPostpartumAtHighRisk = pregnantAndPostpartumAtHighRisk(fetchDate);
        for (SimpleObject highRisk : pregnantAndPostpartumAtHighRisk) {
            payload.add(mapToNewStructure(highRisk, "high_risk_case"));
        }

        System.out.println("Case Surveillance Payload: " + payload);
        return payload;

   /*     JSONObject payload = new JSONObject();

        // Add tested HIV-positive data
        List<SimpleObject> testedPositive = testedHIVPositive(fetchDate);
        payload.put("testedPositive", testedPositive);

        // Add linkage to HIV care data
        List<SimpleObject> linkedToHIVCare = getLinkageToHIVCare(fetchDate);
        payload.put("linkedToHIVCare", linkedToHIVCare);

        List<SimpleObject> pregnantAndPostpartumAtHighRisk = pregnantAndPostpartumAtHighRisk(fetchDate);
        payload.put("pregnantAndPostpartumAtHighRisk", pregnantAndPostpartumAtHighRisk);

        System.out.println("Case Surveillance Payload:=====================" + payload);
        return payload;*/
    }
    private static Map<String, Object> mapToNewStructure(SimpleObject source, String eventType) {
        Map<String, Object> client = new HashMap<>();
        Map<String, Object> event = new HashMap<>();

        // Creating client object
        client.put("county", source.get("county"));
        client.put("subCounty", source.get("subCounty"));
        client.put("ward", source.get("ward"));
        client.put("patientPk", source.get("patientId")); // patientPk = patientId
        client.put("sex", source.get("sex"));
        client.put("dob", source.get("dob"));

        // Creating event object
        event.put("mflCode", source.get("mflCode"));
        event.put("createdAt", source.get("createdAt"));
        event.put("updatedAt", source.get("updatedAt"));

        // Conditional fields for specific events
        if (eventType.equals("new_case")) {
            event.put("positiveHivTestDate", source.get("dateTestedHIV"));
        } else if (eventType.equals("linked_case")) {
            event.put("artStartDate", source.get("artStartDate"));
        } else if (eventType.equals("high_risk_case")) {
            event.put("htsDate", source.get("htsDate"));
        }

        // Combine client and event with eventType
        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("eventType", eventType);
        result.put("event", event);

        return result;
    }
}
