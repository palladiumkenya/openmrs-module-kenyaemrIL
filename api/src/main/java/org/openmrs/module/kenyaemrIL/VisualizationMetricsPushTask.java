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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

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
			
			VisualizationDataExchange vDataExchange = new VisualizationDataExchange();
			JSONObject params = vDataExchange.generateVisualizationPayload(fetchDate);		

			try {
				Boolean results = VisualizationUtils.sendPOST(params.toJSONString());
				System.out.println("Send status ==>" + results);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

        //Set next fetch date start time
			Date nextProcessingDate = new Date();
			nextProcessingDate.setTime(System.currentTimeMillis());
			Date startOfDayMidnight = new Date(nextProcessingDate.getTime() - (1000 * 60 * 60 * 24));
			Date midnightDateTime = OpenmrsUtil.getLastMomentOfDay(startOfDayMidnight);			
			
			globalPropertyObject.setPropertyValue(formatter.format(midnightDateTime));
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

}
