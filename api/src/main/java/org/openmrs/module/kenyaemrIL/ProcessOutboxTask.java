package org.openmrs.module.kenyaemrIL;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementation of a task that processes the IL inbox every one minute .
 */
public class ProcessOutboxTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessOutboxTask.class);


    /**
     * @see AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing task at " + new Date());
//        Fetch non-processed inbox messages
        List<KenyaEMRILMessage> pendingInboxes = fetchILInboxes(false);
        System.out.printf("# of fetched messages: {}", pendingInboxes.size());
        for (KenyaEMRILMessage pendingInbox : pendingInboxes) {
            processFetchedRecord(pendingInbox);
        }
    }

    private void processFetchedRecord(KenyaEMRILMessage pendingInbox) {
//        Process each message and mark as processed
        String message = pendingInbox.getMessage();
        System.out.println("The thing goes skkkkraah");
        System.out.println(message);

    }

    private List<KenyaEMRILMessage> fetchILInboxes(boolean fetchRetired) {
        KenyaEMRILService service = getEMRILService();
        return service.getKenyaEMRILInboxes(fetchRetired);
    }

    private List<KenyaEMRILMessage> fetchILOutboxes(boolean fetchRetired) {
        return getEMRILService().getKenyaEMRILOutboxes(fetchRetired);
    }

    private KenyaEMRILService getEMRILService() {
        return Context.getService(KenyaEMRILService.class);
    }

}