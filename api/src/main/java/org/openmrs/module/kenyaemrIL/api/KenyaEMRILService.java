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
package org.openmrs.module.kenyaemrIL.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemrIL.il.ILAppointment;
import org.openmrs.module.kenyaemrIL.il.ILPerson;
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
     * @param status - boolean value showing whether or not to fetch records that have been processed
     * @return a list of @{@link ILPerson} records satisfying the given criteria
     */
	List<ILPerson> getPersonList(boolean status);

	List<ILPerson> getAddPersonList(boolean status);

	List<ILPerson> getUpdatePersonList(boolean status);

	boolean sendUpdateRequest(ILPerson ilPerson);

	boolean sendAddPersonRequest(ILPerson ilPerson);


	/**
	 * Fetches all Appointment Instances received in the system via the IL
	 * @return  a list of appointments @see {@link ILAppointment}
	 */
	List<ILAppointment> fetchAllAppointments();

	/**
	 *
	 * Fetches a list of appointments based on the processed status
	 * @param processed -  flag for the criteria filtering, @see @{@link org.openmrs.module.kenyaemrIL.KenyaEmrInbox  status}
	 * @return
	 */
	List<ILAppointment> fetchAppointments(boolean processed);

	/**
	 * Deletes/Cancels and appointment
	 * @param ilAppointment appointment to cancel
	 * @return true if successful, false otherwise
	 */
	boolean deleteAppointment(ILAppointment ilAppointment);

	/**
	 * Creates an appointment
	 * @param ilAppointment - the appointment to create
	 * @return the created appointment
	 */
	ILAppointment createAppointment(ILAppointment ilAppointment);

	/**
	 * Updates an appointment
	 * @param ilAppointment - the appointment to uppdate
	 * @return the updated appointment
	 */
	ILAppointment updateAppointment(ILAppointment ilAppointment);







}