package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.json.simple.JSONObject;
import org.openmrs.*;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PersonService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.metadata.OTZMetadata;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.kenyaemrIL.util.ServiceDepartments;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

public class VisualizationDataExchange {

	private static Log log = LogFactory.getLog(VisualizationDataExchange.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static List<Diagnosis> allDiagnosis;
	private static EncounterService service= Context.getEncounterService();
	final static String FAMILY_PLANNING_FORM_UUID = "a52c57d4-110f-4879-82ae-907b0d90add6";
	public static final String IMMUNIZATION = "29c02aff-9a93-46c9-bf6f-48b552fcb1fa";
	public static Concept immunizationConcept = Dictionary.getConcept(Dictionary.IMMUNIZATIONS);
	/**
	 * Generates the payload used to post to visualization server     *
	 *
	 * @param
	 * @return
	 */
	public static JSONObject generateVisualizationPayload(Date fetchDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		JSONObject payloadObj = new JSONObject();
		List<SimpleObject> bedManagement = new ArrayList<SimpleObject>();
		List<SimpleObject> visits = new ArrayList<>();
		List<SimpleObject> visitsByAge = new ArrayList<>();
		List<SimpleObject> outPatientByService = new ArrayList<>();
		List<SimpleObject> immunizations = new ArrayList<>();
		Map<String, Integer> visitsMap;
		Map<String, Integer> diagnosisMap;
		Map<String, Integer> mortalityMap;
		Map<String, Integer> workloadMap;
		Map<String, Integer> visitByAgeMap;
		Map<String, Integer> outpatientByServiceMap;
		Map<String, Integer> immunizationsMap;
		List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
		List<SimpleObject> workload = new ArrayList<SimpleObject>();
		List<SimpleObject> outPatientServices = new ArrayList<SimpleObject>();
		List<SimpleObject> billing = new ArrayList<SimpleObject>();
		List<SimpleObject> billingItems= new ArrayList<SimpleObject>();
		List<SimpleObject> paymentItems= new ArrayList<SimpleObject>();
		List<SimpleObject> inventoryItems= new ArrayList<SimpleObject>();
		List<SimpleObject> queueItems= new ArrayList<SimpleObject>();
		List<SimpleObject> payments = new ArrayList<SimpleObject>();
		List<SimpleObject> inventory = new ArrayList<SimpleObject>();
		List<SimpleObject> mortality = new ArrayList<SimpleObject>();
		List<SimpleObject> queueWaitTime = new ArrayList<SimpleObject>();
		List<SimpleObject> staff = new ArrayList<SimpleObject>();
		List<SimpleObject> staffCount = new ArrayList<SimpleObject>();
		List<SimpleObject> waivers = new ArrayList<SimpleObject>();
		List<SimpleObject> waiversCount = new ArrayList<SimpleObject>();
		String timestamp = formatter.format(fetchDate);

		//Data extraction
		String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());

		//add to list
		payloadObj.put("mfl_code", facilityMfl);
		payloadObj.put("timestamp", timestamp);

		try {
			if (bedManagement.size() > 0) {
				SimpleObject bedManagementObject = new SimpleObject();
				bedManagementObject.put("ward", "");
				bedManagementObject.put("capacity", "");
				bedManagementObject.put("occupancy", "");
				bedManagementObject.put("new_admissions", "");
				bedManagement.add(bedManagementObject);
				payloadObj.put("bed_management", bedManagement);
			} else {
				payloadObj.put("bed_management", bedManagement);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : bed_management : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			visitsMap = allVisits(fetchDate);
			if (!visitsMap.isEmpty()) {
				for (Map.Entry<String, Integer> visitEntry : visitsMap.entrySet()) {
					SimpleObject visitsObject = new SimpleObject();
					visitsObject.put("visit_type", visitEntry.getKey());
					visitsObject.put("total", visitEntry.getValue().toString());
					visits.add(visitsObject);
				}
				payloadObj.put("visits", visits);
			} else {
				payloadObj.put("visits", visits);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : visits : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			visitByAgeMap = outPatientVisitsByAge(fetchDate);
			if (!visitByAgeMap.isEmpty()) {
				for (Map.Entry<String, Integer> visitEntry : visitByAgeMap.entrySet()) {
					SimpleObject visitsByAgeObject = new SimpleObject();
					visitsByAgeObject.put("age", visitEntry.getKey());
					visitsByAgeObject.put("total", visitEntry.getValue().toString());
					visitsByAge.add(visitsByAgeObject);
				}
				payloadObj.put("opd_visits", visitsByAge);
			} else {
				payloadObj.put("opd_visits", visitsByAge);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : OPD Visits : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			outpatientByServiceMap = outPatientVisitsByService(fetchDate);
			if (!outpatientByServiceMap.isEmpty()) {
				for (Map.Entry<String, Integer> visitEntry : outpatientByServiceMap.entrySet()) {
					SimpleObject outpatientByServiceObject = new SimpleObject();
					outpatientByServiceObject.put("service", visitEntry.getKey());
					outpatientByServiceObject.put("total", visitEntry.getValue().toString());
					outPatientByService.add(outpatientByServiceObject);
				}
				payloadObj.put("opd_visits_by_service_type", outPatientByService);
			} else {
				payloadObj.put("opd_visits_by_service_type", outPatientByService);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : OPD Visits By Service Type : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			immunizationsMap = immunizations(fetchDate);
			if(!immunizationsMap.isEmpty()) {
				for (Map.Entry<String, Integer> immunizationEntry : immunizationsMap.entrySet()) {
					SimpleObject immunizationsObject = new SimpleObject();
					immunizationsObject.put("vaccine", immunizationEntry.getKey());
					immunizationsObject.put("total", immunizationEntry.getValue().toString());
					immunizations.add(immunizationsObject);
				}
				payloadObj.put("immunization", immunizations);
			} else {
				payloadObj.put("immunization", immunizations);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : immunization : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			diagnosisMap = allDiagnosis(fetchDate);
			if (!diagnosisMap.isEmpty()) {
				for (Map.Entry<String, Integer> diagnosisEntry : diagnosisMap.entrySet()) {
					SimpleObject diagnosisObject = new SimpleObject();
					diagnosisObject.put("diagnosis_name", diagnosisEntry.getKey());
					diagnosisObject.put("total", diagnosisEntry.getValue().toString());
					diagnosis.add(diagnosisObject);
				}
				payloadObj.put("diagnosis", diagnosis);
			} else {
				payloadObj.put("diagnosis", diagnosis);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : diagnosis : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			workloadMap = workLoad(fetchDate);
			if (!workloadMap.isEmpty()){
				for(Map.Entry<String, Integer> workloadEntry : workloadMap.entrySet()){
				SimpleObject workloadObject = new SimpleObject();
				workloadObject.put("department", workloadEntry.getKey());
				workloadObject.put("total", workloadEntry.getValue().toString());
				workload.add(workloadObject);
				payloadObj.put("workload", workload);
			}
			} else {
				payloadObj.put("workload", workload);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : workload : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			billingItems = getBillingItems(fetchDate);
			if (billingItems.size() > 0) {
				for (int i = 0; i < billingItems.size(); i++) {
					SimpleObject bill = billingItems.get(i);
					SimpleObject billingObject = new SimpleObject();
					billingObject.put("service_type", bill.get("service_type"));
					billingObject.put("invoices", bill.get("invoices"));
					billingObject.put("amount_due", bill.get("amount_due"));
					billingObject.put("amount_paid",bill.get("amount_paid"));
					billingObject.put("balance_due", bill.get("balance_due"));
					billing.add(billingObject);
					payloadObj.put("billing", billing);
				}
			} else {
				payloadObj.put("billing", billing);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : billing : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			paymentItems = getPayments(fetchDate);
			if (paymentItems.size() > 0) {
				for (int i = 0; i < paymentItems.size(); i++) {
					SimpleObject paymentsList= paymentItems.get(i);
					SimpleObject paymentsObject = new SimpleObject();
					paymentsObject.put("payment_mode", paymentsList.get("payment_mode"));
					paymentsObject.put("no_of_patients", paymentsList.get("no_of_patients"));
					paymentsObject.put("amount_paid", paymentsList.get("amount_paid"));
					payments.add(paymentsObject);
					payloadObj.put("payments", payments);
				}
			} else {
				payloadObj.put("payments", payments);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : payments : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			inventoryItems = getInventory(fetchDate);
			if (inventoryItems.size() > 0) {
				for (int i = 0; i < inventoryItems.size(); i++) {
					SimpleObject inventoryList= inventoryItems.get(i);
					SimpleObject inventoryObject = new SimpleObject();
					inventoryObject.put("item_name", inventoryList.get("item_name"));
					inventoryObject.put("item_type", inventoryList.get("item_type"));
					inventoryObject.put("unit_of_measure", inventoryList.get("unit_of_measure"));
					inventoryObject.put("quantity_at_hand", inventoryList.get("quantity_at_hand"));
					inventoryObject.put("quantity_consumed", inventoryList.get("quantity_consumed"));
					inventory.add(inventoryObject);
					payloadObj.put("inventory", inventory);
				}
			} else {
				payloadObj.put("inventory", inventory);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : inventory : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			mortalityMap = mortality(fetchDate);
			if (!mortalityMap.isEmpty()) {
				for (Map.Entry<String, Integer> mortalityEntry : mortalityMap.entrySet()) {
					SimpleObject mortalityObject = new SimpleObject();
					mortalityObject.put("cause_of_death", mortalityEntry.getKey());
					mortalityObject.put("total", mortalityEntry.getValue().toString());
					mortality.add(mortalityObject);
					payloadObj.put("mortality", mortality);
				}
			} else {
				payloadObj.put("mortality", mortality);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : mortality : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			queueItems = getWaitTime(fetchDate);
			if (queueItems.size() > 0) {
				for (int i = 0; i < queueItems.size(); i++) {
					SimpleObject queueList= queueItems.get(i);
					SimpleObject queueObject = new SimpleObject();
					queueObject.put("queue", queueList.get("queue"));
					queueObject.put("average_wait_time", queueList.get("average_wait_time"));
					queueWaitTime.add(queueObject);
					payloadObj.put("wait_time", queueWaitTime);
				}
			} else {
				payloadObj.put("wait_time", queueWaitTime);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : wait time : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			staff = getStaffByCadre(fetchDate);
			if (staff.size() > 0) {
				for (int i = 0; i < staff.size(); i++) {
					SimpleObject staffList= staff.get(i);
					SimpleObject staffObject = new SimpleObject();
					staffObject.put("staff", staffList.get("staff"));
					staffObject.put("staff_count", staffList.get("staff_count"));
					staffCount.add(staffObject);
					payloadObj.put("staff_count", staffCount);
				}
			} else {
				payloadObj.put("staff_count", staffCount);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : staff : " + ex.getMessage());
			ex.printStackTrace();
		}

		try {
			waivers = getTotalWaivers(fetchDate);
			if (waivers.size() > 0) {
				SimpleObject waiversList= waivers.get(0);
				payloadObj.put("waivers", waiversList.get("waivers"));
			} else {
				payloadObj.put("waivers", waiversCount);
			}
		} catch(Exception ex) {
			System.err.println("KenyaEMR IL: ERROR visualization data : waivers : " + ex.getMessage());
			ex.printStackTrace();
		}

		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		System.out.println("Payload generated: " + payloadObj);
		return payloadObj;
	}

	public static Map<String, Integer> allVisits(Date fetchDate) {

		Map<String, Integer> visitMap = new HashMap<>();
		VisitService visitService = Context.getVisitService();
		List<Visit> allVisits = visitService.getVisits(null, null, null, null, fetchDate, null, null, null, null, true, false);
		if (!allVisits.isEmpty()) {
			for (Visit visit : allVisits) {
				String visitType = visit.getVisitType().getName();
				visitMap.put(visitType, visitMap.getOrDefault(visitType, 0) + 1);
			}
		}
		return visitMap;
	}
	public static Map<String, Integer> outPatientVisitsByAge(Date fetchDate) {

		Map<String, Integer> outpatientByByAgeMap = new HashMap<>();
		VisitService visitService = Context.getVisitService();
		List<Visit> allVisits = visitService.getVisits(null, null, null, null, fetchDate, null, null, null, null, true, false);
		if (!allVisits.isEmpty()) {
			for (Visit visit : allVisits) {
				String visitType = visit.getVisitType().getName();
				Patient patient = visit.getPatient();

				if (visitType.equals("Outpatient")) {
					if (patient.getAge() < 5) {
						outpatientByByAgeMap.put("outpatient_under_5", outpatientByByAgeMap.getOrDefault("outpatient_under_5", 0) + 1);
					} else {
						outpatientByByAgeMap.put("outpatient_5_and_above", outpatientByByAgeMap.getOrDefault("outpatient_5_and_above", 0) + 1);
					}
				}
			}
		}
		return outpatientByByAgeMap;
	}
	public static Map<String, Integer> outPatientVisitsByService(Date fetchDate) {

		Map<String, Integer> outpatientByServiceMap = new HashMap<>();
		VisitService visitService = Context.getVisitService();
		List<Visit> allVisits = visitService.getVisits(null, null, null, null, fetchDate, null, null, null, null, true, false);

		if (!allVisits.isEmpty()) {
			for (Visit visit : allVisits) {
				String visitType = visit.getVisitType().getName();

				if (visitType.equals("Outpatient")) {
					List<Encounter> encounters = service.getEncountersByVisit(visit, false);
					if (!encounters.isEmpty()){
						for (Encounter encounter : encounters) {
							String serviceName = encounter.getEncounterType().getName();
							outpatientByServiceMap.put(serviceName, outpatientByServiceMap.getOrDefault(serviceName, 0) + 1);
						}
				}
				}
			}
		}
		return outpatientByServiceMap;
	}

	public static Map<String, Integer> immunizations(Date fetchDate) {

		Map<String, Integer> immunizationsMap = new HashMap<>();
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null,
				null,
				fetchDate,
				null,
				null,
				null,
				Arrays.asList(MetadataUtils.existing(EncounterType.class, IMMUNIZATION)),
				null,
				null,
				null,
				false);
		List<Encounter> immunizationEncounters = service.getEncounters(encounterSearchCriteria);

		if (!immunizationEncounters.isEmpty()) {
			String immunizationGiven;
			for (Encounter encounter : immunizationEncounters) {
				for (Obs obs : encounter.getObs()) {
					if (obs.getConcept().equals(immunizationConcept)) {
						immunizationGiven = obs.getValueCoded().getName().toString();
						if(!immunizationGiven.equals( "None")) {
							immunizationsMap.put(immunizationGiven, immunizationsMap.getOrDefault(immunizationGiven, 0) + 1);
						}
					}
				}
			}
		}
		return immunizationsMap;
	}

	public static Map<String, Integer> allDiagnosis(Date fetchDate) {

		Map<String, Integer> diagnosisMap = new HashMap<>();
		VisitService visitService = Context.getVisitService();
		List<Visit> allVisits = visitService.getVisits(null, null, null, null, fetchDate, null, null, null, null, true, false);

		if (!allVisits.isEmpty()) {
			for (Visit visit : allVisits) {
				if (!visit.getEncounters().isEmpty()) {					
					for (Encounter encounter : visit.getEncounters()) {
						//Get diagnosis
						DiagnosisService diagnosisService = Context.getDiagnosisService();
						List<Diagnosis> allDiagnosis = diagnosisService.getDiagnosesByEncounter(encounter, false, false);
						if (!allDiagnosis.isEmpty()) {
							for (Diagnosis diagnosis : allDiagnosis) {
								String diagnosisName = diagnosis.getDiagnosis().getCoded().getName().getName();
								diagnosisMap.put(diagnosisName, diagnosisMap.getOrDefault(diagnosisName, 0) + 1);
							}
						}
					}
				}
			}
		}
		
	return diagnosisMap;
	}
	/**
	 * Gets details of all bills
	 * @param 
	 * @return details of all bills
	 */
	public static List<SimpleObject> getBillingItems(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);		
		final String sqlSelectQuery = "select cbl.service_id, cbl.bill_id, cbs.name, SUM(cbp.amount), SUM(cbp.amount_tendered), SUM(cbp.amount - cbp.amount_tendered ) from openmrs.cashier_bill_line_item cbl inner join openmrs.cashier_bill_payment cbp on cbl.bill_id = cbp.bill_id inner join openmrs.cashier_billable_service cbs on cbs.service_id = cbl.service_id where date(cbl.date_created) >= '" + effectiveDate + "' or date(cbl.date_changed) >= '" + effectiveDate + "' group by cbl.service_id;";
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
									"invoices", row[1] != null ? row[1].toString() : "",
									"service_type", row[2] != null ? row[2].toString() : "",		
									"amount_due", row[3] != null ? row[3].toString() : "",
									"amount_paid", row[4] != null ? row[4].toString() : "",
									"balance_due", row[5] != null ? row[5].toString() : ""
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to execute query", e);
		}	
		return ret;
	}

	/**
	 * Gets details of all payments
	 * @param
	 * @return details of all payments
	 */
	public static List<SimpleObject> getPayments(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);	
		final String sqlSelectQuery = "select cbm.payment_mode_id, cbm.name, count(cb.patient_id), SUM( cbp.amount_tendered) as amount_paid from openmrs.cashier_bill_payment cbp inner join openmrs.cashier_payment_mode cbm on cbm.payment_mode_id = cbp.payment_mode_id inner join openmrs.cashier_bill cb on cb.bill_id = cbp.bill_id where cbp.date_created >= '" + effectiveDate + "' or cbp.date_changed >= '" + effectiveDate + "' group by cbm.payment_mode_id;";
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
									"payment_mode", row[1] != null ? row[1].toString() : "",
									"no_of_patients", row[2] != null ? row[2].toString() : "",
									"amount_paid", row[3] != null ? row[3].toString() : ""									
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to execute query", e);
		}
		return ret;
	}

	public static Map<String, Integer> mortality(Date midNightDateTime) {
		PersonService personService = Context.getPersonService();
		Map<String, Integer> mortalityMap = new HashMap<>();
		List<Person> deceasedPersons = personService.getPeople("", true);

		List<Form> discForms = Arrays.asList(
				MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_DISCONTINUATION),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_DISCONTINUATION),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DISCONTINUATION),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_HEI_COMPLETION),
				MetadataUtils.existing(Form.class, OTZMetadata._Form.OTZ_DISCONTINUATION_FORM)
		);

		List<Encounter> discontinuationEncounters = Context.getEncounterService().getEncounters(null, null,
				midNightDateTime, null, discForms, null, null, null, null, false);

		Set<Person> personSet = new HashSet<>();
		for (Encounter encounter : discontinuationEncounters) {
			Person person = encounter.getPatient().getPerson();
			personSet.add(person);
		}
		if (!discontinuationEncounters.isEmpty()) {
			for (Encounter encounter : discontinuationEncounters) {

				List<Obs> deathReasonsObs = encounter.getObs().stream()
						.filter(ob -> ob.getConcept().getUuid().equals("1599AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
						.collect(Collectors.toList());
				List<Obs> deathDateObs = encounter.getObs().stream()
						.filter(ob -> ob.getConcept().getUuid().equals("1543AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
						.collect(Collectors.toList());
				Map<Integer, String> obsDeathReasonsMap = new HashMap<>();

				if(!deathReasonsObs.isEmpty()) {

						for (Obs obs : deathReasonsObs) {
							Integer personId = obs.getPersonId();
							String valueCoded = (obs.getValueCoded() != null) ? obs.getValueCoded().getName().getName() : null;
							obsDeathReasonsMap.put(personId, valueCoded);
						}
					}

				Map<Integer, Date> obsDeathDateMap = new HashMap<>();

				for (Obs obs : deathDateObs) {
					Integer personId = obs.getPersonId();
					Date deathDate = (obs.getValueDatetime() != null) ? obs.getValueDatetime() : obs.getObsDatetime();
					obsDeathDateMap.put(personId, deathDate);
				}

				Map<String, Date> mergedMap = new HashMap<>();
				for (Integer key : obsDeathDateMap.keySet()) {
					if (obsDeathReasonsMap.containsKey(key)) {
						Date date = obsDeathDateMap.get(key);
						String reason = obsDeathReasonsMap.get(key);
						mergedMap.put(reason, date);
					}
				}
				for (Map.Entry<String, Date> mergedMapEntry : mergedMap.entrySet()) {
					if (!mergedMapEntry.getValue().before(midNightDateTime)) {
						mortalityMap.put(mergedMapEntry.getKey(), mortalityMap.getOrDefault(mergedMapEntry.getKey(), 0) + 1);
					}
				}
			}
		}
		for (Person person : deceasedPersons) {
			Date dateOfDeath;
			if (!personSet.contains(person) && person.getDeathDate() != null) {
				dateOfDeath = person.getDeathDate();
				if (!dateOfDeath.before(midNightDateTime)) {
					String causeOfDeath = person.getCauseOfDeath().getName().getName();
					mortalityMap.put(causeOfDeath, mortalityMap.getOrDefault(causeOfDeath, 0) + 1);
				}

			}
		}
		return mortalityMap;
	}
	/**
	 * Gets details of  inventory
	 * @param
	 * @return details of  inventory
	 */
	public static List<SimpleObject> getInventory(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);		
		final String sqlSelectQuery = "select sti.common_name, if (sti.is_drug = 1, 'Drug','Non drug'), cn.name, SUM( stt.quantity), SUM( If (stt.quantity<0, stt.quantity*-1,0)) from stockmgmt_stock_item_transaction stt inner join openmrs.stockmgmt_stock_item sti on sti.stock_item_id = stt.stock_item_id inner join openmrs.concept_name cn on cn.concept_id = sti.dispensing_unit_id where stt.date_created >= '" + effectiveDate + "' group by sti.stock_item_id,stt.party_id;";
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
									"item_name", row[0] != null ? row[0].toString() : "",
									"item_type", row[1] != null ? row[1].toString() : "",
									"unit_of_measure", row[2] != null ? row[2].toString() : "",
									"quantity_at_hand", row[3] != null ? row[3].toString() : "",
									"quantity_consumed", row[4] != null ? row[4].toString() : ""
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to execute query", e);
		}		
		return ret;
	}

	public static Map<String, Integer> workLoad(Date midNightDateTime){
		Map<String, Integer> workLoadMap = new HashMap<>();

		List<Form> serviceUnitsForms = Arrays.asList(
				MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_FOLLOW_UP),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANTENATAL_VISIT),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT),
				MetadataUtils.existing(Form.class, FAMILY_PLANNING_FORM_UUID),
				MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DELIVERY)
		);

		List<Encounter> serviceUnitsEncounters = Context.getEncounterService().getEncounters(null, null,
				midNightDateTime, null, serviceUnitsForms, null, null, null, null, false);
		if (!serviceUnitsEncounters.isEmpty()) {
			for (Encounter e : serviceUnitsEncounters) {
				if (e.getForm().equals(MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER))) {
					workLoadMap.put(ServiceDepartments.GENERALOUTPATIENT.getDepartmentName(), workLoadMap.getOrDefault(ServiceDepartments.GENERALOUTPATIENT.getDepartmentName(), 0) + 1);
				}
				if (e.getForm().equals(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHCS_FOLLOW_UP)) || e.getForm().equals(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANTENATAL_VISIT))
						|| e.getForm().equals(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT)) || e.getForm().equals(MetadataUtils.existing(Form.class,FAMILY_PLANNING_FORM_UUID))) {
					workLoadMap.put(ServiceDepartments.MCHANDFAMILYPLANNING.getDepartmentName(), workLoadMap.getOrDefault(ServiceDepartments.MCHANDFAMILYPLANNING.getDepartmentName(), 0) + 1);
				}
				if (e.getForm().equals(MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DELIVERY))) {
					workLoadMap.put(ServiceDepartments.MATERNITY.getDepartmentName(), workLoadMap.getOrDefault(ServiceDepartments.MATERNITY.getDepartmentName(), 0) + 1);
				}
			}
		}

		return workLoadMap;
	}

	/**
	 * Gets details of  wait_time
	 * @param
	 * @return details of  wait_time
	 */
	public static List<SimpleObject> getWaitTime(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		final String sqlSelectQuery = "SELECT tbl.name, ROUND(AVG(tbl.diff), 2)\n" +
				"FROM (select q.name, qe.started_at, qe.ended_at, (TIMESTAMPDIFF(SECOND, qe.started_at, qe.ended_at) / 60) as diff\n" +
				"      from openmrs.queue_entry qe\n" +
				"               inner join openmrs.queue q on q.queue_id = qe.queue_id\n" +
				"               inner join (select t.name, v.visit_id\n" +
				"                           from openmrs.visit v\n" +
				"                                    inner join openmrs.visit_type t on v.visit_type_id = t.visit_type_id where t.name = 'Outpatient') v\n" +
				"                          on qe.visit_id = v.visit_id\n" +
				"      where (date(qe.date_created) >= '" + effectiveDate + "' or date(qe.date_created) >= '" + effectiveDate + "')\n" +
				"        and qe.ended_at is not null) tbl\n" +
				"GROUP BY tbl.name;";
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
									"queue", row[0] != null ? row[0].toString() : "",
									"average_wait_time", row[1] != null ? row[1].toString() : ""									
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to execute query", e);
		}
		return ret;
	}

	/**
	 * Gets details of staff by cadre
	 * @param
	 * @return details of staff by cadre
	 */
	public static List<SimpleObject> getStaffByCadre(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		final String sqlSelectQuery = "SELECT ur.role as role, COUNT(DISTINCT ur.user_id) AS role_count\n" + //
						"FROM user_role ur\n" + //
						"JOIN users u ON ur.user_id = u.user_id\n" + //
						"WHERE role LIKE '%Clinician' \n" + //
						"   OR role LIKE '%Data Clerk' \n" + //
						"   OR role LIKE '%Manager' \n" + //
						"   OR role LIKE '%Pharmacist'\n" + //
						"   OR role LIKE '%Provider'\n" + //
						"   OR role LIKE '%Nurse%'\n" + //
						"   OR role LIKE '%Cashier%'\n" + //
						"   OR role LIKE '%Dentist%'\n" + //
						"and u.date_created >= '" + effectiveDate + "'\n" + //
						"GROUP BY ur.role;";
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
									"staff", row[0] != null ? row[0].toString() : "",
									"staff_count", row[1] != null ? row[1].toString() : ""									
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			System.err.println("KenyaEMR IL: Unable to get staff by cadre: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalArgumentException("Unable to execute query", e);
		}
		return ret;
	}

	/**
	 * Gets details of total waivers
	 * @param
	 * @return details of total waivers
	 */
	public static List<SimpleObject> getTotalWaivers(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		final String sqlSelectQuery = "SELECT sum(amount_tendered) as total FROM openmrs.cashier_bill_payment where payment_mode_id = 7 and date_created >= '" + effectiveDate + "'";
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
									"waivers", row[0] != null ? row[0].toString() : ""									
								));
							}
						}
						finalTx.commit();
					} finally {
						try {
							if (statement != null) {
								statement.close();
							}
						} catch (Exception e) {
						}
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to execute query", e);
		}
		return ret;
	}

}


