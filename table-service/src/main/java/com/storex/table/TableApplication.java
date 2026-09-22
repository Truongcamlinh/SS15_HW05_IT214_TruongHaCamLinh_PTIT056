package com.storex.table;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableScheduling
@RestController
@RequestMapping("/api/tables")
public class TableApplication {
    private static final Logger log = LoggerFactory.getLogger(TableApplication.class);
    private final Map<String, TableResource> tables = new ConcurrentHashMap<>();
    private final long lockTimeoutMs;

    public TableApplication(@Value("${table.lock-timeout-ms:300000}") long lockTimeoutMs) {
        this.lockTimeoutMs = lockTimeoutMs;
        tables.put("B7", new TableResource("B7"));
        tables.put("B8", new TableResource("B8"));
    }
    public static void main(String[] args) { SpringApplication.run(TableApplication.class, args); }

    @PostMapping("/{tableNumber}/reserve")
    ActivityResult reserve(@PathVariable String tableNumber, @RequestBody TableCommand command) {
        TableResource table = tables.computeIfAbsent(tableNumber, TableResource::new);
        synchronized (table) {
            if (table.status != TableStatus.AVAILABLE) {
                return new ActivityResult(false, null, "Bàn đang được giữ hoặc đã được đặt");
            }
            table.status = TableStatus.RESERVED;
            table.reservedBy = command.bookingId();
            table.reservedAt = Instant.now();
            log.info("[TableService] Table {}: AVAILABLE -> RESERVED (Semantic Lock acquired)", tableNumber);
            log.info("[TableService] Table {} reserved for booking {}", tableNumber, command.bookingId());
            return new ActivityResult(true, tableNumber, "Bàn đang được giữ");
        }
    }

    @PostMapping("/{tableNumber}/confirm")
    ActivityResult confirm(@PathVariable String tableNumber, @RequestBody TableCommand command) {
        TableResource table = tables.computeIfAbsent(tableNumber, TableResource::new);
        synchronized (table) {
            if ("FAILED_ALREADY_TAKEN".equals(command.scenario())) {
                log.error("[TableService] ERROR: Table {} is already taken by another booking!", tableNumber);
                return new ActivityResult(false, null, "Already taken");
            }
            if (table.status != TableStatus.RESERVED || !command.bookingId().equals(table.reservedBy)) {
                return new ActivityResult(false, null, "Semantic Lock không thuộc booking này");
            }
            table.status = TableStatus.BOOKED;
            return new ActivityResult(true, tableNumber, "Xác nhận đặt bàn thành công");
        }
    }

    @PostMapping("/{tableNumber}/release")
    TableView release(@PathVariable String tableNumber, @RequestBody TableCommand command) {
        TableResource table = tables.computeIfAbsent(tableNumber, TableResource::new);
        synchronized (table) {
            if (table.status == TableStatus.RESERVED && command.bookingId().equals(table.reservedBy)) {
                table.status = TableStatus.AVAILABLE;
                table.reservedBy = null;
                table.reservedAt = null;
                log.info("[TableService] Table {}: RESERVED -> AVAILABLE (Semantic Lock released)", tableNumber);
            }
            return view(table);
        }
    }

    @GetMapping("/{tableNumber}")
    TableView find(@PathVariable String tableNumber) {
        return view(tables.computeIfAbsent(tableNumber, TableResource::new));
    }

    @Scheduled(fixedDelay = 5000)
    void autoReleaseExpiredLocks() {
        Instant expiredBefore = Instant.now().minusMillis(lockTimeoutMs);
        tables.values().forEach(table -> {
            synchronized (table) {
                if (table.status == TableStatus.RESERVED && table.reservedAt.isBefore(expiredBefore)) {
                    log.warn("[TableService] Auto-release expired lock for table {}", table.tableNumber);
                    table.status = TableStatus.AVAILABLE;
                    table.reservedBy = null;
                    table.reservedAt = null;
                }
            }
        });
    }

    private TableView view(TableResource table) {
        String message = switch (table.status) {
            case RESERVED -> "Bàn đang được giữ";
            case BOOKED -> "Bàn đã được đặt";
            case AVAILABLE -> "Bàn đang trống";
        };
        return new TableView(table.tableNumber, table.status, table.reservedBy, table.reservedAt, message);
    }

    enum TableStatus { AVAILABLE, RESERVED, BOOKED }
    record TableCommand(String bookingId, String scenario) {}
    record ActivityResult(boolean success, String id, String message) {}
    record TableView(String tableNumber, TableStatus status, String reservedBy, Instant reservedAt, String message) {}
    static class TableResource {
        final String tableNumber;
        TableStatus status = TableStatus.AVAILABLE;
        String reservedBy;
        Instant reservedAt;
        TableResource(String tableNumber) { this.tableNumber = tableNumber; }
    }
}
