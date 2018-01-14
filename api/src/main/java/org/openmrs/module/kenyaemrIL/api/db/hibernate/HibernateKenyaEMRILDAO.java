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
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;

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
        System.out.println("About to test this one here " + uniqueId);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("name", uniqueId));
        KenyaEMRILMessage kenyaEMRILMessage = (KenyaEMRILMessage) crit.uniqueResult();
        System.out.println("Just before the return: " + kenyaEMRILMessage);
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
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("messageType", 1));
        crit.add(Restrictions.eq("retired", includeRetired));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessage> getKenyaEMRILOutboxes(Boolean includeRetired) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("messageType", 2));
        crit.add(Restrictions.eq("retired", includeRetired));
        return crit.list();
    }

}