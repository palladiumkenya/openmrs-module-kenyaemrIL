package org.openmrs.module.kenyaemrIL.web.resource;

import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.ILMessageType;
import org.openmrs.module.kenyaemrIL.KenyaEMRIL;
import org.openmrs.module.kenyaemrIL.KenyaEMRILActivator;
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
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
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
public class KenyaEmrInboxResource extends DelegatingCrudResource<KenyaEmrInbox> {
    @Override
    public KenyaEmrInbox getByUniqueId(String s) {
        List<KenyaEmrInbox> inboxes = Context.getService(KenyaEMRILService.class).getKenyaEmrInboxes(s);
        if (inboxes.size() != 0) {
            return inboxes.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected void delete(KenyaEmrInbox kenyaEmrInbox, String s, RequestContext requestContext) throws ResponseException {
        Context.getService(KenyaEMRILService.class).retireKenyaEmrInbox(kenyaEmrInbox, s);
    }

    @Override
    public KenyaEmrInbox newDelegate() throws ResourceDoesNotSupportOperationException {
        throw new ResourceDoesNotSupportOperationException();
    }

    @Override
    public KenyaEmrInbox save(KenyaEmrInbox kenyaEmrInbox) {
        return Context.getService(KenyaEMRILService.class).saveKenyaEmrInbox(kenyaEmrInbox);
    }

    @Override
    public void purge(KenyaEmrInbox kenyaEmrInbox, RequestContext requestContext) throws ResponseException {
        Context.getService(KenyaEMRILService.class).purgeKenyaEmrInbox(kenyaEmrInbox);
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = null;

        if (rep instanceof RefRepresentation) {
            description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("display");
            description.addSelfLink();
            return description;
        } else if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description = new DelegatingResourceDescription();
            description.addProperty("uuid");
            description.addProperty("name");
            description.addProperty("description");
            description.addSelfLink();
            if (rep instanceof DefaultRepresentation) {
                description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
            }
        }
        return description;
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

    @Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        return new NeedsPaging<KenyaEmrInbox>(Context.getService(KenyaEMRILService.class).getAllKenyaEmrInboxes(false), context);
    }

    @Override
    protected PageableResult doSearch(RequestContext context) throws ResponseException {
        String ilMessageTypeUuid = context.getRequest().getParameter("ilMessageType");
        if(ilMessageTypeUuid != null){
            ILMessageType type = Context.getService(KenyaEMRILService.class).getIlMessageTypeByUuid(ilMessageTypeUuid);
            if(type != null){
                List<KenyaEmrInbox> inboxes = Context.getService(KenyaEMRILService.class)
                        .getKenyaEmrInboxesByType(type);
                return new NeedsPaging<KenyaEmrInbox>(inboxes, context);
            }
        }
        return new EmptySearchResult();
    }


}
