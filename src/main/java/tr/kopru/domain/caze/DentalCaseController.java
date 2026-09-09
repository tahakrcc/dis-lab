package tr.kopru.domain.caze;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.caze.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DentalCaseController {

    private final DentalCaseService caseService;

    @PostMapping("/cases")
    public ResponseEntity<Object> createCase(@Valid @RequestBody CreateCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.createCase(request));
    }

    @GetMapping("/cases")
    public ResponseEntity<List<?>> getCases(
            @RequestParam(required = false) UUID partnershipId,
            @RequestParam(required = false) CaseStatus durum) {
        return ResponseEntity.ok(caseService.getCases(partnershipId, durum));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<Object> getCaseById(@PathVariable UUID id) {
        return ResponseEntity.ok(caseService.getCaseById(id));
    }

    @PatchMapping("/cases/{id}/items")
    public ResponseEntity<Object> updateItems(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseItemsRequest request) {
        return ResponseEntity.ok(caseService.updateItems(id, request));
    }

    @PostMapping("/cases/{id}/transitions")
    public ResponseEntity<Object> transitionCase(
            @PathVariable UUID id,
            @Valid @RequestBody CaseTransitionRequest request) {
        return ResponseEntity.ok(caseService.transitionCase(id, request));
    }

    @GetMapping("/cases/{id}/events")
    public ResponseEntity<List<CaseEventResponse>> getCaseEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(caseService.getCaseEvents(id));
    }

    @PostMapping("/cases/{id}/shipments")
    public ResponseEntity<ShipmentResponse> createShipment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.createShipment(id, request));
    }

    @PatchMapping("/shipments/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable UUID id,
            @RequestBody UpdateShipmentRequest request) {
        return ResponseEntity.ok(caseService.updateShipment(id, request));
    }
}
