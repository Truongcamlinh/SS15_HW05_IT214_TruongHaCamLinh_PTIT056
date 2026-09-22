package com.storex.orchestrator.machine;

import com.storex.orchestrator.model.BookingTransaction;

public interface TableBookingStateMachine {
    BookingTransaction start(BookingTransaction transaction);
    BookingTransaction find(String bookingId);
}
