package com.storex.orchestrator.machine;

import static com.storex.orchestrator.model.BookingEvent.*;
import static com.storex.orchestrator.model.BookingState.*;

import com.storex.orchestrator.model.*;
import com.storex.orchestrator.service.PaymentOrchestrationService;
import com.storex.orchestrator.service.TableOrchestrationService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TableBookingStateMachineImpl implements TableBookingStateMachine {
    private static final Logger log = LoggerFactory.getLogger(TableBookingStateMachineImpl.class);
    private final TableOrchestrationService tableService;
    private final PaymentOrchestrationService paymentService;
    private final Map<String, BookingTransaction> transactions = new ConcurrentHashMap<>();

    public TableBookingStateMachineImpl(TableOrchestrationService tableService,
                                        PaymentOrchestrationService paymentService) {
        this.tableService = tableService;
        this.paymentService = paymentService;
    }

    @Override
    public BookingTransaction start(BookingTransaction tx) {
        if (transactions.putIfAbsent(tx.getBookingId(), tx) != null) {
            throw new IllegalArgumentException("bookingId đã tồn tại");
        }
        transition(tx, TABLE_RESERVING, RESERVE_TABLE);
        ActivityResult lock = tableService.reserve(tx);
        if (lock == null || !lock.success()) {
            tx.setMessage(lock == null ? "Table Service không phản hồi" : lock.message());
            transition(tx, CANCELLED, TABLE_UNAVAILABLE);
            return tx;
        }

        transition(tx, BookingState.TABLE_RESERVED, BookingEvent.TABLE_RESERVED);
        transition(tx, PAYMENT_PENDING, PROCESS_PAYMENT);
        ActivityResult payment = paymentService.pay(tx);
        if (payment == null || !payment.success()) {
            tx.setMessage(payment == null ? "Payment Service không phản hồi" : payment.message());
            transition(tx, CANCELLED, PAYMENT_FAILED);
            tableService.release(tx);
            return tx;
        }

        tx.setPaymentId(payment.id());
        transition(tx, PAYMENT_COMPLETED, PAYMENT_SUCCESS);
        transition(tx, BOOKING_CONFIRMING, CONFIRM_BOOKING);
        ActivityResult confirmation = tableService.confirm(tx);
        if (confirmation != null && confirmation.success()) {
            transition(tx, BookingState.BOOKING_CONFIRMED, BookingEvent.BOOKING_CONFIRMED);
            log.info("[Orchestrator] Final State: BOOKING_CONFIRMED for booking {}", tx.getBookingId());
            return tx;
        }

        tx.setMessage(confirmation == null ? "Table Service không phản hồi" : confirmation.message());
        log.warn("[Orchestrator] Table reservation failed for {} (Already taken).", tx.getTableNumber());
        transition(tx, CANCELLED, TABLE_UNAVAILABLE);
        compensate(tx);
        return tx;
    }

    @Override
    public BookingTransaction find(String bookingId) { return transactions.get(bookingId); }

    private void compensate(BookingTransaction tx) {
        if (tx.getPaymentId() != null) {
            log.info("[Orchestrator] Initiating Compensation: Calling refundPayment for booking {}...",
                    tx.getBookingId());
            paymentService.refund(tx);
        }
        tableService.release(tx);
        log.info("[BookingService] Booking {} updated to CANCELLED.", tx.getBookingId());
        log.info("[Final State] Table {} is now AVAILABLE (semantic lock released).", tx.getTableNumber());
    }

    private void transition(BookingTransaction tx, BookingState state, BookingEvent event) {
        BookingState old = tx.getCurrentState();
        tx.setCurrentState(state);
        log.info("[Orchestrator] State: {} -> Event: {} -> New State: {}", old, event, state);
    }
}
