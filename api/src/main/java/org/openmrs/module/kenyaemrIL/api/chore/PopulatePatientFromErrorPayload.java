/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrIL.api.chore;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * documents patient_id column in the IL errors table.
 *
 */
@Component("kenyaemrIL.chore.PopulatePatientFromErrorPayload")
public class PopulatePatientFromErrorPayload extends AbstractChore {

    /**
     * @see AbstractChore#perform(PrintWriter)
     */

    @Override
    public void perform(PrintWriter out) {

        List<KenyaEMRILMessageErrorQueue> errors = Context.getService(KenyaEMRILService.class).fetchAllMhealthErrors(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE));
        PatientIdentifierType cccIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);

        for (KenyaEMRILMessageErrorQueue errorData : errors) {
            String patientCCCNumberFromPayload = ILUtils.getPatientIdentifierFromILPayload(ILUtils.CCC_NUMBER_IDENTIFIER_TYPE, errorData.getMessage());
            List<Patient> patients = Context.getPatientService().getPatients(null, patientCCCNumberFromPayload, Arrays.asList(cccIdType), true);
            Patient patient = null;
            if (patients.size() > 0) {
                patient = patients.get(0);
            }
            if (patient != null) {
                errorData.setPatient(patient);
                service.saveKenyaEMRILMessageErrorQueue(errorData);
            }
        }

        out.println("Completed filling the non-null patient id");
    }
}
