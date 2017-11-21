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
package org.openmrs.module.kenyaemrIL.api.impl;

import org.hibernate.cfg.NotYetImplementedException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemrIL.il.ILAppointment;
import org.openmrs.module.kenyaemrIL.il.ILPerson;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.pharmacy.ILPharmacyOrder;

import java.util.List;

/**
 * It is a default implementation of {@link KenyaEMRILService}.
 */
public class KenyaEMRILServiceImpl extends BaseOpenmrsService implements KenyaEMRILService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private KenyaEMRILDAO dao;
	
	/**
     * @param dao the dao to set
     */
    public void setDao(KenyaEMRILDAO dao) {
	    this.dao = dao;
    }
    
    /**
     * @return the dao
     */
    public KenyaEMRILDAO getDao() {
	    return dao;
    }

    @Override
    public List<ILPerson> getPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPerson> getAddPersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");

    }

    @Override
    public List<ILPerson> getUpdatePersonList(boolean status) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendUpdateRequest(ILPerson ilPerson) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean sendAddPersonRequest(ILPerson ilPerson) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILAppointment> fetchAllAppointments() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILAppointment> fetchAppointments(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deleteAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILAppointment createAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");    }

    @Override
    public ILAppointment updateAppointment(ILAppointment ilAppointment) {
        throw new NotYetImplementedException("Not Yet Implemented");    }

    @Override
    public List<ILPharmacyOrder> fetchAllPharmacyOrders() {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public List<ILPharmacyOrder> fetchPharmacyOrders(boolean processed) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyOrder createPharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public ILPharmacyOrder updatePharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }

    @Override
    public boolean deletePharmacyOrder(ILPharmacyOrder ilPharmacyOrder) {
        throw new NotYetImplementedException("Not Yet Implemented");
    }
}