package org.openmrs.module.kenyaemrIL.il.utils;

import org.openmrs.GlobalProperty;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;

/**
 * @author Stanslaus Odhiambo
 *         Created on 16/01/2018.
 */
public class MessageHeaderSingleton {
    private static final MESSAGE_HEADER messageHeader = new MESSAGE_HEADER();

    private MessageHeaderSingleton() {

    }

    public static MESSAGE_HEADER getMessageHeaderInstance() {
        messageHeader.setSending_application("KENYA EMR");

        GlobalProperty globalPropertyObject = org.openmrs.api.context.Context.getAdministrationService().getGlobalPropertyObject("facility.mflcode");
        messageHeader.setSending_facility(globalPropertyObject.getValue().toString());
        messageHeader.setReceiving_application("IL");
        messageHeader.setReceiving_facility("");
        messageHeader.setMessage_datetime("");
        messageHeader.setSecurity("");
        messageHeader.setMessage_type("");
        messageHeader.setProcessing_id("P");
        return messageHeader;
    }
}
