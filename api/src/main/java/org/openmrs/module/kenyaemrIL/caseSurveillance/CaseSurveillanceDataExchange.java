package org.openmrs.module.kenyaemrIL.caseSurveillance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.TestOrder;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.util.HtsConstants;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.kenyaemrIL.dmi.dmiUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.OrderSearchCriteria;
import org.openmrs.parameter.OrderSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.*;

public class CaseSurveillanceDataExchange {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillanceDataExchange.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String PrEP_INITIAl_FUP_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
    private static final String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
    private static final String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
    private static final String PrEP_NUMBER_IDENTIFIER_TYPE_UUID = "ac64e5cb-e3e2-4efa-9060-0dd715a843a1";
    private static final int PrEP_REGIMEN_CONCEPT_ID = 164515;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    EncounterService encounterService = Context.getEncounterService();

    Concept vlUndetectableConcept = MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_VIRAL_LOAD_QUALITATIVE);

    Concept vlQuatitativeConcept = MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_VIRAL_LOAD);

    EncounterType hivConsultationEncounterType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
    EncounterType mchMotherEncounterType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION);
    EncounterType labResultsEncounterType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.LAB_RESULTS);
    EncounterType labOrderEncounterType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.LAB_ORDER);

    Form greencardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);
    Form ancForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANTENATAL_VISIT);
    Form pncForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT);

    private static final Concept YES = Dictionary.getConcept(Dictionary.YES);
    private static final Concept MIXED_FEEDING = Dictionary.getConcept(Dictionary.MIXED_FEEDING);
    private static final Concept EXCLUSIVE_BREASTFEEDING = Dictionary.getConcept(Dictionary.BREASTFED_EXCLUSIVELY);

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

    // Utility method for creating structured SimpleObject for Pregnant and postpartum women at high risk
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

    // Utility method for creating structured SimpleObject for Pregnant and postpartum women at high risk linked to PrEP
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

    // Utility method for creating structured SimpleObject for VL Eligibility variables
    private SimpleObject mapToVlEligibilityObject(Encounter encounter, String upn, boolean pregnant, boolean breastfeeding, String vlResult, Date vlresultDate, Date vlOrderDate, String artStartDate) {
        Patient patient = encounter.getPatient();
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
                "pregnancyStatus", pregnant ? "Pregnant" : null,
                "breastFeedingStatus", breastfeeding ? "Yes" : null,
                "lastVlResults", vlResult,
                "positiveHivTestDate", null,
                "visitDate", formatDateTime(encounter.getEncounterDatetime()),
                "artStartDate", artStartDate,
                "lastVlOrderDate", formatDateTime(vlOrderDate),
                "lastVlResultsDate", formatDateTime(vlresultDate)
        );
    }

    // Utility method for creating structured SimpleObject for Enhanced adherence
    private SimpleObject mapToEacObject(Encounter encounter, Patient patient, String upn) {
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
                "pregnancyStatus", null,
                "breastFeedingStatus", null,
                "lastVlResults", null,
                "positiveHivTestDate", null,
                "visitDate", null,
                "lastEacEncounterDate", formatDateTime(encounter.getEncounterDatetime()),
                "artStartDate", null,
                "lastVlOrderDate", null,
                "lastVlResultsDate", null
        );
    }

    // Utility method for creating structured SimpleObject for HEI
    private SimpleObject mapToHEIObject(Encounter encounter, Patient patient, String heiNumber) {
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
                "heiId", heiNumber
        );
    }

    // Utility method for creating structured SimpleObject for HEI DNA PCR
    private SimpleObject mapToHEIDnaPcrObject(Encounter encounter, Patient patient, String heiNumber) {
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
                "heiId", heiNumber
        );
    }

    // Utility method for creating structured SimpleObject for HEI DNA PCR
    private SimpleObject mapToHEIWithoutOutcomesObject(Encounter encounter, Patient patient, String heiNumber) {
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
                "heiId", heiNumber
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

        List<EncounterType> linkageEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.DRUG_REGIMEN_EDITOR));
        List<Form> linkageForm = Collections.singletonList(MetadataUtils.existing(Form.class, CommonMetadata._Form.DRUG_REGIMEN_EDITOR));

        // Fetch all encounters within the date
        List<Encounter> linkageToCareEncounters = encounterService.getEncounters(new EncounterSearchCriteria(
                null, null, fetchDate, null, null, linkageForm, linkageEncounterType,
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

            String artStartDate = getArtStartDate(patient);
            if (artStartDate == null) {
                log.warn("Encounter has no ART start date, skipping..."); //todo review
                continue;
            }

            try {
                DateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
                Date artStartDateAsDate = dateFormat.parse(artStartDate);
                if (fetchDate.compareTo(artStartDateAsDate) <= 0) {
                    result.add(mapToLinkageObject(encounter, patient, artStartDate));
                }
            } catch (ParseException e) {
                log.error("Error parsing artStartDate: " + e.getMessage());
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
     * VL eligibility variables
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> eligibleForVl(Date fetchDate) {

        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }

        List<SimpleObject> result = new ArrayList<>();

        List<Encounter> latestEncounters = new ArrayList<>(getLatestEncounters(fetchDate));

        System.out.println("latestEncounters: " + latestEncounters.size());

        if (latestEncounters.isEmpty()) {
            log.warn("No eligible encounters found for VL eligibility, skipping...");
            return result;
        }

        String artStartDate;
        String upn;

        for (Encounter encounter : latestEncounters) {
            String vlResult = "";
            Date vlresultDate = null;
            Date vlOrderDate = null;
            boolean isPregnant = false;
            boolean isBreastFeeding = false;

            Patient patient = encounter.getPatient();

            if (patient == null) {
                log.warn("Encounter has no patient, skipping...");
                continue;
            }
            PatientWrapper patientWrapper = new PatientWrapper(patient);
            upn = patientWrapper.getUniquePatientNumber();

            if( upn == null || upn.isEmpty()) {
                log.warn("Not a CCC client, skipping...");
                continue;
            }

            artStartDate = getArtStartDate(patient);

            Encounter latestMCHEnc = patientWrapper.lastEncounter(mchMotherEncounterType);
            Encounter latestHIVConsultationEnc = patientWrapper.lastEncounter(hivConsultationEncounterType);

           // Encounter latestLabResultsEnc = patientWrapper.lastEncounter(labResultsEncounterType);
            Order latestVlOrder = getLatestVlOrderForPatient(patient); // todo check the impact of fetch date
            System.out.println("latestVlOrder for patient: "+patient.getPatientId() +": "+  latestVlOrder);

            Encounter latestVlResultsEnc = null;
            Encounter latestVlOrderEncounter = null;
            Obs latestVlOrderResultObs = null;

            if(latestVlOrder != null){
                vlOrderDate = latestVlOrder.getDateActivated();
                latestVlOrderEncounter = latestVlOrder.getEncounter();
                latestVlOrderResultObs = getLatestVlResultObs(patient, latestVlOrder);
                latestVlResultsEnc = latestVlOrderResultObs != null ? latestVlOrderResultObs.getEncounter(): null;
                System.out.println("latestVlOrderEncounter for patient: "+patient.getPatientId() +": "+  latestVlOrderEncounter);
                System.out.println("latestVlResultsEnc for patient: "+patient.getPatientId() +": "+  latestVlResultsEnc);
                System.out.println("latestVlOrderResultObs for patient: "+patient.getPatientId() +": "+  latestVlOrderResultObs);
            }

            //TODO Check whether we really need this check
         if(latestMCHEnc == null && latestHIVConsultationEnc == null && (latestVlOrderEncounter == null || latestVlOrderEncounter.getEncounterDatetime().compareTo(fetchDate) < 0)
                 && (latestVlResultsEnc == null || latestVlResultsEnc.getEncounterDatetime().compareTo(fetchDate) < 0)) {
             log.warn("No MCH, HIV consultation or viral load encounters found for patient: " + patient.getPatientId());
             continue;
         }
            Obs undetectableObs = patientWrapper.lastObs(vlUndetectableConcept);
            Obs qtyObs = patientWrapper.lastObs(vlQuatitativeConcept);
            if (latestVlOrderResultObs != null) {
                if (vlUndetectableConcept.equals(latestVlOrderResultObs.getConcept())) {
                    vlResult = undetectableObs.getValueCoded() != null ? undetectableObs.getValueCoded().getName().getName() : "";
                    vlresultDate = undetectableObs.getObsDatetime();
                } else if (vlQuatitativeConcept.equals(latestVlOrderResultObs.getConcept())) {
                    vlResult = qtyObs.getValueNumeric() != null ? qtyObs.getValueNumeric().toString() : "";
                    vlresultDate = qtyObs.getObsDatetime();
                }
            }

            if (latestHIVConsultationEnc != null && latestHIVConsultationEnc.equals(encounter)) {
                isPregnant = isPregnant(encounter);
                isBreastFeeding = isBreastFeeding(encounter);
            } else if (latestMCHEnc != null && latestMCHEnc.equals(encounter) && encounter.getForm() != null && ancForm.equals(encounter.getForm()) ){
                isPregnant = true;

            } else if (latestMCHEnc != null && latestMCHEnc.equals(encounter) && encounter.getForm() != null && pncForm.equals(encounter.getForm()) ){

                Obs infantFeedingObs = encounter.getObs().stream()
                        .filter(obs -> obs.getConcept().equals(Dictionary.getConcept(Dictionary.INFANT_FEEDING_METHOD)))
                        .findFirst()
                        .orElse(null);
                isBreastFeeding = infantFeedingObs != null && infantFeedingObs.getValueCoded() != null && (EXCLUSIVE_BREASTFEEDING.equals(infantFeedingObs.getValueCoded()) || MIXED_FEEDING.equals(infantFeedingObs.getValueCoded()));
            }

            result.add(mapToVlEligibilityObject(encounter, upn, isPregnant, isBreastFeeding, vlResult, vlresultDate, vlOrderDate, artStartDate));
        }

        System.out.println("result: " + result);

        return result;
    }
    /**
     * Patients with enhanced adherence
     *
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> enhancedAdherence(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }

        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        // Get relevant encounter types
        List<EncounterType> eacEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.ENHANCED_ADHERENCE));
        List<Form> eacForm = Collections.singletonList(MetadataUtils.existing(Form.class, HivMetadata._Form.ENHANCED_ADHERENCE_SCREENING));

        // Build EAC encounter search criteria
        EncounterSearchCriteria eacSearchCriteria = new EncounterSearchCriteria(
                null, null, fetchDate, null, null, eacForm, eacEncounterType, null, null, null, false
        );

        List<Encounter> eacEncounters = encounterService.getEncounters(eacSearchCriteria);

        if (!eacEncounters.isEmpty()) {
            eacEncounters.sort(Comparator.comparing(Encounter::getEncounterDatetime));
            Encounter eacEncounter = eacEncounters.get(eacEncounters.size() - 1);
            Patient patient = eacEncounter.getPatient();

            PatientIdentifierType upnIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, Metadata.IdentifierType.UNIQUE_PATIENT_NUMBER);
            PatientIdentifier upnIdentifier = patient.getPatientIdentifier(upnIdentifierType);
            String upn = upnIdentifier != null ? upnIdentifier.getIdentifier() : null;

            result.add(mapToEacObject(eacEncounter, patient, upn));
        }
        return result;
    }

    /**
     * HEI cohort
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> totalHEI(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        Date effectiveDate = Date.from(LocalDate.now().minusMonths(24).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        // Get relevant encounter types
        List<EncounterType> heiEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHCS_ENROLLMENT));
        List<Form> heiEnrollmentForm = Collections.singletonList(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_ENROLLMENT));

        // Build HEI encounter search criteria
        EncounterSearchCriteria heiSearchCriteria = new EncounterSearchCriteria(
                null, null, effectiveDate, null, null, heiEnrollmentForm, heiEncounterType, null, null, null, false
        );

        List<Encounter> heiEncounters = encounterService.getEncounters(heiSearchCriteria);
        if (!heiEncounters.isEmpty()) {
            for (Encounter heiEncounter : heiEncounters) {
                Patient patient = heiEncounter.getPatient();

                String heiNumber = getHEINumber(patient);
                if (patient.getBirthdate() != null && patient.getBirthdate().compareTo(effectiveDate) >= 0 && heiNumber != null) {
                    result.add(mapToHEIObject(heiEncounter, patient, heiNumber));
                }
            }
        }
        return result;
    }

    public List<SimpleObject> heiWithoutDnaPCRResults(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        Date effectiveDate = Date.from(LocalDate.now().minusMonths(24).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        // Get relevant encounter types
        List<EncounterType> heiEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHCS_ENROLLMENT));
        List<Form> heiEnrollmentForm = Collections.singletonList(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_ENROLLMENT));

        // Build HEI encounter search criteria
        EncounterSearchCriteria heiSearchCriteria = new EncounterSearchCriteria(
                null, null, effectiveDate, null, null, heiEnrollmentForm, heiEncounterType, null, null, null, false
        );

        List<Encounter> heiEncounters = encounterService.getEncounters(heiSearchCriteria);
        if (!heiEncounters.isEmpty()) {
            for (Encounter heiEncounter : heiEncounters) {
                Patient patient = heiEncounter.getPatient();

                String heiNumber = getHEINumber(patient);
                if (patient.getBirthdate() != null && patient.getBirthdate().compareTo(effectiveDate) >= 0 && heiNumber != null) {

                    PatientWrapper patientWrapper = new PatientWrapper(patient);

                    Obs obs = patientWrapper.lastObs(MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_DNA_POLYMERASE_CHAIN_REACTION_QUALITATIVE));

                    if (obs == null) {
                        result.add(mapToHEIDnaPcrObject(heiEncounter, patient, heiNumber));
                    }
                }
            }
        }
        return result;
    }

    /**public Encounter getLatestHIVConsultationEncounter() {
    return getLatestHIVConsultationEncounter(null);
}
     * HEIs without documented final Outcome
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> heiWithoutFinalOutcome(Date fetchDate) {
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        Date effectiveDate = Date.from(LocalDate.now().minusMonths(24).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        // Get relevant encounter types
        List<EncounterType> heiEncounterType = Collections.singletonList(MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHCS_ENROLLMENT));
        List<Form> heiEnrollmentForm = Collections.singletonList(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_ENROLLMENT));

        // Build HEI encounter search criteria
        EncounterSearchCriteria heiSearchCriteria = new EncounterSearchCriteria(
                null, null, effectiveDate, null, null, heiEnrollmentForm, heiEncounterType, null, null, null, false
        );
        List<Encounter> heiEncounters = encounterService.getEncounters(heiSearchCriteria);
        if (!heiEncounters.isEmpty()) {
            for (Encounter heiEncounter : heiEncounters) {
                Patient patient = heiEncounter.getPatient();
                String heiNumber = getHEINumber(patient);
                Integer ageInMonths = getAgeInMonths(patient.getBirthdate(), fetchDate);
                if (heiNumber != null && ageInMonths == 24) {
                    PatientWrapper patientWrapper = new PatientWrapper(patient);

                    Obs obs = patientWrapper.lastObs(MetadataUtils.existing(Concept.class, Metadata.Concept.HEI_OUTCOME));

                    if (obs == null || obs.getValueCoded() == null) {
                        result.add(mapToHEIWithoutOutcomesObject(heiEncounter, patient, heiNumber));
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
            payload.add(mapToDatasetStructure(tested, "new_case"));
        }

        // Linked to HIV care data as "linked_case"
        List<SimpleObject> linkedToHIVCare = getLinkageToHIVCare(fetchDate);
        for (SimpleObject linked : linkedToHIVCare) {
            payload.add(mapToDatasetStructure(linked, "linked_case"));
        }

        // Pregnant and postpartum at high risk as "at_risk_pbfw"
        List<SimpleObject> pregnantAndPostpartumAtHighRisk = pregnantAndPostpartumAtHighRisk(fetchDate);
        for (SimpleObject highRisk : pregnantAndPostpartumAtHighRisk) {
            payload.add(mapToDatasetStructure(highRisk, "at_risk_pbfw"));
        }

        // Pregnant and postpartum at high risk as "at_risk_pbfw"
        List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP = pregnantAndPostpartumAtHighRiskLinkedToPrEP(fetchDate);
        for (SimpleObject highRiskLinkedToPrep : pregnantAndPostpartumAtHighRiskLinkedToPrEP) {
            payload.add(mapToDatasetStructure(highRiskLinkedToPrep, "prep_linked_at_risk_pbfw"));
        }
        // Eligible for VL
        List<SimpleObject> eligibleForVl = eligibleForVl(fetchDate);
        for (SimpleObject eligibleForVlVariables : eligibleForVl) {
            payload.add(mapToDatasetStructure(eligibleForVlVariables, "eligible_for_vl"));
        }
        // Enhanced adherence
        List<SimpleObject> enhancedAdherence = enhancedAdherence(fetchDate);
        for (SimpleObject eac : enhancedAdherence) {
            payload.add(mapToDatasetStructure(eac, "unsuppressed_viral_load"));
        }
        // HEI
        //TODO: Update the event_type from hei_at_6_to_8_weeks to all_hei
        List<SimpleObject> allHEI = totalHEI(fetchDate);
        for (SimpleObject hei : allHEI) {
            payload.add(mapToDatasetStructure(hei, "hei_at_6_to_8_weeks"));
        }
        //HEI Without DNA PCR
        List<SimpleObject> dnaPCRResults = heiWithoutDnaPCRResults(fetchDate);
        for (SimpleObject heiWithoutDnaPcr : dnaPCRResults) {
            payload.add(mapToDatasetStructure(heiWithoutDnaPcr, "hei_without_pcr"));
        }

        //HEI Without DNA PCR
        List<SimpleObject> heiWithoutFinalOutcome = heiWithoutFinalOutcome(fetchDate);
        for (SimpleObject heiMissingFinalOutcome : heiWithoutFinalOutcome) {
            payload.add(mapToDatasetStructure(heiMissingFinalOutcome, "hei_without_final_outcome"));
        }
        System.out.println("payload : " + payload);
        return payload;
    }

    private Map<String, Object> mapToDatasetStructure(SimpleObject source, String eventType) {
        Map<String, Object> client = new HashMap<>();
        Map<String, Object> event = new HashMap<>();

        // Helper method to safely extract values from source
        Function<String, String> getStringValue = key -> source.get(key) == null ? null : String.valueOf(source.get(key));
        Function<String, Integer> getIntegerValue = key -> source.get(key) == null ? null : Integer.valueOf(source.get(key).toString());

        // Populate client details
        client.put("county", getStringValue.apply("county"));
        client.put("subCounty", getStringValue.apply("subCounty"));
        client.put("ward", getStringValue.apply("ward"));
        client.put("patientPk", getIntegerValue.apply("patientId"));
        client.put("sex", getStringValue.apply("sex"));
        client.put("dob", getStringValue.apply("dob"));

        // Populate event details
        event.put("mflCode", getIntegerValue.apply("mflCode"));
        event.put("createdAt", getStringValue.apply("createdAt"));
        event.put("updatedAt", getStringValue.apply("updatedAt"));

        // Define reusable fields for eventType cases
        List<String> commonFields = Arrays.asList("positiveHivTestDate", "artStartDate", "pregnancyStatus",
                "breastFeedingStatus", "lastVlOrderDate", "lastVlResults",
                "lastVlResultsDate", "visitDate");

        if ("new_case".equals(eventType)) {
            event.put("positiveHivTestDate", getStringValue.apply("positiveHivTestDate"));
        } else if ("linked_case".equals(eventType)) {
            event.put("artStartDate", getStringValue.apply("artStartDate"));
            event.put("positiveHivTestDate", getStringValue.apply("positiveHivTestDate"));
        } else if ("prep_linked_at_risk_pbfw".equals(eventType)) {
            event.put("prepNumber", getStringValue.apply("prepNumber"));
            event.put("prepRegimen", getStringValue.apply("prepRegimen"));
            event.put("prepStartDate", getStringValue.apply("prepStartDate"));
        } else if ("eligible_for_vl".equals(eventType)) {
            for (String field : commonFields) {
                event.put(field, getStringValue.apply(field));
            }
        } else if ("unsuppressed_viral_load".equals(eventType)) {
            event.put("lastEacEncounterDate", getStringValue.apply("lastEacEncounterDate"));
            for (String field : commonFields) {
                event.put(field, getStringValue.apply(field));
            }
        } else if ("hei_at_6_to_8_weeks".equals(eventType) || "hei_without_pcr".equals(eventType) || "hei_without_final_outcome".equals(eventType)) {
            event.put("heiId", getStringValue.apply("heiId"));
        }
        // Combine client and event with eventType
        Map<String, Object> result = new HashMap<>();
        result.put("client", client);
        result.put("eventType", eventType);
        result.put("event", event);

        return result;
    }

    public String sendCaseSurveillancePayload(List<Map<String, Object>> payload) {
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
                new JsonParser().parse(payloadJson); // Validate JSON syntax
                log.info("Payload is valid JSON");
            } catch (JsonSyntaxException e) {
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
                        System.out.println("Case surveillance payload sent successfully. Response: " + responseContent);
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
            return ("No case surveillance data to send at "+ fetchDate);
        }
        System.out.println("Case surveillance payload: " + jsonPayload);
        return sendCaseSurveillancePayload(payload);
    }

    public String getArtStartDate(Patient patient) {
        CalculationResult artStartDateResults = EmrCalculationUtils
                .evaluateForPatient(InitialArtStartDateCalculation.class, null, patient);

        if (artStartDateResults != null && artStartDateResults.getValue() != null) {
            return formatDateTime((Date) artStartDateResults.getValue());
        } else {
            log.warn("ART Start Date Calculation returned null for patient: " + patient.getPatientId());
            return null;
        }
    }

    public Order getLatestVlOrderForPatient(Patient patient) {
        OrderService orderService = Context.getOrderService();
        OrderSearchCriteria orderSearchCriteria = new OrderSearchCriteria(
                patient,
                null,
                Arrays.asList(vlQuatitativeConcept, vlUndetectableConcept),
                null, null, null, new Date(),
                null,
                false, null, null, null, null,
                true, true, false, false
        );
        List<Order> orders = orderService.getOrders(orderSearchCriteria);
        if (orders != null && !orders.isEmpty()) {
            orders.sort((o1, o2) -> o2.getDateActivated().compareTo(o1.getDateActivated()));
            return orders.get(0);
        }
        return null;
    }

    public List<Encounter> getLatestVlResultEncounters(Date fetchDate) {
        OrderService orderService = Context.getOrderService();
        List<Encounter> vlResultsEncounters = new ArrayList<>();
        OrderSearchCriteriaBuilder orderSearchCriteria = new OrderSearchCriteriaBuilder().
                setConcepts(Arrays.asList(vlQuatitativeConcept, vlUndetectableConcept))
                .setOrderTypes(Collections.singleton(orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID)))
                .setFulfillerStatus(Order.FulfillerStatus.COMPLETED)
                .setAction(Order.Action.NEW)
                .setIsStopped(true)
                .setExcludeCanceledAndExpired(true);
       /* OrderSearchCriteria orderSearchCriteria = new OrderSearchCriteria(null,
                null,
                Arrays.asList(vlQuatitativeConcept, vlUndetectableConcept), Collections.singleton(orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID))
                , null, null,null,
                null,
                true, null, null, Order.Action.NEW, Order.FulfillerStatus.COMPLETED,
                false, true, false, false);*/

      System.out.println("Concepts: "+orderSearchCriteria.build().getConcepts());
      System.out.println("Order types: "+orderSearchCriteria.build().getOrderTypes());
      System.out.println("Fullfiller status: "+orderSearchCriteria.build().getFulfillerStatus());
      System.out.println("Action: "+orderSearchCriteria.build().getAction());
      System.out.println("is stopped: "+orderSearchCriteria.build().isStopped());
      System.out.println("Exclude cancelled and expired: "+orderSearchCriteria.build().getExcludeCanceledAndExpired());
System.out.println("orderSearchCriteria : "+orderSearchCriteria.toString());

       List<Order> orders = orderService.getOrders(orderSearchCriteria.build());

System.out.println("Orders results size: " + orders.size());
System.out.println("Orders results: " + orders);
        if (orders.isEmpty()) {
            return vlResultsEncounters;
        }
        System.out.println("Orders found: " + orders.size());
        System.out.println("Orders from getLatestVlResultEncounters: " + orders);
        for (Order order : orders) {
            if(!Order.FulfillerStatus.COMPLETED.equals(order.getFulfillerStatus()) && (!vlQuatitativeConcept.equals(order.getConcept()) || !vlUndetectableConcept.equals(order.getConcept()))) {
                continue;
            }
            Encounter e = order.getEncounter();
            if (e != null) {
                System.out.println("Encounter ID: " + e.getEncounterId() + ", Date: " + e.getEncounterDatetime());
                Set<Obs> obs = e.getObs();
                for (Obs obsItem : obs) {
                    if (obsItem.getDateCreated().compareTo(fetchDate) >= 0) {
                        vlResultsEncounters.add(obsItem.getEncounter());
                    }
                }

            }
        }
        return vlResultsEncounters;
    }

    public Obs getLatestVlResultObs(Patient patient, Order vlOrder) {

        Obs latestObs;
        PatientWrapper patientWrapper = new PatientWrapper(patient);

        Obs undetectableVLObs = patientWrapper.lastObs(vlUndetectableConcept);
        Obs qtyObs = patientWrapper.lastObs(vlQuatitativeConcept);

        if (qtyObs == null) {
            latestObs = undetectableVLObs;
        } else if (undetectableVLObs != null) {
            latestObs = undetectableVLObs.getDateCreated().after(qtyObs.getDateCreated()) ? undetectableVLObs : qtyObs;
        } else {
            latestObs = qtyObs;
        }
        if (latestObs == null) {
            return null;
        }
        return vlOrder.equals(latestObs.getOrder()) ? latestObs : null;
   }

    public Collection<Encounter> getLatestEncounters(Date fetchDate) {
        System.out.println("Fetch date after processing: " + fetchDate);
        List<Encounter> vlResultsEncounters = getLatestVlResultEncounters(fetchDate);

        EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(
                null, null, fetchDate, new Date(), null,null, Arrays.asList(hivConsultationEncounterType, labOrderEncounterType, labResultsEncounterType, mchMotherEncounterType), null, null, null, false
        );

        // Fetch all matching encounters
        List<Encounter> encounterList = encounterService.getEncounters(encounterSearchCriteria);
        System.out.println("Encounters size before adding results enc: " + encounterList.size());
        System.out.println("Encounters found before adding results enc: " + encounterList);
        encounterList.addAll(vlResultsEncounters);
        System.out.println("Encounters size after adding results enc: " + encounterList.size());
        System.out.println("Encounters found after adding results enc: " + encounterList);
        System.out.println("Total encounters found: " + encounterList.size());

        // Map to remember latest encounter for each patient
        Map<Patient, Encounter> latestEncounterMap = new HashMap<>();
        for (Encounter encounter : encounterList) {
            Patient patient = encounter.getPatient();
            Encounter existingLatest = latestEncounterMap.get(patient);

            if (existingLatest == null
                    || (encounter.getEncounterDatetime() != null
                    && encounter.getEncounterDatetime().after(existingLatest.getEncounterDatetime()))) {
                latestEncounterMap.put(patient, encounter);
            }
        }
        // Return the latest encounter for each patient
        return latestEncounterMap.values();

    }
    public boolean isBreastFeeding(Encounter encounter) {
        boolean  breastfeeding = false;
        Concept breastfeedingStatusQstn = Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING);
        Set<Obs> obsSet = encounter.getObs();

        for (Obs obs : obsSet) {
            if (Objects.equals(obs.getConcept(), breastfeedingStatusQstn)) {
                Concept valueCoded = obs.getValueCoded();
                if (valueCoded != null) {

                    breastfeeding = valueCoded.equals(YES);
                }
            }
        }
        return breastfeeding;
    }

    public boolean isPregnant(Encounter encounter){
        boolean pregnant = false;
        Concept pregnancyStatusQstn = Dictionary.getConcept(Dictionary.PREGNANCY_STATUS);
        Set<Obs> obsSet = encounter.getObs();

        for (Obs obs : obsSet) {

            if (Objects.equals(obs.getConcept(), pregnancyStatusQstn)) {
                Concept valueCoded = obs.getValueCoded();
                if (valueCoded != null) {
                    pregnant = valueCoded.equals(YES);
                }
            }
        }
        return pregnant;
    }
}

