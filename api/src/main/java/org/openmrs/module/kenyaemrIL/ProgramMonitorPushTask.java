package org.openmrs.module.kenyaemrIL;

import org.json.simple.JSONArray;
import org.openmrs.GlobalProperty;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.dmi.DmiDataExchange;
import org.openmrs.module.kenyaemrIL.dmi.dmiUtils;
import org.openmrs.module.kenyaemrIL.caseSurveillance.CaseSurveillanceDataExchange;
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
 * Directly push messages to DMI server & Case surveillance servers
 */
public class ProgramMonitorPushTask extends AbstractTask {

	private static final Logger log = LoggerFactory.getLogger(ProgramMonitorPushTask.class);
	private String url = "http://www.google.com:80/index.html";

    /**
	 * @see AbstractTask#execute()
	 */
	public void execute() {
		System.out.println("DMI DIRECT PUSH: Scheduler started....");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		// Fetch the last date of fetch
		Date dmiFetchDate = null;
		GlobalProperty dmiGlobalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("dmiTask.lastFetchDateAndTime");
/*		try {
			if(dmiGlobalPropertyObject != null && dmiGlobalPropertyObject.getValue() != null){
			String ts = dmiGlobalPropertyObject.getValue().toString();
			dmiFetchDate = formatter.parse(ts);
			} else {
				System.out.println("Global property 'dmiTask.lastFetchDateAndTime' not found or is null.");
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		try {
			Context.openSession();
			// check first if there is internet connectivity before pushing
			URLConnection connection = new URL(url).openConnection();
			connection.connect();
			List<Visit> visits = getComplaintsAndDiagnosis(dmiFetchDate);
            int numberOfVisits = 0;
            if (visits.size() > numberOfVisits) {
				for (Visit visit : visits) {
					if (visit != null) {
						System.out.println("Visit: start sending dmi data");
						DmiDataExchange dmiDataExchange = new DmiDataExchange();
						JSONArray params = dmiDataExchange.generateDMIpostPayload(visit, dmiFetchDate);
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
			Context.flushSession();
		} catch (IOException ioe) {

			try {
				String text = "IL - DMI PUSH: At " + new Date() + " there was connectivity error. ";
				log.warn(text, ioe);
				ioe.printStackTrace(System.out);
			} catch (Exception e) {
				log.error("IL - DMI PUSH: Failed to check internet connectivity", e);
			}
		} finally {
			if(dmiGlobalPropertyObject != null) {
				dmiGlobalPropertyObject.setPropertyValue(formatter.format(new Date()));
				Context.getAdministrationService().saveGlobalProperty(dmiGlobalPropertyObject);
			}
			Context.closeSession();
		}*/
		System.out.println("Case Surveillance data transmission started...");
		try {
			Date csFetchDate = null;
			GlobalProperty csGlobalPropertyObject = Context.getAdministrationService().getGlobalPropertyObject("caseSurveillance.lastFetchDateAndTime");
			try {
				if (csGlobalPropertyObject != null && csGlobalPropertyObject.getValue() != null) {
					String ts = csGlobalPropertyObject.getValue().toString();
					csFetchDate = formatter.parse(ts);
					System.out.println("csFetchDate: "+csFetchDate);
				} else {
					// Handle case where global property might be missing or null
					log.warn("Global property 'caseSurveillance.lastFetchDateAndTime' not found or has a null value.");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
            Context.openSession();
            URLConnection connection = new URL(url).openConnection();
            connection.connect();

            boolean processSuccess = false;
            try {
                CaseSurveillanceDataExchange caseSurveillance = new CaseSurveillanceDataExchange();
                caseSurveillance.processAndSendCaseSurveillancePayload(csFetchDate);
                processSuccess = true;
            } catch (Exception e) {
                log.error("Error during case surveillance process", e);
            }
            if (processSuccess) {
				try {
					String newFetchDate = formatter.format(new Date());
					csGlobalPropertyObject.setPropertyValue(newFetchDate);
					Context.getAdministrationService().saveGlobalProperty(csGlobalPropertyObject);
				} catch (Exception e) {
					log.error("Error updating global property 'caseSurveillance.lastFetchDateAndTime'", e);
				}
			}
			Context.flushSession();
		} catch (Exception ex) {
			log.error("Error during case surveillance data transmission", ex);
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
