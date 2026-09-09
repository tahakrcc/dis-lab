package tr.kopru.domain.catalog;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.catalog.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/service-items")
    public ResponseEntity<List<ServiceItemResponse>> getServiceItems() {
        return ResponseEntity.ok(catalogService.getServiceItems());
    }

    @PostMapping("/service-items")
    @PreAuthorize("hasRole('LAB_ADMIN')")
    public ResponseEntity<ServiceItemResponse> createServiceItem(@Valid @RequestBody CreateServiceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createServiceItem(request));
    }

    @PatchMapping("/service-items/{id}")
    @PreAuthorize("hasRole('LAB_ADMIN')")
    public ResponseEntity<ServiceItemResponse> updateServiceItem(
            @PathVariable UUID id,
            @RequestBody UpdateServiceItemRequest request) {
        return ResponseEntity.ok(catalogService.updateServiceItem(id, request));
    }

    @GetMapping("/partnerships/{id}/prices")
    public ResponseEntity<List<PriceListResponse>> getPartnershipPrices(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getPartnershipPrices(id));
    }

    @PutMapping("/partnerships/{id}/prices")
    @PreAuthorize("hasRole('LAB_ADMIN')")
    public ResponseEntity<PriceListResponse> upsertPrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertPriceRequest request) {
        return ResponseEntity.ok(catalogService.upsertPrice(id, request));
    }
}
