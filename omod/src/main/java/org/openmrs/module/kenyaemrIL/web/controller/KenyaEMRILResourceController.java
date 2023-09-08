/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrIL.web.controller;


import org.openmrs.module.kenyaemrIL.fragment.controller.ShrSummariesFragmentController;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;


/**
 * The web service controller.
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE)
public class KenyaEMRILResourceController extends MainResourceController {

    public static final String KENYAEMR_IL__NAMESPACE = "/kenyaemril";

    /**
     * @see org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController#getNamespace()
     */
    @Override
    public String getNamespace() {
        return RestConstants.VERSION_1 + KenyaEMRILResourceController.KENYAEMR_IL__NAMESPACE;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/expected-ti-patient")
    @ResponseBody
    public SimpleObject shrSummary(@RequestParam("patientUuid") String patientUuid) {
        SimpleObject result = null;
        try {
            result = ShrSummariesFragmentController.constructSHrSummary(patientUuid);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
