/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemrIL.metadata;

import org.openmrs.api.context.Context;
import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.globalProperty;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.personAttributeType;

/**
 * Example metadata bundle
 */
@Component
public class ILMetadata extends AbstractMetadataBundle {

	public static final String GP_DMI_SERVER_POST_END_POINT = "dmi.surveillance.post.api";
	public static final String GP_DMI_SERVER_TOKEN_URL = "dmi.surveillance.token.url";
	public static final String GP_DMI_SERVER_TOKEN = "dmi.surveillance.token";
	public static final String GP_DMI_SERVER_CLIENT_ID = "dmi.surveillance.client.id";
	public static final String GP_DMI_SERVER_CLIENT_SECRET = "dmi.surveillance.client.secret";
	public static final String GP_VISUALIZATION_SERVER_POST_END_POINT = "visualization.metrics.post.api";

	public static final String GP_CS_SERVER_BASE_URL = "case.surveillance.base.url.api";
	public static final String GP_CS_SERVER_TOKEN_URL = "case.surveillance.token.url";
	public static final String GP_CS_SERVER_CLIENT_ID = "case.surveillance.client.id";
	public static final String GP_CS_SERVER_CLIENT_SECRET = "case.surveillance.client.secret";

	public static final class _PersonAttributeType {

		public static final String IL_PATIENT_SOURCE= "ac9a19f2-88af-4f3b-b4c2-f6e57c0d89af";
		public static final String REFERRAL_SOURCE= "c4281b3c-6c01-4213-bd3c-a52f8f6fe223";
		public static final String REFERRAL_STATUS= "df7e9996-23b5-4f66-a799-97498d19850d";
	}
	/**
	 * @see AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		install(personAttributeType("IL Patient Source", "IL Patient Source",
				String.class, null, false, 5.0, _PersonAttributeType.IL_PATIENT_SOURCE));
		install(personAttributeType("Referral Source", "Referral Source",
				String.class, null, false, 5.1, _PersonAttributeType.REFERRAL_SOURCE));
		install(personAttributeType("Referral status", "Referral status",
				String.class, null, false, 5.1, _PersonAttributeType.REFERRAL_STATUS));

		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_POST_END_POINT) == null) {
			install(globalProperty(GP_DMI_SERVER_POST_END_POINT, "A POST API for posting dmi surveillance data", "https://dmistaging.kenyahmis.org/api/case/batch"));
		}
		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_TOKEN_URL) == null) {
			install(globalProperty(GP_DMI_SERVER_TOKEN_URL, "Authorization token URL", "https://keycloak.kenyahmis.org/realms/dmi/protocol/openid-connect/token"));
		}
		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_TOKEN) == null) {
			install(globalProperty(GP_DMI_SERVER_TOKEN, "Authorization token", ""));
		}
		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_ID) == null) {
			install(globalProperty(GP_DMI_SERVER_CLIENT_ID, "Dmi server client id", "kenya-emr-test"));
		}
		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_DMI_SERVER_CLIENT_SECRET) == null) {
			install(globalProperty(GP_DMI_SERVER_CLIENT_SECRET, "Dmi server client secret", "HoWsbHH53zxO0jTSyV52tW5FZaE9wb1J"));
		}

		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_VISUALIZATION_SERVER_POST_END_POINT) == null) {
			install(globalProperty(GP_VISUALIZATION_SERVER_POST_END_POINT, "A POST API for posting visualization metrics data", ""));
		}
		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_CS_SERVER_BASE_URL) == null) {
			install(globalProperty(GP_CS_SERVER_BASE_URL, "A PUT API for posting case surveillance data", ""));
		}

		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_CS_SERVER_TOKEN_URL) == null) {
			install(globalProperty(GP_CS_SERVER_TOKEN_URL, "A POST end point for getting Surveillance server auth token", ""));
		}

		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_CS_SERVER_CLIENT_ID) == null) {
			install(globalProperty(GP_CS_SERVER_CLIENT_ID, "Case surveillance server client id", ""));
		}

		if(Context.getAdministrationService().getGlobalPropertyObject(ILMetadata.GP_CS_SERVER_CLIENT_SECRET) == null) {
			install(globalProperty(GP_CS_SERVER_CLIENT_SECRET, "Case surveillance server client id", ""));
		}
	}
}
