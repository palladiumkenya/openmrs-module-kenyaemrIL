package org.openmrs.module.kenyaemrIL.page.controller;

import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Location;
import org.openmrs.LocationAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.FacilityMetadata;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.shr.FhirConfig;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AppPage("kenyaemr.referral.home")
public class CommunityReferralsHomePageController {
    public static final String NUPI = "f85081e2-b4be-4e48-b3a4-7994b69bb101";

    public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) throws JsonProcessingException, ParseException {

        KenyaEMRILService ilService = Context.getService(KenyaEMRILService.class);
        FhirConfig fhirConfig = Context.getRegisteredComponents(FhirConfig.class).get(0);

// Filter all community referrals with active referral status
        List<ExpectedTransferInPatients> activeCommunityReferralsList = ilService.getCommunityReferrals("COMMUNITY","ACTIVE");

        // Filter all community referrals with active referral status
        List<ExpectedTransferInPatients> completedCommunityReferralsList = ilService.getCommunityReferrals("COMMUNITY","COMPLETED");

        List<SimpleObject> activeReferrals = new ArrayList<SimpleObject>();
        List<SimpleObject> completedReferrals = new ArrayList<SimpleObject>();

        for (ExpectedTransferInPatients expectedTransferInPatients : activeCommunityReferralsList) {
            IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
            ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, expectedTransferInPatients.getPatientSummary());
            String requester = "";
            if (serviceRequest.hasRequester()) {
                if (serviceRequest.getRequester().getDisplay() != null) {
                    Location location = ILUtils.getLocationByMflCode(serviceRequest.getRequester().getDisplay());
                    if (location != null) {
                        requester = location.getName();
                    } else {
                        requester = "Community";
                    }

                } else if (serviceRequest.getRequester().getIdentifier() != null && serviceRequest.getRequester().getIdentifier().getValue() != null) {
                    Location location = ILUtils.getLocationByMflCode(serviceRequest.getRequester().getIdentifier().getValue());
                    if (location != null) {
                        requester = location.getName();
                    } else {
                        requester = "Community";
                    }
                }
            }

              SimpleObject activeReferralsObject = SimpleObject.create("id", expectedTransferInPatients.getId(),
                                                                        "uuid", expectedTransferInPatients.getUuid(),
                                                                        "nupi", expectedTransferInPatients.getNupiNumber(),
                                                                        "dateReferred", serviceRequest.getAuthoredOn() != null? new SimpleDateFormat("yyyy-MM-dd").format(serviceRequest.getAuthoredOn()):"",
                                                                        "referredFrom", requester,
                                                                        "givenName", expectedTransferInPatients.getClientFirstName() != null ? expectedTransferInPatients.getClientFirstName() : "",
                                                                        "middleName", expectedTransferInPatients.getClientMiddleName() != null ? expectedTransferInPatients.getClientMiddleName() : "",
                                                                        "familyName", expectedTransferInPatients.getClientLastName() != null ? expectedTransferInPatients.getClientLastName() : "",
                                                                        "birthdate", kenyaUi.formatDate(expectedTransferInPatients.getClientBirthDate()),
                                                                        "gender", expectedTransferInPatients.getClientGender(),
                                                                        "status", expectedTransferInPatients.getReferralStatus());
                        activeReferrals.add(activeReferralsObject);
                    }
        for (ExpectedTransferInPatients expectedTransferInPatients : completedCommunityReferralsList) {
            IParser parser = fhirConfig.getFhirContext().newJsonParser().setPrettyPrint(true);
            ServiceRequest serviceRequest = parser.parseResource(ServiceRequest.class, expectedTransferInPatients.getPatientSummary());
            String requester = "";
            if (serviceRequest.hasRequester()) {
                if (serviceRequest.getRequester().getDisplay() != null) {
                    requester = serviceRequest.getRequester().getDisplay();
                }
            }


            SimpleObject completedReferralsObject = SimpleObject.create("id", expectedTransferInPatients.getId(),
                    "uuid", expectedTransferInPatients.getUuid(),
                    "nupi", expectedTransferInPatients.getNupiNumber(),
                    "dateReferred", serviceRequest.getAuthoredOn() != null? new SimpleDateFormat("yyyy-MM-dd").format(serviceRequest.getAuthoredOn()):"",
                    "referredFrom", requester,
                    "givenName", expectedTransferInPatients.getClientFirstName() != null ? expectedTransferInPatients.getClientFirstName() : "",
                    "middleName", expectedTransferInPatients.getClientMiddleName() != null ? expectedTransferInPatients.getClientMiddleName() : "",
                    "familyName", expectedTransferInPatients.getClientLastName() != null ? expectedTransferInPatients.getClientLastName() : "",
                    "birthdate", kenyaUi.formatDate(expectedTransferInPatients.getClientBirthDate()),
                    "gender", expectedTransferInPatients.getClientGender(),
                    "status", expectedTransferInPatients.getReferralStatus());
            completedReferrals.add(completedReferralsObject);
        }

        model.put("activeReferralList", ui.toJson(activeReferrals));
        model.put("activeReferralListSize", activeReferrals.size());
        model.put("completedReferralList", ui.toJson(completedReferrals));
        model.put("completedReferralListSize", completedReferrals.size());
    }
}
