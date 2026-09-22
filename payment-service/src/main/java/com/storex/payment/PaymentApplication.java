package com.storex.payment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/payments")
public class PaymentApplication {
    private static final Logger log = LoggerFactory.getLogger(PaymentApplication.class);
    private final List<TransactionRecord> auditTrail = new CopyOnWriteArrayList<>();

    public static void main(String[] args) { SpringApplication.run(PaymentApplication.class, args); }

    @PostMapping("/process")
    ActivityResult process(@RequestBody PaymentCommand command) {
        if ("PAYMENT_FAILED".equals(command.scenario())) {
            return new ActivityResult(false, null, "Thanh toán tiền cọc thất bại");
        }
        TransactionRecord existing = auditTrail.stream()
                .filter(tx -> tx.bookingId().equals(command.bookingId()) && tx.type() == TransactionType.PAYMENT)
                .findFirst().orElse(null);
        if (existing != null) return new ActivityResult(true, existing.transactionId(), "Đã thanh toán trước đó");
        String id = "TX-" + UUID.randomUUID().toString().substring(0, 8);
        auditTrail.add(new TransactionRecord(id, command.bookingId(), -command.amount(),
                TransactionType.PAYMENT, "PROCESSED", Instant.now()));
        log.info("[PaymentService] Payment of {} VND processed for booking {}", command.amount(), command.bookingId());
        return new ActivityResult(true, id, "Thanh toán thành công");
    }

    @PostMapping("/{paymentId}/refund")
    ActivityResult refund(@PathVariable String paymentId, @RequestBody PaymentCommand command) {
        TransactionRecord existing = auditTrail.stream()
                .filter(tx -> tx.bookingId().equals(command.bookingId()) && tx.type() == TransactionType.REFUND)
                .findFirst().orElse(null);
        if (existing != null) return new ActivityResult(true, existing.transactionId(), "Đã hoàn tiền trước đó");
        String id = "RF-" + UUID.randomUUID().toString().substring(0, 8);
        auditTrail.add(new TransactionRecord(id, command.bookingId(), command.amount(),
                TransactionType.REFUND, "PROCESSED", Instant.now()));
        log.info("[RefundActivity] Refund of {} VND processed for booking {}", command.amount(), command.bookingId());
        log.info("[Transaction] Created REFUND record: +{} VND for booking {} (Audit Trail)",
                command.amount(), command.bookingId());
        return new ActivityResult(true, id, "Hoàn tiền thành công cho payment " + paymentId);
    }

    @GetMapping("/audit/{bookingId}")
    List<TransactionRecord> audit(@PathVariable String bookingId) {
        return auditTrail.stream().filter(tx -> tx.bookingId().equals(bookingId)).toList();
    }

    enum TransactionType { PAYMENT, REFUND }
    record PaymentCommand(String bookingId, long amount, String scenario) {}
    record ActivityResult(boolean success, String id, String message) {}
    record TransactionRecord(String transactionId, String bookingId, long amount,
                             TransactionType type, String status, Instant timestamp) {}
}
