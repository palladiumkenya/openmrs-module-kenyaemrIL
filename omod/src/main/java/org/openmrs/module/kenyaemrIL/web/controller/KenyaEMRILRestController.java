package org.openmrs.module.kenyaemrIL.web.controller;


import org.json.JSONObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class KenyaEMRILRestController {
    @RequestMapping(value = "/kenyaemrapi", method = RequestMethod.POST)
    public void greeting(@RequestBody String s) {
        String hel7Type = new JSONObject(s).getJSONObject("MESSAGE_HEADER").getString("MESSAGE_TYPE");
        String source = new JSONObject(s).getJSONObject("MESSAGE_HEADER").getString("SENDING_APPLICATION");
        KenyaEMRILMessage kenyaEMRILMessage = new KenyaEMRILMessage();
        kenyaEMRILMessage.setMessage(s);
        kenyaEMRILMessage.setHl7_type(hel7Type);
        kenyaEMRILMessage.setHl7_type(source);
        kenyaEMRILMessage.setMessage_type(1);
        //save th
        Context.getService(KenyaEMRILService.class).saveKenyaEMRILMessage(kenyaEMRILMessage);

        //new PostUtil<KenyaEMRILMessage>().makePostRequest(kenyaEMRILMessage);
    }
}
