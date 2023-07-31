package org.openmrs.module.kenyaemrIL.programEnrollment;

public class Program_Enrollment_Message {
    private String patient_type;
    private String entry_point;
    private String target_program;
    private PATIENT_REFERRAL_INFORMATION service_request;

    public String getPatient_type() {
        return patient_type;
    }

    public void setPatient_type(String patient_type) {
        this.patient_type = patient_type;
    }

    public String getEntry_point() {
        return entry_point;
    }

    public void setEntry_point(String entry_point) {
        this.entry_point = entry_point;
    }

    public String getTarget_program() {
        return target_program;
    }

    public void setTarget_program(String target_program) {
        this.target_program = target_program;
    }

    public PATIENT_REFERRAL_INFORMATION getService_request() {
        return service_request;
    }

    public void setService_request(PATIENT_REFERRAL_INFORMATION service_request) {
        this.service_request = service_request;
    }
}
