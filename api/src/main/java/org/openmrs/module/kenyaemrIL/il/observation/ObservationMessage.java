package org.openmrs.module.kenyaemrIL.il.observation;

import org.openmrs.module.kenyaemrIL.il.ILMessage;

/**
 * @author Stanslaus Odhiambo
 *         Created on 08/01/2018.
 */
public class ObservationMessage extends ILMessage
{
    private OBSERVATION_RESULT[] observation_result;
    public OBSERVATION_RESULT[] getObservation_result() {
        return observation_result;
    }

    public void setObservation_result(OBSERVATION_RESULT[] observation_result) {
        this.observation_result = observation_result;
    }
}