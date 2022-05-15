package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class COMMON_ORDER_DETAILS {
    private String order_status = "CA";
    private PLACER_ORDER_NUMBER placer_order_number;
    private FILLER_ORDER_NUMBER filler_order_number;
    private String order_control = "CA";
    private String transaction_datetime = "";
    private String notes = "";

    public ORDERING_PHYSICIAN getOrdering_physician() {
        return ordering_physician;
    }

    public void setOrdering_physician(ORDERING_PHYSICIAN ordering_physician) {
        this.ordering_physician = ordering_physician;
    }

    private ORDERING_PHYSICIAN ordering_physician;

     public PLACER_ORDER_NUMBER getPlacer_order_number() {
        return placer_order_number;
    }

    public void setPlacer_order_number(PLACER_ORDER_NUMBER placer_order_number) {
        this.placer_order_number = placer_order_number;
    }

    public FILLER_ORDER_NUMBER getFiller_order_number() {
        return filler_order_number;
    }

    public void setFiller_order_number(FILLER_ORDER_NUMBER filler_order_number) {
        this.filler_order_number = filler_order_number;
    }

    public String getOrder_control() {
        return order_control;
    }

    public void setOrder_control(String order_control) {
        this.order_control = order_control;
    }

    public String getTransaction_datetime() {
        return transaction_datetime;
    }

    public void setTransaction_datetime(String transaction_datetime) {
        this.transaction_datetime = transaction_datetime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOrder_status() {
        return order_status;
    }

    public void setOrder_status(String order_status) {
        this.order_status = order_status;
    }
}
