package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONObject;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationDataExchange;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Directly push visualization metrics to Visualization server
 */
public class VisualizationMetricsPushTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(VisualizationMetricsPushTask.class);
    private static final String CONNECTIVITY_TEST_URL = "http://www.google.com:80/index.html";
    private static final String GLOBAL_PROP_LAST_FETCH = "visualizationTask.lastFetchDateAndTime";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final Object TASK_LOCK = new Object();
    /**
	 * @see AbstractTask#execute()
	 */
    public void execute() {
        synchronized (TASK_LOCK) {
            boolean sessionOpened = false;
            try {
                if (!Context.isSessionOpen()) {
                    Context.openSession();
                    sessionOpened = true;
                }
            Date fetchDate;
            GlobalProperty globalPropertyObject = null;

            try {
                globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(GLOBAL_PROP_LAST_FETCH);
                if (globalPropertyObject != null && globalPropertyObject.getValue() != null) {
                    String ts = globalPropertyObject.getValue().toString();
                    fetchDate = TIMESTAMP_FORMAT.parse(ts);
                } else {
                    fetchDate = new Date();
                    System.out.println("Visualization data push:Last fetch date not found. Defaulting to: " + fetchDate);
                }
                System.out.println("Visualization data push: Last fetch date: " + fetchDate);
            } catch (Exception e) {
                System.err.println("KenyaEMR IL: Error formatting last fetch date. Defaulting to current date:" + e.getMessage() + ":" + e);
                fetchDate = new Date();
            }
            if (!VisualizationUtils.hasInternetConnectivity(CONNECTIVITY_TEST_URL)) {
                    String text = "KenyaEMR IL: Visualization data push at: " + new Date() + " there was connectivity error.";
                    System.err.println(text);
                    return;
                }

                VisualizationDataExchange vDataExchange = new VisualizationDataExchange();

                JSONObject params = vDataExchange.generateVisualizationPayload(fetchDate);

                boolean results = VisualizationUtils.sendPOST(params.toJSONString());

                // Set next fetch date as the last moment of previous day (midnight)
                Date yesterdayMidnight = OpenmrsUtil.getLastMomentOfDay(
                        new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)
                );

                if (results && globalPropertyObject != null) {
                    globalPropertyObject.setPropertyValue(TIMESTAMP_FORMAT.format(yesterdayMidnight));
                    Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
                    System.out.println("Updated last fetch date to: " + TIMESTAMP_FORMAT.format(yesterdayMidnight));
                }
                Context.flushSession();
            } catch (Exception ex) {
                System.err.println("KenyaEMR IL: Visualization PUSH: " + ex.getMessage() + ":" + ex);
            } finally {
                if (sessionOpened) {
                    Context.closeSession();
                }
            }
        }
    }
}
