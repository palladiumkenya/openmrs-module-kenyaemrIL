package org.openmrs.module.kenyaemrIL.page.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.metadata.ILMetadata;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@AppPage("kenyaemr.referral.home")
public class CommunityReferralsHomePageController {
    public static final String NUPI = "f85081e2-b4be-4e48-b3a4-7994b69bb101";

    public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException, ParseException {

        List<SimpleObject> activeReferrals = new ArrayList<SimpleObject>();
        List<SimpleObject> completedReferrals = new ArrayList<SimpleObject>();
        PatientIdentifierType nupiIdType = MetadataUtils.existing(PatientIdentifierType.class, NUPI);
        PersonAttributeType communityReferralSourcePA = Context.getPersonService().getPersonAttributeTypeByUuid(ILMetadata._PersonAttributeType.REFERRAL_SOURCE);
        PersonAttributeType communityRefrralStatusPA = Context.getPersonService().getPersonAttributeTypeByUuid(ILMetadata._PersonAttributeType.REFERRAL_STATUS);
        List<Patient> allPatients = Context.getPatientService().getAllPatients();

        for (Patient patient : allPatients) {
            if (patient.getAttribute(communityReferralSourcePA) != null && patient.getAttribute(communityRefrralStatusPA) != null) {
                String networkError = "";
                    // Get all active community referrals
                if (patient.getAttribute(communityReferralSourcePA).getValue().trim().equalsIgnoreCase("Community")) {
                    if (patient.getAttribute(communityRefrralStatusPA).getValue().trim().equalsIgnoreCase("Active")) {
                        // Get person details for community referrals

                        SimpleObject patientPendingObject = SimpleObject.create("id", patient.getId(), "uuid", patient.getUuid(), "nupi", patient.getPatientIdentifier(nupiIdType).getIdentifier(), "givenName", patient
                                        .getGivenName(), "middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "", "familyName", patient.getFamilyName(), "birthdate", kenyaUi.formatDate(patient.getBirthdate()), "gender", patient.getGender(),
                                "status", patient.getAttribute(communityRefrralStatusPA).getValue().trim());
                        activeReferrals.add(patientPendingObject);
                    }
                }
                // Get all completed community referrals
                if (patient.getAttribute(communityReferralSourcePA).getValue().trim().equalsIgnoreCase("Community")) {
                    if (patient.getAttribute(communityRefrralStatusPA).getValue().trim().equalsIgnoreCase("Completed")) {
                        // Get person details for community referrals
                        SimpleObject completedReferralsObject = SimpleObject.create("id", patient.getId(), "uuid", patient.getUuid(),"nupi", patient.getPatientIdentifier(nupiIdType).getIdentifier(), "givenName", patient
                                        .getGivenName(), "middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "", "familyName", patient.getFamilyName(), "birthdate", kenyaUi.formatDate(patient.getBirthdate()), "gender", patient.getGender(),
                                "status", patient.getAttribute(communityRefrralStatusPA).getValue().trim());
                        completedReferrals.add(completedReferralsObject);
                    }
                }

              }
        }
        model.put("activeReferralList", ui.toJson(activeReferrals));
        model.put("activeReferralListSize", activeReferrals.size());
        model.put("completedReferralList", ui.toJson(completedReferrals));
        model.put("completedReferralListSize", completedReferrals.size());
    }
}
