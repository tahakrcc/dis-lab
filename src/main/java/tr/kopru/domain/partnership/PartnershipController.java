package tr.kopru.domain.partnership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.partnership.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partnerships")
@RequiredArgsConstructor
public class PartnershipController {

    private final PartnershipService partnershipService;

    @PostMapping
    public ResponseEntity<PartnershipResponse> createPartnership(@Valid @RequestBody CreatePartnershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnershipService.createPartnership(request));
    }

    @GetMapping
    public ResponseEntity<List<PartnershipResponse>> getPartnerships() {
        return ResponseEntity.ok(partnershipService.getPartnerships());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PartnershipResponse> updatePartnership(
            @PathVariable UUID id,
            @RequestBody UpdatePartnershipRequest request) {
        return ResponseEntity.ok(partnershipService.updatePartnership(id, request));
    }
}
