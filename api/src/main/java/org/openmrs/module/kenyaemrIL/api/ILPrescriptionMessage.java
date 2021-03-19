package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.RegimenMappingUtils;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION_SIMPLE;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.observation.OBSERVATION_RESULT;
import org.openmrs.module.kenyaemrIL.il.pharmacy.COMMON_ORDER_DETAILS;
import org.openmrs.module.kenyaemrIL.il.pharmacy.FILLER_ORDER_NUMBER;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ORDERING_PHYSICIAN;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_ENCODED_ORDER;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PLACER_ORDER_NUMBER;
import org.openmrs.module.kenyaemrIL.kenyaemrUtils.Utils;
import org.openmrs.ui.framework.SimpleObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generates IL prescription message from a list of encounters
 */
public class ILPrescriptionMessage {

    private static final Log log = LogFactory.getLog(ILPrescriptionMessage.class);
    public static final Locale LOCALE = Locale.ENGLISH;


    public static ILMessage generatePrescriptionMessage(Patient patient, List<Encounter> encounters) {

        Integer heightConcept = 5090;
        Integer weightConcept = 5089;
        ConceptService conceptService = Context.getConceptService();

        ILMessage ilMessage = new ILMessage();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        PATIENT_IDENTIFICATION_SIMPLE patientIdentification = new PATIENT_IDENTIFICATION_SIMPLE();
        COMMON_ORDER_DETAILS commonOrderDetails = new COMMON_ORDER_DETAILS();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        ORDERING_PHYSICIAN orderingPhysician = new ORDERING_PHYSICIAN();
        FILLER_ORDER_NUMBER fillerOrderNumber = new FILLER_ORDER_NUMBER();
        PLACER_ORDER_NUMBER placerOrderNumber = new PLACER_ORDER_NUMBER();
        commonOrderDetails.setFiller_order_number(fillerOrderNumber);
        INTERNAL_PATIENT_ID ipd;

        List<OBSERVATION_RESULT> observationResults = new ArrayList<>();
        OBSERVATION_RESULT observationResult = null;
        //        Form the internal patient IDs
        for (PatientIdentifier patientIdentifier : patient.getIdentifiers()) {
            ipd = new INTERNAL_PATIENT_ID();
            if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("Unique Patient Number")) {
                ipd.setAssigning_authority("CCC");
                ipd.setId(patientIdentifier.getIdentifier());
                ipd.setIdentifier_type("CCC_NUMBER");
                internalPatientIds.add(ipd);
            } else if (patientIdentifier.getIdentifierType().getName().equalsIgnoreCase("MPI GODS NUMBER")) {
                if (patientIdentifier.getIdentifierType().getName() != null) {
                    epd.setAssigning_authority("MPI");
                    epd.setId(patientIdentifier.getIdentifier());
                    epd.setIdentifier_type("GODS_NUMBER");
                    patientIdentification.setExternal_patient_id(epd);
                }
                continue;
            }
        }


        patientIdentification.setInternal_patient_id(internalPatientIds);
        patientIdentification.setExternal_patient_id(epd);


        //Set the patient name
        PATIENT_NAME patientname = new PATIENT_NAME();
        PersonName personName = patient.getPersonName();
        patientname.setFirst_name(personName.getGivenName() != null ? personName.getGivenName() : "");
        patientname.setMiddle_name(personName.getMiddleName() != null ? personName.getMiddleName() : "");
        patientname.setLast_name(personName.getFamilyName() != null ? personName.getFamilyName() : "");
        patientIdentification.setPatient_name(patientname);

        // set the patient orders
        List<PHARMACY_ENCODED_ORDER> pharmacyEncodedOrders = new ArrayList<>();
        PHARMACY_ENCODED_ORDER pharmacyEncodedOrder = null;
        String groupOrderNumber = encounters.get(0).getEncounterId().toString();

