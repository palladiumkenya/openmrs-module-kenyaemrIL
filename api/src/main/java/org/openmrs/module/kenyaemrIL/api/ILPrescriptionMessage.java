package org.openmrs.module.kenyaemrIL.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.EXTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.PATIENT_NAME;
import org.openmrs.module.kenyaemrIL.il.pharmacy.COMMON_ORDER_DETAILS;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ORDERING_PHYSICIAN;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PHARMACY_ENCODED_ORDER;
import org.openmrs.module.kenyaemrIL.il.pharmacy.PLACER_ORDER_NUMBER;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Generates IL prescription message from a list of encounters
 */
public class ILPrescriptionMessage {

    private final Log log = LogFactory.getLog(this.getClass());

    public static ILMessage generatePrescriptionMessage(Patient patient, List<Encounter> encounters) {
        ILMessage ilMessage = new ILMessage();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        PATIENT_IDENTIFICATION patientIdentification = new PATIENT_IDENTIFICATION();
        COMMON_ORDER_DETAILS commonOrderDetails = new COMMON_ORDER_DETAILS();
        List<INTERNAL_PATIENT_ID> internalPatientIds = new ArrayList<INTERNAL_PATIENT_ID>();
        EXTERNAL_PATIENT_ID epd = new EXTERNAL_PATIENT_ID();
        ORDERING_PHYSICIAN orderingPhysician = new ORDERING_PHYSICIAN();
        PLACER_ORDER_NUMBER placerOrderNumber = new PLACER_ORDER_NUMBER();
        INTERNAL_PATIENT_ID ipd;
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

        for (Encounter drugOrderEncounter : encounters) {

            System.out.println("Processing encounter :========================================");

            // check if encounter is for single drug or regimen.
            // use encounter.getOrders().size(): 1 = single, >1 for multiple
            int ordersInAnEncounter = drugOrderEncounter.getOrders().size();
            pharmacyEncodedOrder = new PHARMACY_ENCODED_ORDER();

            if (ordersInAnEncounter > 1) { // process regimen
                List<Order> orderList = new ArrayList<>();
                orderList.addAll(drugOrderEncounter.getOrders());
                Order order = orderList.get(0);
                DrugOrder drugOrder = (DrugOrder) order;

              //  String duration = drugOrder.getDuration() != null ? drugOrder.getDuration().toString() : "";
                if(drugOrder.getDuration() != null) {
                    pharmacyEncodedOrder.setDuration(drugOrder.getDuration().toString());
                }
                String dose = drugOrder.getDose() != null ? drugOrder.getDose().toString() : "";
                String quantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity().toString() : "";
                String instructions = order.getInstructions() != null ? order.getInstructions() : "";
                String frequency = drugOrder.getFrequency() != null ? drugOrder.getFrequency().toString() : "";
                placerOrderNumber.setNumber(order.getOrderId().toString());
                commonOrderDetails.setPlacer_order_number(placerOrderNumber);
                pharmacyEncodedOrder.setCoding_system("NASCOP_CODES");
                pharmacyEncodedOrder.setDosage(drugOrder.getDose() != null ? drugOrder.getDose().toString() : "");
                pharmacyEncodedOrder.setFrequency(frequency);
                pharmacyEncodedOrder.setQuantity_prescribed(quantity);
                pharmacyEncodedOrder.setDrug_name(drugOrder.getOrderGroup().getOrderSet().getName());

                List<String> regimenStrength = new ArrayList<>();
                for (Order regOrder : drugOrderEncounter.getOrders()) {
                    DrugOrder dOrder = (DrugOrder) regOrder;
                    regimenStrength.add( dOrder.getDose().intValue() + dOrder.getDoseUnits().getName().getName());
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
                String dose = drugOrder.getDose() != null ? drugOrder.getDose().toString() : "";
                String quantity = drugOrder.getQuantity() != null ? drugOrder.getQuantity().toString() : "";
                String instructions = order.getInstructions() != null ? order.getInstructions() : "";
                String frequency = drugOrder.getFrequency() != null ? drugOrder.getFrequency().toString() : "";
                placerOrderNumber.setNumber(order.getOrderId().toString());
                commonOrderDetails.setPlacer_order_number(placerOrderNumber);
                pharmacyEncodedOrder.setDuration(duration);
                pharmacyEncodedOrder.setCoding_system("NASCOP_CODES");
                pharmacyEncodedOrder.setDosage(dose);
                pharmacyEncodedOrder.setFrequency(frequency);
                pharmacyEncodedOrder.setQuantity_prescribed(quantity);
                //  pharmacyEncodedOrder.setStrength("Single");
                pharmacyEncodedOrder.setPrescription_notes(instructions);
                String ts = formatter.format(order.getDateActivated());
                pharmacyEncodedOrder.setPharmacy_order_date(ts);
                pharmacyEncodedOrders.add(pharmacyEncodedOrder);
            }
        }

        ilMessage.setPatient_identification(patientIdentification);
        ilMessage.setCommon_order_details(commonOrderDetails);
        ilMessage.setPharmacy_encoded_order(pharmacyEncodedOrders.toArray(new PHARMACY_ENCODED_ORDER[pharmacyEncodedOrders.size()]));
        return ilMessage;
    }


}
