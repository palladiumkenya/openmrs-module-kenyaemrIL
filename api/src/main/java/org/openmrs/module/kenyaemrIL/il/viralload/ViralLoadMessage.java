package org.openmrs.module.kenyaemrIL.il.viralload;

import org.openmrs.module.kenyaemrIL.il.MESSAGE_HEADER;
import org.openmrs.module.kenyaemrIL.il.PATIENT_IDENTIFICATION;
import org.openmrs.module.kenyaemrIL.il.observation.VIRAL_LOAD_RESULT;

/**
 * @author Stanslaus Odhiambo
 * Created on 01/02/2018.
 */
public class ViralLoadMessage {

    private MESSAGE_HEADER message_header;
    private PATIENT_IDENTIFICATION patient_identification;
    private VIRAL_LOAD_RESULT[] viral_load_result;

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

    public VIRAL_LOAD_RESULT[] getViral_load_result() {
        return viral_load_result;
    }

    public void setViral_load_result(VIRAL_LOAD_RESULT[] viral_load_result) {
        this.viral_load_result = viral_load_result;
    }
}
