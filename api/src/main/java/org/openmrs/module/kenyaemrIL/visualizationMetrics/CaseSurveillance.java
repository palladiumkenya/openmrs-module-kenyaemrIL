package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openmrs.PersonAddress;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Obs;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.BASE_CS_URL;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.getBearerToken;

public class CaseSurveillance {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillance.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    // Utility method for null-safe string extraction
    private static String safeGetField(PersonAddress address, Function<PersonAddress, String> mapper) {
        return (address != null) ? mapper.apply(address) : null;
    }

    // Utility method for formatting dates
    private static String formatDateTime(Date date) {
        return (date != null) ? new SimpleDateFormat(DATE_TIME_FORMAT).format(date) : null;
    }
    private static String formatDate(Date date) {
        return (date != null) ? new SimpleDateFormat(DATE_FORMAT).format(date) : null;
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private static SimpleObject mapToTestedPositiveObject(Encounter encounter, Patient patient, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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
                "dateTestedHIV", formatDateTime(encounter.getEncounterDatetime())
        );
    }

    // Utility method for creating structured SimpleObject for patients linked to care
    private SimpleObject mapToLinkageObject(Encounter encounter, Patient patient, String artStartDate, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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
                "dateTestedHIV", "",
                "artStartDate", artStartDate
        );
    }

    // Utility method for creating structured SimpleObject for tested HIV-positive patients
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskObject(Encounter encounter, Patient patient, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(Encounter encounter, Patient patient, String prepNumber, String prepRegimen, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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

    // Utility method for creating structured SimpleObject for VL eligibility variables
    private SimpleObject mapToVlEligibilityObject(Encounter encounter, Patient patient, String artStartDate, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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
                "positiveHivTestDate", formatDateTime(encounter.getEncounterDatetime()),
                "artStartDate", artStartDate,
                "pregnancyStatus", "",
                "breastFeedingStatus", "",
                "lastVlResults", "",
                "lastVlOrderDate", "",
                "lastVlResultsDate", ""
        );
    }

    // Utility method for creating structured SimpleObject for VL eligibility variables
    private SimpleObject mapToEACObject(Encounter encounter, Patient patient, String artStartDate, Date fetchDate) {
        PersonAddress address = patient.getPersonAddress();
        String sex = patient.getGender();
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
                "positiveHivTestDate", formatDateTime(encounter.getEncounterDatetime()),
                "artStartDate", artStartDate,
                "pregnancyStatus", "",
                "breastFeedingStatus", "",
                "lastVlResults", "",
                "lastVlOrderDate", "",
                "lastVlResultsDate", "",
                "lastEacEncounterDate", ""
        );
    }

    /**
     * Retrieves a list of patients tested HIV-positive within the specified date range.
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
            System.out.println("---Linkage encounters: " + linkageToCareEncounters.size() + ":  Encounters-> " + linkageToCareEncounters);
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
    public List<SimpleObject> pregnantAndPostpartumAtHighRisk(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
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
            }
        }

        return result;
    }

    /**
     * Pregnant and postpartum women at high risk of turning HIV+
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
        String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
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

        String PrEP_INITIATION_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
        // Get relevant encounter types
        List<EncounterType> htsEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));
        List<EncounterType> prepEnrollmentEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, PrEP_INITIATION_ENCOUNTER));

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
            boolean isHighRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult);
            boolean isHighestRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);

            if (isHighRiskClient || isHighestRiskClient) {
                // Build HTS encounter search criteria for HTS testing
                EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                        null, null, fetchDate, null, null, testingForms, htsEncounterType,
                        null, null, null, false
                );
                List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
                for (Encounter htsTestEncounter : htsTestEncounters) {
                    if (htsTestEncounter != null && htsTestEncounter.getPatient().equals(patient)) {
                        System.out.println("--screened and has test" + patient.getPatientId());
                        boolean testedNegativeForHIV = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsFinalTestQuestion, htsNegativeResult);
                        boolean ancHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointANC);
                        boolean matHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointMAT);
                        boolean pncHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointPNC);

                        if (testedNegativeForHIV && (ancHTSEntryPoint || matHTSEntryPoint || pncHTSEntryPoint)) {
                            EncounterSearchCriteria startedOnPrEPSearchCriteria = new EncounterSearchCriteria(
                                    null, null, fetchDate, null, null, Collections.singletonList(MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM)), prepEnrollmentEncounterType, null, null, null, false
                            );

                            List<Encounter> prepEnrollmentEncounters = encounterService.getEncounters(startedOnPrEPSearchCriteria);
                            if (!prepEnrollmentEncounters.isEmpty()) {
                                for (Encounter prepEnrollmentEncounter : prepEnrollmentEncounters) {

                                    if (prepEnrollmentEncounter.getPatient().equals(patient)) {
                                        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, "ac64e5cb-e3e2-4efa-9060-0dd715a843a1");
                                        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
                                        String prepNumber = prepIdentifier != null ? prepIdentifier.getIdentifier() : null;
                                        String prepRegimen = null;
                                        Set<Obs> obsSet = prepEnrollmentEncounter.getObs();
                                        for (Obs obs : obsSet) {
                                            if (obs.getConcept().getConceptId() == 164515) { // Check concept ID
                                                Concept valueCoded = obs.getValueCoded();
                                                if (valueCoded != null) {
                                                    prepRegimen = valueCoded.getName().getName(); // Get display name
                                                    System.out.println("Prep regimen name :      " + valueCoded.getName());
                                                    break; // Exit loop once found
                                                }
                                            }
                                        }

                                        result.add(mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(prepEnrollmentEncounter, patient, prepNumber, prepRegimen, fetchDate));
                                        break;
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * VL eligibility variables
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> eligibleForVl(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
        String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
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

        String PrEP_INITIATION_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
        // Get relevant encounter types
        List<EncounterType> htsEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));
        List<EncounterType> prepEnrollmentEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, PrEP_INITIATION_ENCOUNTER));

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
            boolean isHighRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult);
            boolean isHighestRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);

            if (isHighRiskClient || isHighestRiskClient) {
                // Build HTS encounter search criteria for HTS testing
                EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                        null, null, fetchDate, null, null, testingForms, htsEncounterType,
                        null, null, null, false
                );
                List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
                for (Encounter htsTestEncounter : htsTestEncounters) {
                    if (htsTestEncounter != null && htsTestEncounter.getPatient().equals(patient)) {
                        System.out.println("--screened and has test" + patient.getPatientId());
                        boolean testedNegativeForHIV = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsFinalTestQuestion, htsNegativeResult);
                        boolean ancHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointANC);
                        boolean matHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointMAT);
                        boolean pncHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointPNC);

                        if (testedNegativeForHIV && (ancHTSEntryPoint || matHTSEntryPoint || pncHTSEntryPoint)) {
                            EncounterSearchCriteria startedOnPrEPSearchCriteria = new EncounterSearchCriteria(
                                    null, null, fetchDate, null, null, Collections.singletonList(MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM)), prepEnrollmentEncounterType, null, null, null, false
                            );

                            List<Encounter> prepEnrollmentEncounters = encounterService.getEncounters(startedOnPrEPSearchCriteria);
                            if (!prepEnrollmentEncounters.isEmpty()) {
                                for (Encounter prepEnrollmentEncounter : prepEnrollmentEncounters) {

                                    if (prepEnrollmentEncounter.getPatient().equals(patient)) {
                                        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, "ac64e5cb-e3e2-4efa-9060-0dd715a843a1");
                                        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
                                        String prepNumber = prepIdentifier != null ? prepIdentifier.getIdentifier() : null;
                                        String prepRegimen = null;
                                        Set<Obs> obsSet = prepEnrollmentEncounter.getObs();
                                        for (Obs obs : obsSet) {
                                            if (obs.getConcept().getConceptId() == 164515) { // Check concept ID
                                                Concept valueCoded = obs.getValueCoded();
                                                if (valueCoded != null) {
                                                    prepRegimen = valueCoded.getName().getName(); // Get display name
                                                    System.out.println("Prep regimen name :      " + valueCoded.getName());
                                                    break; // Exit loop once found
                                                }
                                            }
                                        }

                                        result.add(mapToVlEligibilityObject(prepEnrollmentEncounter, patient, "", fetchDate));
                                        break;
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        return result;
    }

    public List<SimpleObject> enhancedAdherence(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
        String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
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

        String PrEP_INITIATION_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
        // Get relevant encounter types
        List<EncounterType> htsEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));
        List<EncounterType> prepEnrollmentEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, PrEP_INITIATION_ENCOUNTER));

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
            boolean isHighRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult);
            boolean isHighestRiskClient = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);

            if (isHighRiskClient || isHighestRiskClient) {
                // Build HTS encounter search criteria for HTS testing
                EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                        null, null, fetchDate, null, null, testingForms, htsEncounterType,
                        null, null, null, false
                );
                List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
                for (Encounter htsTestEncounter : htsTestEncounters) {
                    if (htsTestEncounter != null && htsTestEncounter.getPatient().equals(patient)) {
                        System.out.println("--screened and has test" + patient.getPatientId());
                        boolean testedNegativeForHIV = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsFinalTestQuestion, htsNegativeResult);
                        boolean ancHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointANC);
                        boolean matHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointMAT);
                        boolean pncHTSEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsTestEncounter, htsEntryPointQstn, htsEntryPointPNC);

                        if (testedNegativeForHIV && (ancHTSEntryPoint || matHTSEntryPoint || pncHTSEntryPoint)) {
                            EncounterSearchCriteria startedOnPrEPSearchCriteria = new EncounterSearchCriteria(
                                    null, null, fetchDate, null, null, Collections.singletonList(MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM)), prepEnrollmentEncounterType, null, null, null, false
                            );

                            List<Encounter> prepEnrollmentEncounters = encounterService.getEncounters(startedOnPrEPSearchCriteria);
                            if (!prepEnrollmentEncounters.isEmpty()) {
                                for (Encounter prepEnrollmentEncounter : prepEnrollmentEncounters) {

                                    if (prepEnrollmentEncounter.getPatient().equals(patient)) {
                                        PatientIdentifierType prepIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, "ac64e5cb-e3e2-4efa-9060-0dd715a843a1");
                                        PatientIdentifier prepIdentifier = patient.getPatientIdentifier(prepIdentifierType);
                                        String prepNumber = prepIdentifier != null ? prepIdentifier.getIdentifier() : null;
                                        String prepRegimen = null;
                                        Set<Obs> obsSet = prepEnrollmentEncounter.getObs();
                                        for (Obs obs : obsSet) {
                                            if (obs.getConcept().getConceptId() == 164515) { // Check concept ID
                                                Concept valueCoded = obs.getValueCoded();
                                                if (valueCoded != null) {
                                                    prepRegimen = valueCoded.getName().getName(); // Get display name
                                                    System.out.println("Prep regimen name :      " + valueCoded.getName());
                                                    break; // Exit loop once found
                                                }
                                            }
                                        }

                                        result.add(mapToEACObject(prepEnrollmentEncounter, patient, "", fetchDate));
                                        break;
                                    }
                                }
                            }

                        }
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

   /*     List<SimpleObject> eligibleForVl = eligibleForVl(fetchDate);
        for (SimpleObject eligibleForVlVariables : eligibleForVl) {
            payload.add(mapToNewStructure(eligibleForVlVariables, "eligible_for_vl"));
        }
        List<SimpleObject> unsuppressedWithEAC = eligibleForVl(fetchDate);
        for (SimpleObject eac : unsuppressedWithEAC) {
            payload.add(mapToNewStructure(eac, "unsuppressed_viral_load"));
        }*/
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
            event.put("positiveHivTestDate", source.get("dateTestedHIV") == null ? null : String.valueOf(source.get("dateTestedHIV")));
        } else if ("linked_case".equals(eventType)) {
            event.put("artStartDate", source.get("artStartDate") == null ? null : String.valueOf(source.get("artStartDate")));
            event.put("positiveHivTestDate", source.get("positiveHivTestDate") == null ? null : String.valueOf(source.get("positiveHivTestDate")));
        } /*else if ("at_risk_pbfw".equals(eventType)) {
            event.put("htsDate", source.get("htsDate") == null ? null : String.valueOf(source.get("htsDate")));
        } */ else if ("prep_linked_at_risk_pbfw".equals(eventType)) {
            event.put("prepNumber", source.get("prepNumber") == null ? null : String.valueOf(source.get("prepNumber")));
            event.put("prepRegimen", source.get("prepRegimen") == null ? null : String.valueOf(source.get("prepRegimen")));
            event.put("prepStartDate", source.get("prepStartDate") == null ? null : String.valueOf(source.get("prepStartDate")));
        } else if ("eligible_for_vl".equals(eventType)) {
            event.put("positiveHivTestDate", source.get("positiveHivTestDate") == null ? null : String.valueOf(source.get("positiveHivTestDate")));
            event.put("artStartDate", source.get("artStartDate") == null ? null : String.valueOf(source.get("artStartDate")));
            event.put("pregnancyStatus", source.get("pregnancyStatus") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("breastFeedingStatus", source.get("breastFeedingStatus") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlOrderDate", source.get("lastVlOrderDate") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlResults", source.get("lastVlResults") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlResultsDate", source.get("lastVlResultsDate") == null ? null : String.valueOf(source.get("prepStartDate")));
        } else if ("unsuppressed_viral_load".equals(eventType)) {
            event.put("positiveHivTestDate", source.get("positiveHivTestDate") == null ? null : String.valueOf(source.get("positiveHivTestDate")));
            event.put("artStartDate", source.get("artStartDate") == null ? null : String.valueOf(source.get("artStartDate")));
            event.put("pregnancyStatus", source.get("pregnancyStatus") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("breastFeedingStatus", source.get("breastFeedingStatus") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlOrderDate", source.get("lastVlOrderDate") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlResults", source.get("lastVlResults") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastVlResultsDate", source.get("lastVlResultsDate") == null ? null : String.valueOf(source.get("prepStartDate")));
            event.put("lastEacEncounterDate", source.get("lastEacEncounterDate") == null ? null : String.valueOf(source.get("prepStartDate")));
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
            String payloadJson = gson.toJson(payload); // Convert payload to JSON, including nulls

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
                System.out.println("responseContent------------" + responseContent);
                System.out.println("statusCode------------" + statusCode);
                // Handle response codes
                switch (statusCode) {
                    case HttpURLConnection.HTTP_OK:
                    case HttpURLConnection.HTTP_CREATED:
                        log.info("Payload sent successfully. Response: {}", responseContent);
                        System.out.println("Processed successfully------------" + statusCode);
                        return "Success: Payload sent. Response: " + responseContent;
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                        log.error("Bad Request. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Bad Request. Response: " + responseContent;
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        log.error("Unauthorized. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Unauthorized access. Response: " + responseContent;
                    default:
                        log.error("Unexpected Error. Status Code: {}. Response: {}", statusCode, responseContent);
                        return "Error: Unexpected failure. Status Code: " + statusCode + ". Response: " + responseContent;
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while sending case surveillance payload: {}", e.getMessage(), e);
            return "Error: Exception occurred - " + e.getMessage();
        }
    }

    public String processAndSendCaseSurveillancePayload(Date fetchDate) {
        // Step 1: Generate the payload
        List<Map<String, Object>> payload = generateCaseSurveillancePayload(fetchDate);
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String jsonPayload = gson.toJson(payload); // Serialize to JSON

        log.debug("Serialized JSON Payload: " + jsonPayload);
        System.out.println("Case Surveillance Payload Using System.out:=====================" + jsonPayload);
        // Step 2: Check if the payload is valid (e.g., not null or empty)
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            log.warn("No case surveillance data found to send for the given date: {}", fetchDate);
            //   return log.warn("No case surveillance data found to send for the given date: {}", fetchDate);
        }
        System.out.println("JSON PAYLOAD: " + jsonPayload);
        // Step 3: Send the payload
        return sendCaseSurveillancePayload(payload);
    }

}
