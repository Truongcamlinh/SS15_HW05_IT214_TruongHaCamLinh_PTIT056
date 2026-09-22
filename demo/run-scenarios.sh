#!/usr/bin/env bash
set -euo pipefail

printf '\n=== FAILED_ALREADY_TAKEN: tạo PAYMENT và REFUND ===\n'
curl -sS -X POST http://localhost:8080/api/table-bookings \
  -H 'Content-Type: application/json' \
  -d '{"bookingId":"REST-2026-101","tableNumber":"B7","customerName":"Rika","customerEmail":"rika@email.com","depositAmount":500000,"scenario":"FAILED_ALREADY_TAKEN"}'
printf '\n\n=== Audit trail phải có đủ PAYMENT và REFUND ===\n'
curl -sS http://localhost:8081/api/payments/audit/REST-2026-101
printf '\n\n=== Semantic Lock đã được giải phóng ===\n'
curl -sS http://localhost:8082/api/tables/B7
printf '\n'
