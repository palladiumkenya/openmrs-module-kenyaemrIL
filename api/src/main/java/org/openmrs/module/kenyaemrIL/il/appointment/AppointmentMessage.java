package org.openmrs.module.kenyaemrIL.il.appointment;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;

/**
 * @author Stanslaus Odhiambo
 *         Created on 15/01/2018.
 */
public class AppointmentMessage{
    private MESSAGE_HEADER MESSAGE_HEADER;
    private PATIENT_IDENTIFICATION PATIENT_IDENTIFICATION;
    private APPOINTMENT_INFORMATION[] APPOINTMENT_INFORMATION;

    public APPOINTMENT_INFORMATION[] getAPPOINTMENT_INFORMATION ()
    {
        return APPOINTMENT_INFORMATION;
    }

    public void setAPPOINTMENT_INFORMATION (APPOINTMENT_INFORMATION[] APPOINTMENT_INFORMATION)
    {
        this.APPOINTMENT_INFORMATION = APPOINTMENT_INFORMATION;
    }

    public PATIENT_IDENTIFICATION getPATIENT_IDENTIFICATION ()
    {
        return PATIENT_IDENTIFICATION;
    }

    public void setPATIENT_IDENTIFICATION (PATIENT_IDENTIFICATION PATIENT_IDENTIFICATION)
    {
        this.PATIENT_IDENTIFICATION = PATIENT_IDENTIFICATION;
    }

    public MESSAGE_HEADER getMESSAGE_HEADER ()
    {
        return MESSAGE_HEADER;
    }

    public void setMESSAGE_HEADER (MESSAGE_HEADER MESSAGE_HEADER)
    {
        this.MESSAGE_HEADER = MESSAGE_HEADER;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [APPOINTMENT_INFORMATION = "+APPOINTMENT_INFORMATION+", PATIENT_IDENTIFICATION = "+PATIENT_IDENTIFICATION+", MESSAGE_HEADER = "+MESSAGE_HEADER+"]";
    }
}
