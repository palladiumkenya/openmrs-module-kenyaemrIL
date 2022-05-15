/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemrIL.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.module.kenyaemrIL.il.utils.HTTPRequestUtils;
import org.openmrs.module.kenyaemrIL.il.utils.ViralLoadProcessorUtil;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * http://localhost:8080/openmrs/ws/rest/v1/interop/processhl7il
 *
 * The main controller.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/interop")
public class ILRestController extends BaseRestController {

	protected final Log log = LogFactory.getLog(getClass());

	@RequestMapping(method = RequestMethod.POST, value = "/processhl7il")
	@ResponseBody
	public String receiveSHR(HttpServletRequest request) {
		Integer patientID=null;
		String requestBody = null;
		String response = "";
		try {
			requestBody = HTTPRequestUtils.fetchRequestBody(request.getReader());//request.getParameter("encryptedSHR") != null? request.getParameter("encryptedSHR"): null;
			if(requestBody != null) {
				String hel7Type = new JSONObject(requestBody).getJSONObject("MESSAGE_HEADER").getString("MESSAGE_TYPE");
				String source = new JSONObject(requestBody).getJSONObject("MESSAGE_HEADER").getString("SENDING_APPLICATION");
				KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
				kenyaEMRILMessage.setMessage(requestBody);
				kenyaEMRILMessage.setHl7_type(hel7Type);
				kenyaEMRILMessage.setSource(source);
				kenyaEMRILMessage.setMessage_type(1);
				//save the model
				Context.getService(KenyaEMRILService.class).saveKenyaEMRILMessage(kenyaEMRILMessage);
			}
			response = "success";
		} catch (IOException e) {
			System.out.println("Error processing IL Message".concat(e.getMessage()));
			response = "Error processing IL Message".concat(e.getMessage());
		}

		return response;
	}

	/**
	 * end point to process internal request to process lab results
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/labresults") // end point for processing individual lab results
	@ResponseBody
	public Object processIncomingViralLoadResults(HttpServletRequest request) {
		String requestBody = null;
		try {
			requestBody = ViralLoadProcessorUtil.fetchRequestBody(request.getReader());
		} catch (IOException e) {
			return new SimpleObject().add("ServerResponse", "Error extracting request body");
		}

		if (requestBody != null) {

			return ViralLoadProcessorUtil.restEndPointForLabResult(requestBody);

		}
		return new SimpleObject().add("Report", "The request could not be interpreted properly");
	}



}
