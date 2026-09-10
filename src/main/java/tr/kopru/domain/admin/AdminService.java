package tr.kopru.domain.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.Aktiflik;
import tr.kopru.common.ApiException;
import tr.kopru.domain.admin.dto.AdminUserResponse;
import tr.kopru.domain.admin.dto.CreateUserRequest;
import tr.kopru.domain.catalog.ServiceItem;
import tr.kopru.domain.catalog.ServiceItemRepository;
import tr.kopru.domain.catalog.dto.CreateServiceItemRequest;
import tr.kopru.domain.catalog.dto.ServiceItemResponse;
import tr.kopru.domain.org.*;
import tr.kopru.domain.org.dto.CreateOrgRequest;
import tr.kopru.domain.org.dto.MemberResponse;
import tr.kopru.domain.org.dto.OrgResponse;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final PasswordEncoder passwordEncoder;

    // ---- Kullanıcılar ----

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::mapUser).toList();
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        AppUser user = new AppUser();
        user.setKullaniciAdi(request.getKullaniciAdi().toLowerCase().trim());
        user.setAd(request.getAd());
        user.setEmail(request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().toLowerCase().trim() : null);
        user.setParolaHash(passwordEncoder.encode(request.getParola()));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.validationError("Bu kullanici adi veya email zaten kayitli.", null);
        }
        return mapUser(user);
    }

    // ---- Organizasyonlar ----

    @Transactional(readOnly = true)
    public List<OrgResponse> listOrganizations() {
        return organizationRepository.findAll().stream().map(this::mapOrg).toList();
    }

    @Transactional
    public OrgResponse createOrganization(CreateOrgRequest request) {
        Organization org = new Organization();
        org.setTip(request.getTip());
        org.setAd(request.getAd());
        org.setTelefon(request.getTelefon());
        org.setVergiNo(request.getVergiNo());
        org.setAdres(request.getAdres());
        if (request.getAyarlar() != null && !request.getAyarlar().isBlank()) {
            org.setAyarlar(request.getAyarlar());
        }
        org = organizationRepository.save(org);
        return mapOrg(org);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID orgId) {
        return membershipRepository.findByOrganizationIdAndDurum(orgId, Aktiflik.AKTIF).stream()
                .map(this::mapMember)
                .toList();
    }

    // ---- Katalog (lab bazlı) ----

    @Transactional(readOnly = true)
    public List<ServiceItemResponse> listServiceItems(UUID labId) {
        return serviceItemRepository.findAllByLabId(labId).stream().map(this::mapServiceItem).toList();
    }

    @Transactional
    public ServiceItemResponse createServiceItem(UUID labId, CreateServiceItemRequest request) {
        Organization lab = organizationRepository.findById(labId)
                .orElseThrow(() -> ApiException.notFound("Laboratuvar bulunamadi."));
        if (lab.getTip() != OrgTipi.LAB) {
            throw ApiException.validationError("Katalog kalemi yalnizca LAB icin eklenebilir.", null);
        }
        if (serviceItemRepository.existsByLabIdAndAd(labId, request.getAd())) {
            throw ApiException.validationError("Bu isimde bir katalog kalemi zaten mevcut.", null);
        }
        ServiceItem item = new ServiceItem();
        item.setLab(lab);
        item.setAd(request.getAd());
        item.setBirim(request.getBirim());
        item.setAktif(true);
        item = serviceItemRepository.save(item);
        return mapServiceItem(item);
    }

    // ---- mappers ----

    private AdminUserResponse mapUser(AppUser u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .kullaniciAdi(u.getKullaniciAdi())
                .ad(u.getAd())
                .email(u.getEmail())
                .superAdmin(u.isSuperAdmin())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private OrgResponse mapOrg(Organization o) {
        return OrgResponse.builder()
                .id(o.getId())
                .tip(o.getTip())
                .ad(o.getAd())
                .telefon(o.getTelefon())
                .vergiNo(o.getVergiNo())
                .adres(o.getAdres())
                .ayarlar(o.getAyarlar())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private MemberResponse mapMember(Membership m) {
        return MemberResponse.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .userAd(m.getUser().getAd())
                .userEmail(m.getUser().getEmail())
                .orgId(m.getOrganization().getId())
                .rol(m.getRol())
                .durum(m.getDurum())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ServiceItemResponse mapServiceItem(ServiceItem s) {
        return ServiceItemResponse.builder()
                .id(s.getId())
                .labId(s.getLab().getId())
                .ad(s.getAd())
                .birim(s.getBirim())
                .aktif(s.isAktif())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
