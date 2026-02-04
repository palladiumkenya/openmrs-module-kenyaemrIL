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
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.metadata.OTZMetadata;
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
    private static final String PrEP_INITIAl_ENCOUNTER = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
    private static final String HTS_ELIGIBILITY_FORM = "04295648-7606-11e8-adc0-fa7ae01bbebc";
    private static final String PrEP_INITIAL_FORM = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
    private static final String PrEP_RISK_ASSESSMENT_ENCOUNTER_UUID = "6e5ec039-8d2a-4172-b3fb-ee9d0ba647b7";
    private static final String PrEP_RISK_ASSESSMENT_FORM_UUID = "40374909-05fc-4af8-b789-ed9c394ac785";
    private static final Integer CONSENTED_TO_PREP_QSTN = 165094, HTS_RISK_SCR_QSTN = 167163,  HTS_HIGHEST_RISK_ANS = 167164, HTS_HIGH_RISK_ANS = 1408, DEATH_DATE = 1543, CAUSE_OF_DEATH = 1599;
    public static final String PREP_ENROLLMENT_ENC_TYPE = "35468fe8-a889-4cd4-9b35-27ac98bdd750";
    public static final String PREP_ENROLLMENT_FORM = "d63eb2ee-d5e8-4ea4-b5ea-ea3670af03ac";
    public static final String PREP_INITIATION_FORM = "d5ca78be-654e-4d23-836e-a934739be555";
    public static String PrEP_REGIMEN = "164515AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static String TYPE_OF_PrEP = "166866AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static String REASON_FOR_STARTING_PrEP = "159623AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static String REASON_FOR_SWITCHING_PrEP = "4b59ac07-cf72-4f46-b8c0-4f62b1779f7e";
    public static String DATE_SWITCHED_PrEP = "68bfa3f3-1fc7-4d9d-bb41-e897c3c430ef";
    public static String PrEP_FOLLOWUP_FORM = "ee3e2017-52c0-4a54-99ab-ebb542fb8984";
    public static String PrEP_FOLLOWUP_ENCOUNTER_TYPE = "c4a2be28-6673-4c36-b886-ea89b0a42116";
    public static String PrEP_STATUS = "42ad51f2-dc4f-48eb-8440-9a0bd8969374";
    public static String PrEP_REFILL_FORM = "291c03c8-a216-11e9-a2a3-2a2ae2dbcce4";
    public static String PrEP_REFILL_ENCOUNTER_TYPE = "291c0828-a216-11e9-a2a3-2a2ae2dbcce4";

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

    private SimpleObject mapToPrEPUptakeObject(
            Encounter latestPrEPEnc,
            Patient patient,
            String prepNumber,
            String prepMethod,
            Date prepStartDate,
            String prepStatus,
            String reasonForStartingPrEP,
            String reasonForSwitchingPrEP,
            Date dateSwitchedPrep,
            String prepRegimen

           ) {

        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;
        return SimpleObject.create(

                "createdAt", formatDateTime(latestPrEPEnc.getDateCreated()),
                "updatedAt", formatDateTime(latestPrEPEnc.getDateChanged()),
                "patientId", patient.getPatientId(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "prepStartDate", formatDateTime(prepStartDate),
                "prepNumber", prepNumber,
                "prepType", prepMethod,
                "prepRegimen", prepRegimen,
                "prepStatus", prepStatus,
                "reasonForStartingPrep", reasonForStartingPrEP,
                "reasonForSwitchingPrep", reasonForSwitchingPrEP,
                "dateSwitchedPrep", formatDate(dateSwitchedPrep)
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
    private SimpleObject mapToMortalityObject(Patient patient, Date createdAt, Date updatedAt, Date deathDate, String causeOfDeath) {
        PersonAddress address = Optional.ofNullable(patient).map(Patient::getPersonAddress).orElse(null);
        String sex = Optional.ofNullable(patient).map(Patient::getGender).orElse(null);
        Date birthdate = (patient != null) ? patient.getBirthdate() : null;

        return SimpleObject.create(
                "createdAt", formatDateTime(createdAt),
                "updatedAt", formatDateTime(updatedAt),
                "patientId", patient.getPatientId().toString(),
                "county", safeGetField(address, PersonAddress::getCountyDistrict),
                "subCounty", safeGetField(address, PersonAddress::getStateProvince),
                "ward", safeGetField(address, PersonAddress::getAddress6),
                "mflCode", EmrUtils.getMFLCode(),
                "dob", birthdate != null ? formatDate(birthdate) : null,
                "sex", sex != null ? dmiUtils.formatGender(sex) : null,
                "deathDate", formatDateTime(deathDate),
                "causeOfDeath", causeOfDeath
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
            String artStartDate;
            try {
                log.debug("Evaluating ART start date for patientId={}, encounterId={}, encounterDatetime={}",
                        patient.getPatientId(), encounter.getEncounterId(), encounter.getEncounterDatetime());
                artStartDate = getArtStartDate(patient);
            } catch (Exception e) {
                log.error("Failed to evaluate ART start date for patientId={}, encounterId={}",
                        patient.getPatientId(), encounter.getEncounterId(), e);
                continue;
            }

            if (artStartDate == null) {
                log.warn("Encounter has no ART start date, skipping... patientId={}, encounterId={}",
                        patient.getPatientId(), encounter.getEncounterId());
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
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> pregnantAndPostpartumAtHighRisk(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Pregnant and postpartum at high risk dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        if (fetchDate.after(new Date())) {
            throw new IllegalArgumentException("Fetch date cannot be in the future.");
        }

        // PrEP eligibility window: ALL relevant encounters must have encounterDatetime within the last 72 hours
        final Date now = new Date();
        final Date threeDaysAgo = Date.from(java.time.Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS));

        // Still honor incremental fetching, but don't go earlier than the PrEP window
        final Date effectiveFromDate = fetchDate.after(threeDaysAgo) ? fetchDate : threeDaysAgo;
        System.out.println("Effective from date: " + effectiveFromDate);

        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();

        // Cohort concepts
        Concept pregnantQstn = conceptService.getConceptByUuid(Metadata.Concept.PREGNANCY_STATUS);
        Concept breastfeedingQstn = conceptService.getConceptByUuid(Metadata.Concept.CURRENTLY_BREASTFEEDING);
        Concept yes = Dictionary.getConcept(Dictionary.YES);

        Concept htsScrRiskQstn = conceptService.getConcept(HTS_RISK_SCR_QSTN);
        Concept htsScrHighRiskResult = conceptService.getConcept(HTS_HIGH_RISK_ANS);
        Concept htsScrHighestRiskResult = conceptService.getConcept(HTS_HIGHEST_RISK_ANS);

        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsNegativeResult = conceptService.getConcept(HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID);
        // PrEP consent concepts (from PrEP Risk Assessment)
        Concept consentedToPrEPQstn = conceptService.getConcept(CONSENTED_TO_PREP_QSTN);
        Concept consentedToPrEPAnswer = conceptService.getConceptByUuid(Metadata.Concept.YES);

        List<String> missing = new ArrayList<>();
        if (pregnantQstn == null) missing.add("PREGNANCY_STATUS (" + Metadata.Concept.PREGNANCY_STATUS + ")");
        if (breastfeedingQstn == null) missing.add("CURRENTLY_BREASTFEEDING (" + Metadata.Concept.CURRENTLY_BREASTFEEDING + ")");
        if (yes == null) missing.add("YES (" + Dictionary.YES + ")");
        if (htsScrRiskQstn == null) missing.add("HTS_RISK_SCR_QSTN (" + HTS_RISK_SCR_QSTN + ")");
        if (htsScrHighRiskResult == null) missing.add("HTS_HIGH_RISK_ANS (" + HTS_HIGH_RISK_ANS + ")");
        if (htsScrHighestRiskResult == null) missing.add("HTS_HIGHEST_RISK_ANS (" + HTS_HIGHEST_RISK_ANS + ")");
        if (htsFinalTestQuestion == null) missing.add("HTS_FINAL_TEST_CONCEPT_ID (" + HtsConstants.HTS_FINAL_TEST_CONCEPT_ID + ")");
        if (htsNegativeResult == null) missing.add("HTS_NEGATIVE_RESULT_CONCEPT_ID (" + HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID + ")");
        if (consentedToPrEPQstn == null) missing.add("CONSENTED_TO_PREP_QSTN (" + CONSENTED_TO_PREP_QSTN + ")");

        if (!missing.isEmpty()) {
            log.error("Required concepts are missing; cannot build Pregnant and postpartum at high risk linked to PrEP dataset. Missing: {}", missing);
            return result;
        }

        // Metadata
        List<EncounterType> htsEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS)
        );
        List<Form> htsTestingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );
        Form htsEligibilityForm = MetadataUtils.existing(Form.class, HTS_ELIGIBILITY_FORM);

        List<EncounterType> prepRiskAssessmentEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, PrEP_RISK_ASSESSMENT_ENCOUNTER_UUID)
        );
        Form prepRiskAssessmentForm = MetadataUtils.existing(Form.class, PrEP_RISK_ASSESSMENT_FORM_UUID);

        // Pull HTS eligibility encounters ON/AFTER effectiveFromDate (driver set)
        List<Encounter> eligibilityEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                Collections.singletonList(htsEligibilityForm),
                                htsEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        if (eligibilityEncounters.isEmpty()) {
            System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk dataset: 0 records found");
            return result;
        }

        // Pull HTS tests ON/AFTER effectiveFromDate (same window)
        List<Encounter> htsTestEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                htsTestingForms,
                                htsEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        // Pull PrEP risk assessments ON/AFTER effectiveFromDate (same window)
        List<Encounter> prepRiskAssessmentEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                Collections.singletonList(prepRiskAssessmentForm),
                                prepRiskAssessmentEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        Map<Integer, List<Encounter>> testsByPatientId = htsTestEncounters.stream()
                .filter(e -> e != null && e.getPatient() != null && e.getPatient().getPatientId() != null)
                .collect(Collectors.groupingBy(e -> e.getPatient().getPatientId()));

        Map<Integer, List<Encounter>> prepRiskByPatientId = prepRiskAssessmentEncounters.stream()
                .filter(e -> e != null && e.getPatient() != null && e.getPatient().getPatientId() != null)
                .collect(Collectors.groupingBy(e -> e.getPatient().getPatientId()));

        // Helper: pick latest encounter within [from, to] (inclusive)
        final java.util.function.BiFunction<List<Encounter>, Date[], Encounter> latestWithin = (encounters, range) -> {
            if (encounters == null || encounters.isEmpty() || range == null || range.length != 2) {
                return null;
            }
            Date from = range[0];
            Date to = range[1];
            Encounter best = null;
            for (Encounter e : encounters) {
                if (e == null || e.getEncounterDatetime() == null) {
                    continue;
                }
                Date d = e.getEncounterDatetime();
                if (from != null && d.before(from)) {
                    continue;
                }
                if (to != null && d.after(to)) {
                    continue;
                }
                if (best == null || d.after(best.getEncounterDatetime())) {
                    best = e;
                }
            }
            return best;
        };

        // Dedupe per patient (since events can span days within the window)
        Set<Integer> processedPatientIds = new HashSet<>();

        for (Encounter eligibility : eligibilityEncounters) {
            if (eligibility == null || eligibility.getPatient() == null || eligibility.getPatient().getPatientId() == null) {
                continue;
            }

            Patient patient = eligibility.getPatient();
            Integer patientId = patient.getPatientId();

            if (processedPatientIds.contains(patientId)) {
                continue;
            }

            String gender = patient.getGender();
            if (gender == null || !"F".equalsIgnoreCase(gender.trim())) {
                continue;
            }
            if (eligibility.getEncounterDatetime() == null) {
                continue;
            }

            boolean pregnantOrBreastfeeding =
                    EmrUtils.encounterThatPassCodedAnswer(eligibility, pregnantQstn, yes)
                            || EmrUtils.encounterThatPassCodedAnswer(eligibility, breastfeedingQstn, yes);
            if (!pregnantOrBreastfeeding) {
                continue;
            }

            boolean isHighRisk =
                    EmrUtils.encounterThatPassCodedAnswer(eligibility, htsScrRiskQstn, htsScrHighRiskResult)
                            || EmrUtils.encounterThatPassCodedAnswer(eligibility, htsScrRiskQstn, htsScrHighestRiskResult);
            if (!isHighRisk) {
                continue;
            }

            // Enforce ordering + same 3-day window:
            // 1) Eligibility screening is the anchor (must occur first)
            // 2) HTS test and PrEP behavioral assessment must occur AFTER eligibility
            // 3) All three must be within 3 days from eligibility (and within the global 72h window)
            Date eligibilityDateTime = eligibility.getEncounterDatetime();
            Date windowStart = eligibilityDateTime.before(threeDaysAgo) ? threeDaysAgo : eligibilityDateTime;
            Date windowEnd = Date.from(eligibilityDateTime.toInstant().plus(3, java.time.temporal.ChronoUnit.DAYS));
            if (windowEnd.after(now)) {
                windowEnd = now;
            }

            Encounter latestTestInRange = latestWithin.apply(testsByPatientId.get(patientId), new Date[]{windowStart, windowEnd});
            if (latestTestInRange == null || latestTestInRange.getEncounterDatetime() == null) {
                continue;
            }
            if (latestTestInRange.getEncounterDatetime().before(eligibilityDateTime)) {
                continue; // eligibility must be before HTS test
            }
            boolean testedNegative = EmrUtils.encounterThatPassCodedAnswer(latestTestInRange, htsFinalTestQuestion, htsNegativeResult);
            if (!testedNegative) {
                continue;
            }

            Encounter latestPrepRiskInRange = latestWithin.apply(prepRiskByPatientId.get(patientId), new Date[]{windowStart, windowEnd});
            boolean consentedToPrep = latestPrepRiskInRange != null
                    && latestPrepRiskInRange.getEncounterDatetime() != null
                    && !latestPrepRiskInRange.getEncounterDatetime().before(eligibilityDateTime)
                    && EmrUtils.encounterThatPassCodedAnswer(latestPrepRiskInRange, consentedToPrEPQstn, consentedToPrEPAnswer);
            if (!consentedToPrep) {
                continue;
            }

            result.add(mapToPregnantAndPostpartumAtHighRiskObject(eligibility, patient));
            processedPatientIds.add(patientId);
        }

        System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk dataset: " + result.size() + " records found");
        return result;
    }

    /**
     * Pregnant and postpartum patients at high risk linked to PrEP
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> pregnantAndPostpartumAtHighRiskLinkedToPrEP(Date fetchDate) {
        System.out.println("INFO - IL: Started generating Pregnant and postpartum at high risk linked to PrEP dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        if (fetchDate.after(new Date())) {
            throw new IllegalArgumentException("Fetch date cannot be in the future.");
        }

        // PrEP eligibility window: ALL relevant encounters must have encounterDatetime within the last 72 hours
        final Date now = new Date();
        final Date threeDaysAgo = Date.from(java.time.Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS));

        // Still honor incremental fetching, but don't go earlier than the PrEP window
        final Date effectiveFromDate = fetchDate.after(threeDaysAgo) ? fetchDate : threeDaysAgo;

        List<SimpleObject> result = new ArrayList<>();
        ConceptService conceptService = Context.getConceptService();
        EncounterService encounterService = Context.getEncounterService();

        // ---- Concepts used to qualify the cohort (latest HTS eligibility + latest HTS test) ----
        Concept pregnantQstn = conceptService.getConceptByUuid(Metadata.Concept.PREGNANCY_STATUS);
        Concept breastfeedingQstn = conceptService.getConceptByUuid(Metadata.Concept.CURRENTLY_BREASTFEEDING);
        Concept yes = conceptService.getConceptByUuid(Metadata.Concept.YES);

        Concept htsScrRiskQstn = conceptService.getConcept(HTS_RISK_SCR_QSTN);
        Concept htsScrHighRiskResult = conceptService.getConcept(HTS_HIGH_RISK_ANS);
        Concept htsScrHighestRiskResult = conceptService.getConcept(HTS_HIGHEST_RISK_ANS);

        Concept htsFinalTestQuestion = conceptService.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsNegativeResult = conceptService.getConcept(HtsConstants.HTS_NEGATIVE_RESULT_CONCEPT_ID);

        // PrEP consent (from PrEP Risk Assessment)
        Concept consentedToPrEPQstn = conceptService.getConcept(CONSENTED_TO_PREP_QSTN);
        Concept consentedToPrEPAnswer = conceptService.getConceptByUuid(Metadata.Concept.YES);

        if (pregnantQstn == null || breastfeedingQstn == null || yes == null
                || htsScrRiskQstn == null || htsScrHighRiskResult == null || htsScrHighestRiskResult == null
                || htsFinalTestQuestion == null || htsNegativeResult == null
                || consentedToPrEPQstn == null || consentedToPrEPAnswer == null) {
            log.error("Required concepts are missing; cannot build Pregnant and postpartum at high risk linked to PrEP dataset");
            return result;
        }

        // ---- Metadata (forms/types) ----
        List<EncounterType> htsEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS)
        );
        List<Form> htsTestingForms = Arrays.asList(
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );
        Form htsEligibilityForm = MetadataUtils.existing(Form.class, HTS_ELIGIBILITY_FORM);

        // PrEP linkage encounters (what we are reporting on/after fetchDate, but still within the 72-hour window)
        List<EncounterType> prepInitialFUPEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, PrEP_INITIAl_ENCOUNTER)
        );
        Form prepInitialForm = MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM);

        // PrEP consent source
        List<EncounterType> prepRiskAssessmentEncounterType = Collections.singletonList(
                MetadataUtils.existing(EncounterType.class, PrEP_RISK_ASSESSMENT_ENCOUNTER_UUID)
        );
        Form prepRiskAssessmentForm = MetadataUtils.existing(Form.class, PrEP_RISK_ASSESSMENT_FORM_UUID);

        // ---- Pull PrEP linkages on/after effectiveFromDate and within the 72-hour window (driver set) ----
        List<Encounter> prepLinkageEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                Collections.singletonList(prepInitialForm),
                                prepInitialFUPEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        if (prepLinkageEncounters.isEmpty()) {
            System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk linked to PrEP dataset: 0 records found");
            return result;
        }

        // Keep only the latest linkage per patient (avoid duplicates if multiple PrEP encounters exist after fetchDate)
        Map<Integer, Encounter> latestPrepLinkageByPatientId = new HashMap<>();
        for (Encounter e : prepLinkageEncounters) {
            if (e == null || e.getPatient() == null || e.getPatient().getPatientId() == null || e.getEncounterDatetime() == null) {
                continue;
            }
            Integer pid = e.getPatient().getPatientId();
            Encounter existing = latestPrepLinkageByPatientId.get(pid);

            Date candidateDate = e.getEncounterDatetime();
            Date existingDate = existing != null ? existing.getEncounterDatetime() : null;

            if (existing == null || (candidateDate != null && (existingDate == null || candidateDate.after(existingDate)))) {
                latestPrepLinkageByPatientId.put(pid, e);
            }
        }

        if (latestPrepLinkageByPatientId.isEmpty()) {
            System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk linked to PrEP dataset: 0 records found");
            return result;
        }

        // ---- Pull HTS eligibility + HTS tests + PrEP risk assessment within the same 72-hour window ----
        // (We limit to effectiveFromDate to reduce load, since anything older is out of window anyway.)
        List<Encounter> eligibilityEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                Collections.singletonList(htsEligibilityForm),
                                htsEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        List<Encounter> htsTestEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                htsTestingForms,
                                htsEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        List<Encounter> prepRiskAssessmentEncounters = Optional.ofNullable(encounterService.getEncounters(
                        new EncounterSearchCriteria(
                                null, null, effectiveFromDate, null, null,
                                Collections.singletonList(prepRiskAssessmentForm),
                                prepRiskAssessmentEncounterType,
                                null, null, null,
                                false
                        )
                )).orElse(Collections.emptyList()).stream()
                .filter(e -> e != null && e.getEncounterDatetime() != null)
                .filter(e -> !e.getEncounterDatetime().before(threeDaysAgo) && !e.getEncounterDatetime().after(now))
                .collect(Collectors.toList());

        Map<Integer, List<Encounter>> eligibilityByPatientId = eligibilityEncounters.stream()
                .filter(e -> e.getPatient() != null && e.getPatient().getPatientId() != null)
                .collect(Collectors.groupingBy(e -> e.getPatient().getPatientId()));

        Map<Integer, List<Encounter>> htsTestsByPatientId = htsTestEncounters.stream()
                .filter(e -> e.getPatient() != null && e.getPatient().getPatientId() != null)
                .collect(Collectors.groupingBy(e -> e.getPatient().getPatientId()));

        Map<Integer, List<Encounter>> prepRiskAssessmentByPatientId = prepRiskAssessmentEncounters.stream()
                .filter(e -> e.getPatient() != null && e.getPatient().getPatientId() != null)
                .collect(Collectors.groupingBy(e -> e.getPatient().getPatientId()));

        Set<Integer> processedPatientIds = new HashSet<>();

        // ---- Build results: each record = PrEP linkage within window that occurs AFTER screening + test + risk assessment (also within window) ----
        for (Map.Entry<Integer, Encounter> entry : latestPrepLinkageByPatientId.entrySet()) {
            Integer patientId = entry.getKey();
            Encounter prepLinkageEncounter = entry.getValue();
            Patient patient = prepLinkageEncounter.getPatient();

            if (patient == null || patient.getPatientId() == null) {
                continue;
            }
            if (processedPatientIds.contains(patientId)) {
                continue;
            }

            String gender = patient.getGender();
            if (gender == null || !"F".equalsIgnoreCase(gender.trim())) {
                continue;
            }

            Date linkageDateTime = prepLinkageEncounter.getEncounterDatetime();
            if (linkageDateTime == null) {
                continue;
            }

            // Anchor on eligibility screening (must occur before HTS test), and enforce that:
            // eligibility + HTS test + PrEP behavioral assessment happen within the SAME 3-day window.
            Encounter latestEligibility = latestOnOrBefore(eligibilityByPatientId.get(patientId), linkageDateTime);
            if (latestEligibility == null || latestEligibility.getEncounterDatetime() == null) {
                continue;
            }
            Date eligibilityDateTime = latestEligibility.getEncounterDatetime();
            if (linkageDateTime.before(eligibilityDateTime)) {
                continue; // linkage must be after eligibility screening
            }

            boolean pregnantOrBreastfeeding =
                    EmrUtils.encounterThatPassCodedAnswer(latestEligibility, pregnantQstn, yes)
                            || EmrUtils.encounterThatPassCodedAnswer(latestEligibility, breastfeedingQstn, yes);
            if (!pregnantOrBreastfeeding) {
                continue;
            }

            boolean isHighRisk =
                    EmrUtils.encounterThatPassCodedAnswer(latestEligibility, htsScrRiskQstn, htsScrHighRiskResult)
                            || EmrUtils.encounterThatPassCodedAnswer(latestEligibility, htsScrRiskQstn, htsScrHighestRiskResult);
            if (!isHighRisk) {
                continue;
            }

            Date windowStart = eligibilityDateTime.before(threeDaysAgo) ? threeDaysAgo : eligibilityDateTime;
            Date windowEnd = Date.from(eligibilityDateTime.toInstant().plus(3, java.time.temporal.ChronoUnit.DAYS));
            if (windowEnd.after(linkageDateTime)) {
                windowEnd = linkageDateTime; // everything must occur on/before linkage
            }
            if (windowEnd.after(now)) {
                windowEnd = now;
            }

            // PrEP behavioral assessment must be AFTER eligibility and within the 3-day window
            Encounter latestPrepRiskInWindow = latestWithin(prepRiskAssessmentByPatientId.get(patientId), windowStart, windowEnd);
            if (latestPrepRiskInWindow == null || latestPrepRiskInWindow.getEncounterDatetime() == null) {
                continue;
            }
            if (latestPrepRiskInWindow.getEncounterDatetime().before(eligibilityDateTime)) {
                continue; // eligibility must be before behavioral assessment
            }
            boolean consentedToPrep = EmrUtils.encounterThatPassCodedAnswer(latestPrepRiskInWindow, consentedToPrEPQstn, consentedToPrEPAnswer);
            if (!consentedToPrep) {
                continue;
            }

            // HTS test must be AFTER eligibility and within the 3-day window
            Encounter latestHtsTestInWindow = latestWithin(htsTestsByPatientId.get(patientId), windowStart, windowEnd);
            if (latestHtsTestInWindow == null || latestHtsTestInWindow.getEncounterDatetime() == null) {
                continue;
            }
            if (latestHtsTestInWindow.getEncounterDatetime().before(eligibilityDateTime)) {
                continue; // eligibility must be before HTS test
            }

            // Latest test must be HIV negative
            boolean testedNegative = EmrUtils.encounterThatPassCodedAnswer(latestHtsTestInWindow, htsFinalTestQuestion, htsNegativeResult);
            if (!testedNegative) {
                continue;
            }

            String prepNumber = CaseSurveillanceUtils.getPrepNumber(patient);
            String prepRegimen = CaseSurveillanceUtils.getPrepRegimen(patient);

            // Map using the PrEP linkage encounter (dataset is about the linkage event)
            result.add(mapToPregnantAndPostpartumAtHighRiskOnPrEPObject(prepLinkageEncounter, patient, prepNumber, prepRegimen));
            processedPatientIds.add(patientId);
        }

        System.out.println("INFO - IL: Finished generating Pregnant and postpartum at high risk linked to PrEP dataset: " + result.size() + " records found");
        return result;
    }
    @SuppressWarnings("unchecked")
    public List<SimpleObject> eligibleForVl() {
        System.out.println("INFO - IL: Started generating eligible for VL dataset (ETL-based)...");
        Session session = Context.getRegisteredComponent("sessionFactory", SessionFactory.class).getCurrentSession();

        String sql = "select e.patient_id,\n" +
                "b.pregnant,\n" +
                "b.breastFeedingStatus,\n" +
                "b.positiveHivTestDate,\n" +
                "b.visitDate,\n" +
                "b.artStartDate,\n" +
                "b.lastVlOrderDate,\n" +
                "b.lastVlResultsDate,\n" +
                "b.vlOrderReason,\n" +
                "b.dateCreated,\n" +
                "e.upn,\n" +
                "b.vlResult\n" +
                "from (select fup.visit_date,\n" +
                " fup.patient_id,\n" +
                " max(e.visit_date)                                                      as enroll_date,\n" +
                " greatest(max(fup.visit_date), ifnull(max(d.visit_date), '0000-00-00')) as latest_vis_date,\n" +
                " greatest(mid(max(concat(fup.visit_date, fup.next_appointment_date)), 11),\n" +
                "          ifnull(max(d.visit_date), '0000-00-00'))                      as latest_tca,\n" +
                " d.patient_id                                                           as disc_patient,\n" +
                " d.effective_disc_date                                                  as effective_disc_date,\n" +
                " max(d.visit_date)                                                      as date_discontinued,\n" +
                " de.patient_id                                                          as started_on_drugs,\n" +
                " p.unique_patient_no                                                    as upn\n" +
                "from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
                "   join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n" +
                "   join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n" +
                "   left join kenyaemr_etl.etl_drug_event de\n" +
                "             on e.patient_id = de.patient_id and de.program = 'HIV' and\n" +
                "                date(de.date_started) <= date(CURRENT_DATE)\n" +
                "   left outer JOIN\n" +
                "(select patient_id,\n" +
                "       coalesce(date(effective_discontinuation_date), visit_date) visit_date,\n" +
                "       max(date(effective_discontinuation_date)) as               effective_disc_date\n" +
                "from kenyaemr_etl.etl_patient_program_discontinuation\n" +
                "where date(visit_date) <= date(CURRENT_DATE)\n" +
                "  and program_name = 'HIV'\n" +
                "group by patient_id) d on d.patient_id = fup.patient_id\n" +
                "where fup.visit_date <= date(CURRENT_DATE)\n" +
                "group by patient_id\n" +
                "having (started_on_drugs is not null and started_on_drugs <> '')\n" +
                "and (\n" +
                "(\n" +
                "  (timestampdiff(DAY, date(latest_tca), date(CURRENT_DATE)) <= 30 and\n" +
                "   ((date(d.effective_disc_date) > date(CURRENT_DATE) or date(enroll_date) > date(d.effective_disc_date)) or\n" +
                "    d.effective_disc_date is null))\n" +
                "      and\n" +
                "  (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or\n" +
                "   disc_patient is null)\n" +
                "  )\n" +
                ")) e\n" +
                "INNER JOIN (SELECT t.patient_id,\n" +
                "                case t.pregnancy_status when 1065 then 'YES' when 1066 then 'NO' end     as pregnant,\n" +
                "                case t.breastfeeding_status when 1065 then 'YES' when 1066 then 'NO' end as breastFeedingStatus,\n" +
                "                t.date_confirmed_hiv_positive                                            as positiveHivTestDate,\n" +
                "                t.latest_hiv_followup_visit                                              as visitDate,\n" +
                "                t.date_started_art                                                       as artStartDate,\n" +
                "                t.date_test_requested                                                    as lastVlOrderDate,\n" +
                "                t.date_test_result_received                                              as lastVlResultsDate,\n" +
                "                t.order_reason                                                           as vlOrderReason,\n" +
                "                t.date_created                                                           as dateCreated,\n" +
                "                t.vl_result                                                              as vlResult\n" +
                "         FROM (SELECT v.*,\n" +
                "                      d.DOB,\n" +
                "                      -- Substitution Logic: If current is null and requested is later than base/previous, use previous\n" +
                "                      IF(\n" +
                "                              v.vl_result IS NULL\n" +
                "                                  AND v.date_test_result_received IS NULL\n" +
                "                                  AND v.date_test_requested >\n" +
                "                                      GREATEST(COALESCE(v.base_viral_load_test_date, '1900-01-01'),\n" +
                "                                          COALESCE(v.previous_date_test_requested, '1900-01-01')\n" +
                "                                      ),\n" +
                "                              v.previous_test_result,\n" +
                "                              v.vl_result\n" +
                "                      ) AS effective_vl_result,\n" +
                "                      IF(\n" +
                "                              v.vl_result IS NULL\n" +
                "                                  AND v.date_test_result_received IS NULL\n" +
                "                                  AND v.date_test_requested >\n" +
                "                                      GREATEST(COALESCE(v.base_viral_load_test_date, '1900-01-01'),\n" +
                "                                             COALESCE(v.previous_date_test_requested, '1900-01-01')\n" +
                "                                      ),\n" +
                "                              v.previous_date_test_requested,\n" +
                "                              v.date_test_requested\n" +
                "                      ) AS effective_date_requested\n" +
                "               FROM kenyaemr_etl.etl_viral_load_validity_tracker v\n" +
                "                        INNER JOIN kenyaemr_etl.etl_patient_demographics d\n" +
                "                                   ON v.patient_id = d.patient_id\n" +
                "               WHERE v.date_test_requested <= CURRENT_DATE) t\n" +
                "         WHERE (\n" +
                "             (TIMESTAMPDIFF(MONTH, t.date_started_art, CURRENT_DATE) >= 3 AND\n" +
                "              t.base_viral_load_test_result IS NULL) -- First VL new on ART\n" +
                "                 OR\n" +
                "             (\n" +
                "                 (t.pregnancy_status = 1065 OR t.breastfeeding_status = 1065)\n" +
                "                     AND TIMESTAMPDIFF(MONTH, t.date_started_art, CURRENT_DATE) >= 3\n" +
                "                     AND\n" +
                "                 (t.effective_vl_result IS NOT NULL AND t.effective_date_requested < CURRENT_DATE)\n" +
                "                     AND (t.order_reason NOT IN (159882, 1434, 2001237, 163718))\n" +
                "                 )\n" +
                "                 OR\n" +
                "             (\n" +
                "                 t.lab_test = 856 AND t.effective_vl_result >= 200\n" +
                "                     AND TIMESTAMPDIFF(MONTH, t.effective_date_requested, CURRENT_DATE) >= 3\n" +
                "                 )\n" +
                "                 OR\n" +
                "             (\n" +
                "                 ((t.lab_test = 1305 AND t.effective_vl_result = 1302) OR t.effective_vl_result < 200)\n" +
                "                     AND TIMESTAMPDIFF(MONTH, t.effective_date_requested, CURRENT_DATE) >= 6\n" +
                "                     AND TIMESTAMPDIFF(YEAR, t.DOB, t.effective_date_requested) BETWEEN 0 AND 24\n" +
                "                 )\n" +
                "                 OR\n" +
                "             (\n" +
                "                 ((t.lab_test = 1305 AND t.effective_vl_result = 1302) OR t.effective_vl_result < 200)\n" +
                "                     AND TIMESTAMPDIFF(MONTH, t.effective_date_requested, CURRENT_DATE) >= 12\n" +
                "                     AND TIMESTAMPDIFF(YEAR, t.DOB, t.effective_date_requested) > 24\n" +
                "                 )\n" +
                "                 OR\n" +
                "             (\n" +
                "                 (t.pregnancy_status = 1065 OR t.breastfeeding_status = 1065)\n" +
                "                     AND TIMESTAMPDIFF(MONTH, t.date_started_art, CURRENT_DATE) >= 3\n" +
                "                     AND (\n" +
                "                     t.order_reason IN (159882, 1434, 2001237, 163718)\n" +
                "                         AND TIMESTAMPDIFF(MONTH, t.effective_date_requested, CURRENT_DATE) >= 6\n" +
                "                     )\n" +
                "                     AND ((t.lab_test = 1305 AND t.effective_vl_result = 1302) OR\n" +
                "                          (t.effective_vl_result < 200))\n" +
                "                 )\n" +
                "             )\n" +
                "           AND NOT (\n" +
                "             t.vl_result IS NULL\n" +
                "                 AND t.date_test_result_received IS NULL\n" +
                "                 AND t.base_viral_load_test_result IS NULL\n" +
                "                 AND t.previous_test_result IS NULL\n" +
                "             )) b on e.patient_id = b.patient_id\n" +
                "group by b.patient_id;\n";

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
     * Patients who have died since the last fetch date (mortality)
     */
    public List<SimpleObject> mortality(Date fetchDate) {
        System.out.println("INFO - IL: Started generating mortality dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        if (fetchDate.after(new Date())) {
            throw new IllegalArgumentException("Fetch date cannot be in the future.");
        }

        List<SimpleObject> result = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();
        PersonService personService = Context.getPersonService();

        // Discontinuation forms where death details may be captured as Obs
        List<Form> discForms = Arrays.asList(
                MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_DISCONTINUATION),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_DISCONTINUATION),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DISCONTINUATION),
                MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_HEI_COMPLETION),
                MetadataUtils.existing(Form.class, OTZMetadata._Form.OTZ_DISCONTINUATION_FORM)
        );

        // Pull discontinuation encounters since fetchDate (source for death obs)
        List<Encounter> discontinuationEncounters = Optional.ofNullable(
                encounterService.getEncounters(null, null, fetchDate, null, discForms, null, null, null, null, false)
        ).orElse(Collections.emptyList());

        // Dedupe: one record per patient
        Set<Integer> processedPatientIds = new HashSet<>();

        // 1) Prefer Obs-derived death details from discontinuation encounters
        for (Encounter encounter : discontinuationEncounters) {
            if (encounter == null || encounter.getPatient() == null || encounter.getPatient().getPatientId() == null) {
                continue;
            }

            Patient patient = encounter.getPatient();
            Integer patientId = patient.getPatientId();
            if (processedPatientIds.contains(patientId)) {
                continue;
            }

            Date obsDeathDate = null;
            String obsCauseOfDeath = null;

            for (Obs ob : Optional.ofNullable(encounter.getObs()).orElse(Collections.emptySet())) {
                if (ob == null || ob.getConcept() == null || ob.getConcept().getUuid() == null) {
                    continue;
                }
                int conceptId = ob.getConcept().getConceptId();

                if (DEATH_DATE.equals(conceptId)) {
                    Date candidate = (ob.getValueDatetime() != null) ? ob.getValueDatetime() : ob.getObsDatetime();
                    if (candidate != null && (obsDeathDate == null || candidate.after(obsDeathDate))) {
                        obsDeathDate = candidate;
                    }
                } else if (CAUSE_OF_DEATH.equals(conceptId)) {
                    if (ob.getValueCoded() != null && ob.getValueCoded().getName() != null) {
                        obsCauseOfDeath = ob.getValueCoded().getName().getName();
                    }
                }
            }

            // qualify only those who died since fetchDate
            if (obsDeathDate == null || obsDeathDate.before(fetchDate)) {
                continue;
            }

            result.add(mapToMortalityObject(
                    patient,
                    encounter.getDateCreated(),
                    encounter.getDateChanged(),
                    obsDeathDate,
                    obsCauseOfDeath
            ));
            processedPatientIds.add(patientId);
        }

        // 2) Add remaining deceased persons from Person.deathDate / Person.causeOfDeath
        List<Person> deceasedPersons = Optional.ofNullable(personService.getPeople("", true)).orElse(Collections.emptyList());
        for (Person person : deceasedPersons) {
            if (person == null || person.getDeathDate() == null) {
                continue;
            }
            if (person.getDeathDate().before(fetchDate)) {
                continue;
            }
            if (!(person instanceof Patient)) {
                continue;
            }

            Patient patient = (Patient) person;
            if (patient.getPatientId() == null) {
                continue;
            }

            Integer patientId = patient.getPatientId();
            if (processedPatientIds.contains(patientId)) {
                continue;
            }

            String causeOfDeath = null;
            if (person.getCauseOfDeath() != null && person.getCauseOfDeath().getName() != null) {
                causeOfDeath = person.getCauseOfDeath().getName().getName();
            }

            result.add(mapToMortalityObject(
                    patient,
                    patient.getDateCreated(),
                    patient.getDateChanged(),
                    person.getDeathDate(),
                    causeOfDeath
            ));
            processedPatientIds.add(patientId);
        }

        System.out.println("INFO - IL: Finished generating mortality dataset: " + result.size() + " records found");
        return result;
    }

    /**
     *
     * @param fetchDate
     * @return
     */
    public List<SimpleObject> prEPUptake(Date fetchDate) {
        System.out.println("INFO - IL: Started generating PrEP uptake dataset... ");
        if (fetchDate == null) {
            throw new IllegalArgumentException("Fetch date cannot be null");
        }
        final Date effectiveFromDate = aMomentBefore(fetchDate);

        EncounterService encounterService = Context.getEncounterService();
        List<SimpleObject> result = new ArrayList<>();

        // --- Metadata (resolve once) ---
        Form prepEnrollmentForm = MetadataUtils.existing(Form.class, PREP_ENROLLMENT_FORM);
        Form prepInitiationForm = MetadataUtils.existing(Form.class, PREP_INITIATION_FORM);

        EncounterType prepEnrollmentEncounterType =
                MetadataUtils.existing(EncounterType.class, PREP_ENROLLMENT_ENC_TYPE);

        Form prepInitialForm = MetadataUtils.existing(Form.class, PrEP_INITIAL_FORM);
        EncounterType prepInitialEncounterType =
                MetadataUtils.existing(EncounterType.class, PrEP_INITIAl_ENCOUNTER);

        List<Form> prepVisitForms = Arrays.asList(
                MetadataUtils.existing(Form.class, PrEP_FOLLOWUP_FORM),
                MetadataUtils.existing(Form.class, PrEP_REFILL_FORM)
        );
        List<EncounterType> prepVisitEncounterTypes = Arrays.asList(
                MetadataUtils.existing(EncounterType.class, PrEP_FOLLOWUP_ENCOUNTER_TYPE),
                MetadataUtils.existing(EncounterType.class, PrEP_REFILL_ENCOUNTER_TYPE)
        );

        // Enrollment-related forms
        List<Form> prepEnrollmentForms = Arrays.asList(prepEnrollmentForm, prepInitiationForm);

        // Pull relevant encounters since last fetch (driver set)
        List<Encounter> enrollmentSinceFetch = Optional.ofNullable(encounterService.getEncounters(
                new EncounterSearchCriteria(
                        null, null, effectiveFromDate, null, null,
                        prepEnrollmentForms,
                        Collections.singleton(prepEnrollmentEncounterType),
                        null, null, null,
                        false
                ))).orElse(Collections.emptyList());

        List<Encounter> initialsSinceFetch = Optional.ofNullable(encounterService.getEncounters(
                new EncounterSearchCriteria(
                        null, null, effectiveFromDate, null, null,
                        Collections.singletonList(prepInitialForm),
                        Collections.singleton(prepInitialEncounterType),
                        null, null, null,
                        false
                ))).orElse(Collections.emptyList());

        List<Encounter> followupsAndRefillsSinceFetch = Optional.ofNullable(encounterService.getEncounters(
                new EncounterSearchCriteria(
                        null, null, effectiveFromDate, null, null,
                        prepVisitForms,
                        prepVisitEncounterTypes,
                        null, null, null,
                        false
                ))).orElse(Collections.emptyList());

        // Latest SINCE fetch per patient
        Map<Integer, Encounter> latestEnrollmentSinceFetchByPatientId = new HashMap<>();
        Map<Integer, Encounter> latestInitialSinceFetchByPatientId = new HashMap<>();
        Map<Integer, Encounter> latestVisitSinceFetchByPatientId = new HashMap<>();

        for (Encounter e : enrollmentSinceFetch) {
            mergeLatest(latestEnrollmentSinceFetchByPatientId, e);
        }
        for (Encounter e : initialsSinceFetch) {
            mergeLatest(latestInitialSinceFetchByPatientId, e);
        }
        for (Encounter e : followupsAndRefillsSinceFetch) {
            mergeLatest(latestVisitSinceFetchByPatientId, e);
        }

        // Cohort = union of keys across the three maps
        Set<Integer> patientIds = new HashSet<>();
        patientIds.addAll(latestEnrollmentSinceFetchByPatientId.keySet());
        patientIds.addAll(latestInitialSinceFetchByPatientId.keySet());
        patientIds.addAll(latestVisitSinceFetchByPatientId.keySet());

        if (patientIds.isEmpty()) {
            System.out.println("INFO - IL: Finished generating PrEP uptake dataset: 0 records found");
            return result;
        }

        for (Integer patientId : patientIds) {
            if (patientId == null) {
                continue;
            }

            Encounter latestSinceFetch = latestOf(
                    latestOf(
                            latestFromMap(latestEnrollmentSinceFetchByPatientId, patientId),
                            latestFromMap(latestInitialSinceFetchByPatientId, patientId)
                    ),
                    latestFromMap(latestVisitSinceFetchByPatientId, patientId)
            );

            if (latestSinceFetch == null || latestSinceFetch.getEncounterDatetime() == null) {
                continue;
            }
            if (latestSinceFetch.getEncounterDatetime().before(fetchDate)) {
                continue;
            }

            Patient patient = latestSinceFetch.getPatient();
            if (patient == null) {
                continue;
            }

            List<Encounter> patientEnrollments = encounterService.getEncounters(
                    patient, null, null, null,
                    prepEnrollmentForms,
                    Collections.singleton(prepEnrollmentEncounterType),
                    null, null, null,
                    false
            );

            if (patientEnrollments == null || patientEnrollments.isEmpty()) {
                continue;
            }

            Encounter latestEnrollmentAllTime = CaseSurveillanceUtils.getLatestEncounter(patientEnrollments);
            if (latestEnrollmentAllTime == null || latestEnrollmentAllTime.getEncounterDatetime() == null) {
                continue;
            }

            Date prepStartDate = computePrepStartDateFromEnrollmentEncounters(patientEnrollments);
            if (prepStartDate == null) {
                prepStartDate = latestEnrollmentAllTime.getEncounterDatetime();
            }

            List<Encounter> patientInitials = encounterService.getEncounters(
                    patient, null, null, null,
                    Collections.singletonList(prepInitialForm),
                    Collections.singleton(prepInitialEncounterType),
                    null, null, null,
                    false
            );
            Encounter latestInitialAllTime = CaseSurveillanceUtils.getLatestEncounter(patientInitials);

            Encounter latestInitialSinceFetch = latestFromMap(latestInitialSinceFetchByPatientId, patientId);
            Encounter latestVisitSinceFetch = latestFromMap(latestVisitSinceFetchByPatientId, patientId);
            Encounter latestClinicalTouchpointSinceFetch = latestOf(latestInitialSinceFetch, latestVisitSinceFetch);
            Encounter enrollmentSince = latestFromMap(latestEnrollmentSinceFetchByPatientId, patientId);
            Encounter sourceEncounter = (latestClinicalTouchpointSinceFetch != null)
                    ? latestClinicalTouchpointSinceFetch
                    : (enrollmentSince != null ? enrollmentSince : latestEnrollmentAllTime);

            Encounter initialForReason = (latestInitialSinceFetch != null) ? latestInitialSinceFetch : latestInitialAllTime;

            String prepStatus = firstNonBlank(
                    getCodedValue(sourceEncounter, PrEP_STATUS),
                    getCodedValue(latestEnrollmentAllTime, PrEP_STATUS)
            );

            String reasonForSwitching = firstNonBlank(
                    getCodedValue(sourceEncounter, REASON_FOR_SWITCHING_PrEP),
                    getCodedValue(latestEnrollmentAllTime, REASON_FOR_SWITCHING_PrEP),
                    (latestInitialAllTime != null ? getCodedValue(latestInitialAllTime, REASON_FOR_SWITCHING_PrEP) : null)
            );

            Date dateSwitchedPrep = firstNonNull(
                    CaseSurveillanceUtils.getDateValue(sourceEncounter, DATE_SWITCHED_PrEP),
                    CaseSurveillanceUtils.getDateValue(latestEnrollmentAllTime, DATE_SWITCHED_PrEP),
                    (latestInitialAllTime != null ? CaseSurveillanceUtils.getDateValue(latestInitialAllTime, DATE_SWITCHED_PrEP) : null)
            );

            String prepMethod = firstNonBlank(
                    getCodedValue(sourceEncounter, TYPE_OF_PrEP),
                    getCodedValue(latestEnrollmentAllTime, TYPE_OF_PrEP),
                    (latestInitialAllTime != null ? getCodedValue(latestInitialAllTime, TYPE_OF_PrEP) : null)
            );

            String prepRegimen = firstNonBlank(
                    getCodedValue(sourceEncounter, PrEP_REGIMEN),
                    getCodedValue(latestEnrollmentAllTime, PrEP_REGIMEN),
                    (latestInitialAllTime != null ? getCodedValue(latestInitialAllTime, PrEP_REGIMEN) : null)
            );

            String reasonForStartingPrep = CaseSurveillanceUtils.getCodedValue(
                    initialForReason,
                    REASON_FOR_STARTING_PrEP
            );
            if (prepStartDate == null) {
                prepStartDate = latestEnrollmentAllTime.getEncounterDatetime();
            }
            result.add(mapToPrEPUptakeObject(
                    latestSinceFetch,
                    patient,
                    getPrepNumber(patient),
                    prepMethod,
                    prepStartDate,
                    prepStatus,
                    reasonForStartingPrep,
                    reasonForSwitching,
                    dateSwitchedPrep,
                    prepRegimen
            ));
        }

        System.out.println("INFO - IL: Finished generating PrEP uptake dataset: " + result.size() + " records found");
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
        // Mortality
        List<SimpleObject> mortality = mortality(fetchDate);
        for (SimpleObject death : mortality) {
            payload.add(mapToDatasetStructure(death, "mortality"));
        }

        List<SimpleObject> prepUptake = prEPUptake(fetchDate);
        for (SimpleObject prep : prepUptake) {
            payload.add(mapToDatasetStructure(prep, "prep_uptake"));
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

        Integer patientId = getIntegerValue.apply("patientId");
        String shaNumber = CaseSurveillanceUtils.resolveShaNumber(patientId);

        // Populate client details
        client.put("county", getStringValue.apply("county"));
        client.put("subCounty", getStringValue.apply("subCounty"));
        client.put("ward", getStringValue.apply("ward"));
        client.put("patientPk", getIntegerValue.apply("patientId"));
        client.put("sex", getStringValue.apply("sex"));
        client.put("dob", getStringValue.apply("dob"));
        client.put("shaNumber", shaNumber);

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
        } else if ("mortality".equals(eventType)) {
            event.put("deathDate", getStringValue.apply("deathDate"));
            event.put("causeOfDeath", getStringValue.apply("causeOfDeath"));
        } else if ("prep_uptake".equals(eventType)) {
            event.put("prepNumber", getStringValue.apply("prepNumber"));
            event.put("prepStartDate", getStringValue.apply("prepStartDate"));
            event.put("prepStatus", getStringValue.apply("prepStatus"));
            event.put("reasonForStartingPrep", getStringValue.apply("reasonForStartingPrep"));
            event.put("reasonForSwitchingPrep", getStringValue.apply("reasonForSwitchingPrep"));
            event.put("dateSwitchedPrep", getStringValue.apply("dateSwitchedPrep"));
            event.put("prepType", getStringValue.apply("prepType"));
            event.put("prepRegimen", getStringValue.apply("prepRegimen"));
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
        System.out.println("Case surveillance Payload: " + jsonPayload);
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

