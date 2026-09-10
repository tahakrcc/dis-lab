package tr.kopru.domain.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.catalog.dto.*;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.domain.partnership.Partnership;
import tr.kopru.domain.partnership.PartnershipRepository;
import tr.kopru.tenant.TenantContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ServiceItemRepository serviceItemRepository;
    private final PriceListEntryRepository priceListEntryRepository;
    private final OrganizationRepository organizationRepository;
    private final PartnershipRepository partnershipRepository;

    @Transactional
    public ServiceItemResponse createServiceItem(CreateServiceItemRequest request) {
        UUID activeOrgId = TenantContext.getOrgId();
        if (activeOrgId == null) {
            throw ApiException.validationError("Aktif organizasyon secilmelidir (X-Org-Id).", null);
        }

        Organization org = organizationRepository.findById(activeOrgId)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));

        if (org.getTip() != OrgTipi.LAB) {
            throw ApiException.forbidden("Katalog kalemi sadece LAB organizasyonlari tarafindan olusturulabilir.");
        }

        if (serviceItemRepository.existsByLabIdAndAd(activeOrgId, request.getAd())) {
            throw ApiException.validationError("Bu isimde bir katalog kalemi zaten mevcut.", null);
        }

        ServiceItem item = new ServiceItem();
        item.setLab(org);
        item.setAd(request.getAd());
        item.setBirim(request.getBirim());
        item.setAktif(true);
        item = serviceItemRepository.save(item);

        return mapToServiceItemResponse(item);
    }

    @Transactional
    public ServiceItemResponse updateServiceItem(UUID id, UpdateServiceItemRequest request) {
        UUID activeOrgId = TenantContext.getOrgId();
        ServiceItem item = serviceItemRepository.findByIdAndLabId(id, activeOrgId)
                .orElseThrow(() -> ApiException.notFound("Hizmet kalemi bulunamadi veya yetkiniz yok."));

        if (request.getAd() != null) {
            item.setAd(request.getAd());
        }
        if (request.getAktif() != null) {
            item.setAktif(request.getAktif());
        }
        item = serviceItemRepository.save(item);

        return mapToServiceItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ServiceItemResponse> getServiceItems() {
        UUID activeOrgId = TenantContext.getOrgId();
        if (activeOrgId == null) {
            return serviceItemRepository.findAll().stream().map(this::mapToServiceItemResponse).toList();
        }

        Organization org = organizationRepository.findById(activeOrgId)
                .orElseThrow(() -> ApiException.notFound("Organizasyon bulunamadi."));

        if (org.getTip() == OrgTipi.LAB) {
            return serviceItemRepository.findAllByLabId(activeOrgId).stream()
                    .map(this::mapToServiceItemResponse)
                    .toList();
        } else {
            // Klinik: Ortak olunan lab'larin kataloglari
            List<Partnership> partnerships = partnershipRepository.findAllByOrgId(activeOrgId);
            List<ServiceItem> items = new ArrayList<>();
            for (Partnership p : partnerships) {
                items.addAll(serviceItemRepository.findAllByLabId(p.getLab().getId()));
            }
            return items.stream().map(this::mapToServiceItemResponse).toList();
        }
    }

    @Transactional(readOnly = true)
    public List<PriceListResponse> getPartnershipPrices(UUID partnershipId) {
        return priceListEntryRepository.findAllActiveByPartnershipId(partnershipId).stream()
                .map(this::mapToPriceResponse)
                .toList();
    }

    @Transactional
    public PriceListResponse upsertPrice(UUID partnershipId, UpsertPriceRequest request) {
        Partnership partnership = partnershipRepository.findByIdWithOrgs(partnershipId)
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        ServiceItem item = serviceItemRepository.findById(request.getServiceItemId())
                .orElseThrow(() -> ApiException.notFound("Katalog kalemi bulunamadi."));

        if (!item.getLab().getId().equals(partnership.getLab().getId())) {
            throw ApiException.validationError("Katalog kalemi ortakliktaki laboratuvara ait degil.", null);
        }

        // Kural: mevcut aktif kaydin gecerli_bitis'ini kapatip YENI kayit acar, UPDATE etmez!
        List<PriceListEntry> overlaps = priceListEntryRepository.findOverlappingActiveEntries(
                partnershipId, item.getId(), request.getGecerliBaslangic());

        for (PriceListEntry existing : overlaps) {
            if (existing.getGecerliBaslangic().isBefore(request.getGecerliBaslangic())) {
                existing.setGecerliBitis(request.getGecerliBaslangic());
                priceListEntryRepository.save(existing);
            } else {
                // Eger ayni gun veya sonra baslayan varsa, pasife aliyoruz
                existing.setAktif(false);
                priceListEntryRepository.save(existing);
            }
        }
        // Eski kayitlarin UPDATE'i, yeni kaydin INSERT'inden ONCE veritabanina gitmeli;
        // aksi halde Hibernate INSERT'i UPDATE'ten once calistirir ve iki aktif kayit
        // ayni tarih araliginda cakisip exclusion constraint'i (23P01) ihlal eder.
        if (!overlaps.isEmpty()) {
            priceListEntryRepository.flush();
        }

        PriceListEntry newEntry = new PriceListEntry();
        newEntry.setPartnership(partnership);
        newEntry.setServiceItem(item);
        newEntry.setFiyat(request.getFiyat());
        newEntry.setGecerliBaslangic(request.getGecerliBaslangic());
        newEntry.setAktif(true);
        newEntry = priceListEntryRepository.save(newEntry);

        return mapToPriceResponse(newEntry);
    }

    private ServiceItemResponse mapToServiceItemResponse(ServiceItem s) {
        return ServiceItemResponse.builder()
                .id(s.getId())
                .labId(s.getLab().getId())
                .ad(s.getAd())
                .birim(s.getBirim())
                .aktif(s.isAktif())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private PriceListResponse mapToPriceResponse(PriceListEntry p) {
        return PriceListResponse.builder()
                .id(p.getId())
                .partnershipId(p.getPartnership().getId())
                .serviceItemId(p.getServiceItem().getId())
                .serviceItemAd(p.getServiceItem().getAd())
                .serviceItemBirim(p.getServiceItem().getBirim())
                .fiyat(p.getFiyat())
                .gecerliBaslangic(p.getGecerliBaslangic())
                .gecerliBitis(p.getGecerliBitis())
                .aktif(p.isAktif())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
