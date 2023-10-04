package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONArray;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.dmi.DmiDataExchange;
import org.openmrs.module.kenyaemrIL.dmi.dmiUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.ui.framework.SimpleObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Directly push messages to DMI server
 */
public class DmiDirectPushTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(DmiDirectPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("DMI DIRECT PUSH: Scheduler started....");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
       // Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("dmiTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
            System.out.println("Printing fetchDate ==> "+fetchDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Context.openSession();

            // check first if there is internet connectivity before pushing
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            List<Encounter> encounters = getComplaintsAndDiagnosis(fetchDate);
            for (Encounter encounter : encounters) {
                if (encounter != null) {
                    DmiDataExchange dmiDataExchange = new DmiDataExchange();
                    JSONArray params = dmiDataExchange.generateDMIpostPayload(encounter,fetchDate);
                    System.out.println("Printing payload ==> "+params);

                    try {
                        SimpleObject results = dmiUtils.sendPOST(params.toJSONString());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                }
            }

            Date nextProcessingDate = new Date();
            globalPropertyObject.setPropertyValue(formatter.format(nextProcessingDate));
            Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
            Context.flushSession();
        } catch (IOException ioe) {

            try {
                String text = "IL - DMI PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - DMI PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Gets a list of patients who have had complaints or diagnosis recorded in triage or greencard forms since the last timestamp
     * @param date last timestamp
     * @return a list of patients who have had complaints or diagnosis recorded as at the provided timestamp
     */
    private List<Encounter> getComplaintsAndDiagnosis (Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e inner join " +
                "( " +
                " select encounter_type_id, uuid, name from encounter_type where uuid in ('d1059fb9-a079-4feb-a749-eedd709ae542','a0034eee-1940-4e35-847f-97537a35d05e')" +
                " ) et on et.encounter_type_id=e.encounter_type " +
                " inner join obs o on o.encounter_id=e.encounter_id and o.voided=0 " +
                " and o.concept_id in (5219,6042)");
        q.append("where e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'");
        q.append(" and e.voided = 0  ");

        List<Encounter> encounters = new ArrayList<>();
        EncounterService encounterService = Context.getEncounterService();
        List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
        for (List<Object> row : queryData) {
            Integer encounterId = (Integer) row.get(0);
            Encounter e = encounterService.getEncounter(encounterId);
            encounters.add(e);
        }
        return encounters;

    }
}
