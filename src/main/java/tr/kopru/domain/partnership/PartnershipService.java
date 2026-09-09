package tr.kopru.domain.partnership;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.domain.partnership.dto.*;
import tr.kopru.tenant.TenantContext;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public PartnershipResponse createPartnership(CreatePartnershipRequest request) {
        Organization lab = organizationRepository.findById(request.getLabId())
                .orElseThrow(() -> ApiException.notFound("Laboratuvar organizasyonu bulunamadi."));
        Organization clinic = organizationRepository.findById(request.getClinicId())
                .orElseThrow(() -> ApiException.notFound("Klinik organizasyonu bulunamadi."));

        if (lab.getTip() != OrgTipi.LAB) {
            throw ApiException.validationError("labId bir LAB organizasyonuna ait olmalidir.", null);
        }
        if (clinic.getTip() != OrgTipi.KLINIK) {
            throw ApiException.validationError("clinicId bir KLINIK organizasyonuna ait olmalidir.", null);
        }

        if (partnershipRepository.findByLabIdAndClinicId(lab.getId(), clinic.getId()).isPresent()) {
            throw ApiException.validationError("Bu ortaklik zaten mevcut.", null);
        }

        Partnership partnership = new Partnership();
        partnership.setLab(lab);
        partnership.setClinic(clinic);
        if (request.getVadeGun() != null) {
            partnership.setVadeGun(request.getVadeGun());
        }
        partnership = partnershipRepository.save(partnership);

        return mapToResponse(partnership);
    }

    @Transactional(readOnly = true)
    public List<PartnershipResponse> getPartnerships() {
        UUID activeOrgId = TenantContext.getOrgId();
        List<Partnership> list;
        if (activeOrgId != null) {
            list = partnershipRepository.findAllByOrgId(activeOrgId);
        } else {
            list = partnershipRepository.findAll();
        }
        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public PartnershipResponse updatePartnership(UUID id, UpdatePartnershipRequest request) {
        Partnership partnership = partnershipRepository.findByIdWithOrgs(id)
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        if (request.getDurum() != null) {
            partnership.setDurum(request.getDurum());
        }
        if (request.getVadeGun() != null) {
            partnership.setVadeGun(request.getVadeGun());
        }

        partnership = partnershipRepository.save(partnership);
        return mapToResponse(partnership);
    }

    private PartnershipResponse mapToResponse(Partnership p) {
        return PartnershipResponse.builder()
                .id(p.getId())
                .labId(p.getLab().getId())
                .labAd(p.getLab().getAd())
                .clinicId(p.getClinic().getId())
                .clinicAd(p.getClinic().getAd())
                .durum(p.getDurum())
                .vadeGun(p.getVadeGun())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
