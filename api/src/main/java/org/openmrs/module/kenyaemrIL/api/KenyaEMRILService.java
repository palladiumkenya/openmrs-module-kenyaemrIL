/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrIL.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyDispense;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured in moduleApplicationContext.xml.
 * <p>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(KenyaEMRILService.class).someMethod();
 * </code>
 *
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface KenyaEMRILService extends OpenmrsService {
     
	/*
     * Add service methods here
	 * 
	 */

    /**
     * Processes and returns a list of available IL Person records received from the IL, typically ADTA04 and ADTA08
     *
     * @param status - boolean value showing whether or not to fetch records that have been processed
     * @return a list of @{@link ILMessage} records satisfying the given criteria
     */
    List<ILMessage> getPersonList(boolean status);

    List<ILMessage> getAddPersonList(boolean status);

    List<ILMessage> getUpdatePersonList(boolean status);

    boolean sendUpdateRequest(ILMessage ilMessage);

    boolean sendAddPersonRequest(ILMessage ilMessage);



    /*    Pharmacy Orders     */
    List<ILPharmacyOrder> fetchAllPharmacyOrders();

    List<ILPharmacyOrder> fetchPharmacyOrders(boolean processed);

    ILPharmacyOrder createPharmacyOrder(ILPharmacyOrder ilPharmacyOrder);

    ILPharmacyOrder updatePharmacyOrder(ILPharmacyOrder ilPharmacyOrder);

    boolean deletePharmacyOrder(ILPharmacyOrder ilPharmacyOrder);


    //    Pharmacy Dispense
    List<ILPharmacyDispense> fetchAllPharmacyDispenses();

    List<ILPharmacyDispense> fetchPharmacyDispenses(boolean processed);

    ILPharmacyDispense createPharmacyDispense(ILPharmacyDispense ilPharmacyDispense);

    ILPharmacyDispense updatePharmacyDispense(ILPharmacyDispense ilPharmacyDispense);

    boolean deletePharmacyDispense(ILPharmacyDispense ilPharmacyDispense);



    KenyaEMRILMessage getKenyaEMRILMessageByUuid(String uniqueId);

    KenyaEMRILMessage saveKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage);

    List<KenyaEMRILMessage> getKenyaEMRILInboxes(Boolean includeRetired);

    List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired);

    void deleteKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage);

    List<KenyaEMRILMessage> getAllKenyaEMRILMessages(Boolean includeAll);


//    Process incoming IL requests

    /**
     * Processes a create person request from the IL
     * @param ilMessage
     * @return
     */
    boolean processCreatePatientRequest(ILMessage ilMessage);

    boolean processUpdatePatientRequest(ILMessage ilMessage);

    boolean processPharmacyOrder(ILMessage ilMessage);

    boolean processPharmacyDispense(ILMessage ilMessage);

    boolean processAppointmentSchedule(ILMessage ilMessage);

    boolean processLabOrder(ILMessage ilMessage);

    boolean processObservationResult(ILMessage ilMessage);

    boolean processViralLoad(ILMessage ilMessage);

    boolean process731Adx(ILMessage ilMessage);
}