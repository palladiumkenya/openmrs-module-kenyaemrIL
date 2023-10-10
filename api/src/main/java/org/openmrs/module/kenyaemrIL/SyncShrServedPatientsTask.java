package org.openmrs.module.kenyaemrIL;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.GlobalProperty;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SyncShrServedPatientsTask extends AbstractTask {
    @Override
    public void execute() {
        System.out.println("Executing  SHR Sync Task .................");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /**Fetch the last date of sync*/
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("syncShrServedPatients.lastFetchDateAndTime");

        try {
            fetchDate = formatter.parse(globalPropertyObject.getValue().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatedCompletedReferrals(fetchDate);
    }

    public void updatedCompletedReferrals(Date date) {
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);
        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        List<ExpectedTransferInPatients> completedCommunityReferralsList = ilService.getCommunityReferrals("COMMUNITY", "COMPLETED");

        PersonAttribute referralStatusAttribute = new PersonAttribute();
        PersonAttributeType referralStatusAttributeType = Context.getPersonService().getPersonAttributeTypeByUuid("df7e9996-23b5-4f66-a799-97498d19850d");

        for (ExpectedTransferInPatients patient : completedCommunityReferralsList) {
            if (referralStatusAttributeType != null) {
                referralStatusAttribute.setAttributeType(referralStatusAttributeType);
                referralStatusAttribute.setValue("Completed");
                patient.getPatient().addAttribute(referralStatusAttribute);
                Context.getPatientService().savePatient(patient.getPatient());
            }

            IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
            ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, patient.getPatientSummary());
            serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.COMPLETED);
            try {
                fhirConfig.updateReferral(serviceRequest); // submit directly to shr
//                fhirConfig.postReferralResourceToOpenHim(serviceRequest); // submit via openHim to shr
                patient.setReferralStatus("FINAL");
                Context.getService(KenyaEMRILService.class).createPatient(patient);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
