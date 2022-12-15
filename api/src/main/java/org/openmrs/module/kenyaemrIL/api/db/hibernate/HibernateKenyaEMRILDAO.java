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
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemrIL.api.ILPatientRegistration;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILRegistration;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaemrMhealthOutboxMessage;
import org.openmrs.module.kenyaemrIL.util.ILUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.io.IOException;
import java.util.Arrays;
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

    @Override
    public KenyaemrMhealthOutboxMessage getMhealthOutboxMessageByUuid(String uuid) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaemrMhealthOutboxMessage.class);
        crit.add(Restrictions.eq("uuid", uuid));
        KenyaemrMhealthOutboxMessage mhealthOutboxMessage = (KenyaemrMhealthOutboxMessage) crit.uniqueResult();
        return mhealthOutboxMessage;
    }

    @Override
    public KenyaemrMhealthOutboxMessage saveMhealthOutboxMessage(KenyaemrMhealthOutboxMessage KenyaemrMhealthMessageOutbox) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(KenyaemrMhealthMessageOutbox);
        return KenyaemrMhealthMessageOutbox;
    }

    @Override
    public void deleteMhealthOutboxMessage(KenyaemrMhealthOutboxMessage KenyaemrMhealthOutboxMessage) {
        this.sessionFactory.getCurrentSession().delete(KenyaemrMhealthOutboxMessage);
    }

    @Override
    public List<KenyaemrMhealthOutboxMessage> getAllMhealthOutboxMessages(Boolean includeAll) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaemrMhealthOutboxMessage.class);
        crit.add(Restrictions.eq("retired", false));
        return crit.list();
    }

    @Override
    public List<KenyaemrMhealthOutboxMessage> getKenyaEMROutboxMessagesToSend(boolean b) {
        String IL_MESSAGES_MAX_BATCH_FETCH_SIZE = "kenyaemrIL.ilMessagesMaxBatchFetch";
        GlobalProperty batchSize = Context.getAdministrationService().getGlobalPropertyObject(IL_MESSAGES_MAX_BATCH_FETCH_SIZE);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaemrMhealthOutboxMessage.class);
        crit.add(Restrictions.eq("message_type", 2));
        crit.add(Restrictions.eq("retired", false));
        crit.setMaxResults(Integer.parseInt(batchSize.getValue().toString()));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessage> fetchAllViralLoadResults(boolean status) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessage.class);
        crit.add(Restrictions.eq("message_type", 1));
        crit.add(Restrictions.eq("hl7_type", "ORU^VL"));
        crit.add(Restrictions.eq("retired", status));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessageErrorQueue> fetchAllViralLoadErrors() {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessageErrorQueue.class);
        crit.add(Restrictions.eq("message_type", 1));
        crit.add(Restrictions.eq("hl7_type", "ORU^VL"));
        return crit.list();
    }

    @Override
    public List<KenyaEMRILMessageErrorQueue> fetchAllMhealthErrors() {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessageErrorQueue.class);
        crit.add(Restrictions.eq("middleware", "Direct"));
        return crit.list();
    }

    @Override
    public void reQueueErrors(String errorList) {
        if (Context.isAuthenticated()) {

            if (errorList.equals("all")) {
                List<KenyaEMRILMessageErrorQueue> errors = fetchAllMhealthErrors();

                for (KenyaEMRILMessageErrorQueue errorData : errors) {
                    KenyaemrMhealthOutboxMessage queueData = ILUtils.createMhealthOutboxMessageFromErrorMessage(errorData);

                    ILUtils.createRegistrationILMessage(errorData);

                    saveMhealthOutboxMessage(queueData);
                    purgeILErrorQueueMessage(errorData);
                }
            } else {
                String[] uuidList = errorList.split(",");
                for (String uuid : uuidList) {
                    KenyaEMRILMessageErrorQueue errorData = getKenyaEMRILErrorMessageByUuid(uuid);
                    KenyaemrMhealthOutboxMessage queueData = ILUtils.createMhealthOutboxMessageFromErrorMessage(errorData);

                    ILUtils.createRegistrationILMessage(errorData);

                    saveMhealthOutboxMessage(queueData);
                    purgeILErrorQueueMessage(errorData);
                }
            }
        }
    }

    @Override
    public void purgeILErrorQueueMessage(KenyaEMRILMessageErrorQueue kenyaEMRILMessageErrorQueue) {
        this.sessionFactory.getCurrentSession().delete(kenyaEMRILMessageErrorQueue);

    }

    @Override
    public KenyaEMRILMessageErrorQueue getKenyaEMRILErrorMessageByUuid(String uniqueId) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessageErrorQueue.class);
        crit.add(Restrictions.eq("uuid", uniqueId));
        KenyaEMRILMessageErrorQueue errorQueueMessage = (KenyaEMRILMessageErrorQueue) crit.uniqueResult();
        return errorQueueMessage;
    }

    @Override
    public void purgeErrors(String errorList) {
        if (Context.isAuthenticated()) {

            if (errorList.equals("all")) {
                List<KenyaEMRILMessageErrorQueue> errors = fetchAllMhealthErrors();

                for (KenyaEMRILMessageErrorQueue errorData : errors) {
                    purgeILErrorQueueMessage(errorData);
                }
            } else {
                String[] uuidList = errorList.split(",");
                for (String uuid : uuidList) {
                    KenyaEMRILMessageErrorQueue errorData = getKenyaEMRILErrorMessageByUuid(uuid);
                    purgeILErrorQueueMessage(errorData);
                }
            }
        }
    }

    @Override
    public List<KenyaEMRILMessageArchive> fetchRecentArchives() {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRILMessageArchive.class);
        crit.add(Restrictions.eq("middleware", "Direct"));
        crit.addOrder(Order.desc("message_id"));
        crit.setMaxResults(500);
        return crit.list();
    }

}