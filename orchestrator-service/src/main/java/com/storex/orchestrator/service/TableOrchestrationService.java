package com.storex.orchestrator.service;

import com.storex.orchestrator.model.ActivityResult;
import com.storex.orchestrator.model.BookingTransaction;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TableOrchestrationService {
    private final RestClient client;
    public TableOrchestrationService(RestClient.Builder builder,
            @Value("${services.table-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }
    public ActivityResult reserve(BookingTransaction tx) {
        return client.post().uri("/api/tables/{number}/reserve", tx.getTableNumber())
                .body(command(tx)).retrieve().body(ActivityResult.class);
    }
    public ActivityResult confirm(BookingTransaction tx) {
        return client.post().uri("/api/tables/{number}/confirm", tx.getTableNumber())
                .body(command(tx)).retrieve().body(ActivityResult.class);
    }
    public void release(BookingTransaction tx) {
        client.post().uri("/api/tables/{number}/release", tx.getTableNumber())
                .body(command(tx)).retrieve().toBodilessEntity();
    }
    private Map<String, String> command(BookingTransaction tx) {
        return Map.of("bookingId", tx.getBookingId(), "scenario", tx.getScenario());
    }
}
