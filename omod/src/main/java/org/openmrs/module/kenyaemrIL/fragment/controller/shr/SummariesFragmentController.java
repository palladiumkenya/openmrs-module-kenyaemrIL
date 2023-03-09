/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrIL.fragment.controller.shr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for a patient summary details
 */
public class SummariesFragmentController {
    protected static final Log log = LogFactory.getLog(SummariesFragmentController.class);
    private AdministrationService administrationService = Context.getAdministrationService();
    public static final String NATIONAL_UNIQUE_PATIENT_IDENTIFIER = "f85081e2-b4be-4e48-b3a4-7994b69bb101";


    public void controller(@FragmentParam("patient") Patient patient, FragmentModel model) {


        PatientIdentifier patientIdentifier = patient.getPatientIdentifier(Context.getPatientService().getPatientIdentifierTypeByUuid(NATIONAL_UNIQUE_PATIENT_IDENTIFIER));
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);


        // get the patient encounters based on this unique ID
        Bundle patientResourceBundle;
        Bundle observationResourceBundle;
        org.hl7.fhir.r4.model.Resource fhirResource;
        org.hl7.fhir.r4.model.Resource fhirObservationResource;
        org.hl7.fhir.r4.model.Patient fhirPatient = null;
        org.hl7.fhir.r4.model.Observation fhirObservation = null;
        if(patientIdentifier != null) {
            patientResourceBundle = fhirConfig.fetchPatientResource(patientIdentifier.getIdentifier());

            if (patientResourceBundle == null || patientResourceBundle.getEntry().isEmpty()) {
                return;
            }
            fhirResource = patientResourceBundle.getEntry().get(0).getResource();
            if(fhirResource.getResourceType().toString().equals("Patient")) {
                fhirPatient = (org.hl7.fhir.r4.model.Patient) fhirResource;
            }

            if (fhirPatient != null) {
                observationResourceBundle = fhirConfig.fetchObservationResource(fhirPatient);
                if (!observationResourceBundle.getEntry().isEmpty()) {
                    for (int i = 0; i < observationResourceBundle.getEntry().size(); i++) {
                        fhirObservationResource = observationResourceBundle.getEntry().get(i).getResource();
                        fhirObservation = (org.hl7.fhir.r4.model.Observation) fhirObservationResource;

                        System.out.println("Obs: " + fhirObservation.getCode().getCodingFirstRep().getDisplay() + ", value: " + getObservationValue(fhirObservation));
                    }
                }
            }

        }

        model.addAttribute("patient", fhirPatient != null? fhirPatient.getBirthDate() : "Not found");
    }

    private String getObservationValue(org.hl7.fhir.r4.model.Observation fhirObservation) {
        if (fhirObservation != null) {
            if (fhirObservation.getValue() instanceof Quantity) {
                return fhirObservation.getValueQuantity().getValue().toString();
            } else if (fhirObservation.getValue() instanceof CodeableConcept) {
                return fhirObservation.getValueCodeableConcept().getCodingFirstRep().getDisplay();
            } else if (fhirObservation.getValue() instanceof DateTimeType) {
                return fhirObservation.getValueDateTimeType().getValue().toString();
            } else if (fhirObservation.getValue() instanceof IntegerType) {
                return fhirObservation.getValueIntegerType().getValue().toString();
            } else if (fhirObservation.getValue() instanceof BooleanType) {
                return fhirObservation.getValueBooleanType().getValue().toString();
            } else if (fhirObservation.getValue() instanceof StringType) {
                return fhirObservation.getValueStringType().getValue();
            }
        }
        return "";
    }

}
