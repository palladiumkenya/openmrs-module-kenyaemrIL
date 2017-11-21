package org.openmrs.module.kenyaemrIL.il;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class PatientVisit {
    private String visitDate;
    private String patientSource;
    private String hivCareEnrollmentDate;
    private String patientType;

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }

    public String getPatientSource() {
        return patientSource;
    }

    public void setPatientSource(String patientSource) {
        this.patientSource = patientSource;
    }

    public String getHivCareEnrollmentDate() {
        return hivCareEnrollmentDate;
    }

    public void setHivCareEnrollmentDate(String hivCareEnrollmentDate) {
        this.hivCareEnrollmentDate = hivCareEnrollmentDate;
    }

    public String getPatientType() {
        return patientType;
    }

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }
}
