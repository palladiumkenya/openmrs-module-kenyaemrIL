package org.openmrs.module.kenyaemrIL.il.utils;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;

import java.util.Date;

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
        messageHeader.setSending_facility("");
        messageHeader.setReceiving_application("IL");
        messageHeader.setReceiving_facility("");
        messageHeader.setMessage_datetime("");
        messageHeader.setSecurity("");
        messageHeader.setMessage_type("");
        messageHeader.setProcessing_id("P");
        return messageHeader;
    }
}
