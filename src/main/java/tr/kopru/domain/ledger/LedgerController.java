package tr.kopru.domain.ledger;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.ledger.dto.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partnerships/{id}")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/ledger")
    public ResponseEntity<LedgerReportResponse> getLedger(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ledgerService.getLedgerReport(id, from, to));
    }

    @PostMapping("/payments")
    public ResponseEntity<LedgerEntryResponse> createPayment(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createPayment(id, request));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('LAB_ADMIN', 'KLINIK_ADMIN')")
    public ResponseEntity<LedgerEntryResponse> createAdjustment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createAdjustment(id, request));
    }
}
