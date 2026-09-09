package tr.kopru.domain.org;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tr.kopru.domain.org.dto.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @PostMapping
    public ResponseEntity<OrgResponse> createOrganization(@Valid @RequestBody CreateOrgRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.createOrganization(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrgResponse> getOrganization(@PathVariable UUID id) {
        return ResponseEntity.ok(orgService.getOrganization(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orgService.addMember(id, request));
    }
}
