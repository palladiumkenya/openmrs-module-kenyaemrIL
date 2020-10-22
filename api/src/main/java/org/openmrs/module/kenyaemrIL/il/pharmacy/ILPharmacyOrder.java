package org.openmrs.module.kenyaemrIL.il.pharmacy;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILPharmacyOrder {
    private COMMON_ORDER_DETAILS COMMONORDERDETAILS;
    private List<PharmacyEncodedOrder> encodedOrderList;
    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;


    public COMMON_ORDER_DETAILS getCOMMONORDERDETAILS() {
        return COMMONORDERDETAILS;
    }

    public void setCOMMONORDERDETAILS(COMMON_ORDER_DETAILS COMMONORDERDETAILS) {
        this.COMMONORDERDETAILS = COMMONORDERDETAILS;
    }

    public List<PharmacyEncodedOrder> getEncodedOrderList() {
        return encodedOrderList;
    }

    public void setEncodedOrderList(List<PharmacyEncodedOrder> encodedOrderList) {
        this.encodedOrderList = encodedOrderList;
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
