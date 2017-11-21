package org.openmrs.module.kenyaemrIL.il.pharmacy;

/**
 * @author Stanslaus Odhiambo
 *         Created on 21/11/2017.
 */
public class CommonOrderDetails {
    private String orderStatus;
    private PlacerOrderNumber placerOrderNumber;
    private FillerOrderNumber fillerOrderNumber;
    private String orderControl;
    private String transactionDateTime;
    private String notes;
    private OrderingPhysician orderingPhysician;

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PlacerOrderNumber getPlacerOrderNumber() {
        return placerOrderNumber;
    }

    public void setPlacerOrderNumber(PlacerOrderNumber placerOrderNumber) {
        this.placerOrderNumber = placerOrderNumber;
    }

    public FillerOrderNumber getFillerOrderNumber() {
        return fillerOrderNumber;
    }

    public void setFillerOrderNumber(FillerOrderNumber fillerOrderNumber) {
        this.fillerOrderNumber = fillerOrderNumber;
    }

    public String getOrderControl() {
        return orderControl;
    }

    public void setOrderControl(String orderControl) {
        this.orderControl = orderControl;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OrderingPhysician getOrderingPhysician() {
        return orderingPhysician;
    }

    public void setOrderingPhysician(OrderingPhysician orderingPhysician) {
        this.orderingPhysician = orderingPhysician;
    }
}
