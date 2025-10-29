package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationDataExchange;
import org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationUtils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Directly push visualization metrics to Visualization server
 */
public class VisualizationMetricsPushTask extends AbstractTask {

	/*private static final Logger log = LoggerFactory.getLogger(VisualizationMetricsPushTask.class);
	private String url = "http://www.google.com:80/index.html";
*/
    private static final Logger log = LoggerFactory.getLogger(VisualizationMetricsPushTask.class);
    // Use a standard endpoint or make this configurable
    private static final String CONNECTIVITY_TEST_URL = "http://www.google.com:80/index.html";
    private static final String GLOBAL_PROP_LAST_FETCH = "visualizationTask.lastFetchDateAndTime";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
	 * @see AbstractTask#execute()
	 */
	public void execute() {

        DebugStats debugStats = new DebugStats(); // capture resource usage (start)
        debugStats.capture("START");

        Date fetchDate = null;
        GlobalProperty globalPropertyObject = null;

        try {
            globalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject(GLOBAL_PROP_LAST_FETCH);
            if (globalPropertyObject != null && globalPropertyObject.getValue() != null) {
                String ts = globalPropertyObject.getValue().toString();
                fetchDate = TIMESTAMP_FORMAT.parse(ts);
            } else {
                fetchDate = new Date();
            }
            log.debug("Fetched last fetch date: {}", fetchDate);
            System.out.println("Fetched last fetch date: {}"+ fetchDate);
        } catch (Exception e) {
            log.warn("KenyaEMR IL: Error formatting last fetch date: {}", e.getMessage(), e);
            fetchDate = new Date();
        }

        boolean sessionOpened = false;
        try {
            if (!Context.isSessionOpen()) {
                Context.openSession();
                sessionOpened = true;
                System.out.println("KenyaEMR IL: Visualization PUSH: Session opened at "+new Date());
            }

            // check internet connectivity before pushing data
            debugStats.capture("CHECK_CONNECTIVITY");
            if (!hasInternetConnectivity(CONNECTIVITY_TEST_URL)) {
                String text = "KenyaEMR IL: Visualization PUSH: At " + new Date() + " there was connectivity error.";
                log.warn(text);
                return;
            }
            debugStats.capture("CONNECTED");

            VisualizationDataExchange vDataExchange = new VisualizationDataExchange();
            debugStats.capture("INIT_METRICS_PAYLOAD");

            JSONObject params = vDataExchange.generateVisualizationPayload(fetchDate);

            log.debug("Generated visualization payload: {} bytes", params.toJSONString().length());
            log.info("KenyaEMR IL: sending visualization data: {}", params.toJSONString());
            debugStats.capture("SEND_PAYLOAD_PRE");

            boolean results = VisualizationUtils.sendPOST(params.toJSONString());

            debugStats.capture("SEND_PAYLOAD_POST");
            log.info("Send status ==> {}", results);

            // Set next fetch date as the last moment of previous day (midnight)
            Date yesterdayMidnight = OpenmrsUtil.getLastMomentOfDay(
                    new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)
            );

            if (results && globalPropertyObject != null) {
                globalPropertyObject.setPropertyValue(TIMESTAMP_FORMAT.format(yesterdayMidnight));
                Context.getAdministrationService().saveGlobalProperty(globalPropertyObject);
                System.out.println("Updated last fetch date to: {}"+TIMESTAMP_FORMAT.format(yesterdayMidnight));
                log.debug("Updated last fetch date to: {}", TIMESTAMP_FORMAT.format(yesterdayMidnight));
            }
            Context.flushSession();

            debugStats.capture("END");
            debugStats.logAll();
        } catch (Exception ex) {
            log.warn("KenyaEMR IL: Visualization PUSH: Caught error at {}: {}", new Date(), ex.getMessage(), ex);
        } finally {
            System.out.println("Session opened? "+ sessionOpened+ ". KenyaEMR IL: Visualization PUSH: Session closed about to  "+new Date());
            if (sessionOpened) {
                System.out.println("KenyaEMR IL: Visualization PUSH: OPen Session closed at "+new Date());
                Context.closeSession();
                System.out.println("KenyaEMR IL: Visualization PUSH: Session closed at "+new Date());
            }
        }

	}
    /**
     * Checks Internet connectivity using a test URL.
     * @param testUrl URL to test
     * @return true if connected, false otherwise
     */
    private boolean hasInternetConnectivity(String testUrl) {
        try {
            URLConnection connection = new URL(testUrl).openConnection();
            connection.setConnectTimeout(4000);
            connection.connect();
            return true;
        } catch (Exception ex) {
            log.warn("KenyaEMR IL: Unable to connect to {}", testUrl, ex);
            return false;
        }
    }

    /**
     * Utility class for debug and resource stats.
     */
    private static class DebugStats {
        private final List<String> checkpoints = new java.util.ArrayList<String>();
        private final List<Long> times = new java.util.ArrayList<Long>();
        private final List<MemoryUsage> heapUsages = new java.util.ArrayList<MemoryUsage>();
        private final List<MemoryUsage> nonHeapUsages = new java.util.ArrayList<MemoryUsage>();

        DebugStats() {
            // empty
        }

        void capture(String checkpoint) {
            checkpoints.add(checkpoint);
            times.add(System.currentTimeMillis());
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            heapUsages.add(memBean.getHeapMemoryUsage());
            nonHeapUsages.add(memBean.getNonHeapMemoryUsage());
        }

        void logAll() {
            if (checkpoints.size() < 2) {
                log.debug("No debug stats to report.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("VisualizationMetricsPushTask debug timings and resource stats:");
            for (int i = 1; i < checkpoints.size(); i++) {
                String cp1 = checkpoints.get(i-1);
                String cp2 = checkpoints.get(i);
                long dt = times.get(i) - times.get(i-1);

                MemoryUsage heap = heapUsages.get(i);
                MemoryUsage nonHeap = nonHeapUsages.get(i);
                sb.append(String.format("\nCheckpoint [%s -> %s]: %d ms | Heap %d MB used / %d MB (max)", cp1, cp2, dt,
                        heap.getUsed()/(1024*1024), heap.getMax()/(1024*1024)));
                sb.append(String.format(" | NonHeap %d MB used / %d MB (max)",
                        nonHeap.getUsed()/(1024*1024), nonHeap.getMax()/(1024*1024)));
            }
           System.out.println("Memory usage: "+sb);
            log.info(sb.toString());
        }
    }
}
