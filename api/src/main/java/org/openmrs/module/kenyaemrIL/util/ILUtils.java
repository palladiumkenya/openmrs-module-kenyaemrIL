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

package org.openmrs.module.kenyaemrIL.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Miscellaneous utility methods
 */
public class ILUtils {

	public static final String GP_IL_CONFIG_DIR = "kenyaemrIL.drugsMappingDirectory";
	public static final String GP_IL_LAST_PHARMACY_MESSAGE_ENCOUNTER = "kenyaemrIL.lastPharmacyMessageEncounter";
	public static final String GP_MLAB_SERVER_REQUEST_URL = "kenyaemrIL.endpoint.mlab.pull";
	public static final String GP_MLAB_SERVER_API_TOKEN = "";
	public static final String GP_SSL_VERIFICATION_ENABLED = "kemrorder.ssl_verification_enabled";
    public static final String GP_USHAURI_SSL_VERIFICATION_ENABLED = "kemr.ushauri.ssl_verification_enabled";
    public static final String GP_USHAURI_PUSH_SERVER_URL = "kenyaemrIL.endpoint.ushauri.push";
    public static String GP_MHEALTH_MIDDLEWARE_TO_USE = "kemr.mhealth.middlware";
    public static String HL7_REGISTRATION_MESSAGE = "ADT^A04";
    public static String HL7_REGISTRATION_UPDATE_MESSAGE = "ADT^A08";
    public static String HL7_APPOINTMENT_MESSAGE = "SIU^S12";


    /**
	 * Checks whether a date has any time value
	 * @param date the date
	 * @return true if the date has time
	 * @should return true only if date has time
	 */
	public static boolean dateHasTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.HOUR) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.SECOND) != 0 || cal.get(Calendar.MILLISECOND) != 0;
	}

	/**
	 * Checks if a given date is today
	 * @param date the date
	 * @return true if date is today
	 */
	public static boolean isToday(Date date) {
		return DateUtils.isSameDay(date, new Date());
	}

	/**
	 * Converts a WHO stage concept to a WHO stage number
	 * @param c the WHO stage concept
	 * @return the WHO stage number (null if the concept isn't a WHO stage)
	 */
//	public static Integer whoStage(Concept c) {
//		if (c != null) {
//			if (c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_1_ADULT)) || c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_1_PEDS))) {
//				return 1;
//			}
//			if (c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_2_ADULT)) || c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_2_PEDS))) {
//				return 2;
//			}
//			if (c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_3_ADULT)) || c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_3_PEDS))) {
//				return 3;
//			}
//			if (c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_4_ADULT)) || c.equals(Dictionary.getConcept(Dictionary.WHO_STAGE_4_PEDS))) {
//				return 4;
//			}
//		}
//		return null;
//	}

	/**
	 * Parses a CSV list of strings, returning all trimmed non-empty values
	 * @param csv the CSV string
	 * @return the concepts
	 */
	public static List<String> parseCsv(String csv) {
		List<String> values = new ArrayList<String>();

		for (String token : csv.split(",")) {
			token = token.trim();

			if (!StringUtils.isEmpty(token)) {
				values.add(token);
			}
		}
		return values;
	}

	/**
	 * Parses a CSV list of concept ids, UUIDs or mappings
	 * @param csv the CSV string
	 * @return the concepts
	 */
