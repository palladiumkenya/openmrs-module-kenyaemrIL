package org.openmrs.module.kenyaemrIL.il.utils;

import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.util.PrivilegeConstants;

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

        Location location = getDefaultLocation();
        String facilityMfl = getDefaultLocationMflCode(location);
        messageHeader.setSending_facility(facilityMfl);
        messageHeader.setReceiving_application("IL");
        messageHeader.setReceiving_facility("");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        messageHeader.setMessage_datetime(formatter.format(new Date()));
        messageHeader.setSecurity("");
        messageHeader.setMessage_type(messageType);
        messageHeader.setProcessing_id("P");
        return messageHeader;
    }

    public static Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    public static String getDefaultLocationMflCode(Location location) {
        String MASTER_FACILITY_CODE = "8a845a89-6aa5-4111-81d3-0af31c45c002";

        if(location == null) {
            location = getDefaultLocation();
        }
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            for (LocationAttribute attr : location.getAttributes()) {
                if (attr.getAttributeType().getUuid().equals(MASTER_FACILITY_CODE) && !attr.isVoided()) {
                    return attr.getValueReference();
                }
            }
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }
        return null;
    }
}
