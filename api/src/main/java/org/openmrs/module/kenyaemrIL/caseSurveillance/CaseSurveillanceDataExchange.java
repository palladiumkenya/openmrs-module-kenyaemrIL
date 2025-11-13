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
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
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
import org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;
import static org.openmrs.module.kenyaemr.util.ServerInformation.getKenyaemrInformation;
import static org.openmrs.module.kenyaemrIL.util.CaseSurveillanceUtils.*;

public class CaseSurveillanceDataExchange {
    private static final Logger log = LoggerFactory.getLogger(CaseSurveillanceDataExchange.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String PrEP_INITIAl_FUP_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
    private static final String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
    private static final String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";

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
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "positiveHivTestDate", formatDateTime(encounter.getEncounterDatetime())
        );
    }

    private static SimpleObject mapToRollCallObject() {
        return SimpleObject.create(
                "mflCode", EmrUtils.getMFLCode(),
                "emrVersion", getKenyaemrInformation().get("version")
        );
    }

    // Utility method for creating structured SimpleObject for patients linked to care
    private SimpleObject mapToLinkageObject(Encounter encounter, Patient patient, String artStartDate) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "positiveHivTestDate", null,
                "artStartDate", artStartDate
        );
    }

    // Utility method for creating structured SimpleObject for Pregnant and postpartum women at high risk
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskObject(Encounter encounter, Patient patient) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null
        );
    }

    // Utility method for creating structured SimpleObject for Pregnant and postpartum women at high risk linked to PrEP
    private SimpleObject mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(Encounter encounter, Patient patient, String prepNumber, String prepRegimen) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "prepStartDate", formatDateTime(encounter.getEncounterDatetime()),
                "prepNumber", prepNumber,
                "prepRegimen", prepRegimen
        );
    }

    // Utility method for creating structured SimpleObject for VL Eligibility variables
    private SimpleObject mapToVlEligibilityObject(String createdAt,Integer patientId, String pregnant, String breastfeeding, String vlResult, String vlresultDate,String positiveHivTestDate,
                                                  String visitDate, String artStartDate,String vlOrderDate, Integer vlOrderReason, String upn) {
        Patient patient = Context.getPatientService().getPatient(patientId);
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", createdAt,
                "updatedAt", null,
                "patientId", patientId,
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "pregnancyStatus", pregnant,
                "breastFeedingStatus", breastfeeding,
                "lastVlResults", vlResult,
                "positiveHivTestDate", positiveHivTestDate,
                "visitDate", visitDate,
                "artStartDate", artStartDate,
                "lastVlOrderDate", vlOrderDate,
                "lastVlResultsDate", vlresultDate,
                "vlOrderReason", vlOrderReason != null ? CaseSurveillanceUtils.getConceptByConceptId(vlOrderReason).getName().getName() : null
        );
    }

    // Utility method for creating structured SimpleObject for Enhanced adherence
    private SimpleObject mapToEacObject(Encounter encounter, Patient patient, String upn) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
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
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "heiId", heiNumber
        );
    }

    // Utility method for creating structured SimpleObject for HEI DNA PCR
    private SimpleObject mapToHEIDnaPcrObject(Encounter encounter, Patient patient, String heiNumber) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "heiId", heiNumber
        );
    }

    // Utility method for creating structured SimpleObject for HEI DNA PCR
    private SimpleObject mapToHEIWithoutOutcomesObject(Encounter encounter, Patient patient, String heiNumber) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(
                "createdAt", formatDateTime(encounter.getDateCreated()),
                "updatedAt", formatDateTime(encounter.getDateChanged()),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "heiId", heiNumber
        );
    }

    /**
     * Retrieves a list of patients tested HIV-positive since the last fetch date
     */
    public Set<SimpleObject> testedHIVPositive(Date fetchDate) {
        System.out.println("INFO - IL: Started generating HIV+ cases dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        Set<SimpleObject> result = new HashSet<>();
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
        encounters.sort(Comparator.comparing(Encounter::getEncounterDatetime));
        Set<Integer> processedPatientIds = new HashSet<>();
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
            if (processedPatientIds.contains(patient.getId())) {
                // This patient's first encounter has already been added, skip this one
                continue;
            }
            if (EmrUtils.encounterThatPassCodedAnswer(encounter, htsFinalTestQuestion, htsPositiveResult)) {
                result.add(mapToTestedPositiveObject(encounter, patient));
                processedPatientIds.add(patient.getId());
            }
        }
        OrderService orderService = Context.getOrderService();
        OrderSearchCriteriaBuilder orderSearchCriteriaBuilder = new OrderSearchCriteriaBuilder().setOrderTypes(Collections.singletonList(orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID))).setActivatedOnOrAfterDate(fetchDate).setConcepts(Collections.singletonList(Dictionary.getConcept(Dictionary.HIV_DNA_POLYMERASE_CHAIN_REACTION_QUALITATIVE)));

        List<Order> dnaPCROrders = orderService.getOrders(orderSearchCriteriaBuilder.build());
        if (!dnaPCROrders.isEmpty()) {
            for (Order order : dnaPCROrders) {
                Patient patient = order.getPatient();
                if (processedPatientIds.contains(patient.getPatientId())) {
                    // This patient's first order has already been added, skip this one
                    continue;
                }
                PatientWrapper patientWrapper = new PatientWrapper(patient);
                Obs obs = patientWrapper.lastObs(MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_DNA_POLYMERASE_CHAIN_REACTION_QUALITATIVE));

                Encounter e = order.getEncounter();
                if (obs != null && obs.getValueCoded() == MetadataUtils.existing(Concept.class, Metadata.Concept.POSITIVE)) {
                    result.add(mapToTestedPositiveObject(e, patient));
                    processedPatientIds.add(patient.getId());
                }
            }
        }
        System.out.println("INFO - IL: Finished generating HIV+ cases dataset: "+ result.size() + " records found");
        return result;
    }
    /**
     * Retrieves a list of patients linked to HIV care since the last fetch date
     */
    public List<SimpleObject> getLinkageToHIVCare(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Linked to HIV Care dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        List<EncounterType> linkageEncounterTypes = Arrays.asList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.DRUG_REGIMEN_EDITOR),
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS));
        List<Form> linkageForms = Arrays.asList(MetadataUtils.existing(Form.class, CommonMetadata._Form.DRUG_REGIMEN_EDITOR), MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_LINKAGE));

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
            String artStartDate = getArtStartDate(patient);

            if (artStartDate == null) {
                log.warn("Encounter has no ART start date, skipping...");
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
        System.out.println("INFO - IL: Finished generating Linked to HIV Care dataset: "+ result.size() + " records found");
        return result;
    }

    /**
     * Pregnant and postpartum women at high risk of turning HIV+
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> pregnantAndPostpartumAtHighRisk(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Pregnant and postpartum at high risk dataset... ");
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
        if (screeningEncounters == null || screeningEncounters.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Patient> highRiskPatients = new HashSet<>();
        for (Encounter htsScreeningEncounter : screeningEncounters) {
            Patient patient = htsScreeningEncounter.getPatient();
            if (patient == null) {
                continue;
            }
         //   if (patient == null || "M".equals(patient.getGender())) continue;

            boolean isHighRisk = EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighRiskResult)
                    || EmrUtils.encounterThatPassCodedAnswer(htsScreeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);

            if (isHighRisk) {
                highRiskPatients.add(patient);
            }
        }
        EncounterSearchCriteria htsTestSearchCriteria = new EncounterSearchCriteria(
                null, null, fetchDate, null, null, testingForms,
                Collections.singletonList(MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS)),
                null, null, null, false
        );
        List<Encounter> htsTestEncounters = encounterService.getEncounters(htsTestSearchCriteria);
        if( htsTestEncounters == null || htsTestEncounters.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Patient, List<Encounter>> testEncountersByPatient = htsTestEncounters.stream()
                .filter(e -> e.getPatient() != null)
                .collect(Collectors.groupingBy(Encounter::getPatient));

        // Evaluate each high-risk patient
        for (Encounter screening : screeningEncounters) {
            Patient patient = screening.getPatient();
            if (!highRiskPatients.contains(patient) || !"F".equals(patient.getGender())) {
                continue;
            }
            List<Encounter> patientTests = testEncountersByPatient.getOrDefault(patient, Collections.emptyList());

            // Check if patient meets criteria based on test encounters
            for (Encounter testEncounter : patientTests) {
                if (EmrUtils.encounterThatPassCodedAnswer(testEncounter, htsFinalTestQuestion, htsNegativeResult)
                        && (EmrUtils.encounterThatPassCodedAnswer(testEncounter, htsEntryPointQstn, htsEntryPointANC)
                        || EmrUtils.encounterThatPassCodedAnswer(testEncounter, htsEntryPointQstn, htsEntryPointMAT)
                        || EmrUtils.encounterThatPassCodedAnswer(testEncounter, htsEntryPointQstn, htsEntryPointPNC))) {
                    result.add(mapToPregnantAndPostpartumAtHighRiskObject(screening, patient));
                    break;
                }
            }
        }
        System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk dataset: "+ result.size() + " records found");
        return result;
    }

    public List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Pregnant and postpartum at high risk linked to PrEP dataset... ");
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

        Concept htsEligibilityCurrentOnPrEPQstn = conceptService.getConcept(165203);
        Concept htsEligibilityCurrentOnPrEPResult = Dictionary.getConcept(Dictionary.YES);

        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsNegativeResult = conceptService.getConcept(HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID);

        if (htsFinalTestQuestion == null || htsNegativeResult == null) {
            log.error("Required HTS or PrEP concepts are missing");
            return result;
        }

        // Encounter Types
        List<EncounterType> htsEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS)
        );
        List<EncounterType> prepInitialFUPEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, PrEP_INITIAl_FUP_ENCOUNTER)
        );

        List<Form> testingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );
        Form prepInitialForm = MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM);
        Form htsEligibilityForm = MetadataUtils.existing(Form.class, HTS_ELIGIBILITY_FORM);

        // Pre-Fetch encounters
        Map<Patient, List<Encounter>> htsScreeningEncountersMap = encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null,
                        Collections.singletonList(htsEligibilityForm), htsEncounterType, null, null, null, false)
        ).stream().collect(Collectors.groupingBy(Encounter::getPatient));

        Map<Patient, List<Encounter>> htsTestEncountersMap = encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null, testingForms, htsEncounterType, null, null, null, false)
        ).stream().collect(Collectors.groupingBy(Encounter::getPatient));

        Map<Patient, List<Encounter>> prepEncountersMap = encounterService.getEncounters(
                new EncounterSearchCriteria(null, null, fetchDate, null, null,
                        Collections.singletonList(MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM)),
                        prepInitialFUPEncounterType, null, null, null, false)
        ).stream().collect(Collectors.groupingBy(Encounter::getPatient));

        Set<Integer> processedPatientIds = new HashSet<>();

        // Screening encounters
        for (Map.Entry<Patient, List<Encounter>> entry : htsScreeningEncountersMap.entrySet()) {
            Patient patient = entry.getKey();
            if (patient == null || !"F".equals(patient.getGender()) || processedPatientIds.contains(patient.getId())) continue;

            for (Encounter screeningEncounter : entry.getValue()) {
                // Check high-risk flags
                boolean isHighRisk = EmrUtils.encounterThatPassCodedAnswer(screeningEncounter, htsScrRiskQstn, htsScrHighRiskResult)
                        || EmrUtils.encounterThatPassCodedAnswer(screeningEncounter, htsScrRiskQstn, htsScrHighestRiskResult);
                if (!isHighRisk) continue;

                // Get all test encounters for patient
                List<Encounter> testEncounters = htsTestEncountersMap.getOrDefault(patient, Collections.emptyList());

                for (Encounter htsEncounter : testEncounters) {
                    boolean testedNegative = EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsFinalTestQuestion, htsNegativeResult);
                    boolean hasEntryPoint = EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointANC)
                            || EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointMAT)
                            || EmrUtils.encounterThatPassCodedAnswer(htsEncounter, htsEntryPointQstn, htsEntryPointPNC);
                    if (!testedNegative || !hasEntryPoint) continue;

                    // Check PrEP linkage (either currently on PrEP or has PrEP encounters)
                    boolean isCurrentlyOnPrEP = EmrUtils.encounterThatPassCodedAnswer(screeningEncounter, htsEligibilityCurrentOnPrEPQstn, htsEligibilityCurrentOnPrEPResult);
                    List<Encounter> prepEncounters = prepEncountersMap.getOrDefault(patient, Collections.emptyList());
                    if (!isCurrentlyOnPrEP && prepEncounters.isEmpty()) continue;

                    // Collect PrEP details
                    String prepNumber = CaseSurveillanceUtils.getPrepNumber(patient);
                    String prepRegimen = CaseSurveillanceUtils.getPrepRegimen(patient);

                    result.add(mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(htsEncounter, patient, prepNumber, prepRegimen));
                    processedPatientIds.add(patient.getId());
                    break;
                }
            }
        }
        System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk linked to PrEP dataset: "+ result.size() + " records found");
        return result;
    }
    @SuppressWarnings("unchecked")
    public List<SimpleObject> eligibleForVl() {
        System.out.println("INFO - IL: Started generating eligible for VL dataset (ETL-based)...");
        Session session = Context.getRegisteredComponent("sessionFactory", SessionFactory.class).getCurrentSession();

        String sql = "select e.patient_id,\n" +
                "       b.pregnant,\n" +
                "       b.breastFeedingStatus,\n" +
                "       b.positiveHivTestDate,\n" +
                "       b.visitDate,\n" +
                "       b.artStartDate,\n" +
                "       b.lastVlOrderDate,\n" +
                "       b.lastVlResultsDate,\n" +
                "       b.vlOrderReason,\n" +
                "       b.dateCreated,\n" +
                "       e.upn,\n" +
                "       b.vlResult\n" +
                "from (select fup.visit_date,\n" +
                "             fup.patient_id,\n" +
                "             max(e.visit_date)                                                      as enroll_date,\n" +
                "             greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n" +
                "             greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n" +
                "                      ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n" +
                "             d.patient_id                                                           as disc_patient,\n" +
                "             d.effective_disc_date                                                  as effective_disc_date,\n" +
                "             max(d.visit_date)                                                      as date_discontinued,\n" +
                "             de.patient_id                                                          as started_on_drugs,\n" +
                "             p.unique_patient_no                                                    as upn\n" +
                "      from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
                "               join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n" +
                "               join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n" +
                "               left join kenyaemr_etl.etl_drug_event de\n" +
                "                         on e.patient_id = de.patient_id and de.program = 'HIV' and\n" +
                "                            date(de.date_started) <= date(CURRENT_DATE)\n" +
                "               left outer JOIN\n" +
                "           (select patient_id,\n" +
                "                   coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n" +
                "                   max(date(effective_discontinuation_date)) as               effective_disc_date\n" +
                "            from kenyaemr_etl.etl_patient_program_discontinuation\n" +
                "            where date(visit_date) <= date(CURRENT_DATE)\n" +
                "              and program_name = 'HIV'\n" +
                "            group by patient_id) d on d.patient_id = fup.patient_id\n" +
                "      where fup.visit_date <= date(CURRENT_DATE)\n" +
                "      group by patient_id\n" +
                "      having (started_on_drugs is not null and started_on_drugs <> '')\n" +
                "         and (\n" +
                "          (\n" +
                "              (timestampdiff(DAY, date(latest_tca), date(CURRENT_DATE)) <= 30 and\n" +
                "               ((date(d.effective_disc_date) > date(CURRENT_DATE) or date(enroll_date) > date(d.effective_disc_date)) or\n" +
                "                d.effective_disc_date is null))\n" +
                "                  and\n" +
                "              (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or\n" +
                "               disc_patient is null)\n" +
                "              )\n" +
                "          )) e\n" +
                "         INNER JOIN (select v.patient_id,case pregnancy_status when 1065 then 'YES' when 1066 then 'NO' end     as pregnant,\n" +
                "                            case v.breastfeeding_status when 1065 then 'YES' when 1066 then 'NO' end as breastFeedingStatus,\n" +
                "                            v.date_confirmed_hiv_positive                                            as positiveHivTestDate,\n" +
                "                            v.latest_hiv_followup_visit                                              as visitDate,\n" +
                "                            v.date_started_art                                                       as artStartDate,\n" +
                "                            v.date_test_requested                                                    as lastVlOrderDate,\n" +
                "                            v.date_test_result_received                                              as lastVlResultsDate,\n" +
                "                            v.order_reason                                                           as vlOrderReason,\n" +
                "                            v.date_created                                                           as dateCreated,\n" +
                "                            v.vl_result                                                           as vlResult\n" +
                "                     from kenyaemr_etl.etl_viral_load_validity_tracker v\n" +
                "                              inner join kenyaemr_etl.etl_patient_demographics d on v.patient_id = d.patient_id\n" +
                "                     where ((TIMESTAMPDIFF(MONTH, v.date_started_art, date(CURRENT_DATE)) >= 3 and\n" +
                "                             v.base_viral_load_test_result is null) -- First VL new on ART+\n" +
                "                         OR ((v.pregnancy_status = 1065 or v.breastfeeding_status = 1065) and\n" +
                "                             TIMESTAMPDIFF(MONTH, v.date_started_art, date(CURRENT_DATE)) >= 3 and\n" +
                "                             (v.vl_result is not null and\n" +
                "                              v.date_test_requested < date(CURRENT_DATE)) and\n" +
                "                             (v.order_reason not in (159882, 1434, 2001237, 163718))) -- immediate for PG & BF+\n" +
                "                         OR (v.lab_test = 856 AND v.vl_result >= 200 AND\n" +
                "                             TIMESTAMPDIFF(MONTH, v.date_test_requested, date(CURRENT_DATE)) >= 3) -- Unsuppressed VL+\n" +
                "                         OR (((v.lab_test = 1305 AND v.vl_result = 1302) OR v.vl_result < 200) AND\n" +
                "                             TIMESTAMPDIFF(MONTH, v.date_test_requested, date(CURRENT_DATE)) >= 6 and\n" +
                "                             TIMESTAMPDIFF(YEAR, d.DOB, v.date_test_requested) BETWEEN 0 AND 24) -- 0-24 with last suppressed vl+\n" +
                "                         OR (((v.lab_test = 1305 AND v.vl_result = 1302) OR v.vl_result < 200) AND\n" +
                "                             TIMESTAMPDIFF(MONTH, v.date_test_requested, date(CURRENT_DATE)) >= 12 and\n" +
                "                             TIMESTAMPDIFF(YEAR, d.DOB, v.date_test_requested) > 24) -- > 24 with last suppressed vl+\n" +
                "                         OR ((v.pregnancy_status = 1065 or v.breastfeeding_status = 1065) and\n" +
                "                             TIMESTAMPDIFF(MONTH, v.date_started_art, date(CURRENT_DATE)) >= 3\n" +
                "                             and (v.order_reason in (159882, 1434, 2001237, 163718) and\n" +
                "                                  TIMESTAMPDIFF(MONTH, v.date_test_requested, date(CURRENT_DATE)) >= 6) and\n" +
                "                             ((v.lab_test = 1305 AND v.vl_result = 1302) OR (v.vl_result < 200))) -- PG & BF after PG/BF baseline < 200\n" +
                "                               )) b on e.patient_id = b.patient_id;";

        List<Object[]> rows = session.createSQLQuery(sql)
                .list();

        List<SimpleObject> result = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (Object[] row : rows) {
            Integer patientId = row[0] != null ? Integer.parseInt(row[0].toString()) : null;
            String pregnant = safeToString(row[1]);
            String breastFeedingStatus = safeToString(row[2]);
            String positiveHivTestDate = formatDateTime((Date)row[3]);
            String visitDate = formatDateTime((Date)row[4]);
            String artStartDate = formatDateTime((Date)row[5]);
            String lastVlOrderDate = formatDateTime((Date)row[6]);
            String lastVlResultsDate = formatDateTime((Date)row[7]);
            Integer orderReason = row[8] != null ? Integer.parseInt(row[8].toString()) : null;
            String createdAt = formatDateTime((Date)row[9]);
            String upn = safeToString(row[10]);
            String vlResult = safeToString(row[11]);

            result.add(mapToVlEligibilityObject(createdAt, patientId,pregnant,breastFeedingStatus,vlResult,lastVlResultsDate,positiveHivTestDate,visitDate,artStartDate,lastVlOrderDate,orderReason, upn));

        }
        System.out.println("INFO - IL: Finished generating eligible for VL dataset (ETL-based): "+ result.size() + " records found");
        return result;
    }

    /**
     * Patients with enhanced adherence
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> enhancedAdherence(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Enhanced Adherence dataset... ");
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
            Set<Integer> processedPatientIds = new HashSet<>();
            for (Encounter encounter : eacEncounters) {
                if (encounter == null) {
                    continue;
                }

                Patient patient = encounter.getPatient();

                if (patient == null) {
                    continue;
                }

                PatientIdentifierType upnIdentifierType = MetadataUtils.existing(PatientIdentifierType.class, Metadata.IdentifierType.UNIQUE_PATIENT_NUMBER);
                PatientIdentifier upnIdentifier = patient.getPatientIdentifier(upnIdentifierType);
                String upn = upnIdentifier != null ? upnIdentifier.getIdentifier() : null;

                if (processedPatientIds.contains(patient.getId())) {
                    // This patient's first encounter has already been added, skip this one
                    continue;
                }
                result.add(mapToEacObject(encounter, patient, upn));
                processedPatientIds.add(patient.getId());
            }
        }
        System.out.println("INFO - IL: Finished generating Enhanced Adherence dataset: " + result.size() + " records found");
        return result;
    }

    /**
     * HEI cohort
     *
     * @return
     */
    //todo Confirm whether transmission is cumulative
    public List<SimpleObject> totalHEI() {
        System.out.println("INFO - IL: Started generating all HEIs dataset... ");
        Date effectiveDate = Date.from(LocalDate.now().minusMonths(24).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

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
        System.out.println("INFO - IL: Finished generating all HEIs dataset: " + result.size() + " records found");
        return result;
    }

    public List<SimpleObject> heiWithoutDnaPCRResults() {
        System.out.println("INFO - IL: Started generating HEI without DNA PCR test dataset... ");
        Date effectiveDate = Date.from(LocalDate.now().minusWeeks(8).atStartOfDay(ZoneId.systemDefault()).toInstant().plus(0, ChronoUnit.DAYS));
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
                if (patient.getBirthdate() != null && isBetween6And8WeeksOld(patient.getBirthdate(), new Date()) && heiNumber != null) {

                    PatientWrapper patientWrapper = new PatientWrapper(patient);

                    Obs obs = patientWrapper.lastObs(MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_DNA_POLYMERASE_CHAIN_REACTION_QUALITATIVE));

                    if (obs == null || obs.getValueCoded() == null) {
                        result.add(mapToHEIDnaPcrObject(heiEncounter, patient, heiNumber));
                    }
                }
            }
        }
        System.out.println("INFO - IL: Finished generating HEI without DNA PCR test dataset: "+ result.size() + " records found");
        return result;
    }


    /** HEIs without documented final Outcome
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> heiWithoutFinalOutcome(Date fetchDate) {
        System.out.println("INFO - IL: Started generating HEI without final outcome dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }

        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();

        // Pre-fetch metadata (avoid repeated calls inside loop)
        EncounterType heiEncounterType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHCS_ENROLLMENT);
        Form heiEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_ENROLLMENT);
        Concept hivStatusConcept = MetadataUtils.existing(Concept.class, Metadata.Concept.HIV_STATUS);

        // Compute a birthdate cutoff for 24 months to filter before loading all encounters
        LocalDate contextLocal = fetchDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate minBirthDate = contextLocal.minusMonths(24); // inclusive window
        Date minBirthDateAsDate = Date.from(minBirthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        LocalDate earliestRelevantDate = contextLocal.minusMonths(24);
        Date earliestEncounterDate = Date.from(earliestRelevantDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // Search only HEI encounters where patient birthdate is within 24 months window
        EncounterSearchCriteria heiSearchCriteria = new EncounterSearchCriteria(
                null, null, earliestEncounterDate, null, null,
                Collections.singletonList(heiEnrollmentForm),
                Collections.singletonList(heiEncounterType),
                null, null, null,
                false
        );
        List<Encounter> heiEncounters = encounterService.getEncounters(heiSearchCriteria);

        for (Encounter heiEncounter : heiEncounters) {
            Patient patient = heiEncounter.getPatient();
            // Skip early if outside the 24-month window (avoid unnecessary wrapper creation)
            if (patient.getBirthdate() == null || patient.getBirthdate().before(minBirthDateAsDate)) {
                continue;
            }
            String heiNumber = getHEINumber(patient);
            if (heiNumber == null) {
                continue;
            }

            // Final validation using MySQL-like month calculation (ensures no April edge cases)
            int ageInMonths = getAgeInMonths(patient.getBirthdate(), fetchDate);
            if (ageInMonths == 24) {
                PatientWrapper patientWrapper = new PatientWrapper(patient);
                Obs obs = patientWrapper.lastObs(hivStatusConcept);

                if (obs == null || obs.getValueCoded() == null) {
                    result.add(mapToHEIWithoutOutcomesObject(heiEncounter, patient, heiNumber));
                }
            }
        }
        System.out.println("INFO - IL: Finished generating HEI without final outcome dataset: "+ result.size() + " records found");
        return result;
    }

    /**
     * Generates the case surveillance payload for visualization metrics.
     */
    public List<Map<String, Object>> generateCaseSurveillancePayload(Date fetchDate) {
        List<Map<String, Object>> payload = new ArrayList<>();

        // Add roll_call data
        SimpleObject rollCallData = mapToRollCallObject();
        payload.add(mapToDatasetStructure(rollCallData, "roll_call"));

        // Tested HIV-positive data as "new_case"
        Set<SimpleObject> testedPositive = testedHIVPositive(fetchDate);
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
        // HEI
        //TODO: Update the event_type from hei_at_6_to_8_weeks to all_hei
        List<SimpleObject> allHEI = totalHEI();
        for (SimpleObject hei : allHEI) {
            payload.add(mapToDatasetStructure(hei, "hei_at_6_to_8_weeks"));
        }
        //HEI Without DNA PCR
        List<SimpleObject> dnaPCRResults = heiWithoutDnaPCRResults();
        for (SimpleObject heiWithoutDnaPcr : dnaPCRResults) {
            payload.add(mapToDatasetStructure(heiWithoutDnaPcr, "hei_without_pcr"));
        }
        //HEI Without DNA PCR
        List<SimpleObject> heiWithoutFinalOutcome = heiWithoutFinalOutcome(fetchDate);
        for (SimpleObject heiMissingFinalOutcome : heiWithoutFinalOutcome) {
            payload.add(mapToDatasetStructure(heiMissingFinalOutcome, "hei_without_final_outcome"));
        }
        // Enhanced adherence
        List<SimpleObject> enhancedAdherence = enhancedAdherence(fetchDate);
        for (SimpleObject eac : enhancedAdherence) {
            payload.add(mapToDatasetStructure(eac, "unsuppressed_viral_load"));
        }
        // Eligible for VL
        List<SimpleObject> eligibleForVl = eligibleForVl();
        for (SimpleObject eligibleForVlVariables : eligibleForVl) {
            payload.add(mapToDatasetStructure(eligibleForVlVariables, "eligible_for_vl"));
        }
        return payload;
    }

    private Map<String, Object> mapToDatasetStructure(SimpleObject source, String eventType) {
        Map<String, Object> client = new HashMap<>();
        Map<String, Object> event = new HashMap<>();

        // Helper method to safely extract values from source
        Function<String, String> getStringValue = key -> source.get(key) == null ? null : String.valueOf(source.get(key));
        Function<String, Integer> getIntegerValue = key -> source.get(key) == null ? null : Integer.valueOf(source.get(key).toString());

        // Special handling for roll_call event type which doesn't include client data
        if ("roll_call".equals(eventType)) {
            // For roll_call, we only need the event data (mflCode and emrVersion)
            event.put("mflCode", source.get("mflCode"));
            event.put("emrVersion", source.get("emrVersion"));

            // Create final payload structure
            Map<String, Object> result = new HashMap<>();
            result.put("eventType", eventType);
            result.put("event", event);
            return result;
        }

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
            event.put( "vlOrderReason", getStringValue.apply("vlOrderReason"));
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
        System.out.println("Sending case surveillance payload: " + payload.size());
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
                    case HttpURLConnection.HTTP_ACCEPTED:
                        System.out.println("Case surveillance payload accepted. Response: " + responseContent);
                        return "Success: Payload accepted. Response: " + responseContent;
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
        log.warn("processAndSendCaseSurveillancePayload called. Fetch date: {}", fetchDate);
        List<Map<String, Object>> payload = generateCaseSurveillancePayload(fetchDate);
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String jsonPayload = gson.toJson(payload); // Serialize to JSON
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            return ("No case surveillance data to send at " + fetchDate);
        }
        System.out.println("Case surveillance Payload size: " + jsonPayload.getBytes(StandardCharsets.UTF_8).length + " bytes");
        return sendCaseSurveillancePayload(payload);
    }

    public String getArtStartDate(Patient patient) {
        try {
            CalculationResult artStartDateResults = EmrCalculationUtils
                    .evaluateForPatient(InitialArtStartDateCalculation.class, null, patient);
            if (artStartDateResults != null && artStartDateResults.getValue() != null) {
                return formatDateTime((Date) artStartDateResults.getValue());
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Error evaluating InitialArtStartDateCalculation for patient {}: {}", patient.getPatientId(), e.getMessage(), e);
        }
        return null;
    }
}

