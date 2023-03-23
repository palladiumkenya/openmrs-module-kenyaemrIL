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
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by codehub on 10/30/15.
 * A fragment controller for a patient summary details
 */
public class SummariesFragmentController {
    protected static final Log log = LogFactory.getLog(SummariesFragmentController.class);
    private AdministrationService administrationService = Context.getAdministrationService();
    public static final String NATIONAL_UNIQUE_PATIENT_IDENTIFIER = "f85081e2-b4be-4e48-b3a4-7994b69bb101";


    public void controller(FragmentModel model) {
        Patient patient = Context.getPatientService().getPatientByUuid("9c3ca7bf-8110-43d8-a35d-f6f2509f3c05");
        PatientIdentifier patientIdentifier = patient.getPatientIdentifier(Context.getPatientService().getPatientIdentifierTypeByUuid(NATIONAL_UNIQUE_PATIENT_IDENTIFIER));
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        List<Visit> allVisits = Context.getVisitService().getVisitsByPatient(patient);
        Date lastVisitDate = !allVisits.isEmpty() ? allVisits.get(0).getStartDatetime() : null;

        System.out.println("Last visit date: " + lastVisitDate.toString());
        // get the patient encounters based on this unique ID
        Bundle patientResourceBundle;
        Bundle observationResourceBundle;
        org.hl7.fhir.r4.model.Resource fhirResource;
        org.hl7.fhir.r4.model.Resource fhirObservationResource;
        org.hl7.fhir.r4.model.Patient fhirPatient = null;
        org.hl7.fhir.r4.model.Observation fhirObservation = null;
        List<SimpleObject> vitalObs = new ArrayList<SimpleObject>();
        List<SimpleObject> labObs = new ArrayList<SimpleObject>();
        List<SimpleObject> complaints = new ArrayList<SimpleObject>();
        List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
        if (patientIdentifier != null) {
            patientResourceBundle = fhirConfig.fetchPatientResource(patientIdentifier.getIdentifier());

            if (patientResourceBundle != null && !patientResourceBundle.getEntry().isEmpty()) {
                fhirResource = patientResourceBundle.getEntry().get(0).getResource();
                if (fhirResource.getResourceType().toString().equals("Patient")) {
                    fhirPatient = (org.hl7.fhir.r4.model.Patient) fhirResource;
                }

                if (fhirPatient != null) {
                    //Observations
                    if (lastVisitDate != null) {
                        observationResourceBundle = fhirConfig.fetchObservationResource(fhirPatient, lastVisitDate);
                    } else {
                        observationResourceBundle = fhirConfig.fetchObservationResource(fhirPatient);
                    }

                    if (!observationResourceBundle.getEntry().isEmpty()) {
                        for (int i = 0; i < observationResourceBundle.getEntry().size(); i++) {
                            fhirObservationResource = observationResourceBundle.getEntry().get(i).getResource();
                            fhirObservation = (org.hl7.fhir.r4.model.Observation) fhirObservationResource;
                            for (Coding c : fhirObservation.getCode().getCoding()) {
                                if (fhirConfig.vitalConcepts().contains(c.getCode())) {
                                    vitalObs.add(SimpleObject.create(
                                            "display", c.getDisplay(),
                                            "date", ILUtils.getObservationValue(fhirObservation),
                                            "value", new SimpleDateFormat("yyyy-MM-dd").format(fhirObservation.getEffectiveDateTimeType().toCalendar().getTime())));
                                }

                                if (fhirConfig.labConcepts().contains(c.getCode())) {
                                    labObs.add(SimpleObject.create(
                                            "display", c.getDisplay(),
                                            "date", ILUtils.getObservationValue(fhirObservation),
                                            "value", new SimpleDateFormat("yyyy-MM-dd").format(fhirObservation.getEffectiveDateTimeType().toCalendar().getTime())));
                                }

                                if (fhirConfig.presentingComplaints().contains(c.getCode())) {
                                    complaints.add(SimpleObject.create(
                                            "display", c.getDisplay(),
                                            "date", ILUtils.getObservationValue(fhirObservation),
                                            "value", new SimpleDateFormat("yyyy-MM-dd").format(fhirObservation.getEffectiveDateTimeType().toCalendar().getTime())));
                                }

                                if (fhirConfig.diagnosisObs().contains(c.getCode())) {
                                    diagnosis.add(SimpleObject.create(
                                            "display", c.getDisplay(),
                                            "date", ILUtils.getObservationValue(fhirObservation),
                                            "value", new SimpleDateFormat("yyyy-MM-dd").format(fhirObservation.getEffectiveDateTimeType().toCalendar().getTime())));
                                }
                            }
                        }
                    }
                }

                //Conditions
                Bundle complaintsBundle = fhirConfig.fetchConditions(fhirPatient);
                if (complaintsBundle.hasEntry()) {
                    complaintsBundle.getEntry().forEach(e -> {
                        Condition condition = (Condition) e.getResource();

                    });
                }


            }
        }

        model.addAttribute("vitalsObs", vitalObs);
        model.addAttribute("labObs", labObs);
        model.addAttribute("complaints", complaints);
        model.addAttribute("diagnosis", diagnosis);
    }


}
