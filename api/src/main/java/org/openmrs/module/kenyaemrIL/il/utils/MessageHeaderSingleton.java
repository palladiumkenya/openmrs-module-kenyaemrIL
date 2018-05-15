package org.openmrs.module.kenyaemrIL.il.utils;

import org.openmrs.GlobalProperty;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Stanslaus Odhiambo
 *         Created on 16/01/2018.
 */
public class MessageHeaderSingleton {
    private static final MESSAGE_HEADER messageHeader = new MESSAGE_HEADER();

    private MessageHeaderSingleton() {

    }

    public static MESSAGE_HEADER getMessageHeaderInstance(String messageType) {
        messageHeader.setSending_application("KENYAEMR");

        GlobalProperty globalPropertyObject = org.openmrs.api.context.Context.getAdministrationService().getGlobalPropertyObject("facility.mflcode");
        messageHeader.setSending_facility(globalPropertyObject.getValue().toString());
        messageHeader.setReceiving_application("IL");
        messageHeader.setReceiving_facility("");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        messageHeader.setMessage_datetime(formatter.format(new Date()));
        messageHeader.setSecurity("");
        messageHeader.setMessage_type(messageType);
        messageHeader.setProcessing_id("P");
        return messageHeader;
    }
}