        for (Encounter drugOrderEncounter : encounters) {

            // check if encounter is for single drug or regimen.
            // use encounter.getOrders().size(): 1 = single, >1 for multiple
            int ordersInAnEncounter = drugOrderEncounter.getOrders().size();
            pharmacyEncodedOrder = new PHARMACY_ENCODED_ORDER();

            if (ordersInAnEncounter > 1) { // process regimen
                List<Order> orderList = new ArrayList<>();
                orderList.addAll(drugOrderEncounter.getOrders());

                Comparator compareById = new Comparator() {
                    @Override
                    public int compare(Object o1,Object o2){
                        Order s1=(Order) o1;
                        Order s2=(Order) o2;
                        return Integer.compare(s1.getOrderId(), s2.getOrderId());
                    }
                };

                Collections.sort(orderList, compareById);
                Order order = orderList.get(0);
                DrugOrder drugOrder = (DrugOrder) order;

                String duration = drugOrder.getDuration() != null ? String.valueOf(drugOrder.getDuration().intValue()) : "";
                String quantity = drugOrder.getQuantity() != null ? String.valueOf(drugOrder.getQuantity().intValue()) : "";
                String instructions = order.getInstructions() != null ? order.getInstructions() : "";
                String frequency = "";
                if (drugOrder.getFrequency() != null && drugOrder.getFrequency().getConcept() != null) {
                    frequency = drugOrder.getFrequency().getConcept().getShortNameInLocale(LOCALE) != null ? drugOrder.getFrequency().getConcept().getShortNameInLocale(LOCALE).getName() : drugOrder.getFrequency().getConcept().getName().getName();
                }
                // setting this to the order id of the first element in the group

                // do this just once
                if (StringUtils.isBlank(placerOrderNumber.getNumber() )) {
                    placerOrderNumber.setNumber(groupOrderNumber);
                    commonOrderDetails.setPlacer_order_number(placerOrderNumber);
                    orderingPhysician.setFirst_name(drugOrder.getOrderer().getPerson().getGivenName());
                    orderingPhysician.setMiddle_name(drugOrder.getOrderer().getPerson().getMiddleName());
                    orderingPhysician.setLast_name(drugOrder.getOrderer().getPerson().getFamilyName());
                    commonOrderDetails.setOrdering_physician(orderingPhysician);

                }

                Encounter currentRegimenEncounter = RegimenMappingUtils.getLastEncounterForProgram(patient, "ARV");
                SimpleObject regimenDetails = RegimenMappingUtils.buildRegimenChangeObject(currentRegimenEncounter.getObs(), currentRegimenEncounter);
                String regimenName = (String) regimenDetails.get("regimenShortDisplay");
                String regimenLine = (String) regimenDetails.get("regimenLine");
                String nascopCode = "";
                if (StringUtils.isNotBlank(regimenName )) {
                    nascopCode = RegimenMappingUtils.getDrugNascopCodeByDrugNameAndRegimenLine(regimenName, regimenLine);
                }

                if (StringUtils.isBlank(nascopCode) && StringUtils.isNotBlank(regimenLine)) {
                    nascopCode = RegimenMappingUtils.getNonStandardCodeFromRegimenLine(regimenLine);
                    instructions += ". Prescribed drugs: " + regimenName;
                }

                pharmacyEncodedOrder.setCoding_system("NASCOP_CODES");
                pharmacyEncodedOrder.setDosage(drugOrder.getDose() != null ? String.valueOf(drugOrder.getDose().intValue()) : "");
                pharmacyEncodedOrder.setFrequency(frequency);
                pharmacyEncodedOrder.setDuration(duration);
                pharmacyEncodedOrder.setQuantity_prescribed(quantity);
                pharmacyEncodedOrder.setDrug_name(StringUtils.isNotBlank(nascopCode) ? nascopCode : regimenName);

                // we are setting the group's order number to that of the first element in the group.
                // when processing dispense message from ADT, this should be checked and handled appropriately
                pharmacyEncodedOrder.setPrescription_number(drugOrder.getOrderId().toString());

                List<String> regimenStrength = new ArrayList<>();
                for (Order regOrder : orderList) {
                    DrugOrder dOrder = (DrugOrder) regOrder;
                    regimenStrength.add( dOrder.getDose().intValue() + (dOrder.getDoseUnits().getShortNameInLocale(LOCALE) != null ? dOrder.getDoseUnits().getShortNameInLocale(LOCALE).getName() : dOrder.getDoseUnits().getName().getName()));
                }

                pharmacyEncodedOrder.setStrength(StringUtils.join(regimenStrength, "/"));
                pharmacyEncodedOrder.setPrescription_notes(instructions);
                String ts = formatter.format(order.getDateActivated());
                pharmacyEncodedOrder.setPharmacy_order_date(ts);
                pharmacyEncodedOrders.add(pharmacyEncodedOrder);

            } else { // process single drug

                List<Order> orderList = new ArrayList<>();
                orderList.addAll(drugOrderEncounter.getOrders());
                Order order = orderList.get(0);
                DrugOrder drugOrder = (DrugOrder) order;


                String duration = drugOrder.getDuration() != null ? drugOrder.getDuration().toString() : "";
                String dose = drugOrder.getDose() != null ? String.valueOf(drugOrder.getDose().intValue()) : "";
                String quantity = drugOrder.getQuantity() != null ? String.valueOf(drugOrder.getQuantity().intValue()) : "";
                String instructions = order.getInstructions() != null ? order.getInstructions() : "";
                String frequency = "";
                if (drugOrder.getFrequency() != null && drugOrder.getFrequency().getConcept() != null) {
                    frequency = drugOrder.getFrequency().getConcept().getShortNameInLocale(LOCALE) != null ? drugOrder.getFrequency().getConcept().getShortNameInLocale(LOCALE).getName() : drugOrder.getFrequency().getConcept().getName().getName();
                }
                // do this just once
                if (StringUtils.isBlank(placerOrderNumber.getNumber() )) {
                    placerOrderNumber.setNumber(groupOrderNumber);
                    commonOrderDetails.setPlacer_order_number(placerOrderNumber);
                    orderingPhysician.setFirst_name(drugOrder.getOrderer().getPerson().getGivenName());
                    orderingPhysician.setMiddle_name(drugOrder.getOrderer().getPerson().getMiddleName());
                    orderingPhysician.setLast_name(drugOrder.getOrderer().getPerson().getFamilyName());
                    commonOrderDetails.setOrdering_physician(orderingPhysician);
                }

                pharmacyEncodedOrder.setDrug_name(drugOrder.getConcept().getName().getName());
                pharmacyEncodedOrder.setDuration(duration);
                pharmacyEncodedOrder.setCoding_system("NASCOP_CODES");
                pharmacyEncodedOrder.setDosage(dose);
                pharmacyEncodedOrder.setFrequency(frequency);
                pharmacyEncodedOrder.setQuantity_prescribed(quantity);
                pharmacyEncodedOrder.setStrength(drugOrder.getDose().intValue() + (drugOrder.getDoseUnits().getShortNameInLocale(LOCALE) != null ? drugOrder.getDoseUnits().getShortNameInLocale(LOCALE).getName() : drugOrder.getDoseUnits().getName().getName()));
                pharmacyEncodedOrder.setPrescription_number(drugOrder.getOrderId().toString());

                pharmacyEncodedOrder.setPrescription_notes(instructions);
                String ts = formatter.format(order.getDateActivated());
                pharmacyEncodedOrder.setPharmacy_order_date(ts);
                pharmacyEncodedOrders.add(pharmacyEncodedOrder);
            }
        }

        // extract triage information

        try {
            List<Obs> latestWeightObs = Utils.getNLastObs(conceptService.getConcept(weightConcept), patient, 1);

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

        try {
            List<Obs> latestHeightObs = Utils.getNLastObs(conceptService.getConcept(heightConcept), patient, 1);
            if (latestHeightObs.size() > 0) {
                Obs heightObs = latestHeightObs.get(0);

                // compose observation object
                OBSERVATION_RESULT heightObservationResult = new OBSERVATION_RESULT();
                heightObservationResult.setObservation_identifier("HEIGHT");

                heightObservationResult.setSet_id("");
                heightObservationResult.setCoding_system("");
                heightObservationResult.setValue_type("NM");
                heightObservationResult.setObservation_value(String.valueOf(heightObs.getValueNumeric()));
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

        ilMessage.setPatient_identification_simple(patientIdentification);
        ilMessage.setCommon_order_details(commonOrderDetails);
        ilMessage.setPharmacy_encoded_order(pharmacyEncodedOrders.toArray(new PHARMACY_ENCODED_ORDER[pharmacyEncodedOrders.size()]));

        ilMessage.setObservation_result(observationResults.toArray(new OBSERVATION_RESULT[observationResults.size()]));
        return ilMessage;
    }

}
