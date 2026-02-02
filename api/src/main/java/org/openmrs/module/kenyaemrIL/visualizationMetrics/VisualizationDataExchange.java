package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.json.simple.JSONObject;
import org.openmrs.Concept;
import org.openmrs.Diagnosis;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PersonService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.metadata.OTZMetadata;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationUtils.mapToBreakdownList;
import static org.openmrs.module.kenyaemrIL.visualizationMetrics.VisualizationUtils.nullIfBlank;

public class VisualizationDataExchange {

	private static final Log log = LogFactory.getLog(VisualizationDataExchange.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static List<Diagnosis> allDiagnosis;
	private static EncounterService service= Context.getEncounterService();
	public static final String IMMUNIZATION = "29c02aff-9a93-46c9-bf6f-48b552fcb1fa";
	public static Concept immunizationConcept = Dictionary.getConcept(Dictionary.IMMUNIZATIONS);
	/**
	 * Generates the payload used to post to visualization server*
	 *
	 * @param
	 * @return
	 */
	public static JSONObject generateVisualizationPayload(Date fetchDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		JSONObject payloadObj = new JSONObject();
		List<SimpleObject> bedManagement = new ArrayList<SimpleObject>();
		List<SimpleObject> immunizations = new ArrayList<>();
		Map<String, Integer> visitsMap;
		Map<String, Integer> diagnosisMap;
		Map<String, Integer> mortalityMap;
		Map<String, Integer> workloadMap;
		Map<String, Integer> visitByAgeMap;
        Map<String, Integer> inpatientVisitsByAgeMap;
        Map<String, Integer> outpatientByServiceMap;
		Map<String, Integer> immunizationsMap;
		List<SimpleObject> diagnosis = new ArrayList<SimpleObject>();
		List<SimpleObject> workload = new ArrayList<SimpleObject>();
		List<SimpleObject> billing = new ArrayList<SimpleObject>();
		List<SimpleObject> billingItems= new ArrayList<SimpleObject>();
		List<SimpleObject> paymentItems= new ArrayList<SimpleObject>();
		List<SimpleObject> inventoryItems= new ArrayList<SimpleObject>();
		List<SimpleObject> queueItems= new ArrayList<SimpleObject>();
		List<SimpleObject> payments = new ArrayList<SimpleObject>();
		List<SimpleObject> paymentsByDepartment = new ArrayList<SimpleObject>();
		List<SimpleObject> inventory = new ArrayList<SimpleObject>();
		List<SimpleObject> mortality = new ArrayList<SimpleObject>();
		List<SimpleObject> queueWaitTime = new ArrayList<SimpleObject>();
		List<SimpleObject> staff = new ArrayList<SimpleObject>();
		List<SimpleObject> loggedInUsers = new ArrayList<SimpleObject>();
		List<SimpleObject> ipdPatientsByWard = new ArrayList<SimpleObject>();
		List<SimpleObject> ipdWardPatients = new ArrayList<SimpleObject>();
		List<SimpleObject> ipdAgePatients = new ArrayList<SimpleObject>();
		List<SimpleObject> staffCount = new ArrayList<SimpleObject>();
		List<SimpleObject> loggedInUsersCount = new ArrayList<SimpleObject>();
		List<SimpleObject> waivers = new ArrayList<SimpleObject>();
		List<SimpleObject> waiversCount = new ArrayList<SimpleObject>();
		String timestamp = formatter.format(fetchDate);
		Long shaPatients = 0L;

		//Data extraction
		String facilityMfl = MessageHeaderSingleton.getDefaultLocationMflCode(MessageHeaderSingleton.getDefaultLocation());
		String version = getKenyaEMRModuleVersion();

		//add to list
		payloadObj.put("mfl_code", facilityMfl);
		payloadObj.put("timestamp", timestamp);
		payloadObj.put("version", version);

        System.out.println("KenyaEMR IL: Visualization: generating Visualization data started for mfl_code "+facilityMfl+" since "+fetchDate+ "at "+new Date());
		try {
            System.out.println("KenyaEMR IL: Visualization: Generating bed management data...");
			bedManagement = getBedManagement(fetchDate);
			payloadObj.put("bed_management", bedManagement);
            System.out.println("KenyaEMR IL: Visualization: Finished generating bed management data.");
		} catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating bed management data: "+ ex.getMessage()+":"+ ex);
		}
		try {
			// Fetch visit data
            System.out.println("KenyaEMR IL: Visualization: Generating visits data...");
            VisitService visitService = Context.getVisitService();
            Collection<VisitType> visitTypes = Arrays.asList(
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.INPATIENT),
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.OUTPATIENT)
            );
            List<Visit> allVisits = visitService.getVisits(visitTypes, null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> outPatientVisits = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.OUTPATIENT)), null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> inPatientVisits = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.INPATIENT)), null, null, null, fetchDate, null, null, null, null, true, false);

            visitsMap = allVisits(allVisits);
            outpatientByServiceMap = outPatientVisitsByService(outPatientVisits, fetchDate);
            inpatientVisitsByAgeMap = inPatientVisitsByAge(inPatientVisits);
            visitByAgeMap = outPatientVisitsByAge(outPatientVisits);

			// Prepare the visits structure
			List<SimpleObject> visitDetails = new ArrayList<>();

			// Group visits by visit_type
            for (Map.Entry<String, Integer> visitEntry : visitsMap.entrySet()) {
                SimpleObject visitsObject = new SimpleObject();
                final String visitType = visitEntry.getKey();
                visitsObject.put("visit_type", visitType);
                visitsObject.put("total", visitEntry.getValue().toString());

                // Attach age breakdown
                if ("Outpatient".equals(visitType) && !visitByAgeMap.isEmpty()) {
                    visitsObject.put("age_details", mapToBreakdownList(visitByAgeMap, "age", "total"));
                } else if ("Inpatient".equals(visitType) && !inpatientVisitsByAgeMap.isEmpty()) {
                    visitsObject.put("age_details", mapToBreakdownList(inpatientVisitsByAgeMap, "age", "total"));
                }
                visitDetails.add(visitsObject);
            }

			// Prepare the service type structure
            List<SimpleObject> serviceDetails = mapToBreakdownList(outpatientByServiceMap, "service", "total");
            SimpleObject visitsPayload = new SimpleObject();
            visitsPayload.put("category", "visit_type");
            visitsPayload.put("details", visitDetails);

            SimpleObject servicePayload = new SimpleObject();
            servicePayload.put("category", "service_type");
            servicePayload.put("details", serviceDetails);

            List<SimpleObject> visitsList = new ArrayList<>();
            visitsList.add(visitsPayload);
            visitsList.add(servicePayload);

            payloadObj.put("visits", visitsList);

            System.out.println("KenyaEMR IL: Visualization: Finished generating visits data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating visits data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating immunizations data...");
			immunizationsMap = immunizations(fetchDate);
			if(!immunizationsMap.isEmpty()) {
                for (Map.Entry<String, Integer> immunizationEntry : immunizationsMap.entrySet()) {
                    SimpleObject immunizationsObject = new SimpleObject();
                    immunizationsObject.put("Vaccine", immunizationEntry.getKey());
                    immunizationsObject.put("total", immunizationEntry.getValue().toString());
                    immunizations.add(immunizationsObject);
                }
            }
				payloadObj.put("Immunization", immunizations);

            System.out.println("KenyaEMR IL: Visualization: Finished generating Immunization data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating Immunization data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating diagnosis data...");
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
            System.out.println("KenyaEMR IL: Visualization: Finished generating diagnosis data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating diagnosis data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating workload data...");
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
            System.out.println("KenyaEMR IL: Visualization: Finished generating workload data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating workload data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating billing data...");
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
					billingObject.put("total_refunds", bill.get("total_refunds"));
					billing.add(billingObject);
					payloadObj.put("billing", billing);
				}
			} else {
				payloadObj.put("billing", billing);
			}
            System.out.println("KenyaEMR IL: Visualization: Finished generating billing data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating billing data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating payments data...");
			paymentItems = getPayments(fetchDate);
			paymentsByDepartment = getPaymentByDepartment(fetchDate);

			if (!paymentItems.isEmpty()) {
				SimpleObject paymentModeCategory = new SimpleObject();
				paymentModeCategory.put("category", "payment_mode");
				List<SimpleObject> paymentModeDetails = new ArrayList<>();
				for (SimpleObject paymentItem : paymentItems) {
					SimpleObject paymentDetails = new SimpleObject();
					paymentDetails.put("payment_mode", paymentItem.get("payment_mode"));
					paymentDetails.put("no_of_patients", paymentItem.get("no_of_patients"));
					paymentDetails.put("amount_paid", paymentItem.get("amount_paid"));
					paymentModeDetails.add(paymentDetails);
				}
				paymentModeCategory.put("details", paymentModeDetails);
				payments.add(paymentModeCategory);
			} else {
				payloadObj.put("payments", payments);
			}

			if (!paymentsByDepartment.isEmpty()) {
				SimpleObject departmentCategory = new SimpleObject();
				departmentCategory.put("category", "department");

				List<SimpleObject> departmentDetails = new ArrayList<>();
				for (SimpleObject departmentItem : paymentsByDepartment) {
					SimpleObject departmentDetailsObject = new SimpleObject();
					departmentDetailsObject.put("department", departmentItem.get("department"));
					departmentDetailsObject.put("amount_paid", departmentItem.get("amount_paid"));
					departmentDetails.add(departmentDetailsObject);
				}

				departmentCategory.put("details", departmentDetails);
				payments.add(departmentCategory);
			}
			// Add all payments to the payload object
			payloadObj.put("payments", payments);

            System.out.println("KenyaEMR IL: Visualization: Finished generating payments data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating payments data: "+ ex.getMessage()+":"+ ex);
		}
		try {
            System.out.println("KenyaEMR IL: Visualization: Generating inventory data...");
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
			log.error("KenyaEMR IL: Visualization: Error building inventory payload: " + ex.getMessage(), ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating mortality data...");
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
            System.out.println("KenyaEMR IL: Visualization: Finished generating mortality data.");
            System.out.println("Mortality Data Payload " + mortality);
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating mortality data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating waiting times data...");
			queueItems = getWaitTime(fetchDate);
			if (queueItems.size() > 0) {
				for (int i = 0; i < queueItems.size(); i++) {
					SimpleObject queueList= queueItems.get(i);
					SimpleObject queueObject = new SimpleObject();
					queueObject.put("queue", queueList.get("queue"));
					queueObject.put("total_wait_time", queueList.get("total_wait_time"));
					queueObject.put("patient_count", queueList.get("patient_count"));
					queueWaitTime.add(queueObject);
					payloadObj.put("wait_time", queueWaitTime);
				}
			} else {
				payloadObj.put("wait_time", queueWaitTime);
			}
            System.out.println("KenyaEMR IL: Visualization: Finished waiting times data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating waiting times data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating staffing data...");
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
            System.out.println("KenyaEMR IL: Visualization: Finished generating staffing data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating staffing data: "+ ex.getMessage()+":"+ ex);
		}

		try {
			System.out.println("KenyaEMR IL: Visualization: Generating Logged in users data...");
			loggedInUsers = getLoggedInUsers(fetchDate);
			if (!loggedInUsers.isEmpty()) {
				for (int i = 0; i < loggedInUsers.size(); i++) {
					SimpleObject loggedInUsersList = loggedInUsers.get(i);
					SimpleObject loggedInUsersObject = new SimpleObject();
					loggedInUsersObject.put("logged_in_users_with_roles", loggedInUsersList.get("logged_in_users_with_roles"));
					loggedInUsersObject.put("total_active_users_with_roles", loggedInUsersList.get("total_active_users_with_roles"));
					loggedInUsersCount.add(loggedInUsersObject);
					payloadObj.put("logged_in_users_with_roles", loggedInUsersCount);
				}
			} else {
				payloadObj.put("logged_in_users_with_roles", loggedInUsersCount);
			}
			System.out.println("KenyaEMR IL: Visualization: Finished generating Logged in users data.");
		} catch (Exception ex) {
			System.err.println("KenyaEMR IL:Visualization: Error generating Logged in users data: " + ex.getMessage() + ":" + ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating payment waivers data...");
			waivers = getTotalWaivers(fetchDate);
            payloadObj.put("waivers",waivers);
            System.out.println("KenyaEMR IL: Visualization: Finished generating payment waivers data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating payment waivers data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating SHA enrollments data...");
			 shaPatients = getTotalPatientsOnSHA(fetchDate);
			if (shaPatients > 0) {
				payloadObj.put("sha_enrollments", shaPatients);

			} else {
				payloadObj.put("sha_enrollments", "");
			}
            System.out.println("KenyaEMR IL: Visualization: Finished generating SHA enrollments data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating SHA enrollments data: "+ ex.getMessage()+":"+ ex);
		}

		try {
            System.out.println("KenyaEMR IL: Visualization: Generating admissions by age data...");
			ipdAgePatients = getInpatientsByAge(fetchDate);
			if (ipdAgePatients.size() > 0) {
                List<SimpleObject> admissionsByAge = new ArrayList<SimpleObject>();
				for (int i = 0; i < ipdAgePatients.size(); i++) {
					SimpleObject patientList= ipdAgePatients.get(i);
					SimpleObject ipdObject = new SimpleObject();
					ipdObject.put("age", patientList.get("age"));
					ipdObject.put("no_of_patients", patientList.get("no_of_patients"));
                    admissionsByAge.add(ipdObject);
				}
                payloadObj.put("admissions", admissionsByAge);
			} else {
				payloadObj.put("admissions", ipdAgePatients);
			}
            System.out.println("KenyaEMR IL: Visualization: Finished generating admissions data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating admissions data: "+ ex.getMessage()+":"+ ex);
		}
		try {
            System.out.println("KenyaEMR IL: Visualization: Generating admissions by ward data...");
			ipdWardPatients = getInpatientsByWard(fetchDate);
			if (ipdWardPatients.size() > 0) {
                List<SimpleObject> admissionsByWard = new ArrayList<SimpleObject>();
				for (int i = 0; i < ipdWardPatients.size(); i++) {
					SimpleObject patientList= ipdWardPatients.get(i);
					SimpleObject ipdObject = new SimpleObject();
					ipdObject.put("ward", patientList.get("ward"));
					ipdObject.put("no_of_patients", patientList.get("no_of_patients"));
                    admissionsByWard.add(ipdObject);
				}
                payloadObj.put("admissions", admissionsByWard);
			} else {
				payloadObj.put("admissions", ipdPatientsByWard);
			}
            System.out.println("KenyaEMR IL: Visualization: Finished generating admissions by ward data.");
        } catch(Exception ex) {
            System.err.println("KenyaEMR IL:Visualization: Error generating admissions by ward data: "+ ex.getMessage()+":"+ ex);
		}

		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        System.out.println("Payload " + payloadObj);

		return payloadObj;
	}

	public static Map<String, Integer> allVisits(List<Visit> allVisits) {
		Map<String, Integer> visitMap = new HashMap<>();
			for (Visit visit : allVisits) {
                if (visit.getVisitType() == null || visit.getVisitType().getName() == null) {
                    continue;
                }
				String visitType = visit.getVisitType().getName();
                visitMap.merge(visitType, 1, Integer::sum);
			}
            log.info("allVisits: " + visitMap);
		return visitMap;
	}

	public static Map<String, Integer> outPatientVisitsByAge(List<Visit> outPatientVisits) {
        Map<String, Integer> outpatientByAgeMap = new HashMap<>();

        if (outPatientVisits == null || outPatientVisits.isEmpty()) {
            return outpatientByAgeMap;
        }

        final String UNDER_5 = "Outpatient Under 5";
        final String ABOVE_5 = "Outpatient 5 And Above";

        for (Visit visit : outPatientVisits) {
            VisitType visitTypeObj = visit.getVisitType();
            if (visitTypeObj == null) continue;

            Patient patient = visit.getPatient();
            if (patient == null) continue;

            int age = patient.getAge();
            String key = (age < 5) ? UNDER_5 : ABOVE_5;
            outpatientByAgeMap.merge(key, 1, Integer::sum);
        }
        return outpatientByAgeMap;
	}

    public static Map<String, Integer> mortuaryVisitsByAge(List<Visit> mortuaryAdmissions) {
        Map<String, Integer> mortuaryByAgeMap = new HashMap<>();

        if (mortuaryAdmissions == null || mortuaryAdmissions.isEmpty()) {
            return mortuaryByAgeMap;
        }

        final String UNDER_5 = "Mortuary Under 5";
        final String ABOVE_5 = "Mortuary 5 And Above";

        for (Visit visit : mortuaryAdmissions) {
            Patient patient = visit.getPatient();
            if (patient == null) continue;

            int age = patient.getAge();
            String key = (age < 5) ? UNDER_5 : ABOVE_5;
            mortuaryByAgeMap.merge(key, 1, Integer::sum);
        }
        return mortuaryByAgeMap;
    }

	public static Map<String, Integer> inPatientVisitsByAge(List<Visit> inpatientVisits) {
        Map<String, Integer> inpatientByAgeMap = new HashMap<>();

        if (inpatientVisits == null || inpatientVisits.isEmpty()) {
            return inpatientByAgeMap;
        }

        final String UNDER_5 = "Inpatient Under 5";
        final String ABOVE_5 = "Inpatient 5 And Above";

        for (Visit visit : inpatientVisits) {
            Patient patient = visit.getPatient();
            if (patient == null) continue;

            int age = patient.getAge();
            String key = (age < 5) ? UNDER_5 : ABOVE_5;
            inpatientByAgeMap.merge(key, 1, Integer::sum);
        }
        return inpatientByAgeMap;
	}

	public static Map<String, Integer> outPatientVisitsByService(List<Visit> outPatientVisits, Date fetchDate) {
		Map<String, Integer> outpatientByServiceMap = new HashMap<>();
		if (outPatientVisits == null || outPatientVisits.isEmpty()) {
			return outpatientByServiceMap;
		}

		// 1. Collect unique patients to minimize database hits
		Set<Patient> uniquePatients = outPatientVisits.stream()
				.map(Visit::getPatient)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		// 2. Fetch all relevant bills for these patients since the fetchDate
		IBillService billingService = Context.getService(IBillService.class);
		Map<Integer, List<Bill>> billsByPatientId = new HashMap<>();

		for (Patient patient : uniquePatients) {
			Bill template = new Bill();
			template.setPatient(patient);
			BillSearch search = new BillSearch(template, fetchDate, null, false);
			List<Bill> patientBills = billingService.getBills(search);
			billsByPatientId.put(patient.getPatientId(), patientBills);
		}

		// 3. Map service types to visit counts (ensuring unique visits per type)
		Map<String, Set<Integer>> serviceTypeVisitIds = new HashMap<>();

		for (Visit visit : outPatientVisits) {
			Integer patientId = visit.getPatient().getPatientId();
			List<Bill> bills = billsByPatientId.getOrDefault(patientId, Collections.emptyList());

			for (Bill bill : bills) {
				if (bill.getLineItems() == null) continue;

				for (BillLineItem lineItem : bill.getLineItems()) {
					BillableService service = lineItem.getBillableService();

					// Determine service type name, defaulting to "Other" if null
					String serviceTypeName = "Other";
					if (service != null && service.getServiceType() != null) {
						serviceTypeName = service.getServiceType().getName().getName();
					}

					// Count this visit once per service type
					serviceTypeVisitIds.putIfAbsent(serviceTypeName, new HashSet<>());
					Set<Integer> countedVisits = serviceTypeVisitIds.get(serviceTypeName);

					if (!countedVisits.contains(visit.getVisitId())) {
						outpatientByServiceMap.merge(serviceTypeName, 1, Integer::sum);
						countedVisits.add(visit.getVisitId());
					}
				}
			}
		}
		return outpatientByServiceMap;
	}

	public static Map<String, Integer> immunizations(Date fetchDate) {
		Map<String, Integer> immunizationsMap = new HashMap<>();
        EncounterSearchCriteriaBuilder searchCriteria = new EncounterSearchCriteriaBuilder().setFromDate(fetchDate).setEncounterTypes(Collections.singletonList(MetadataUtils.existing(EncounterType.class, IMMUNIZATION)));
        List<Encounter> immunizationEncounters = service.getEncounters(searchCriteria.createEncounterSearchCriteria());

        if (!immunizationEncounters.isEmpty()) {
            immunizationEncounters.stream()
                    .flatMap(encounter -> encounter.getObs().stream())
                    .filter(obs -> obs.getConcept().equals(immunizationConcept))
                    .map(obs -> obs.getValueCoded().getName().toString())
                    .filter(immunizationGiven -> !"None".equals(immunizationGiven))
                    .forEach(immunizationGiven ->
                            immunizationsMap.merge(immunizationGiven, 1, Integer::sum)
                    );
		}
		return immunizationsMap;
	}

    /**
     * Retrieves a mapping of diagnosis names to their respective counts, based on the provided fetch date.
     * The query filters diagnosis data based on specific criteria and aggregates results grouped by diagnosis names.
     *
     * @param fetchDate the date from which diagnoses should be considered in the query; only diagnoses on or after this date are included
     * @return a map where the keys are diagnosis names (String) and the values are their respective counts (Integer)
     * @throws IllegalArgumentException if the query execution fails or any error occurs during data retrieval
     */
    public static Map<String, Integer> allDiagnosis(Date fetchDate) {
        Map<String, Integer> diagnosisMap = new HashMap<>();
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(fetchDate);
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);

        final String sqlSelectQuery = "SELECT\n" +
                "    cn.name AS diagnosis_name,\n" +
                "    COUNT(*) AS total\n" +
                "FROM (\n" +
                "         SELECT encounter_id, MAX(dx_rank) AS chosen_idx_rank\n" +
                "         FROM encounter_diagnosis\n" +
                "         WHERE dx_rank = 2\n" +
                "         GROUP BY encounter_id\n" +
                "     ) chosen_diagnosis\n" +
                "         JOIN encounter_diagnosis d\n" +
                "              ON d.encounter_id = chosen_diagnosis.encounter_id\n" +
                "                  AND d.dx_rank = chosen_diagnosis.chosen_idx_rank\n" +
                "         JOIN encounter e\n" +
                "              ON d.encounter_id = e.encounter_id\n" +
                "                  AND e.voided = 0\n" +
                "         JOIN concept_name cn\n" +
                "              ON d.diagnosis_coded = cn.concept_id\n" +
                "                  AND cn.locale = 'en'\n" +
                "                  AND cn.concept_name_type = 'FULLY_SPECIFIED'\n" +
                "WHERE e.encounter_datetime >= '"+effectiveDate+"'\n" +
                "GROUP BY cn.name;";

        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            String diagnosisName = resultSet.getString("diagnosis_name");
                            int total = resultSet.getInt("total");
                            diagnosisMap.put(diagnosisName, total);
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new IllegalArgumentException("Unable to execute diagnosis summary query", e);
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
		final String sqlSelectQuery = "WITH bill_line_items AS (\n" +
				"    SELECT\n" +
				"        bli.bill_id,\n" +
				"        bli.service_id,\n" +
				"        cn_fs.name AS department,\n" +
				"        bli.price,\n" +
				"        bli.line_item_order,\n" +
				"        bli.date_created\n" +
				"    FROM cashier_bill_line_item bli\n" +
				"             INNER JOIN cashier_billable_service cbs\n" +
				"                        ON bli.service_id = cbs.service_id\n" +
				"             LEFT JOIN concept c\n" +
				"                       ON c.concept_id = cbs.service_type\n" +
				"                           AND c.retired = 0\n" +
				"             LEFT JOIN concept_name cn_fs\n" +
				"                       ON cn_fs.concept_id = cbs.service_type\n" +
				"                           AND cn_fs.voided = 0\n" +
				"                           AND cn_fs.locale = 'en'\n" +
				"                           AND cn_fs.concept_name_type = 'FULLY_SPECIFIED'\n" +
				"    WHERE bli.date_created >= '"+effectiveDate+"'\n" +
				"      AND bli.payment_status IN ('PAID', 'PENDING', 'CREDITED')\n" +
				"      AND bli.voided = 0\n" +
				"),\n" +
				"     bill_payment_totals AS (\n" +
				"         SELECT\n" +
				"             bill_id,\n" +
				"             SUM(amount_tendered) AS total_tendered\n" +
				"         FROM cashier_bill_payment\n" +
				"         WHERE date_created >= '"+effectiveDate+"'\n" +
				"           AND voided = 0\n" +
				"         GROUP BY bill_id\n" +
				"     ),\n" +
				"     charge_lines_ranked AS (\n" +
				"         SELECT\n" +
				"             bli.bill_id,\n" +
				"             bli.service_id,\n" +
				"             bli.department,\n" +
				"             bli.price AS line_item_amount,\n" +
				"             bli.line_item_order,\n" +
				"             COALESCE(bpt.total_tendered, 0) AS total_tendered,\n" +
				"\n" +
				"             -- Sum of previous line item amounts in order\n" +
				"             COALESCE(\n" +
				"                     SUM(bli.price) OVER (\n" +
				"                         PARTITION BY bli.bill_id\n" +
				"                         ORDER BY bli.line_item_order, bli.service_id\n" +
				"                         ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING\n" +
				"                         ),\n" +
				"                     0\n" +
				"             ) AS running_before\n" +
				"         FROM bill_line_items bli\n" +
				"                  LEFT JOIN bill_payment_totals bpt\n" +
				"                            ON bpt.bill_id = bli.bill_id\n" +
				"         WHERE bli.price > 0\n" +
				"     ),\n" +
				"     per_charge_line_item AS (\n" +
				"         SELECT\n" +
				"             bill_id,\n" +
				"             service_id,\n" +
				"             department,\n" +
				"             line_item_amount,\n" +
				"             -- Waterfall allocation by line_item_order:\n" +
				"             -- pay this line with whatever is left after paying earlier lines\n" +
				"             GREATEST(\n" +
				"                     LEAST(total_tendered - running_before, line_item_amount),\n" +
				"                     0\n" +
				"             ) AS amount_paid,\n" +
				"\n" +
				"             line_item_amount - GREATEST(\n" +
				"                     LEAST(total_tendered - running_before, line_item_amount),\n" +
				"                     0\n" +
				"                                ) AS remaining_balance\n" +
				"         FROM charge_lines_ranked\n" +
				"     ),\n" +
				"     refund_totals AS (\n" +
				"         SELECT\n" +
				"             service_id,\n" +
				"             department,\n" +
				"             COUNT(DISTINCT bill_id) AS refund_invoice_count,\n" +
				"             SUM(-price) AS total_refunds\n" +
				"         FROM bill_line_items\n" +
				"         WHERE price < 0\n" +
				"         GROUP BY service_id, department\n" +
				"     ),\n" +
				"     charge_totals AS (\n" +
				"         SELECT\n" +
				"             service_id,\n" +
				"             department AS service_type,\n" +
				"             COUNT(DISTINCT bill_id) AS invoice_count,\n" +
				"             SUM(line_item_amount) AS bill_amount,\n" +
				"             SUM(amount_paid) AS amount_paid,\n" +
				"             SUM(remaining_balance) AS balance_due\n" +
				"         FROM per_charge_line_item\n" +
				"         GROUP BY service_id, department\n" +
				"     )\n" +
				"SELECT\n" +
				"    COALESCE(c.service_id, r.service_id) AS service_id,\n" +
				"    COALESCE(c.service_type, r.department) AS service_type,\n" +
				"    COALESCE(c.invoice_count, 0) AS invoices,\n" +
				"    COALESCE(c.bill_amount, 0) AS amount_due,\n" +
				"    COALESCE(c.amount_paid, 0) AS amount_paid,\n" +
				"    COALESCE(c.balance_due, 0) AS balance_due,\n" +
				"    COALESCE(r.total_refunds, 0) AS total_refunds\n" +
				"FROM charge_totals c\n" +
				"         LEFT JOIN refund_totals r\n" +
				"                   ON r.service_id = c.service_id\n" +
				"                       AND r.department = c.service_type\n" +
				"UNION ALL\n" +
				"SELECT\n" +
				"    r.service_id,\n" +
				"    r.department as service_type,\n" +
				"    0 AS invoices,\n" +
				"    0 AS amount_due,\n" +
				"    0 AS amount_paid,\n" +
				"    0 AS balance_due,\n" +
				"    r.total_refunds\n" +
				"FROM refund_totals r\n" +
				"         LEFT JOIN charge_totals c\n" +
				"                   ON c.service_id = r.service_id\n" +
				"                       AND c.service_type = r.department\n" +
				"WHERE c.service_id IS NULL\n" +
				"ORDER BY service_type, service_id;\n";
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
									"service_type", row[1] != null ? row[1].toString() : "",
									"invoices", row[2] != null ? row[2].toString() : "",
									"amount_due", nullIfBlank(row[3]),
									"amount_paid", nullIfBlank(row[4]),
									"balance_due", nullIfBlank(row[5]),
									"total_refunds", nullIfBlank(row[6])
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
        final String sqlSelectQuery = "WITH bill_totals AS (\n" +
				"    -- Calculate the total value of all charges (positive prices) per bill\n" +
				"    SELECT bill_id, SUM(price) AS total_bill_value\n" +
				"    FROM cashier_bill_line_item\n" +
				"    WHERE price > 0 AND voided = 0\n" +
				"    GROUP BY bill_id\n" +
				"),\n" +
				"     refund_per_bill AS (\n" +
				"         -- Sum of negative line items (refunds) per bill\n" +
				"         SELECT bill_id, SUM(-price) AS bill_refunds\n" +
				"         FROM cashier_bill_line_item\n" +
				"         WHERE price < 0 AND voided = 0\n" +
				"         GROUP BY bill_id\n" +
				"     ),\n" +
				"     payment_ratios AS (\n" +
				"         SELECT\n" +
				"             p.bill_id,\n" +
				"             pm.name AS payment_mode,\n" +
				"             b.patient_id,\n" +
				"             p.amount_tendered,\n" +
				"             -- Subtract share of refund: (Amount Tendered - (Total Refund * (This Payment / Total Bill Charges)))\n" +
				"             ROUND((p.amount_tendered - (COALESCE(r.bill_refunds, 0) * (p.amount_tendered / NULLIF(bt.total_bill_value, 0)))),2) AS net_paid\n" +
				"         FROM cashier_bill_payment p\n" +
				"                  INNER JOIN cashier_bill b ON p.bill_id = b.bill_id\n" +
				"                  INNER JOIN cashier_payment_mode pm ON p.payment_mode_id = pm.payment_mode_id\n" +
				"                  INNER JOIN bill_totals bt ON p.bill_id = bt.bill_id\n" +
				"                  LEFT JOIN refund_per_bill r ON p.bill_id = r.bill_id\n" +
				"         WHERE p.date_created >= '"+effectiveDate+"'\n" +
				"           AND p.voided = 0 AND b.voided = 0\n" +
				"     )\n" +
				"SELECT\n" +
				"    payment_mode,\n" +
				"    COUNT(DISTINCT patient_id) AS no_of_patients,\n" +
				"    SUM(net_paid) AS amount_paid\n" +
				"FROM payment_ratios\n" +
				"GROUP BY payment_mode\n" +
				"ORDER BY amount_paid DESC;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();

                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            ret.add(SimpleObject.create(
                                    "payment_mode", row[0] != null ? row[0].toString() : "",
                                    "no_of_patients", nullIfBlank(row[1]),
                                    "amount_paid", nullIfBlank(row[2])
                            ));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
			log.error("KenyaEMR IL: Visualization: getPayments failed: " + e.getMessage(), e);
			throw new IllegalArgumentException("Unable to execute query: " + e.getMessage(), e);
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
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
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
                }
            });
            // Only commit if we started the transaction here
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            // Rollback if we started transaction and something went wrong
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
	}

	public static Map<String, Integer> workLoad(Date midNightDateTime){
		Map<String, Integer> workLoadMap = new HashMap<>();
		List<Visit> visits = Context.getVisitService().getVisits(null, null, null, null, midNightDateTime, null, null, null,null, true, false);
			for(Visit visit : visits){
				workLoadMap.put("Registration",workLoadMap.getOrDefault("Registration", 0) + 1);
					for (Encounter e : visit.getEncounters()) {
						workLoadMap.put(e.getEncounterType().getName(), workLoadMap.getOrDefault(e.getEncounterType().getName(), 0) + 1);
				}
			}
		return workLoadMap;
	}

	/**
	 * Gets details of  wait_time
	 * @param
	 * @return details of  wait_time
     *
	 */
	public static List<SimpleObject> getWaitTime(Date fetchDate) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(fetchDate);
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
        final String sqlSelectQuery =
                "SELECT tbl.name,ROUND(SUM(tbl.diff), 2), count(tbl.patient_id) as patient_count FROM " +
                        "(select q.name,qe.started_at,qe.ended_at, TIMESTAMPDIFF(SECOND, qe.started_at, qe.ended_at) / 60 as diff, qe.patient_id " +
                        " from openmrs.queue_entry qe " +
                        " inner join openmrs.queue q on q.queue_id = qe.queue_id " +
                        " where (qe.date_created >=  '" + effectiveDate + "' or qe.date_changed >= '" + effectiveDate + "' ) and qe.ended_at is not null) tbl GROUP BY tbl.name;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            ret.add(SimpleObject.create(
                                    "queue", row[0] != null ? row[0].toString() : "",
                                    "total_wait_time", row[1] != null ? row[1].toString() : "",
                                    "patient_count", row[2] != null ? row[2].toString() : ""
                            ));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
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
        final String sqlSelectQuery = "SELECT ur.role as role, COUNT(DISTINCT ur.user_id) AS role_count\n" +
				"                FROM user_role ur\n" +
				"                JOIN users u ON ur.user_id = u.user_id\n" +
				"                WHERE (role LIKE '%Clinician'\n" +
				"                    OR role LIKE '%Clerk'\n" +
				"                    OR role LIKE '%Lab Technician%'\n" +
				"                    OR role LIKE '%Doctor%'\n" +
				"                    OR role LIKE '%Health Record Officer'\n" +
				"                    OR role LIKE '%Finance Administrator%'\n" +
				"                    OR role LIKE '%Inventory%'\n" +
				"                    OR role LIKE '%Manager'\n" +
				"                    OR role LIKE '%Nutritionist'\n" +
				"                    OR role LIKE '%Orthopedics'\n" +
				"                    OR role LIKE '%Physiotherapist'\n" +
				"                    OR role LIKE '%Radiologist'\n" +
				"                    OR role LIKE '%Receptionist%Registration%'\n" +
				"                    OR role LIKE '%Stock%'\n" +
				"                    OR role LIKE '%HTS%'\n" +
				"                    OR role LIKE '%ICT Officer'\n" +
				"                    OR role LIKE '%Mortician'\n" +
				"                    OR role LIKE '%Pathologist'\n" +
				"                    OR role LIKE '%Social Worker'\n" +
				"                    OR role LIKE '%Public Health Officer/Surveillance Officer'\n" +
				"                    OR role LIKE '%Insurance Agent'\n" +
				"                    OR role LIKE '%Counselor%'\n" +
				"                    OR role LIKE '%Therapist%'\n" +
				"                    OR role LIKE '%Peer%'\n" +
				"                    OR role LIKE '%Pharmacist'\n" +
				"                    OR role LIKE '%Provider'\n" +
				"                    OR role LIKE '%Nurse%'\n" +
				"                    OR role LIKE '%Cashier%'\n" +
				"                    OR role LIKE '%Dentist%')\n" +
				"                and u.date_created >= '" + effectiveDate + "'\n" +
				"                GROUP BY ur.role;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
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
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("KenyaEMR IL: Unable to get staff by cadre: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
	}

	/**
	 * Retrieves a list of users who are currently logged in based on their last login timestamp
	 * and filters them by specific roles. It also calculates the total number of active users
	 * with relevant roles.
	 * @param fetchDate The date used as a reference to determine users who have logged in since that time.
	 * @return A list of {@code SimpleObject} instances, where each object contains:
	 *         - "logged_in_users_with_roles": The number of logged-in users who match the specified roles.
	 *         - "total_active_users_with_roles": The total number of active users matching the specified roles.
	 * @throws IllegalArgumentException If any error occurs while executing the query.
	 */
	public static List<SimpleObject> getLoggedInUsers(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
		final String sqlSelectQuery = "SELECT\n" +
				"    (SELECT COUNT(DISTINCT p.user_id)\n" +
				"     FROM user_property p\n" +
				"              JOIN user_role r ON p.user_id = r.user_id\n" +
				"     WHERE p.property = 'lastLoginTimestamp'\n" +
				"       AND p.property_value >= UNIX_TIMESTAMP('"+effectiveDate+"') * 1000\n" +
				"       AND (r.role LIKE '%Clinician'\n" +
				"         OR r.role LIKE '%Clerk'\n" +
				"         OR r.role LIKE '%Lab Technician%'\n" +
				"         OR r.role LIKE '%Doctor%'\n" +
				"         OR r.role LIKE '%Health Record Officer'\n" +
				"         OR r.role LIKE '%Finance Administrator%'\n" +
				"         OR r.role LIKE '%Inventory%'\n" +
				"         OR r.role LIKE '%Manager'\n" +
				"         OR r.role LIKE '%Nutritionist'\n" +
				"         OR r.role LIKE '%Orthopedics'\n" +
				"         OR r.role LIKE '%Physiotherapist'\n" +
				"         OR r.role LIKE '%Radiologist'\n" +
				"         OR r.role LIKE '%Receptionist%Registration%'\n" +
				"         OR r.role LIKE '%Stock%'\n" +
				"         OR r.role LIKE '%HTS%'\n" +
				"         OR r.role LIKE '%ICT Officer'\n" +
				"         OR r.role LIKE '%Mortician'\n" +
				"         OR r.role LIKE '%Pathologist'\n" +
				"         OR r.role LIKE '%Social Worker'\n" +
				"         OR r.role LIKE '%Public Health Officer/Surveillance Officer'\n" +
				"         OR r.role LIKE '%Insurance Agent'\n" +
				"         OR r.role LIKE '%Counselor%'\n" +
				"         OR r.role LIKE '%Therapist%'\n" +
				"         OR r.role LIKE '%Peer%'\n" +
				"         OR r.role LIKE '%Pharmacist'\n" +
				"         OR r.role LIKE '%Provider'\n" +
				"         OR r.role LIKE '%Nurse%'\n" +
				"         OR r.role LIKE '%Cashier%'\n" +
				"         OR r.role LIKE '%Dentist%')\n" +
				"    ) AS logged_in_users_with_roles,\n" +
				"    (SELECT COUNT(DISTINCT u.user_id)\n" +
				"     FROM users u\n" +
				"              JOIN user_role r ON u.user_id = r.user_id\n" +
				"     WHERE u.retired = 0\n" +
				"       AND (r.role LIKE '%Clinician'\n" +
				"         OR r.role LIKE '%Clerk'\n" +
				"         OR r.role LIKE '%Lab Technician%'\n" +
				"         OR r.role LIKE '%Doctor%'\n" +
				"         OR r.role LIKE '%Health Record Officer'\n" +
				"         OR r.role LIKE '%Finance Administrator%'\n" +
				"         OR r.role LIKE '%Inventory%'\n" +
				"         OR r.role LIKE '%Manager'\n" +
				"         OR r.role LIKE '%Nutritionist'\n" +
				"         OR r.role LIKE '%Orthopedics'\n" +
				"         OR r.role LIKE '%Physiotherapist'\n" +
				"         OR r.role LIKE '%Radiologist'\n" +
				"         OR r.role LIKE '%Receptionist%Registration%'\n" +
				"         OR r.role LIKE '%Stock%'\n" +
				"         OR r.role LIKE '%HTS%'\n" +
				"         OR r.role LIKE '%ICT Officer'\n" +
				"         OR r.role LIKE '%Mortician'\n" +
				"         OR r.role LIKE '%Pathologist'\n" +
				"         OR r.role LIKE '%Social Worker'\n" +
				"         OR r.role LIKE '%Public Health Officer/Surveillance Officer'\n" +
				"         OR r.role LIKE '%Insurance Agent'\n" +
				"         OR r.role LIKE '%Counselor%'\n" +
				"         OR r.role LIKE '%Therapist%'\n" +
				"         OR r.role LIKE '%Peer%'\n" +
				"         OR r.role LIKE '%Pharmacist'\n" +
				"         OR r.role LIKE '%Provider'\n" +
				"         OR r.role LIKE '%Nurse%'\n" +
				"         OR r.role LIKE '%Cashier%'\n" +
				"         OR r.role LIKE '%Dentist%')\n" +
				"    ) AS total_active_users_with_roles;";
		final List<SimpleObject> ret = new ArrayList<SimpleObject>();
		Transaction tx = null;
		try {
			Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
			boolean startedTx = false;
			if (!hibSession.getTransaction().isActive()) {
				tx = hibSession.beginTransaction();
				startedTx = true;
			}
			sf.getCurrentSession().doWork(connection -> {
				try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
					try (ResultSet resultSet = statement.executeQuery()) {
						ResultSetMetaData metaData = resultSet.getMetaData();
						while (resultSet.next()) {
							Object[] row = new Object[metaData.getColumnCount()];
							for (int i = 1; i <= metaData.getColumnCount(); i++) {
								row[i - 1] = resultSet.getObject(i);
							}
							ret.add(SimpleObject.create(
									"logged_in_users_with_roles", row[0] != null ? row[0].toString() : "",
									"total_active_users_with_roles", row[1] != null ? row[1].toString() : ""
							));
						}
					}
				}
			});
			if (startedTx && tx != null && tx.isActive()) {
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			log.error("KenyaEMR IL: Unable to get staff by cadre: " + e.getMessage());
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
        final String sqlSelectQuery = "SELECT sum(amount_tendered) as total FROM openmrs.cashier_bill_payment where payment_mode_id = 17 and date_created >= '" + effectiveDate + "'";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
							String waivers = row[0] == null ? null : row[0].toString().trim();
                            ret.add(SimpleObject.create(
                                    "waivers", (waivers == null || waivers.isEmpty()) ? null : waivers));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
	}
	/**
	 * Gets KenyaEMR Version running
	 * @param
	 * @return KenyaEMR Version
	 */
	public static String getKenyaEMRModuleVersion() {
		// Retrieve the module by its ID
		Module module = ModuleFactory.getModuleById("kenyaemr");
		if (module != null) {
			// Return the version of the module
			return module.getVersion();
		}

		return "Unknown version";
	}

	/**
	 * @param fetchDate
	 * @return
	 */
    public static List<SimpleObject> getInpatientsByWard(Date fetchDate) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(fetchDate);
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
        final String sqlSelectQuery = "select b.name as ward, count(bm.patient_id) as no_of_patients\n" +
                "from bed_patient_assignment_map bm\n" +
                "         inner join encounter e on bm.encounter_id = e.encounter_id\n" +
                "         inner join (select bl.location_id, l.name, bl.bed_id\n" +
                "                     from bed_location_map bl\n" +
                "                              inner join location l on l.location_id = bl.location_id) b on bm.bed_id = b.bed_id\n" +
                "         inner join (select b.status, bt.bed_type_id, b.bed_id\n" +
                "                     from bed b\n" +
                "                              inner join bed_type bt on b.bed_type_id = bt.bed_type_id) t on bm.bed_id = t.bed_id\n" +
                "where date_started >= '" + effectiveDate + "'\n" +
                "  and status = 'OCCUPIED'\n" +
                "group by ward;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            ret.add(SimpleObject.create(
                                    "ward", row[0] != null ? row[0].toString() : "",
                                    "no_of_patients", row[1] != null ? row[1].toString() : ""
                            ));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
    }

	/**
	 * @param fetchDate
     *
	 * @return
	 */
    public static List<SimpleObject> getInpatientsByAge(Date fetchDate) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(fetchDate);
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
        final String sqlSelectQuery = "select if(timestampdiff(YEAR, date(p.birthdate), date(current_date)) < 5, 'Child', 'Adult') as age,\n" +
                "       count(bm.patient_id)                                                                 as no_of_patients\n" +
                "from bed_patient_assignment_map bm\n" +
                "         inner join encounter e on bm.encounter_id = e.encounter_id\n" +
                "         inner join (select bl.location_id, l.name, bl.bed_id\n" +
                "                     from bed_location_map bl\n" +
                "                              inner join location l on l.location_id = bl.location_id) b on bm.bed_id = b.bed_id\n" +
                "         inner join (select b.status, bt.bed_type_id, b.bed_id\n" +
                "                     from bed b\n" +
                "                              inner join bed_type bt on b.bed_type_id = bt.bed_type_id) t on bm.bed_id = t.bed_id\n" +
                "         inner join person p on bm.patient_id = p.person_id\n" +
                "where date_started >= '" + effectiveDate + "'\n" +
                "  and status = 'OCCUPIED'\n" +
                "group by age;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            ret.add(SimpleObject.create(
                                    "age", row[0] != null ? row[0].toString() : "",
                                    "no_of_patients", row[1] != null ? row[1].toString() : ""
                            ));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
    }

	/**
	 * @param fetchDate
	 * @return
	 */
    public static List<SimpleObject> getPaymentByDepartment(Date fetchDate) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String effectiveDate = sd.format(fetchDate);
        DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);
        final String sqlSelectQuery = "WITH bill_line_items AS (\n" +
				"    SELECT\n" +
				"        bli.bill_id,\n" +
				"        bli.service_id,\n" +
				"        COALESCE(cn_fs.name, 'Unassigned') AS department,\n" +
				"        bli.price,\n" +
				"        bli.line_item_order\n" +
				"    FROM cashier_bill_line_item bli\n" +
				"             INNER JOIN cashier_billable_service cbs ON bli.service_id = cbs.service_id\n" +
				"             LEFT JOIN concept_name cn_fs ON cn_fs.concept_id = cbs.service_type\n" +
				"        AND cn_fs.voided = 0 AND cn_fs.locale = 'en' AND cn_fs.concept_name_type = 'FULLY_SPECIFIED'\n" +
				"    WHERE bli.date_created >= '"+effectiveDate+"'\n" +
				"      AND bli.payment_status IN ('PAID', 'PENDING', 'CREDITED')\n" +
				"      AND bli.voided = 0\n" +
				"),\n" +
				"     bill_payment_totals AS (\n" +
				"         -- Only include bills that have at least one payment entry\n" +
				"         SELECT bill_id, SUM(amount_tendered) AS total_tendered\n" +
				"         FROM cashier_bill_payment\n" +
				"         WHERE date_created >= '"+effectiveDate+"' AND voided = 0\n" +
				"         GROUP BY bill_id\n" +
				"         HAVING SUM(amount_tendered) > 0\n" +
				"     ),\n" +
				"     department_net AS (\n" +
				"         -- INNER JOIN ensures we only attribute charges to bills that were actually paid\n" +
				"         SELECT\n" +
				"             department,\n" +
				"             GREATEST(LEAST(bpt.total_tendered -\n" +
				"                            COALESCE(SUM(bli.price) OVER (PARTITION BY bli.bill_id ORDER BY bli.line_item_order, bli.service_id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0),\n" +
				"                            bli.price), 0) AS amount\n" +
				"         FROM bill_line_items bli\n" +
				"                  INNER JOIN bill_payment_totals bpt ON bpt.bill_id = bli.bill_id\n" +
				"         WHERE bli.price > 0\n" +
				"         UNION ALL\n" +
				"         -- Subtract refunds\n" +
				"         SELECT department, price AS amount\n" +
				"         FROM bill_line_items\n" +
				"         WHERE price < 0\n" +
				"     )\n" +
				"SELECT\n" +
				"    department,\n" +
				"    SUM(amount) AS amount_paid\n" +
				"FROM department_net\n" +
				"GROUP BY department\n" +
				"HAVING SUM(amount) <> 0 -- Only return rows where money was actually paid/not refunded\n" +
				"ORDER BY department;";
        final List<SimpleObject> ret = new ArrayList<SimpleObject>();
        Transaction tx = null;
        try {
            Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
            boolean startedTx = false;
            if (!hibSession.getTransaction().isActive()) {
                tx = hibSession.beginTransaction();
                startedTx = true;
            }
            sf.getCurrentSession().doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        while (resultSet.next()) {
                            Object[] row = new Object[metaData.getColumnCount()];
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            ret.add(SimpleObject.create(
                                    "department", row[0] != null ? row[0].toString() : "",
                                    "amount_paid", nullIfBlank(row[1])
                            ));
                        }
                    }
                }
            });
            if (startedTx && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("KenyaEMR IL: Unable to get payment by department: " + e.getMessage(), e);
            throw new IllegalArgumentException("Unable to execute query", e);
        }
        return ret;
    }

	/**
	 * Gets details of total patients registered in SHA - Social Health Agency
	 *
	 * @param
	 * @return details of total patients with SHA Number
	 */
	public static Long getTotalPatientsOnSHA(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String effectiveDate = sd.format(fetchDate);
		Long ret = null;
		String hivTestedPositiveQuery = "Select count(pi.patient_id) from patient_identifier pi inner join patient_identifier_type pt on pi.identifier_type = pt.patient_identifier_type_id and pt.uuid = '24aedd37-b5be-4e08-8311-3721b8d5100d' and pi.date_created >= '" + effectiveDate + "'";
		try {
			Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
			ret = (Long) Context.getAdministrationService().executeSQL(hivTestedPositiveQuery, true).get(0).get(0);
		} finally {
			Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		}
		return ret;
	}

	/**
	 * Retrieves bed management data
	 * This method executes a query to gather details about bed allocation, occupancy,
	 * and availability across different wards and bed types
	 */
	public static List<SimpleObject> getBedManagement(Date fetchDate) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DbSessionFactory sf = Context.getRegisteredComponents(DbSessionFactory.class).get(0);

		final String sqlSelectQuery = "WITH bed_state AS (\n" +
				"    SELECT\n" +
				"        blm.location_id,\n" +
				"        btag.name      AS bed_tag,\n" +
				"        bt.name        AS bed_type,\n" +
				"        COUNT(b.bed_id) AS actual_beds,\n" +
				"        SUM(CASE WHEN bpam.bed_id IS NOT NULL THEN 1 ELSE 0 END) AS occupied_count\n" +
				"    FROM bed b\n" +
				"             JOIN bed_location_map blm\n" +
				"                  ON blm.bed_id = b.bed_id\n" +
				"             JOIN bed_tag_map btm\n" +
				"                  ON btm.bed_id = b.bed_id\n" +
				"                      AND btm.voided = 0\n" +
				"             JOIN bed_tag btag\n" +
				"                  ON btag.bed_tag_id = btm.bed_tag_id\n" +
				"                      AND btag.voided = 0\n" +
				"             LEFT JOIN bed_type bt\n" +
				"                       ON bt.bed_type_id = b.bed_type_id\n" +
				"                           AND bt.retired = 0\n" +
				"             LEFT JOIN bed_patient_assignment_map bpam\n" +
				"                       ON bpam.bed_id = b.bed_id\n" +
				"                           AND bpam.date_stopped IS NULL\n" +
				"                           AND bpam.voided = 0\n" +
				"    WHERE b.voided = 0\n" +
				"    GROUP BY blm.location_id, btag.name,bt.name\n" +
				")\n" +
				"SELECT\n" +
				"    l.name AS ward,\n" +
				"    REPLACE(REPLACE(lat.name, 'Ward Authorized ', ''), 's', '') AS bed_tag,\n" +
				"    bs.bed_type,\n" +
				"    CAST(la.value_reference AS UNSIGNED) AS authorized_capacity,\n" +
				"    COALESCE(bs.actual_beds, 0) AS actual_beds,\n" +
				"    COALESCE(bs.occupied_count, 0) AS occupied_beds,\n" +
				"    -- Available = Physical Beds - Occupied Beds\n" +
				"    COALESCE(bs.actual_beds, 0) - COALESCE(bs.occupied_count, 0) AS available_beds\n" +
				"FROM location l\n" +
				"         JOIN location_tag_map ltm ON l.location_id = ltm.location_id\n" +
				"         JOIN location_tag lt ON ltm.location_tag_id = lt.location_tag_id\n" +
				"    AND lt.name = 'Admission Location'\n" +
				"         JOIN location_attribute la ON la.location_id = l.location_id AND la.voided = 0\n" +
				"         JOIN location_attribute_type lat ON lat.location_attribute_type_id = la.attribute_type_id\n" +
				"    AND lat.retired = 0\n" +
				"    AND lat.name REGEXP '^Ward Authorized (Beds|Cots|Incubators)'\n" +
				"         LEFT JOIN bed_state bs ON bs.location_id = l.location_id\n" +
				"    AND lat.name = CONCAT('Ward Authorized ', bs.bed_tag, 's')\n" +
				"WHERE l.retired = 0\n" +
				"ORDER BY l.name, bed_tag,bed_type;";
		final List<SimpleObject> ret = new ArrayList<SimpleObject>();
		Transaction tx = null;
		try {
			Session hibSession = sf.getHibernateSessionFactory().getCurrentSession();
			boolean startedTx = false;
			if (!hibSession.getTransaction().isActive()) {
				tx = hibSession.beginTransaction();
				startedTx = true;
			}
			sf.getCurrentSession().doWork(connection -> {
				try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
					try (ResultSet resultSet = statement.executeQuery()) {
						ResultSetMetaData metaData = resultSet.getMetaData();
						while (resultSet.next()) {
							Object[] row = new Object[metaData.getColumnCount()];
							for (int i = 1; i <= metaData.getColumnCount(); i++) {
								row[i - 1] = resultSet.getObject(i);
							}
							ret.add(SimpleObject.create(
									"ward", row[0] != null ? row[0].toString() : "",
									"bed_tag", row[1] != null ? row[1].toString() : "",
									"bed_type", row[2] != null ? row[2].toString() : "",
									"authorized_capacity", row[3] != null ? Integer.parseInt(row[3].toString()) : 0,
									"actual_beds", row[4] != null ? Integer.parseInt(row[4].toString()) : 0,
									"occupied_beds", row[5] != null ? Integer.parseInt(row[5].toString()) : 0,
									"available_beds", row[6] != null ? Integer.parseInt(row[6].toString()) : 0
							));
						}
					}
				}
			});
			if (startedTx && tx != null && tx.isActive()) {

				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			log.error("KenyaEMR IL: Unable to get bed data: " + e.getMessage(), e);
			throw new IllegalArgumentException("Unable to execute query", e);
		}
		return ret;
	}
}


