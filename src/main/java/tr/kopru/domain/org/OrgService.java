package tr.kopru.domain.org;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.common.Aktiflik;
import tr.kopru.domain.org.dto.*;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;
import tr.kopru.tenant.TenantContext;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public OrgResponse createOrganization(CreateOrgRequest request) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw ApiException.unauthenticated("Oturum acilmamis.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        Organization org = new Organization();
        org.setTip(request.getTip());
        org.setAd(request.getAd());
        org.setTelefon(request.getTelefon());
        org.setVergiNo(request.getVergiNo());
        org.setAdres(request.getAdres());
        if (request.getAyarlar() != null) {
            org.setAyarlar(request.getAyarlar());
        }
        org = organizationRepository.save(org);

        // Creator automatically becomes admin
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setOrganization(org);
        membership.setRol(request.getTip() == OrgTipi.LAB ? RolTipi.LAB_ADMIN : RolTipi.KLINIK_ADMIN);
        membership.setDurum(Aktiflik.AKTIF);
        membershipRepository.save(membership);

        return mapToResponse(org);
    }

    @Transactional(readOnly = true)
    public OrgResponse getOrganization(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));
        return mapToResponse(org);
    }

    @Transactional
    public MemberResponse addMember(UUID orgId, AddMemberRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));

        // Validate role against org type
        if (org.getTip() == OrgTipi.LAB && request.getRol() != RolTipi.LAB_ADMIN && request.getRol() != RolTipi.LAB_TEKNISYEN) {
            throw ApiException.validationError("LAB organizasyonuna sadece LAB_ADMIN veya LAB_TEKNISYEN atanabilir.", null);
        }
        if (org.getTip() == OrgTipi.KLINIK && (request.getRol() == RolTipi.LAB_ADMIN || request.getRol() == RolTipi.LAB_TEKNISYEN)) {
            throw ApiException.validationError("KLINIK organizasyonuna LAB rolleri atanamaz.", null);
        }

        // Hedef kullanici henuz uye olmadigindan RLS altinda gorunmez -> definer lookup ile id coz
        java.util.List<UUID> ids = userRepository.findUserIdByUsername(request.getKullaniciAdi().toLowerCase().trim());
        if (ids.isEmpty()) {
            throw ApiException.notFound("Bu kullanici adina sahip kullanici bulunamadi.");
        }
        UUID targetUserId = ids.get(0);

        if (membershipRepository.findByUserIdAndOrganizationId(targetUserId, orgId).isPresent()) {
            throw ApiException.validationError("Bu kullanici zaten organizasyonun uyesidir.", null);
        }

        Membership membership = new Membership();
        membership.setUser(userRepository.getReferenceById(targetUserId));
        membership.setOrganization(org);
        membership.setRol(request.getRol());
        membership.setDurum(Aktiflik.AKTIF);
        // saveAndFlush -> kullanici artik admin ile ayni org'u paylastigi icin sonrasinda gorunur
        membership = membershipRepository.saveAndFlush(membership);

        AppUser user = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        return MemberResponse.builder()
                .id(membership.getId())
                .userId(user.getId())
                .userAd(user.getAd())
                .userEmail(user.getEmail())
                .orgId(org.getId())
                .rol(membership.getRol())
                .durum(membership.getDurum())
                .createdAt(membership.getCreatedAt())
                .build();
    }

    private OrgResponse mapToResponse(Organization org) {
        return OrgResponse.builder()
                .id(org.getId())
                .tip(org.getTip())
                .ad(org.getAd())
                .telefon(org.getTelefon())
                .vergiNo(org.getVergiNo())
                .adres(org.getAdres())
                .ayarlar(org.getAyarlar())
                .createdAt(org.getCreatedAt())
                .build();
    }
}
