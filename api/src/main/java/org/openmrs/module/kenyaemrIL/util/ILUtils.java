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
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.api.ILPatientRegistration;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.INTERNAL_PATIENT_ID;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;
import org.openmrs.module.metadatadeploy.MetadataUtils;

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
import java.util.Arrays;
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
	public static final String CCC_NUMBER_IDENTIFIER_TYPE = "CCC_NUMBER";
	public static String GP_MHEALTH_MIDDLEWARE_TO_USE = "kemr.mhealth.middlware";
    public static String HL7_REGISTRATION_MESSAGE = "ADT^A04";
    public static String HL7_REGISTRATION_UPDATE_MESSAGE = "ADT^A08";
    public static String HL7_APPOINTMENT_MESSAGE = "SIU^S12";
    public static String HL7_ACTIVE_REFERRAL_MESSAGE = "SIU^S20";
    public static String HL7_COMPLETE_REFERRAL_MESSAGE = "SIU^S21";
	public static final String REGISTRATION_DOES_NOT_EXIST_IN_THE_USHAURI_SYSTEM = "does not exists in the Ushauri system";
	public static final String INVALID_CCC_NUMBER_IN_USHAURI = "The CCC must be 10 digits"; // a substring in the error message
	public static final String CCC_NUMBER_ALREADY_EXISTS_IN_USHAURI = "The CCC number already exists."; // a substring in the error message

	public static final String GP_SHR_SERVER_URL = "http://localhost:8098/fhir/";
	public static final String GP_SHR_USER_NAME = "";
	public static final String GP_SHR_PASSWORD = "";

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

	/**
	 * Gets the configured middleware from the global property.
	 * Valid options include:
	 * 1. IL - ideal for use cases where only the IL is used
	 * 2. Direct - this is a short term measure to enable send data directly to ushauri server
	 * 3. Hybrid - configures the system for both IL and Direct options
	 * @return
	 */
	public static String getMiddlewareInuse() {
		GlobalProperty gpMiddlewareConfig = Context.getAdministrationService().getGlobalPropertyObject(ILUtils.GP_MHEALTH_MIDDLEWARE_TO_USE);
		if (gpMiddlewareConfig != null && gpMiddlewareConfig.getPropertyValue() != null) {
			return gpMiddlewareConfig.getPropertyValue();
		}
		return null;
	}

	/**
	 * Creates a copy of MhealthOutboxMessage from KenyaEMRILMessage
	 * @param kenyaEMRILMessage
	 * @return
	 */
	public static KenyaEMRInteropMessage createMhealthOutboxFromILMessage(KenyaEMRILMessage kenyaEMRILMessage) {

		KenyaEMRInteropMessage outboxMessage = new KenyaEMRInteropMessage();
		outboxMessage.setHl7_type(kenyaEMRILMessage.getHl7_type());
		outboxMessage.setSource(kenyaEMRILMessage.getSource());
		outboxMessage.setMessage(kenyaEMRILMessage.getMessage());
		outboxMessage.setDescription("");
		outboxMessage.setName("");
		if (kenyaEMRILMessage.getPatient() != null) {
			outboxMessage.setPatient(kenyaEMRILMessage.getPatient());
		}
		outboxMessage.setMessage_type(kenyaEMRILMessage.getMessage_type());
		return outboxMessage;
	}

	/**
	 * Create MhealthOutboxMessage from an error message.
	 * This is used when reconstructing the message from errors on re-queue
	 * @param errorMessage
	 * @return
	 */
	public static KenyaEMRInteropMessage createMhealthOutboxMessageFromErrorMessage(KenyaEMRILMessageErrorQueue errorMessage) {

		KenyaEMRInteropMessage outboxMessage = new KenyaEMRInteropMessage();
		outboxMessage.setHl7_type(errorMessage.getHl7_type());
		outboxMessage.setSource(errorMessage.getSource());
		outboxMessage.setMessage(errorMessage.getMessage());
		outboxMessage.setDescription("");
		outboxMessage.setName("");
		if (errorMessage.getPatient() != null) {
			outboxMessage.setPatient(errorMessage.getPatient());
		}
		outboxMessage.setMessage_type(errorMessage.getMessage_type());
		return outboxMessage;
	}

	/**
	 * Create archive from mhealth outbox message
	 * @param message
	 * @return
	 */
	public static KenyaEMRILMessageArchive createArchiveForMhealthOutbox(KenyaEMRInteropMessage message) {

		KenyaEMRILMessageArchive archiveMessage = new KenyaEMRILMessageArchive();
		archiveMessage.setHl7_type(message.getHl7_type());
		archiveMessage.setSource(message.getSource());
		archiveMessage.setMessage(message.getMessage());
		archiveMessage.setDescription("");
		archiveMessage.setName("");
		archiveMessage.setMessage_type(message.getMessage_type());
		return archiveMessage;
	}

	/**
	 * Create archive message from il message
	 * @param message
	 * @return
	 */
	public static KenyaEMRILMessageArchive createArchiveForIlMessage(KenyaEMRILMessage message) {

		KenyaEMRILMessageArchive archiveMessage = new KenyaEMRILMessageArchive();
		archiveMessage.setHl7_type(message.getHl7_type());
		archiveMessage.setSource(message.getSource());
		archiveMessage.setMessage(message.getMessage());
		archiveMessage.setDescription("");
		archiveMessage.setName("");
		archiveMessage.setMessage_type(message.getMessage_type());
		return archiveMessage;
	}

	/**
	 * Extract CCC Number from IL payload
	 * @param patientIdentifierType
	 * @param payload
	 * @return
	 */
	public static String getPatientIdentifierFromILPayload(String patientIdentifierType, String payload) {
		if (StringUtils.isBlank(patientIdentifierType) || StringUtils.isBlank(payload)) {
			return null;
		}


		ObjectMapper mapper = new ObjectMapper();
		ILMessage ilMessage;
		try {
			ilMessage = mapper.readValue(payload.toLowerCase(), ILMessage.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<INTERNAL_PATIENT_ID> idList = ilMessage.getPatient_identification().getInternal_patient_id();
		for (INTERNAL_PATIENT_ID id : idList) {
			if (id.getIdentifier_type().equalsIgnoreCase(patientIdentifierType)) {
				return id.getId();
			}
		}

		return null;

	}

	public static void createRegistrationILMessage(KenyaEMRILMessageErrorQueue errorData) {
		if (errorData.getStatus() != null && (errorData.getStatus().contains(REGISTRATION_DOES_NOT_EXIST_IN_THE_USHAURI_SYSTEM) || errorData.getStatus().contains(INVALID_CCC_NUMBER_IN_USHAURI))) { // missing registration in Ushauri server
			Patient patient = null;
			// check first patient_id
			if (errorData.getPatient() != null) {
				patient = errorData.getPatient();
			}

			// check using CCC number
			if (patient == null) {
				PatientIdentifierType cccIdType = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
				String patientCCCNumberFromPayload = ILUtils.getPatientIdentifierFromILPayload(ILUtils.CCC_NUMBER_IDENTIFIER_TYPE, errorData.getMessage());
				List<Patient> patients = Context.getPatientService().getPatients(null, patientCCCNumberFromPayload, Arrays.asList(cccIdType), true);
				if (patients.size() > 0) {
					patient = patients.get(0);
				}
			}

			if (patient != null) {
				ILMessage ilMessage = ILPatientRegistration.iLPatientWrapper(patient);
				KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
				service.sendAddPersonRequest(ilMessage);
			}
		}
	}

	public static String getShrServerUrl() {
		//return Context.getAdministrationService().getGlobalProperty(ILUtils.GP_SHR_SERVER_URL);
		return ILUtils.GP_SHR_SERVER_URL;
	}

	public static String getShrUserName() {
		return Context.getAdministrationService().getGlobalProperty(ILUtils.GP_SHR_USER_NAME);
	}

	public static String getShrPassword() {
		return Context.getAdministrationService().getGlobalProperty(ILUtils.GP_SHR_PASSWORD);
	}
}