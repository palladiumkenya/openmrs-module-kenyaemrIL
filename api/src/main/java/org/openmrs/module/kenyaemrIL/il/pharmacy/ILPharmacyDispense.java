package org.openmrs.module.kenyaemrIL.il.pharmacy;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILPharmacyDispense {

    private List<PharmacyEncodedOrder> encodedOrders;
    private List<PharmacyDispense> dispenseList;
    private PharmacyDispense[] dispense_information;
    private CommonOrderDetails[] commonOrderDetails;
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;



    public List<PharmacyEncodedOrder> getEncodedOrders() {
        return encodedOrders;
    }

    public void setEncodedOrders(List<PharmacyEncodedOrder> encodedOrders) {
        this.encodedOrders = encodedOrders;
    }

    public List<PharmacyDispense> getDispenseList() {
        return dispenseList;
    }

    public void setDispenseList(List<PharmacyDispense> dispenseList) {
        this.dispenseList = dispenseList;
    }

    public PharmacyDispense[] getDispense_information() {
        return dispense_information;
    }

    public void setDispense_information(PharmacyDispense[] dispense_information) {
        this.dispense_information = dispense_information;
    }

    public CommonOrderDetails[] getCommon_Order_information() {
        return commonOrderDetails;
    }

    public void setCommon_Order_information(CommonOrderDetails[] common_order_information) {
        this.commonOrderDetails = commonOrderDetails;
    }

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
}
