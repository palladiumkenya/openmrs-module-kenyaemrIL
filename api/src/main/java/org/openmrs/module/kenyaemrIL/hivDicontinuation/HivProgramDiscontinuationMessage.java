package org.openmrs.module.kenyaemrIL.hivDicontinuation;

import org.openmrs.module.kenyaemrIL.hivDicontinuation.artReferral.PATIENT_REFERRAL_INFORMATION;

public class HivProgramDiscontinuationMessage {
    private String discontinuation_reason;
    private String effective_discontinuation_date;
    private PATIENT_REFERRAL_INFORMATION service_request;

    public String getDiscontinuation_reason() {
        return discontinuation_reason;
    }

    public void setDiscontinuation_reason(String discontinuation_reason) {
        this.discontinuation_reason = discontinuation_reason;
    }

    public String getEffective_discontinuation_date() {
        return effective_discontinuation_date;
    }

    public void setEffective_discontinuation_date(String effective_discontinuation_date) {
        this.effective_discontinuation_date = effective_discontinuation_date;
    }

    public PATIENT_REFERRAL_INFORMATION getService_request() {
        return service_request;
    }

    public void setService_request(PATIENT_REFERRAL_INFORMATION service_request) {
        this.service_request = service_request;
    }
}
