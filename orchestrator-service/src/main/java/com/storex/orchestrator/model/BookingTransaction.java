package com.storex.orchestrator.model;

public class BookingTransaction {
    private final String bookingId;
    private final String tableNumber;
    private final String customerName;
    private final String customerEmail;
    private final long depositAmount;
    private final String scenario;
    private BookingState currentState = BookingState.INITIATED;
    private String paymentId;
    private String message;

    public BookingTransaction(BookingRequest request) {
        this.bookingId = request.bookingId();
        this.tableNumber = request.tableNumber();
        this.customerName = request.customerName();
        this.customerEmail = request.customerEmail();
        this.depositAmount = request.depositAmount();
        this.scenario = request.scenario() == null ? "SUCCESS" : request.scenario();
    }
    public String getBookingId() { return bookingId; }
    public String getTableNumber() { return tableNumber; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public long getDepositAmount() { return depositAmount; }
    public String getScenario() { return scenario; }
    public BookingState getCurrentState() { return currentState; }
    public void setCurrentState(BookingState state) { this.currentState = state; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
