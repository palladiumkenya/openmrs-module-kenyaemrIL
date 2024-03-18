package org.openmrs.module.kenyaemrIL.util;

public enum ServiceDepartments {
    GENERALOUTPATIENT("General Outpatient"),
    SPECIALIZEDCLINICS("Specialized Clinics"),
    MCHANDFAMILYPLANNING("MCH And FP"),
    DENTAL("Dental"),
    OTHERSERVICES("Other OPD Services"),
    INPATIENT("Inpatient"),
    MATERNITY("Maternity"),
    ORTHOPEDIC("Orthopedic"),
    SPECIALIZEDSERVICES("Specialiized Services"),
    PHARMACY("Pharmacy"),
    MORTUARY("Mortuary"),
    MEDICALRECORDS("Medical Records"),
    FINANCE("Finance");
    private final String departmentName;
    ServiceDepartments(String departmentName) {
        this.departmentName = departmentName;
    }
    public String getDepartmentName() {
        return departmentName;
    }
}
