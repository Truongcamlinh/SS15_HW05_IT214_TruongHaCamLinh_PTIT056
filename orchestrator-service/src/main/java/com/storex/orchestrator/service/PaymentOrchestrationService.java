package com.storex.orchestrator.service;

import com.storex.orchestrator.model.ActivityResult;
import com.storex.orchestrator.model.BookingTransaction;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentOrchestrationService {
    private final RestClient client;
    public PaymentOrchestrationService(RestClient.Builder builder,
            @Value("${services.payment-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }
    public ActivityResult pay(BookingTransaction tx) {
        return client.post().uri("/api/payments/process").body(command(tx))
                .retrieve().body(ActivityResult.class);
    }
    public ActivityResult refund(BookingTransaction tx) {
        return client.post().uri("/api/payments/{id}/refund", tx.getPaymentId()).body(command(tx))
                .retrieve().body(ActivityResult.class);
    }
    private Map<String, Object> command(BookingTransaction tx) {
        return Map.of("bookingId", tx.getBookingId(), "amount", tx.getDepositAmount(),
                "scenario", tx.getScenario());
    }
}
