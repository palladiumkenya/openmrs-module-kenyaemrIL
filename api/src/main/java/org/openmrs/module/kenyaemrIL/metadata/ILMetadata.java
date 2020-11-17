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

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.personAttributeType;

/**
 * Example metadata bundle
 */
@Component
public class ILMetadata extends AbstractMetadataBundle {

	public static final class _PersonAttributeType {

		public static final String IL_PATIENT_SOURCE= "ac9a19f2-88af-4f3b-b4c2-f6e57c0d89af";
	}
	/**
	 * @see AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		install(personAttributeType("IL Patient Source", "IL Patient Source",
				String.class, null, false, 5.0, _PersonAttributeType.IL_PATIENT_SOURCE));
	}
}