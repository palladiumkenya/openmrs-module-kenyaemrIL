package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONObject;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationDataExchange;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
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
 * Directly push visualization metrics to Visualization server
 */
public class VisualizationMetricsPushTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(VisualizationMetricsPushTask.class);
    private String url = "http://www.google.com:80/index.html";

    /**
     * @see AbstractTask#execute()
     */
    public void execute() {
        System.out.println("Visualization PUSH: Scheduler started....");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
       // Fetch the last date of fetch
        Date fetchDate = null;
        GlobalProperty globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("visualizationTask.lastFetchDateAndTime");

        try {
            String ts = globalPropertyObject.getValue().toString();
            fetchDate = formatter.parse(ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Context.openSession();

            // check first if there is internet connectivity before pushing
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            List<Encounter> encounters = getVisualizationMetrics(fetchDate);
            for (Encounter encounter : encounters) {
                if (encounter != null) {
                    VisualizationDataExchange vDataExchange = new VisualizationDataExchange();
                    JSONObject params = vDataExchange.generateVisualizationPayload(fetchDate);
                    System.out.println("Payload to Visualization server ==> "+params);

                    try {
                        Boolean results = VisualizationUtils.sendPOST(params.toJSONString());
                        System.out.println("Send status ==>"+results);
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
                String text = "IL - Visualization PUSH: At " + new Date() + " there was connectivity error. ";
                log.warn(text);
            } catch (Exception e) {
                log.error("IL - Visualization PUSH: Failed to check internet connectivity", e);
            }
        } finally {
            Context.closeSession();

        }
    }

    /**
     * Gets a list of visualization matrices since the last timestamp
     * @param date last timestamp
     * @return a list of visualization matrices recorded as at the provided timestamp
     */
    private List<Encounter> getVisualizationMetrics (Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(date);
        StringBuilder q = new StringBuilder();
        q.append("select e.encounter_id ");
        q.append("from encounter e ");
        q.append("where e.date_created >= '" + effectiveDate + "' or e.date_changed >= '" + effectiveDate + "'");
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
