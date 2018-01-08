/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemrIL.web.resource;

import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.hl7.HL7Source;
import org.openmrs.module.kenyaemrIL.api.ILMessageType;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.ILTest;
import org.openmrs.module.kenyaemrIL.web.controller.KenyaEMRILResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.MetadataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * {@link Resource} for {@link HL7Source}, supporting standard CRUD operations
 */
@Resource(name = RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE + "/iltest", supportedClass = ILTest.class, supportedOpenmrsVersions = {
        "1.8.*", "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.0.*", "2.1.*"})
public class ILTestResource extends MetadataDelegatingCrudResource<ILTest> {

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#getByUniqueId(String)
     */
    @Override
    public ILTest getByUniqueId(String uniqueId) {
        return Context.getService(KenyaEMRILService.class).getILTestByUuid(uniqueId);
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#getRepresentationDescription(Representation)
     */
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        return null;
    }

    /**
     * @see MetadataDelegatingCrudResource#getCreatableProperties()
     */
    @Override
    public DelegatingResourceDescription getCreatableProperties() {
        DelegatingResourceDescription description = super.getCreatableProperties();
        //description is set as optional on the superclass, we need to over ride that
        description.addRequiredProperty("description");

        return description;
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#newDelegate()
     */
    @Override
    public ILTest newDelegate() {
        return new ILTest();
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingResource#purge(Object,
     * RequestContext)
     */
    @Override
    public void purge(ILTest delegate, RequestContext context) throws ResponseException {
        throw new ResourceDoesNotSupportOperationException();
    }

    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceHandler#save(Object)
     */
    @Override
    public ILTest save(ILTest delegate) {
        if (Context.getAuthenticatedUser() != null) {
            delegate.setCreator(Context.getAuthenticatedUser());
        } else {
            delegate.setCreator(new User(1));
        }
        delegate.setMessageType(ILMessageType.INBOUND.getValue());
        return Context.getService(KenyaEMRILService.class).saveILTest(delegate);
    }


    /**
     * @see org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doGetAll(org.openmrs.module.webservices.rest.web.RequestContext)
     */
    @Override
    protected NeedsPaging<ILTest> doGetAll(RequestContext context) {
        return new NeedsPaging<ILTest>(Context.getService(KenyaEMRILService.class).getAllILTests(context.getIncludeAll()), context);
    }


}
