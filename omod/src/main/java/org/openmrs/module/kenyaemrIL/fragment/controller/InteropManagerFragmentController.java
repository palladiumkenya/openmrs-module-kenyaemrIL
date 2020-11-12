package org.openmrs.module.kenyaemrIL.fragment.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.json.JSONObject;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemrIL.api.ILPrescriptionMessage;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILMessage;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * controller for pivotTableCharts fragment
 */
public class InteropManagerFragmentController {
    private final Log log = LogFactory.getLog(getClass());

    public void controller(FragmentModel model){
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);

        final String sqlSelectQuery = "SELECT date_created, hl7_type, source, retired, status FROM openmrs.il_message order by date_created desc limit 10;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();

        try {
            sf.getCurrentSession().doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);

                    try {

                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet != null) {
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            while (resultSet.next()) {
                                Object[] row = new Object[metaData.getColumnCount()];
                                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                    row[i - 1] = resultSet.getObject(i);
                                }
                                ret.add(SimpleObject.create(
                                        "date_created", row[0] != null ? row[0].toString() : "",
                                        "message_type", row[1] != null? row[1].toString() : "",
                                        "source", row[2] != null ? row[2].toString() : "",
                                        "status", row[3].toString().equals("1") ? "Processed": "Pending",
                                        "error", row[4] != null ? row[4].toString() : ""
                                ));
                            }
                        }
                    }
                    finally {
                        try {
                            if (statement != null) {
                                statement.close();
                            }
                        }
                        catch (Exception e) {}
                    }
                }
            });
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to execute query", e);
        }

        model.put("logs", ret);
    }
    public JSONObject fetchDataSets(){

        List<Patient> allPatients = Context.getPatientService().getAllPatients();
        JSONObject x = new JSONObject();
        x.put("patients", allPatients);
        return x;
    }
    public List<SimpleObject> refreshTables(UiUtils ui) {

        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);

        final String sqlSelectQuery = "SELECT date_created, hl7_type, source, retired, status FROM openmrs.il_message order by date_created desc limit 10;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {

            tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
            final Transaction finalTx = tx;
            sf.getCurrentSession().doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    try {

                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet != null) {
                            ResultSetMetaData metaData = resultSet.getMetaData();

                            while (resultSet.next()) {
                                Object[] row = new Object[metaData.getColumnCount()];
                                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                    row[i - 1] = resultSet.getObject(i);
                                }

                                ret.add(SimpleObject.create(
                                        "date_created", row[0] != null ? row[0].toString() : "",
                                        "message_type", row[1] != null? row[1].toString() : "",
                                        "source", row[2] != null ? row[2].toString() : "",
                                        "status", row[3].toString().equals("1") ? "Processed": "Pending",
                                        "error", row[4] != null ? row[4].toString() : ""
                                ));
                            }
                        }
                        finalTx.commit();
                    }
                    finally {
                        try {
                            if (statement != null) {
                                statement.close();
                            }
                        }
                        catch (Exception e) {}
                    }
                }
            });
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to execute query", e);
        }

        return ret;
    }
    public List<SimpleObject> errorMessages(UiUtils ui) {

        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);

        final String sqlSelectQuery = "SELECT date_created, hl7_type, source, retired, status FROM openmrs.il_message where message_type = 1 and status <> 'Success' order by date_created desc limit 10;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {

            tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
            final Transaction finalTx = tx;
            sf.getCurrentSession().doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    try {

                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet != null) {
                            ResultSetMetaData metaData = resultSet.getMetaData();

                            while (resultSet.next()) {
                                Object[] row = new Object[metaData.getColumnCount()];
                                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                    row[i - 1] = resultSet.getObject(i);
                                }
                                ret.add(SimpleObject.create(
                                        "date_created", row[0] != null ? row[0].toString() : "",
                                        "message_type", row[1] != null? row[1].toString() : "",
                                        "source", row[2] != null ? row[2].toString() : "",
                                        "status", row[3].toString().equals("1") ? "Processed": "Pending",
                                        "error", row[4] != null ? row[4].toString() : ""
                                ));
                            }
                        }
                        finalTx.commit();
                    }
                    finally {
                        try {
                            if (statement != null) {
                                statement.close();
                            }
                        }
                        catch (Exception e) {}
                    }
                }
            });
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to execute query", e);
        }

        return ret;
    }

    public SimpleObject postPrescriptionMessage(@RequestParam(value = "patient") Patient patient, UiUtils ui) {

        SimpleObject ret = SimpleObject.create("status", "Successful");

        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid ='7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3' " +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join orders o on o.encounter_id=e.encounter_id and o.voided=0 and o.order_action='NEW' and o.date_stopped is null " );
        q.append("where e.patient_id = " + patient.getPatientId() + " ");
        q.append(" and e.voided = 0 group by e.encounter_id ");

        List<Encounter> encounters = new ArrayList<Encounter>();
        EncounterService encounterService = Context.getEncounterService();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Encounter e = encounterService.getEncounter(encounterId);
            encounters.add(e);
        }
        System.out.println("No of drug encounters found: " + encounters.size());

        ILMessage ilMessage = ILPrescriptionMessage.generatePrescriptionMessage(patient, encounters);
        KenyaEMRILService service = Context.getService(KenyaEMRILService.class);
        service.logPharmacyOrders(ilMessage);

        return ret;
    }
}