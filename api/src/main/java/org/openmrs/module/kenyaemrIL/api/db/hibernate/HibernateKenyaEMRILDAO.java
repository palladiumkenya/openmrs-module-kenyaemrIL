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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.db.KenyaEMRILDAO;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageArchive;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessageErrorQueue;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILRegistration;
import org.openmrs.module.kenyaemrIL.mhealth.KenyaEMRInteropMessage;
import org.openmrs.module.kenyaemrIL.programEnrollment.ExpectedTransferInPatients;
import org.openmrs.module.kenyaemrIL.util.ILUtils;

import java.util.ArrayList;
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
    public KenyaEMRInteropMessage getMhealthOutboxMessageByUuid(String uuid) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRInteropMessage.class);
        crit.add(Restrictions.eq("uuid", uuid));
        KenyaEMRInteropMessage mhealthOutboxMessage = (KenyaEMRInteropMessage) crit.uniqueResult();
        return mhealthOutboxMessage;
    }

    @Override
    public KenyaEMRInteropMessage saveMhealthOutboxMessage(KenyaEMRInteropMessage KenyaemrMhealthMessageOutbox) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(KenyaemrMhealthMessageOutbox);
        return KenyaemrMhealthMessageOutbox;
    }

    @Override
    public void deleteMhealthOutboxMessage(KenyaEMRInteropMessage KenyaEMRInteropMessage) {
        this.sessionFactory.getCurrentSession().delete(KenyaEMRInteropMessage);
    }

    @Override
    public List<KenyaEMRInteropMessage> getAllMhealthOutboxMessages(Boolean includeAll) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRInteropMessage.class);
        crit.add(Restrictions.eq("retired", false));
        return crit.list();
    }

    @Override
    public List<KenyaEMRInteropMessage> getAllMhealthOutboxMessagesByHl7Type(List<String> hl7MessageTypes, Boolean includeAll) {
        String stringQuery = "SELECT kenyaEMRInteropMessage FROM KenyaEMRInteropMessage AS kenyaEMRInteropMessage WHERE ";
        if (includeAll) {
            stringQuery += " retired = 1";
        } else {
            stringQuery += " retired = 0";
        }

        if (!hl7MessageTypes.isEmpty()) {
            stringQuery += " AND kenyaEMRInteropMessage.hl7_type IN (:hl7MessageTypes)";
        }

        Query query = this.sessionFactory.getCurrentSession().createQuery(
                stringQuery);
        if (!hl7MessageTypes.isEmpty()) {
            query.setParameterList("hl7MessageTypes", hl7MessageTypes);
        }
        return query.list();
    }

    @Override
    public List<KenyaEMRInteropMessage> getKenyaEMROutboxMessagesToSend(boolean b) {
        String IL_MESSAGES_MAX_BATCH_FETCH_SIZE = "kenyaemrIL.ilMessagesMaxBatchFetch";
        GlobalProperty batchSize = Context.getAdministrationService().getGlobalPropertyObject(IL_MESSAGES_MAX_BATCH_FETCH_SIZE);
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(KenyaEMRInteropMessage.class);
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
    public List<KenyaEMRILMessageErrorQueue> fetchAllMhealthErrors(List<String> hl7MessageTypes) {
        String stringQuery = "SELECT kenyaEMRILMessageErrorQueue FROM KenyaEMRILMessageErrorQueue AS kenyaEMRILMessageErrorQueue WHERE retired = 0 and middleware = 'Direct' ";

        if (!hl7MessageTypes.isEmpty()) {
            stringQuery += " AND kenyaEMRILMessageErrorQueue.hl7_type IN (:hl7MessageTypes)";
        }

        Query query = this.sessionFactory.getCurrentSession().createQuery(
                stringQuery);
        if (!hl7MessageTypes.isEmpty()) {
            query.setParameterList("hl7MessageTypes", hl7MessageTypes);
        }
        return query.list();
    }

    @Override
    public void reQueueErrors(String errorList) {
        if (Context.isAuthenticated()) {

            if (errorList.equals("all")) {
                List<KenyaEMRILMessageErrorQueue> errors = fetchAllMhealthErrors(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE));

                for (KenyaEMRILMessageErrorQueue errorData : errors) {
                    //TODO: fire this for the different message types
                    KenyaEMRInteropMessage queueData = ILUtils.createMhealthOutboxMessageFromErrorMessage(errorData);

                    ILUtils.createRegistrationILMessage(errorData);
                    // we dont want to queue because a new message is generated. Assuming the CCC number has been updated to the required format
                    if (errorData.getStatus() != null && (!errorData.getStatus().contains(ILUtils.INVALID_CCC_NUMBER_IN_USHAURI) || !errorData.getStatus().contains(ILUtils.CCC_NUMBER_ALREADY_EXISTS_IN_USHAURI))) {
                        saveMhealthOutboxMessage(queueData);
                    }

                    purgeILErrorQueueMessage(errorData);
                }
            } else {
                String[] uuidList = errorList.split(",");
                for (String uuid : uuidList) {
                    KenyaEMRILMessageErrorQueue errorData = getKenyaEMRILErrorMessageByUuid(uuid);
                    KenyaEMRInteropMessage queueData = ILUtils.createMhealthOutboxMessageFromErrorMessage(errorData);

                    ILUtils.createRegistrationILMessage(errorData);

                    // we dont want to queue because a new message is generated. Assuming the CCC number has been updated to the required format
                    if (errorData.getStatus() != null && (!errorData.getStatus().contains(ILUtils.INVALID_CCC_NUMBER_IN_USHAURI) || !errorData.getStatus().contains(ILUtils.CCC_NUMBER_ALREADY_EXISTS_IN_USHAURI))) {
                        saveMhealthOutboxMessage(queueData);
                    }
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
                List<KenyaEMRILMessageErrorQueue> errors = fetchAllMhealthErrors(Arrays.asList(ILUtils.HL7_APPOINTMENT_MESSAGE));

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
    public List<KenyaEMRILMessageArchive> fetchRecentArchives(List<String> hl7MessageTypes) {
        String stringQuery = "SELECT kenyaEMRILMessageArchive FROM KenyaEMRILMessageArchive AS kenyaEMRILMessageArchive WHERE retired = 0 and middleware = 'Direct' ";

        if (!hl7MessageTypes.isEmpty()) {
            stringQuery += " AND kenyaEMRILMessageArchive.hl7_type IN (:hl7MessageTypes) order by kenyaEMRILMessageArchive.message_id desc ";
        }

        Query query = this.sessionFactory.getCurrentSession().createQuery(
                stringQuery).setMaxResults(500);
        if (!hl7MessageTypes.isEmpty()) {
            query.setParameterList("hl7MessageTypes", hl7MessageTypes);
        }
        return query.list();

    }

    @Override
    public ExpectedTransferInPatients createPatient(ExpectedTransferInPatients expectedTransferInPatient) {
        this.sessionFactory.getCurrentSession().saveOrUpdate(expectedTransferInPatient);
        return expectedTransferInPatient;
    }

    @Override
    public List<ExpectedTransferInPatients> getAllTransferIns() {
        Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(ExpectedTransferInPatients.class);
        return criteria.list();
    }

    @Override
    public List<ExpectedTransferInPatients> getAllTransferInsByServiceType(String serviceType) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(ExpectedTransferInPatients.class);
        if(serviceType != null){
            crit.add(Restrictions.eq("service_type", serviceType));
          }
        return crit.list();
    }

    @Override
    public List<ExpectedTransferInPatients> getTransferInPatient(Patient patient) {
        if (patient == null) return null;
        String stringQuery = "SELECT expectedTransferInPatient FROM ExpectedTransferInPatients AS expectedTransferInPatient WHERE patient = :patient AND retired = 0";
        Query query = this.sessionFactory.getCurrentSession().createQuery(
                stringQuery);
        query.setParameter("patient", patient);
        List<ExpectedTransferInPatients> expectedTransferInPatients = query.list();
        return expectedTransferInPatients;
    }

    @Override
    public List<ExpectedTransferInPatients> getCommunityReferrals(String serviceType,String referralStatus) {
        if (serviceType == null) return null;
        String stringQuery = "SELECT expectedTransferInPatient FROM ExpectedTransferInPatients AS expectedTransferInPatient WHERE serviceType = :serviceType AND referralStatus = :referralStatus";
        Query query = this.sessionFactory.getCurrentSession().createQuery(
                stringQuery);
        query.setParameter("serviceType", serviceType);
        query.setParameter("referralStatus", referralStatus);
        List<ExpectedTransferInPatients> getCommunityReferrals = query.list();
        return getCommunityReferrals;
    }

    @Override
    public ExpectedTransferInPatients getCommunityReferralsById(Integer id) {
        Criteria crit = this.sessionFactory.getCurrentSession().createCriteria(ExpectedTransferInPatients.class);
        crit.add(Restrictions.eq("id", id));
        ExpectedTransferInPatients expectedTransferInPatient = (ExpectedTransferInPatients) crit.uniqueResult();
        return expectedTransferInPatient;
    }

}