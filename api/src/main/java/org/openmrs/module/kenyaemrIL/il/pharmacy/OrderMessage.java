package org.openmrs.module.kenyaemrIL.il.pharmacy;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

public class OrderMessage {

    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private COMMON_ORDER_DETAILS common_order_details;
    private PHARMACY_ENCODED_ORDER[] pharmacy_encoded_orders;


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
    public COMMON_ORDER_DETAILS getCommon_Order_Details() { return common_order_details; }

    public void setCommon_Order_Details(COMMON_ORDER_DETAILS common_order_details) {
        this.common_order_details = common_order_details;
    }

    public PHARMACY_ENCODED_ORDER[] getPharmacy_encoded_order() {
        return pharmacy_encoded_orders;
    }

    public void setPharmacy_encoded_order(PHARMACY_ENCODED_ORDER[] pharmacy_encoded_orders) {
        this.pharmacy_encoded_orders = pharmacy_encoded_orders;
    }

}