//	public static List<Concept> parseConcepts(String csv) {
//		List<String> identifiers = parseCsv(csv);
//		List<Concept> concepts = new ArrayList<Concept>();
//
//		for (String identifier : identifiers) {
//			if (StringUtils.isNumeric(identifier)) {
//				concepts.add(Context.getConceptService().getConcept(Integer.valueOf(identifier)));
//			}
//			else {
//				concepts.add(Dictionary.getConcept(identifier));
//			}
//		}
//		return concepts;
//	}

	/**
	 * Unlike in OpenMRS core, a user can only be one provider in KenyaEMR
	 * @param user the user
	 * @return the provider or null
	 */
	public static Provider getProvider(User user) {
		Person person = user.getPerson();
		Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(person);
		return providers.size() > 0 ? providers.iterator().next() : null;
	}

	/**
	 * Finds the last encounter during the program enrollment with the given encounter type
	 *
	 * @param type the encounter type
	 *
	 * @return the encounter
	 */
	public static Encounter lastEncounter(Patient patient, EncounterType type) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null, Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}

	/**
	 * Finds the first encounter during the program enrollment with the given encounter type
	 *
	 * @param type the encounter type
	 *
	 * @return the encounter
	 */
	public static Encounter firstEncounter(Patient patient, EncounterType type) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null, Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(0) : null;
	}

	/**
	 * Finds the last encounter of a given type entered via a given form.
	 *
	 * @param encounterType the type of encounter
	 * @param form          the form through which the encounter was entered.
	 */
	public static Encounter encounterByForm(Patient patient, EncounterType encounterType, Form form) {
		List<Form> forms = null;
		if (form != null) {
			forms = new ArrayList<Form>();
			forms.add(form);
		}
		EncounterService encounterService = Context.getEncounterService();
		List<Encounter> encounters = encounterService.getEncounters
				(
						patient,
						null,
						null,
						null,
						forms,
						Collections.singleton(encounterType),
						null,
						null,
						null,
						false
				);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}

	public static List<EncounterType> getAllEncounterTypesOfInterest() {
		List<EncounterType> encounterTypes = new ArrayList<>();

		EncounterType greencardEncounter = Context.getEncounterService().getEncounterTypeByUuid("a0034eee-1940-4e35-847f-97537a35d05e");   //last greencard followup
		EncounterType hivEnrollmentEncounter = Context.getEncounterService().getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc");  //hiv enrollment
		EncounterType drugOrderEncounter = Context.getEncounterService().getEncounterTypeByUuid("7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3");  //last drug order
		EncounterType mchMotherEncounter = Context.getEncounterService().getEncounterTypeByUuid("3ee036d8-7c13-4393-b5d6-036f2fe45126");  //mch mother enrollment
		//Fetch all encounters

		encounterTypes.add(hivEnrollmentEncounter);
		encounterTypes.add(greencardEncounter);
		encounterTypes.add(drugOrderEncounter);
		encounterTypes.add(mchMotherEncounter);

		return encounterTypes;
	}

	/**
	 * Default SSL context
	 *
	 * @return
	 */
	public static SSLConnectionSocketFactory sslConnectionSocketFactoryDefault() {
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				SSLContexts.createDefault(),
				new String[]{"TLSv1.2"},
				null,
				SSLConnectionSocketFactory.getDefaultHostnameVerifier());
		return sslsf;
	}

	/**
	 * Builds an SSL context for disabling/bypassing SSL verification
	 *
	 * @return
	 */
	public static SSLConnectionSocketFactory sslConnectionSocketFactoryWithDisabledSSLVerification() {
		SSLContextBuilder builder = SSLContexts.custom();
		try {
			builder.loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
					return true;
				}
			});
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}
		SSLContext sslContext = null;
		try {
			sslContext = builder.build();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslContext, new X509HostnameVerifier() {
			@Override
			public void verify(String host, SSLSocket ssl)
					throws IOException {
			}

			@Override
			public void verify(String host, X509Certificate cert)
					throws SSLException {
			}

			@Override
			public void verify(String host, String[] cns,
							   String[] subjectAlts) throws SSLException {
			}

			@Override
			public boolean verify(String s, SSLSession sslSession) {
				return true;
			}
		});
		return sslsf;
	}

	public static String getILUsage() {
		GlobalProperty gpUseILEnabled = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MHEALTH_MIDDLEWARE_TO_USE);
		boolean sendDirectToUshauri = false;
		if (gpUseILEnabled != null) {
			String useILPropValue = gpUseILEnabled.getPropertyValue();
			if (StringUtils.isNotBlank(useILPropValue) && useILPropValue.equalsIgnoreCase("false")) {
				sendDirectToUshauri = true;
			}
		}
		return null;
	}
}