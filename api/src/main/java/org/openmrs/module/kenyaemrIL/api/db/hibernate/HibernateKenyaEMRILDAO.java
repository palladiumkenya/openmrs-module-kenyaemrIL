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
package org.openmrs.module.kenyaemrIL.api.db.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILRegistration;

import java.util.List;

/**
 * It is a default implementation of  {@link KenyaEMRILDAO}.
 */
public class HibernateKenyaEMRILDAO implements KenyaEMRILDAO {
    protected final Log log = LogFactory.getLog(this.getClass());

    private SessionFactory sessionFactory;

    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @return the sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public KenyaEMRILMessage getKenyaEMRILMessageByUuid(String uniqueId) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("uuid", uniqueId));
        KenyaEMRILMessage kenyaEMRILMessage = (KenyaEMRILMessage) crit.uniqueResult();
        return kenyaEMRILMessage;
    }

    @Override
    public List<KenyaEMRILMessage> getAllKenyaEMRILMessages(Boolean includeRetired) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("retired", includeRetired));
        return crit.list();
    }

    @Override
    public KenyaEMRILMessage createKenyaEMRILMessage(KenyaEMRILMessage delegate) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(delegate);
        return delegate;
    }

    @Override
    public void deleteKenyaEMRILMessage(KenyaEMRILMessage kenyaEMRILMessage) {
        this.sessionFactory.getCurrentSession().delete(kenyaEMRILMessage);
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILInboxes(Boolean includeRetired) {
        String IL_MESSAGES_MAX_BATCH_FETCH_SIZE = "kenyaemrIL.ilMessagesMaxBatchFetch";
        GlobalProperty batchSize = Context.getAdministrationService().getGlobalPropertyObject(IL_MESSAGES_MAX_BATCH_FETCH_SIZE);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("message_type", 1));
        crit.add(Restrictions.eq("retired", includeRetired));
        crit.setMaxResults(Integer.parseInt(batchSize.getValue().toString()));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired) {
        String IL_MESSAGES_MAX_BATCH_FETCH_SIZE = "kenyaemrIL.ilMessagesMaxBatchFetch";
        GlobalProperty batchSize = Context.getAdministrationService().getGlobalPropertyObject(IL_MESSAGES_MAX_BATCH_FETCH_SIZE);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("message_type", 2));
        crit.add(Restrictions.eq("retired", includeRetired));
        crit.setMaxResults(Integer.parseInt(batchSize.getValue().toString()));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILStatus(String status) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("status", status));
        return crit.list();
    }

    @Override
    public KenyaEMRILMessageArchive createKenyaEMRILMessageArchive(KenyaEMRILMessageArchive delegate) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(delegate);
        return delegate;
    }

    @Override
    public KenyaEMRILMessageErrorQueue createKenyaEMRILMessageErrorQueue(KenyaEMRILMessageErrorQueue delegate) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(delegate);
        return delegate;
    }

    // Adding kenyaemrILRegistrations
    @Override
    public KenyaEMRILRegistration getKenyaEMRILRegistrationByUuid(String uniqueId) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILRegistration.class);
        crit.add(Restrictions.eq("uuid", uniqueId));
        KenyaEMRILRegistration KenyaEMRILRegistration = (KenyaEMRILRegistration) crit.uniqueResult();
        return KenyaEMRILRegistration;
    }

    @Override
    public KenyaEMRILRegistration getKenyaEMRILRegistrationForPatient(Patient patient) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILRegistration.class);
        crit.add(Restrictions.eq("patient_id", patient.getPatientId()));
        crit.setMaxResults(1);
        KenyaEMRILRegistration KenyaEMRILRegistration = (KenyaEMRILRegistration) crit.uniqueResult();
        return KenyaEMRILRegistration;
    }

    @Override
    public KenyaEMRILRegistration createKenyaEMRILRegistration(KenyaEMRILRegistration delegate) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(delegate);
        return delegate;
    }


    @Override
    public List<KenyaEMRILRegistration> getKenyaEMRILRegistration(Boolean includeRetired) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILRegistration.class);
        crit.add(Restrictions.eq("retired", includeRetired));
        return crit.list();
    }


    @Override
    public List<KenyaEMRILRegistration> getAllKenyaEMRILRegistration(Boolean includeRetired) {
        String IL_MESSAGES_MAX_BATCH_FETCH_SIZE = "kenyaemrIL.ilMessagesMaxBatchFetch";
        GlobalProperty batchSize = Context.getAdministrationService().getGlobalPropertyObject(IL_MESSAGES_MAX_BATCH_FETCH_SIZE);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILRegistration.class);
        crit.add(Restrictions.eq("message_type", 1));
        crit.add(Restrictions.eq("retired", includeRetired));
        crit.setMaxResults(Integer.parseInt(batchSize.getValue().toString()));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILRegistration> getKenyaEMRILRegistrationStatus(String status) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILRegistration.class);
        crit.add(Restrictions.eq("status", status));
        return crit.list();
    }

}