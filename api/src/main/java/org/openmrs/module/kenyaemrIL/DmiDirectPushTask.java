package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONArray;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
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
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Context.openSession();

			// check first if there is internet connectivity before pushing
			URLConnection connection = new URL(url).openConnection();
			connection.connect();

			List<Visit> visits = getComplaintsAndDiagnosis(fetchDate);
			if (visits.size() > 1) {
				for (Visit visit : visits) {
					if (visit != null) {
						DmiDataExchange dmiDataExchange = new DmiDataExchange();
						JSONArray params = dmiDataExchange.generateDMIpostPayload(visit, fetchDate);
						System.out.println("Payload to DMI server ==> " + params);
						if (!params.isEmpty()) {
							try {
								SimpleObject results = dmiUtils.sendPOST(params.toJSONString());
							} catch (Exception e) {
								System.out.println(e.getMessage());
							}
						}

					}
				}
			}

			globalPropertyObject.setPropertyValue(formatter.format(new Date()));
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
	 * Gets a list of patients who have had complaints or diagnosis or labs recorded in triage or greencard forms since the last timestamp
	 *
	 * @param date last timestamp
	 * @return a list of patients who have had complaints or diagnosis or labs recorded as at the provided timestamp
	 */
	private List<Visit> getComplaintsAndDiagnosis(Date date) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(date);
		StringBuilder q = new StringBuilder();
		q.append("select v.visit_id ");
		q.append("from visit v ");
		q.append("where v.date_stopped >= '" + effectiveDate + "' ");

		List<Visit> visits = new ArrayList<>();
		VisitService visitService = Context.getVisitService();
		List<List<Object>> queryData = Context.getAdministrationService().executeSQL(q.toString(), true);
		for (List<Object> row : queryData) {
			Integer visitId = (Integer) row.get(0);
			Visit e = visitService.getVisit(visitId);
			visits.add(e);
		}
		return visits;

	}
}
