package com.storex.orchestrator.controller;

import com.storex.orchestrator.machine.TableBookingStateMachine;
import com.storex.orchestrator.model.BookingRequest;
import com.storex.orchestrator.model.BookingTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/table-bookings")
public class BookingController {
    private final TableBookingStateMachine machine;
    public BookingController(TableBookingStateMachine machine) { this.machine = machine; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingTransaction create(@RequestBody BookingRequest request) {
        return machine.start(new BookingTransaction(request));
    }
    @GetMapping("/{bookingId}")
    BookingTransaction find(@PathVariable String bookingId) { return machine.find(bookingId); }
}
