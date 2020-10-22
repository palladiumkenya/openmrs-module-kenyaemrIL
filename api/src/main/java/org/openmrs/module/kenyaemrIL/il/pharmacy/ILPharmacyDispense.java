package org.openmrs.module.kenyaemrIL.il.pharmacy;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILPharmacyDispense {

    private List<PHARMACY_ENCODED_ORDER> encodedOrders;
    private List<PHARMACY_DISPENSE> dispenseList;
    private PHARMACY_DISPENSE[] dispense_information;
    private COMMON_ORDER_DETAILS common_order_details;
    private PHARMACY_ENCODED_ORDER[] pharmacy_encoded_orders;
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;

    public MESSAGE_HEADER getMessage_header() {
        return message_header;
    }

    public void setMessage_header(MESSAGE_HEADER message_header) {
        this.message_header = message_header;
    }

    public PATIENT_IDENTIFICATION getPatient_identification() {
        return patient_identification;
    }

    public void setPatient_identification(PATIENT_IDENTIFICATION patient_identification) {
        this.patient_identification = patient_identification;
    }



    public List<PHARMACY_ENCODED_ORDER> getEncodedOrders() {
        return encodedOrders;
    }

    public void setEncodedOrders(List<PHARMACY_ENCODED_ORDER> encodedOrders) {
        this.encodedOrders = encodedOrders;
    }

    public List<PHARMACY_DISPENSE> getDispenseList() {
        return dispenseList;
    }

    public void setDispenseList(List<PHARMACY_DISPENSE> dispenseList) {
        this.dispenseList = dispenseList;
    }

    public PHARMACY_DISPENSE[] getDispense_information() { return dispense_information; }

    public void setDispense_information(PHARMACY_DISPENSE[] dispense_information) {
        this.dispense_information = dispense_information;
    }

    public COMMON_ORDER_DETAILS getCommon_Order_Details() { return common_order_details; }

    public void setCommon_Order_Details(COMMON_ORDER_DETAILS common_order_details) {
        this.common_order_details = common_order_details;
    }

    public PHARMACY_ENCODED_ORDER[] getEncodedOrderList() {
        return pharmacy_encoded_orders;
    }

    public void setEncodedOrderList(PHARMACY_ENCODED_ORDER[] pharmacy_encoded_orders) {
        this.pharmacy_encoded_orders = pharmacy_encoded_orders;
    }


}
