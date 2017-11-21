package org.openmrs.module.kenyaemrIL.il.pharmacy;

import org.openmrs.module.kenyaemrIL.il.MessageHeader;
import org.openmrs.module.kenyaemrIL.il.PatientIdentification;

import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class ILPharmacyOrder {
    private CommonOrderDetails commonOrderDetails;
    private List<PharmacyEncodedOrder> encodedOrderList;
    private PatientIdentification patientIdentification;
    private MessageHeader messageHeader;


    public CommonOrderDetails getCommonOrderDetails() {
        return commonOrderDetails;
    }

    public void setCommonOrderDetails(CommonOrderDetails commonOrderDetails) {
        this.commonOrderDetails = commonOrderDetails;
    }

    public List<PharmacyEncodedOrder> getEncodedOrderList() {
        return encodedOrderList;
    }

    public void setEncodedOrderList(List<PharmacyEncodedOrder> encodedOrderList) {
        this.encodedOrderList = encodedOrderList;
    }

    public PatientIdentification getPatientIdentification() {
        return patientIdentification;
    }

    public void setPatientIdentification(PatientIdentification patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public void setMessageHeader(MessageHeader messageHeader) {
        this.messageHeader = messageHeader;
    }
}
