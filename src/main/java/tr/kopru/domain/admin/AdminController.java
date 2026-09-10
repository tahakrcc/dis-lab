package tr.kopru.domain.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.admin.dto.AdminUserResponse;
import tr.kopru.domain.admin.dto.CreateUserRequest;
import tr.kopru.domain.catalog.CatalogService;
import tr.kopru.domain.catalog.dto.CreateServiceItemRequest;
import tr.kopru.domain.catalog.dto.PriceListResponse;
import tr.kopru.domain.catalog.dto.ServiceItemResponse;
import tr.kopru.domain.catalog.dto.UpdateServiceItemRequest;
import tr.kopru.domain.catalog.dto.UpsertPriceRequest;
import tr.kopru.domain.partnership.dto.UpdatePartnershipRequest;
import tr.kopru.domain.org.OrgService;
import tr.kopru.domain.org.dto.AddMemberRequest;
import tr.kopru.domain.org.dto.CreateOrgRequest;
import tr.kopru.domain.org.dto.MemberResponse;
import tr.kopru.domain.org.dto.OrgResponse;
import tr.kopru.domain.partnership.PartnershipService;
import tr.kopru.domain.partnership.dto.CreatePartnershipRequest;
import tr.kopru.domain.partnership.dto.PartnershipResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final OrgService orgService;
    private final PartnershipService partnershipService;
    private final CatalogService catalogService;

    // ---- Kullanıcılar ----
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
    }

    // ---- Organizasyonlar ----
    @GetMapping("/organizations")
    public ResponseEntity<List<OrgResponse>> listOrganizations() {
        return ResponseEntity.ok(adminService.listOrganizations());
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrgResponse> createOrganization(@Valid @RequestBody CreateOrgRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createOrganization(request));
    }

    @GetMapping("/organizations/{id}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.listMembers(id));
    }

    @PostMapping("/organizations/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.addMember(id, request));
    }

    @DeleteMapping("/members/{membershipId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID membershipId) {
        adminService.deactivateMember(membershipId);
        return ResponseEntity.noContent().build();
    }

    // ---- Ortaklıklar ----
    @GetMapping("/partnerships")
    public ResponseEntity<List<PartnershipResponse>> listPartnerships() {
        return ResponseEntity.ok(partnershipService.getPartnerships());
    }

    @PostMapping("/partnerships")
    public ResponseEntity<PartnershipResponse> createPartnership(@Valid @RequestBody CreatePartnershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnershipService.createPartnership(request));
    }

    @PatchMapping("/partnerships/{id}")
    public ResponseEntity<PartnershipResponse> updatePartnership(
            @PathVariable UUID id, @RequestBody UpdatePartnershipRequest request) {
        return ResponseEntity.ok(partnershipService.updatePartnership(id, request));
    }

    // ---- Katalog & fiyat ----
    @GetMapping("/labs/{labId}/service-items")
    public ResponseEntity<List<ServiceItemResponse>> listServiceItems(@PathVariable UUID labId) {
        return ResponseEntity.ok(adminService.listServiceItems(labId));
    }

    @PostMapping("/labs/{labId}/service-items")
    public ResponseEntity<ServiceItemResponse> createServiceItem(
            @PathVariable UUID labId, @Valid @RequestBody CreateServiceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createServiceItem(labId, request));
    }

    @PatchMapping("/labs/{labId}/service-items/{id}")
    public ResponseEntity<ServiceItemResponse> updateServiceItem(
            @PathVariable UUID labId, @PathVariable UUID id, @RequestBody UpdateServiceItemRequest request) {
        return ResponseEntity.ok(adminService.updateServiceItem(labId, id, request));
    }

    @GetMapping("/partnerships/{id}/prices")
    public ResponseEntity<List<PriceListResponse>> getPrices(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getPartnershipPrices(id));
    }

    @PutMapping("/partnerships/{id}/prices")
    public ResponseEntity<PriceListResponse> upsertPrice(
            @PathVariable UUID id, @Valid @RequestBody UpsertPriceRequest request) {
        return ResponseEntity.ok(catalogService.upsertPrice(id, request));
    }
}
