package com.storex.orchestrator.model;

public record BookingRequest(String bookingId, String tableNumber, String customerName,
                             String customerEmail, long depositAmount, String scenario) {}
