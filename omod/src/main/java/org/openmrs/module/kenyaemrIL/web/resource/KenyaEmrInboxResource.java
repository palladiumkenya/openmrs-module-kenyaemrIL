package org.openmrs.module.kenyaemrIL.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.KenyaEmrInbox;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.web.controller.KenyaEMRILResourceController;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.*;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */

@Resource(name = RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE
        + "/inbox", supportedClass = KenyaEmrInbox.class, supportedOpenmrsVersions = {"1.9.*", "1.10.*", "1.11.*",
        "1.12.*", "2.0.*", "2.1.*"})
public class KenyaEmrInboxResource extends DataDelegatingCrudResource<KenyaEmrInbox> {

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
    public KenyaEmrInbox newDelegate() {
        return new KenyaEmrInbox();
    }

    /**
     * @see DelegatingCrudResource#save(java.lang.Object)
     */
    @Override
    public KenyaEmrInbox save(KenyaEmrInbox kenyaEmrInbox) {
        return Context.getService(KenyaEMRILService.class).saveKenyaEmrInbox(kenyaEmrInbox);
    }

    /**
     * Fetches an inbox by uuid, if no match is found, it tries to look up one with a matching
     * name with the assumption that the passed parameter is a location name
     *
     * @see DelegatingCrudResource#getByUniqueId(java.lang.String)
     */
    @Override
    public KenyaEmrInbox getByUniqueId(String uuid) {
        return Context.getService(KenyaEMRILService.class).getKenyaEmrInboxByUuid(uuid);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#purge(java.lang.Object,
     * org.openmrs.module.webservices.rest.web.RequestContext)
     */
    @Override
    public void purge(KenyaEmrInbox kenyaEmrInbox, RequestContext requestContext) throws ResponseException {
        Context.getService(KenyaEMRILService.class).purgeKenyaEmrInbox(kenyaEmrInbox);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doGetAll(org.openmrs.module.webservices.rest.web.RequestContext)
     */

    @Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        return new NeedsPaging<KenyaEmrInbox>(Context.getService(KenyaEMRILService.class).getAllKenyaEmrInboxes(false), context);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doSearch(org.openmrs.module.webservices.rest.web.RequestContext)
     * A query string and/or a tag uuid can be passed in; if both are passed in, returns an
     * intersection of the results; excludes retired locations
     */

    @Override
    protected PageableResult doSearch(RequestContext context) throws ResponseException {
        String ilMessageTypeUuid = context.getRequest().getParameter("messageType");
        if (ilMessageTypeUuid != null) {
            List<KenyaEmrInbox> inboxes = Context.getService(KenyaEMRILService.class)
                    .getKenyaEmrInboxesByType(ilMessageTypeUuid);
            return new NeedsPaging<KenyaEmrInbox>(inboxes, context);
        }
        return new EmptySearchResult();
    }

//    End


    @Override
    public void delete(KenyaEmrInbox kenyaEmrInbox, String s, RequestContext requestContext) throws ResponseException {
        Context.getService(KenyaEMRILService.class).retireKenyaEmrInbox(kenyaEmrInbox, s);
    }


    @Override
    public Object create(SimpleObject postBody, RequestContext context) throws ResponseException {
//        TODO - Do some conversion here for the postBody
        Object savedIdentifierSource = null;
        ArrayList<String> errors = new ArrayList<String>();

        return ConversionUtil.convertToRepresentation(savedIdentifierSource, Representation.DEFAULT);

    }

    @Override
    public Object update(String uuid, SimpleObject updateBody, RequestContext context) throws ResponseException {
//        TODO - Do some conversion here for the postBody
        Object updatedKenyaEmrInbox = null;
        ArrayList<String> errors = new ArrayList<String>();

        return ConversionUtil.convertToRepresentation(updatedKenyaEmrInbox, Representation.DEFAULT);

    }


}
