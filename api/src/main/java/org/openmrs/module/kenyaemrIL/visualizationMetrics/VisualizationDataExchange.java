package org.openmrs.module.kenyaemrIL.visualizationMetrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PersonService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.metadata.OTZMetadata;
import org.openmrs.module.kenyaemrIL.il.utils.MessageHeaderSingleton;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.util.PrivilegeConstants;

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

public class VisualizationDataExchange {

	private static final Log log = LogFactory.getLog(VisualizationDataExchange.class);
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static List<Diagnosis> allDiagnosis;
	private static EncounterService service= Context.getEncounterService();
	final static String FAMILY_PLANNING_FORM_UUID = "a52c57d4-110f-4879-82ae-907b0d90add6";
	public static final String IMMUNIZATION = "29c02aff-9a93-46c9-bf6f-48b552fcb1fa";
	public static Concept immunizationConcept = Dictionary.getConcept(Dictionary.IMMUNIZATIONS);
	/**
	 * Generates the payload used to post to visualization server*
	 *
	 * @param
	 * @return
	 */
	public static JSONObject generateVisualizationPayload(Date fetchDate) {
        System.out.println("KenyaEMR IL: Visualization: generateVisualizationPayload started for fetchDate="+fetchDate);
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
        Map<String, Integer> casualtyVisitsByAgeMap;
        Map<String, Integer> mortuaryVisitsByAgeMap;
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
		List<SimpleObject> ipdPatientsByWard = new ArrayList<SimpleObject>();
		List<SimpleObject> ipdWardPatients = new ArrayList<SimpleObject>();
		List<SimpleObject> ipdAgePatients = new ArrayList<SimpleObject>();
		List<SimpleObject> staffCount = new ArrayList<SimpleObject>();
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

        log.info("Visualization: Payload assembly started for mfl_code=" + facilityMfl + ", timestamp=" + timestamp);

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
            log.error("Visualization: Error building bed_management: "+ ex.getMessage(), ex);
		}
		try {
			// Fetch visit data

            VisitService visitService = Context.getVisitService();
            Collection<VisitType> visitTypes = Arrays.asList(
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.INPATIENT),
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.OUTPATIENT)/*,
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.CASUALTY),
                MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.MORTUARY)*/
            );
            List<Visit> allVisits = visitService.getVisits(visitTypes, null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> outPatientVisits = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.OUTPATIENT)), null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> inPatientVisits = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.INPATIENT)), null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> casualtyPatientVisits = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.CASUALTY)), null, null, null, fetchDate, null, null, null, null, true, false);
            List<Visit> mortuaryAdmissions = visitService.getVisits(Collections.singleton(MetadataUtils.existing(VisitType.class, CommonMetadata._VisitType.MORTUARY)), null, null, null, fetchDate, null, null, null, null, true, false);

            visitsMap = allVisits(allVisits);
            outpatientByServiceMap = outPatientVisitsByService(outPatientVisits);
            inpatientVisitsByAgeMap = inPatientVisitsByAge(inPatientVisits);
            visitByAgeMap = outPatientVisitsByAge(outPatientVisits);
            casualtyVisitsByAgeMap = casualtyVisitsByAge(casualtyPatientVisits);
            mortuaryVisitsByAgeMap = mortuaryVisitsByAge(mortuaryAdmissions);

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
                } /*else if ("Inpatient".equals(visitType) && !inpatientVisitsByAgeMap.isEmpty()) {
                    visitsObject.put("age_details", mapToBreakdownList(inpatientVisitsByAgeMap, "age", "total"));
                } else if ("Casualty/Emergency".equals(visitType) && !casualtyVisitsByAgeMap.isEmpty()) {
                    visitsObject.put("age_details", mapToBreakdownList(casualtyVisitsByAgeMap, "age", "total"));
                } else if ("Mortuary".equals(visitType) && !mortuaryVisitsByAgeMap.isEmpty()) {
                    visitsObject.put("age_details", mapToBreakdownList(mortuaryVisitsByAgeMap, "age", "total"));
                }*/
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
            log.info("Visualization: Visits data loaded. visitTypes="+visitsMap.keySet()+" outpatientServiceTypes="+ outpatientByServiceMap.keySet());
            System.out.println("Visualization: Visits data loaded. visitTypes="+visitsMap.keySet()+" outpatientServiceTypes="+ outpatientByServiceMap.keySet());

		} catch (Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building visits payload: " + ex.getMessage(), ex);
			System.err.println("KenyaEMR IL: Visualization: Error building visits payload: " + ex.getMessage());
		}

		try {
			immunizationsMap = immunizations(fetchDate);
			if(!immunizationsMap.isEmpty()) {
                for (Map.Entry<String, Integer> immunizationEntry : immunizationsMap.entrySet()) {
                    SimpleObject immunizationsObject = new SimpleObject();
                    immunizationsObject.put("Vaccine", immunizationEntry.getKey());
                    immunizationsObject.put("total", immunizationEntry.getValue().toString());
                    immunizations.add(immunizationsObject);
                }
            }
            System.out.println("KenyaEMR IL: Visualization: Immunizations data loaded. immunizations="+immunizationsMap.keySet());
				payloadObj.put("Immunization", immunizations);

		} catch(Exception ex) {
            log.error("KenyaEMR IL: Visualization: Error building immunization payload: " + ex.getMessage(), ex);
            System.err.println("KenyaEMR IL: Visualization: Error building immunization payload: " + ex.getMessage());
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
            System.out.println("KenyaEMR IL: Visualization: Diagnosis data loaded. diagnosis="+diagnosisMap.keySet());
		} catch(Exception ex) {
            log.error("KenyaEMR IL: Visualization: Error building diagnosis payload: " + ex.getMessage(), ex);
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
            System.out.println("KenyaEMR IL: Visualization: Workload data loaded. workload="+workloadMap.keySet());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building workload payload: " + ex.getMessage(), ex);
			System.err.println("KenyaEMR IL: Visualization: Error building workload payload: " + ex.getMessage());
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
            System.out.println("KenyaEMR IL: Visualization: Billing data loaded. billing="+billingItems.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building billing payload: " + ex.getMessage(), ex);
		}

		try {
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
System.out.println("KenyaEMR IL: Visualization: Payment data loaded. payments="+payments.size());
			// Add all payments to the payload object
			payloadObj.put("payments", payments);

		} catch (Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building payments payload: " + ex.getMessage(), ex);
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
            System.out.println("KenyaEMR IL: Visualization: Inventory data loaded. inventory="+inventoryItems.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building inventory payload: " + ex.getMessage(), ex);
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
			log.error("KenyaEMR IL: Visualization: Error building mortality payload: " + ex.getMessage(), ex);
		}

		try {
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
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  wait time : " + ex.getMessage(), ex);
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
            System.out.println("KenyaEMR IL: Visualization: Staff data loaded. staff="+staff.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  staff : " + ex.getMessage(), ex);
		}

		try {
			waivers = getTotalWaivers(fetchDate);
			if (waivers.size() > 0) {
				SimpleObject waiversList= waivers.get(0);
				payloadObj.put("waivers","");
			} else {
				payloadObj.put("waivers", "");
			}
            System.out.println("KenyaEMR IL: Visualization: Waivers data loaded. waivers="+waivers.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  waivers : " + ex.getMessage(), ex);
		}

		try {
			 shaPatients = getTotalPatientsOnSHA(fetchDate);
			if (shaPatients > 0) {
				payloadObj.put("sha_enrollments", shaPatients);

			} else {
				payloadObj.put("sha_enrollments", "");
			}
            System.out.println("KenyaEMR IL: Visualization: SHA data loaded. shaPatients="+shaPatients);
		} catch (Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  shaPatients : " + ex.getMessage(), ex);
		}

		try {
			ipdAgePatients = getInpatientsByAge(fetchDate);
			if (ipdAgePatients.size() > 0) {
				for (int i = 0; i < ipdAgePatients.size(); i++) {
					SimpleObject patientList= ipdAgePatients.get(i);
					SimpleObject ipdObject = new SimpleObject();
					ipdObject.put("age", patientList.get("age"));
					ipdObject.put("no_of_patients", patientList.get("no_of_patients"));
					ipdAgePatients.add(ipdObject);
					payloadObj.put("admissions", ipdAgePatients);
				}
			} else {
				payloadObj.put("admissions", ipdAgePatients);
			}
            System.out.println("KenyaEMR IL: Visualization: IPD by age data loaded. ipdAgePatients="+ipdAgePatients.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  IPD by ward : " + ex.getMessage(),ex);
		}
		try {
			ipdWardPatients = getInpatientsByWard(fetchDate);
			if (ipdWardPatients.size() > 0) {
				for (int i = 0; i < ipdWardPatients.size(); i++) {
					SimpleObject patientList= ipdWardPatients.get(i);
					SimpleObject ipdObject = new SimpleObject();
					ipdObject.put("ward", patientList.get("ward"));
					ipdObject.put("no_of_patients", patientList.get("no_of_patients"));
					ipdPatientsByWard.add(ipdObject);
					payloadObj.put("admissions", ipdPatientsByWard);
				}
			} else {
				payloadObj.put("admissions", ipdPatientsByWard);
			}
            System.out.println("KenyaEMR IL: Visualization: IPD by ward data loaded. ipdPatientsByWard="+ipdPatientsByWard.size());
		} catch(Exception ex) {
			log.error("KenyaEMR IL: Visualization: Error building  IPD by ward : " + ex.getMessage(), ex);
		}

		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
        System.out.println("KenyaEMR IL: Visualization: Payload data loaded. payloadObj="+payloadObj);
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
            System.out.println("KenyaEMR IL: Visualization: allVisits data loaded. visitMap="+visitMap.size());
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
        System.out.println("KenyaEMR IL: Visualization: outPatientVisitsByAge data loaded. outpatientByAgeMap="+outpatientByAgeMap.size());
        return outpatientByAgeMap;
	}

    public static Map<String, Integer> casualtyVisitsByAge(List<Visit> casualtyVisits) {
        Map<String, Integer> casualtyByAgeMap = new HashMap<>();

        if (casualtyVisits == null || casualtyVisits.isEmpty()) {
            return casualtyByAgeMap;
        }

        final String UNDER_5 = "Casualty/Emergency Under 5";
        final String ABOVE_5 = "Casualty/Emergency 5 And Above";

        for (Visit visit : casualtyVisits) {
            VisitType visitTypeObj = visit.getVisitType();
            if (visitTypeObj == null) continue;

            Patient patient = visit.getPatient();
            if (patient == null) continue;

            int age = patient.getAge();
            String key = (age < 5) ? UNDER_5 : ABOVE_5;
            casualtyByAgeMap.merge(key, 1, Integer::sum);
        }
        System.out.println("KenyaEMR IL: Visualization: casualtyVisitsByAge data loaded. casualtyByAgeMap="+casualtyByAgeMap.size());
        return casualtyByAgeMap;
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

    public static Map<String, Integer> outPatientVisitsByService(List<Visit> outPatientVisits) {

        Map<String, Integer> outpatientByServiceMap = new HashMap<>();

        EncounterSearchCriteriaBuilder searchCriteria = new EncounterSearchCriteriaBuilder().setVisits(outPatientVisits);

        List<Encounter> encounters = service.getEncounters(searchCriteria.createEncounterSearchCriteria());

        if (!encounters.isEmpty()) {
            for (Encounter encounter : encounters) {
                if (encounter.getEncounterType() == null || encounter.getEncounterType().getName() == null) {
                    continue;
                }
                String serviceName = encounter.getEncounterType().getName();
                outpatientByServiceMap.merge(serviceName, 1, Integer::sum);
                log.debug("outpatientByServiceMap: " + outpatientByServiceMap);
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

    public static Map<String, Integer> allDiagnosis(Date fetchDate) {
        Map<String, Integer> diagnosisMap = new HashMap<>();
        EncounterSearchCriteriaBuilder encounterSearchCriteriaBuilder = new EncounterSearchCriteriaBuilder().setFromDate(fetchDate).setIncludeVoided(false);
        List<Encounter> encounters = service.getEncounters(encounterSearchCriteriaBuilder.createEncounterSearchCriteria());
        DiagnosisService diagnosisService = Context.getDiagnosisService();

        encounters.stream()
                .flatMap(encounter -> diagnosisService.getDiagnosesByEncounter(encounter, false, false).stream())
                .filter(diagnosis -> diagnosis.getDiagnosis() != null &&
                        diagnosis.getDiagnosis().getCoded() != null &&
                        diagnosis.getDiagnosis().getCoded().getName() != null)
                .map(diagnosis -> diagnosis.getDiagnosis().getCoded().getName().getName())
                .forEach(name -> diagnosisMap.merge(name, 1, Integer::sum));
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
        System.out.println("KenyaEMR IL: Visualization: getPayments data loaded. ret="+ret.size());
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
        System.out.println("KenyaEMR IL: Visualization: mortality data loaded. mortalityMap="+mortalityMap.size());
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
        System.out.println("KenyaEMR IL: Visualization: getInventory data loaded. ret="+ret.size());
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
            System.out.println("KenyaEMR IL: Visualization: workLoad data loaded. workLoadMap="+workLoadMap.size());
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
		final String sqlSelectQuery = "SELECT tbl.name,ROUND(SUM(tbl.diff), 2), count(tbl.patient_id) as patient_count FROM (select q.name,qe.started_at,qe.ended_at, TIMESTAMPDIFF(SECOND, qe.started_at, qe.ended_at) / 60 as diff, qe.patient_id from openmrs.queue_entry qe\n" +
				"    inner join openmrs.queue q on q.queue_id = qe.queue_id where (qe.date_created >=  '"+effectiveDate+"' or qe.date_changed >= '"+effectiveDate+"' ) and qe.ended_at is not null) tbl GROUP BY tbl.name;";
		final List<SimpleObject> ret = new ArrayList<SimpleObject>();
		Transaction tx = null;
		try {

			tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
			final Transaction finalTx = tx;
			sf.getCurrentSession().doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
					PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    ResultSet resultSet = statement.executeQuery();
					try {

						if (resultSet != null) {
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
						finalTx.commit();
					} finally {
                        try {
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
        System.out.println("KenyaEMR IL: Visualization: getWaitTime data loaded. ret="+ret.size());
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
                    ResultSet resultSet = statement.executeQuery();
					try {
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
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
			log.error("KenyaEMR IL: Unable to get staff by cadre: " + e.getMessage());
			e.printStackTrace();
			throw new IllegalArgumentException("Unable to execute query", e);
		}
        System.out.println("KenyaEMR IL: Visualization: getStaffByCadre data loaded. ret="+ret.size());
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
                    ResultSet resultSet = statement.executeQuery();
					try {
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
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
        System.out.println("KenyaEMR IL: Visualization: getTotalWaivers data loaded. ret="+ret.size());
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

			tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
			final Transaction finalTx = tx;
			sf.getCurrentSession().doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
					PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    ResultSet resultSet = statement.executeQuery();
					try {
						if (resultSet != null) {
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
						finalTx.commit();
					} finally {
                        try {
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
        System.out.println("KenyaEMR IL: Visualization: getInpatientsByWard data loaded. ret="+ret.size());
		return ret;
	}

	/**
	 * @param fetchDate
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
            tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
            sf.getCurrentSession().doWork(connection -> {
                // try-with-resources ensures statement/resultset closing
                try (PreparedStatement statement = connection.prepareStatement(sqlSelectQuery)) {
                    statement.setString(1, effectiveDate);
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
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalArgumentException("Unable to execute query", e);
        }
		/*try {

			tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
			final Transaction finalTx = tx;
			sf.getCurrentSession().doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
					PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    ResultSet resultSet = statement.executeQuery();
					try {
						if (resultSet != null) {
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
						finalTx.commit();
					} finally {
                        try {
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
		}*/
        System.out.println("KenyaEMR IL: Visualization: getInpatientsByAge data loaded. ret="+ret.size());
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
		final String sqlSelectQuery = "select cbs.name as department,\n" +
				"       sum(price) as amount_paid\n" +
				"from cashier_bill_line_item bli\n" +
				"         inner join cashier_billable_service cbs\n" +
				"                    on bli.service_id = cbs.service_id and bli.payment_status = 'PAID'\n" +
				"         inner join cashier_bill_payment cbp on bli.bill_id = cbp.bill_id\n" +
				" where cbp.date_created >= '"+effectiveDate+"' and cbp.voided = 0\n" +
				"group by department;";
		final List<SimpleObject> ret = new ArrayList<SimpleObject>();
		Transaction tx = null;
		try {

			tx = sf.getHibernateSessionFactory().getCurrentSession().beginTransaction();
			final Transaction finalTx = tx;
			sf.getCurrentSession().doWork(new Work() {

				@Override
				public void execute(Connection connection) throws SQLException {
					PreparedStatement statement = connection.prepareStatement(sqlSelectQuery);
                    ResultSet resultSet = statement.executeQuery();
					try {
						if (resultSet != null) {
							ResultSetMetaData metaData = resultSet.getMetaData();

							while (resultSet.next()) {
								Object[] row = new Object[metaData.getColumnCount()];
								for (int i = 1; i <= metaData.getColumnCount(); i++) {
									row[i - 1] = resultSet.getObject(i);
								}

								ret.add(SimpleObject.create(
										"department", row[0] != null ? row[0].toString() : "",
										"amount_paid", row[1] != null ? row[1].toString() : ""
								));
							}
						}
						finalTx.commit();
					} finally {
                        try {
                            if (resultSet != null) resultSet.close();
                        } catch (Exception e) {}
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
			log.error("KenyaEMR IL: Unable to get payment by department: " + e.getMessage(),e);;
			throw new IllegalArgumentException("Unable to execute query", e);
		}
        System.out.println("KenyaEMR IL: Visualization: getPaymentByDepartment data loaded. ret="+ret.size());
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
        System.out.println("KenyaEMR IL: Visualization: getTotalPatientsOnSHA data loaded. ret="+ret);;
		return ret;
	}

}


