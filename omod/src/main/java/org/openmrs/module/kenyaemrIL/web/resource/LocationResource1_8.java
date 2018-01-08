package org.openmrs.module.kenyaemrIL.web.resource;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.web.controller.KenyaEMRILResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.MetadataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * {@link Resource} for {@link Location}, supporting standard CRUD operations
 */
@Resource(name = RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE + "/location", supportedClass = Location.class, supportedOpenmrsVersions = "1.8.*")
public class LocationResource1_8 extends MetadataDelegatingCrudResource<Location> {

    /**
     * @see DelegatingCrudResource#getRepresentationDescription(Representation)
     */
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        if (rep instanceof DefaultRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("display");
            description.addProperty("name");
            description.addProperty("description");
            description.addProperty("address1");
            description.addProperty("address2");
            description.addProperty("cityVillage");
            description.addProperty("stateProvince");
            description.addProperty("country");
            description.addProperty("postalCode");
            description.addProperty("latitude");
            description.addProperty("longitude");
            description.addProperty("countyDistrict");
            description.addProperty("address3");
            description.addProperty("address4");
            description.addProperty("address5");
            description.addProperty("address6");
            description.addProperty("tags", Representation.REF);
            description.addProperty("parentLocation", Representation.REF);
            description.addProperty("childLocations", Representation.REF);
            description.addProperty("retired");
            description.addSelfLink();
            description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            return description;
        } else if (rep instanceof FullRepresentation) {
            DelegatingResourceDescription description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("display");
            description.addProperty("name");
            description.addProperty("description");
            description.addProperty("address1");
            description.addProperty("address2");
            description.addProperty("cityVillage");
            description.addProperty("stateProvince");
            description.addProperty("country");
            description.addProperty("postalCode");
            description.addProperty("latitude");
            description.addProperty("longitude");
            description.addProperty("countyDistrict");
            description.addProperty("address3");
            description.addProperty("address4");
            description.addProperty("address5");
            description.addProperty("address6");
            description.addProperty("tags", Representation.DEFAULT);
            description.addProperty("parentLocation", Representation.DEFAULT);
            description.addProperty("childLocations", Representation.DEFAULT);
            description.addProperty("retired");
            description.addProperty("auditInfo");
            description.addSelfLink();
            return description;
        }
        return null;
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#getCreatableProperties()
     */
    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = new DelegatingResourceDescription();

        description.addRequiredProperty("name");

        description.addProperty("description");
        description.addProperty("address1");
        description.addProperty("address2");
        description.addProperty("cityVillage");
        description.addProperty("stateProvince");
        description.addProperty("country");
        description.addProperty("postalCode");
        description.addProperty("latitude");
        description.addProperty("longitude");
        description.addProperty("countyDistrict");
        description.addProperty("address3");
        description.addProperty("address4");
        description.addProperty("address5");
        description.addProperty("address6");
        description.addProperty("tags");
        description.addProperty("parentLocation");
        description.addProperty("childLocations");

        return description;
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#getUpdatableProperties()
     */
    @Override
    public DelegatingResourceDescription getUpdatableProperties() {
        return getCreatableProperties();
    }

    /**
     * @see DelegatingCrudResource#newDelegate()
     */
    @Override
    public Location newDelegate() {
        return new Location();
    }

    /**
     * @see DelegatingCrudResource#save(java.lang.Object)
     */
    @Override
    public Location save(Location location) {
        return Context.getLocationService().saveLocation(location);
    }

    /**
     * Fetches a location by uuid, if no match is found, it tries to look up one with a matching
     * name with the assumption that the passed parameter is a location name
     *
     * @see DelegatingCrudResource#getByUniqueId(java.lang.String)
     */
    @Override
    public Location getByUniqueId(String uuid) {
        Location location = Context.getLocationService().getLocationByUuid(uuid);
        //We assume the caller was fetching by name
        if (location == null)
            location = Context.getLocationService().getLocation(uuid);

        return location;
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#purge(java.lang.Object,
     * org.openmrs.module.webservices.rest.web.RequestContext)
     */
    @Override
    public void purge(Location location, RequestContext context) throws ResponseException {
        if (location == null)
            return;
        Context.getLocationService().purgeLocation(location);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doGetAll(org.openmrs.module.webservices.rest.web.RequestContext)
     */
    @Override
    protected NeedsPaging<Location> doGetAll(RequestContext context) {
        return new NeedsPaging<Location>(Context.getLocationService().getAllLocations(context.getIncludeAll()), context);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doSearch(org.openmrs.module.webservices.rest.web.RequestContext)
     * A query string and/or a tag uuid can be passed in; if both are passed in, returns an
     * intersection of the results; excludes retired locations
     */
    @Override
    protected PageableResult doSearch(RequestContext context) {

        LocationService locationService = Context.getLocationService();

        String tagUuid = context.getParameter("tag");
        String query = context.getParameter("q");

        List<Location> locationsByTag = null;
        List<Location> locationsByQuery = null;

        if (tagUuid != null) {
            LocationTag locationTag = locationService.getLocationTagByUuid(tagUuid);
            locationsByTag = locationService.getLocationsByTag(locationTag);
        }

        if (query != null) {
            locationsByQuery = locationService.getLocations(query);
        }

        if (locationsByTag == null) {
            return new NeedsPaging<Location>(locationsByQuery, context);
        } else if (locationsByQuery == null) {
            return new NeedsPaging<Location>(locationsByTag, context);
        } else {
            return new NeedsPaging<Location>(
                    (List<Location>) CollectionUtils.intersection(locationsByQuery, locationsByTag), context);
        }
    }
}
