package org.openmrs.module.kenyaemrIL;

import java.util.Date;
import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemrIL.api.KenyaEMRILService;
import org.openmrs.module.kenyaemrIL.il.KenyaEMRILMessage;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a task that processes the IL inbox every one minute .
 */
public class ProcessInboxTask extends AbstractTask {

    // Logger
    private static final Logger log = LoggerFactory.getLogger(ProcessInboxTask.class);


    /**
     * @see org.openmrs.scheduler.tasks.AbstractTask#execute()
     */
    @Override
    public void execute() {
        log.info("Executing task at " + new Date());
//        Fetch non-processed inbox messages
        List<KenyaEMRILMessage> pendingInboxes = fetchILInboxes(false);
        System.out.printf("# of fetched messages: {}", pendingInboxes.size());
        for (final KenyaEMRILMessage pendingInbox : pendingInboxes) {

//            Run new patient creation in a different thread for speedy execution
            new Thread(new Runnable() {
                @Override
                public void run() {
                    processFetchedRecord(pendingInbox);
                }
            }).start();
        }


    }

    private void processFetchedRecord(KenyaEMRILMessage pendingInbox) {
//        Process each message and mark as processed
        String message = pendingInbox.getMessage();

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