package org.openmrs.module.kenyaemrIL.kenyaemrUtils;

import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.util.PrivilegeConstants;

import java.util.*;

public class Utils {

    public static List<Obs> getNLastObs(Concept concept, Patient patient, Integer nLast) throws Exception {
        List<Obs> obs = Context.getObsService().getObservations(
                Arrays.asList(Context.getPersonService().getPerson(patient.getPersonId())),
                null,
                Arrays.asList(concept),
                null,
                null,
                null,
                null,
                nLast,
                null,
                null,
                null,
                false);
        return obs;
    }

    public static Obs getLatestObs(Patient patient, String conceptIdentifier) {
        Concept concept = Context.getConceptService().getConceptByUuid(conceptIdentifier);
        List<Obs> obs = Context.getObsService().getObservationsByPersonAndConcept(patient, concept);
        if (obs.size() > 0) {
            // these are in reverse chronological order
            return obs.get(0);
        }
        return null;
    }

    /**
     * Finds the last encounter during the program enrollment with the given encounter type
     *
     * @param type the encounter type
     *
     * @return the encounter
     */
    public static Encounter lastEncounter(Patient patient, EncounterType type) {
        List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null, Collections.singleton(type), null, null, null, false);
        return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
    }

    /**
     * getEncounters(Patient who, Location loc, Date fromDate, Date toDate,
     Collection<Form> enteredViaForms, Collection<EncounterType> encounterTypes, Collection<Provider> providers,
     Collection<VisitType> visitTypes, Collection<Visit> visits, boolean includeVoided);
     * @return
     */


    public static List<Encounter> getEncounters (Patient patient, List<Form> forms) {

        return Context.getEncounterService().getEncounters(patient, null, null, null, forms, null, null, null, null, false);

    }

    public static List<Obs> getEncounterObservationsForQuestions(Person patient, Encounter encounter, List<Concept> questions) {
        /**
         * getObservations(List<Person> whom, List<Encounter> encounters, List<Concept> questions,
         List<Concept> answers, List<PERSON_TYPE> personTypes, List<Location> locations, List<String> sort,
         Integer mostRecentN, Integer obsGroupId, Date fromDate, Date toDate, boolean includeVoidedObs)
         */
        return Context.getObsService().getObservations(Arrays.asList(patient), Arrays.asList(encounter), questions, null, null, null, null, null, null, null, null, false);
    }

    public static Location getDefaultLocation() {
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
            GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
            return gp != null ? ((Location) gp.getValue()) : null;
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

    }

    public static String getDefaultLocationMflCode(Location location) {
        String MASTER_FACILITY_CODE = "8a845a89-6aa5-4111-81d3-0af31c45c002";

        if(location == null) {
            location = getDefaultLocation();
        }
        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            for (LocationAttribute attr : location.getAttributes()) {
                if (attr.getAttributeType().getUuid().equals(MASTER_FACILITY_CODE) && !attr.isVoided()) {
                    return attr.getValueReference();
                }
            }
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }
        return null;
    }


    public static Location getLocationFromMFLCode(String mflCode) {

        String MASTER_FACILITY_CODE = "8a845a89-6aa5-4111-81d3-0af31c45c002";

        try {
            Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
            LocationAttributeType facilityMflCode = Context.getLocationService().getLocationAttributeTypeByUuid(MASTER_FACILITY_CODE);
            Map<LocationAttributeType, Object> mflCodeMap = new HashMap<LocationAttributeType, Object>();
            mflCodeMap.put(facilityMflCode, mflCode);

            List<Location> locationForMfl = Context.getLocationService().getLocations(null, null, mflCodeMap, false, null,null);

            return locationForMfl.size() > 0 ? locationForMfl.get(0) : getDefaultLocation();
        }
        finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }
    }


}
