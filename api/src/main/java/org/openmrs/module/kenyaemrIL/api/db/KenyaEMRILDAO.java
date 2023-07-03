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
package org.openmrs.module.kenyaemrIL.api.db;

import org.openmrs.Patient;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILRegistration;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;

import java.util.List;

/**
 *  Database methods for {@link KenyaEMRILService}.
 */
public interface KenyaEMRILDAO {
	/*
	 * Add DAO methods here
	 */

    KenyaEMRILMessage getKenyaEMRILMessageByUuid(String uniqueId);

    KenyaEMRILMessage createKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage);

    void deleteKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage);

    List<KenyaEMRILMessage> getKenyaEMRILInboxes(Boolean includeRetired);

    List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired);

    List<KenyaEMRILMessage> getAllKenyaEMRILMessages(Boolean includeAll);

    List<KenyaEMRILMessage> getKenyaEMRILStatus(String status);

    // Adding kenyaILMessageArchive
    KenyaEMRILMessageArchive createKenyaEMRILMessageArchive(KenyaEMRILMessageArchive kenyaEMRILMessageArchive);

    // Adding kenyaILMessageErrorQueue
    KenyaEMRILMessageErrorQueue createKenyaEMRILMessageErrorQueue(KenyaEMRILMessageErrorQueue kenyaEMRILMessageErrorQueue);

    // Adding kenyaemrILRegistrations

    KenyaEMRILRegistration getKenyaEMRILRegistrationByUuid(String uniqueId);

    KenyaEMRILRegistration getKenyaEMRILRegistrationForPatient(Patient patient);

    KenyaEMRILRegistration createKenyaEMRILRegistration(KenyaEMRILRegistration kenyaEMRILRegistration);

    List<KenyaEMRILRegistration> getKenyaEMRILRegistration(Boolean includeRetired);

    List<KenyaEMRILRegistration> getAllKenyaEMRILRegistration(Boolean includeAll);

    List<KenyaEMRILRegistration> getKenyaEMRILRegistrationStatus(String status);

    // additions to support for data exchange with mhealth apps

    KenyaEMRInteropMessage getMhealthOutboxMessageByUuid(String uuid);

    KenyaEMRInteropMessage saveMhealthOutboxMessage(KenyaEMRInteropMessage KenyaemrMhealthMessageOutbox);

    void deleteMhealthOutboxMessage(KenyaEMRInteropMessage KenyaEMRInteropMessage);

    List<KenyaEMRInteropMessage> getAllMhealthOutboxMessages(Boolean includeAll);

    List<KenyaEMRInteropMessage> getKenyaEMROutboxMessagesToSend(boolean b);

    List<KenyaEMRILMessage> fetchAllViralLoadResults(boolean status);

    List<KenyaEMRILMessageErrorQueue> fetchAllViralLoadErrors();

    List<KenyaEMRILMessageErrorQueue> fetchAllMhealthErrors();

    void reQueueErrors(String errorList);

    void purgeILErrorQueueMessage(KenyaEMRILMessageErrorQueue kenyaEMRILMessageErrorQueue);

    KenyaEMRILMessageErrorQueue getKenyaEMRILErrorMessageByUuid(String uniqueId);

    void purgeErrors(String errorList);

    List<KenyaEMRILMessageArchive> fetchRecentArchives();

}