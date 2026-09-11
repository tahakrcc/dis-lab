package tr.kopru.domain.caze;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.caze.dto.CaseItemInputDto;
import tr.kopru.domain.ledger.LedgerEntry;
import tr.kopru.domain.ledger.LedgerEntryRepository;
import tr.kopru.domain.ledger.LedgerType;
import tr.kopru.domain.org.Membership;
import tr.kopru.domain.org.MembershipRepository;
import tr.kopru.domain.org.RolTipi;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseStateMachine {

    private final DentalCaseRepository caseRepository;
    private final CaseEventRepository eventRepository;
    private final CaseItemRepository itemRepository;
    private final AppUserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;
    private final tr.kopru.ws.CaseWsHandler caseWsHandler;

    @Transactional
    public DentalCase transition(UUID caseId, String aksiyon, UUID actorUserId, Map<String, Object> payload) {
        AppUser actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        // teslimEt kilitlemesi SELECT ... FOR UPDATE ile yapilir
        DentalCase dentalCase;
        if ("teslimEt".equalsIgnoreCase(aksiyon)) {
            dentalCase = caseRepository.findByIdForUpdate(caseId)
                    .orElseThrow(() -> ApiException.notFound("Vaka bulunamadi."));
        } else {
            dentalCase = caseRepository.findByIdWithItems(caseId)
                    .orElseThrow(() -> ApiException.notFound("Vaka bulunamadi."));
        }

        CaseStatus currentStatus = dentalCase.getDurum();
        Set<RolTipi> actorRoles = getUserRolesInCase(dentalCase, actorUserId);

        CaseStatus nextStatus;
        EventType eventType = EventType.DURUM;
        String detayJson = "{}";

        switch (aksiyon) {
            case "gonder" -> {
                validateRoles(actorRoles, Set.of(RolTipi.HEKIM, RolTipi.KLINIK_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.TASLAK) {
                    throw ApiException.invalidTransition("gonder sadece TASLAK durumunda uygulanabilir.");
                }
                nextStatus = CaseStatus.FIYAT_MUTABAKATI;
            }

            case "karsiTeklif" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN, RolTipi.HEKIM, RolTipi.KLINIK_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.FIYAT_MUTABAKATI) {
                    throw ApiException.invalidTransition("karsiTeklif sadece FIYAT_MUTABAKATI durumunda uygulanabilir.");
                }

                // Kalemleri guncelle ve fiyati tekrar hesapla
                if (payload != null && payload.containsKey("items")) {
                    applyItemsUpdate(dentalCase, payload.get("items"));
                }

                dentalCase.setFiyatVersiyon(dentalCase.getFiyatVersiyon() + 1);
                dentalCase.setOnayKlinikVersiyon(null);
                dentalCase.setOnayLabVersiyon(null);
                dentalCase.setOnayKlinikAt(null);
                dentalCase.setOnayLabAt(null);

                nextStatus = CaseStatus.FIYAT_MUTABAKATI;
                eventType = EventType.FIYAT;
                detayJson = serializePayload(payload);
            }

            case "klinikOnayla" -> {
                validateRoles(actorRoles, Set.of(RolTipi.HEKIM, RolTipi.KLINIK_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.FIYAT_MUTABAKATI) {
                    throw ApiException.invalidTransition("klinikOnayla sadece FIYAT_MUTABAKATI durumunda uygulanabilir.");
                }

                Integer requestedVersion = extractTargetVersion(payload);
                if (requestedVersion != null && requestedVersion != dentalCase.getFiyatVersiyon()) {
                    throw ApiException.versionMismatch("Onaylanmak istenen fiyat versiyonu (" + requestedVersion +
                            ") guncel versiyon (" + dentalCase.getFiyatVersiyon() + ") ile uyusmuyor.");
                }

                dentalCase.setOnayKlinikVersiyon(dentalCase.getFiyatVersiyon());
                dentalCase.setOnayKlinikAt(OffsetDateTime.now());

                // Iki onay da ayni guncel versiyonda ise durum ONAYLANDI olur
                if (Objects.equals(dentalCase.getOnayLabVersiyon(), dentalCase.getFiyatVersiyon())) {
                    nextStatus = CaseStatus.ONAYLANDI;
                } else {
                    nextStatus = CaseStatus.FIYAT_MUTABAKATI;
                }
                eventType = EventType.ONAY;
            }

            case "labOnayla" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.FIYAT_MUTABAKATI) {
                    throw ApiException.invalidTransition("labOnayla sadece FIYAT_MUTABAKATI durumunda uygulanabilir.");
                }

                Integer requestedVersion = extractTargetVersion(payload);
                if (requestedVersion != null && requestedVersion != dentalCase.getFiyatVersiyon()) {
                    throw ApiException.versionMismatch("Onaylanmak istenen fiyat versiyonu (" + requestedVersion +
                            ") guncel versiyon (" + dentalCase.getFiyatVersiyon() + ") ile uyusmuyor.");
                }

                dentalCase.setOnayLabVersiyon(dentalCase.getFiyatVersiyon());
                dentalCase.setOnayLabAt(OffsetDateTime.now());

                if (Objects.equals(dentalCase.getOnayKlinikVersiyon(), dentalCase.getFiyatVersiyon())) {
                    nextStatus = CaseStatus.ONAYLANDI;
                } else {
                    nextStatus = CaseStatus.FIYAT_MUTABAKATI;
                }
                eventType = EventType.ONAY;
            }

            case "uretimeAl" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN, RolTipi.LAB_TEKNISYEN), aksiyon);
                if (currentStatus != CaseStatus.ONAYLANDI) {
                    throw ApiException.invalidTransition("uretimeAl sadece ONAYLANDI durumunda uygulanabilir.");
                }
                nextStatus = CaseStatus.URETIMDE;
            }

            case "provayaGonder" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN, RolTipi.LAB_TEKNISYEN), aksiyon);
                if (currentStatus != CaseStatus.URETIMDE) {
                    throw ApiException.invalidTransition("provayaGonder sadece URETIMDE durumunda uygulanabilir.");
                }
                nextStatus = CaseStatus.PROVA;
            }

            case "revizyonIste" -> {
                validateRoles(actorRoles, Set.of(RolTipi.HEKIM, RolTipi.KLINIK_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.PROVA) {
                    throw ApiException.invalidTransition("revizyonIste sadece PROVA durumunda uygulanabilir.");
                }
                dentalCase.setRevizyonSayisi(dentalCase.getRevizyonSayisi() + 1);
                nextStatus = CaseStatus.URETIMDE;
            }

            case "tamamla" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN, RolTipi.LAB_TEKNISYEN), aksiyon);
                if (currentStatus != CaseStatus.PROVA && currentStatus != CaseStatus.URETIMDE) {
                    throw ApiException.invalidTransition("tamamla sadece PROVA veya URETIMDE durumunda uygulanabilir.");
                }
                nextStatus = CaseStatus.TAMAMLANDI;
            }

            case "teslimEt" -> {
                validateRoles(actorRoles, Set.of(RolTipi.LAB_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.TAMAMLANDI) {
                    throw ApiException.invalidTransition("teslimEt sadece TAMAMLANDI durumunda uygulanabilir.");
                }

                // Borc kaydi olusturma (idempotent guard)
                createDebtLedgerEntry(dentalCase);

                nextStatus = CaseStatus.TESLIM_EDILDI;
            }

            case "iptal" -> {
                validateRoles(actorRoles, Set.of(RolTipi.KLINIK_ADMIN, RolTipi.LAB_ADMIN), aksiyon);
                if (currentStatus != CaseStatus.TASLAK &&
                        currentStatus != CaseStatus.FIYAT_MUTABAKATI &&
                        currentStatus != CaseStatus.ONAYLANDI) {
                    throw ApiException.invalidTransition("URETIMDE veya sonrasindaki vaka iptal edilemez.");
                }
                if (payload != null && payload.containsKey("iptalNeden")) {
                    dentalCase.setIptalNeden(String.valueOf(payload.get("iptalNeden")));
                }
                nextStatus = CaseStatus.IPTAL;
            }

            default -> throw ApiException.invalidTransition("Bilinmeyen aksiyon: " + aksiyon);
        }

        // Tek bir yerde durum guncellemesi
        dentalCase.setDurum(nextStatus);
        dentalCase = caseRepository.save(dentalCase);

        // Denetim izi: case_event
        CaseEvent event = new CaseEvent();
        event.setDentalCase(dentalCase);
        event.setActorUser(actor);
        event.setTur(eventType);
        event.setEskiDurum(currentStatus);
        event.setYeniDurum(nextStatus);
        event.setDetay(detayJson);
        eventRepository.save(event);

        caseWsHandler.broadcast(dentalCase.getId(),
                java.util.Map.of("type", "status", "caseId", dentalCase.getId().toString(), "durum", nextStatus.name()));

        return dentalCase;
    }

    private void createDebtLedgerEntry(DentalCase dentalCase) {
        BigDecimal matrah = dentalCase.getToplamTutar();
        BigDecimal kdvOrani = new BigDecimal("20.00");
        BigDecimal kdvTutari = matrah.multiply(kdvOrani)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal tutar = matrah.add(kdvTutari);

        LedgerEntry entry = new LedgerEntry();
        entry.setPartnership(dentalCase.getPartnership());
        entry.setDentalCase(dentalCase);
        entry.setTur(LedgerType.BORC);
        entry.setMatrah(matrah);
        entry.setKdvOrani(kdvOrani);
        entry.setKdvTutari(kdvTutari);
        entry.setTutar(tutar);
        entry.setAciklama("Vaka teslimat borcu: " + dentalCase.getKod());
        entry.setBelgeTarihi(LocalDate.now());

        try {
            ledgerEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.alreadyDelivered("Bu vaka icin teslimat ve borc kaydi zaten mevcut.");
        }
    }

    @SuppressWarnings("unchecked")
    private void applyItemsUpdate(DentalCase dentalCase, Object itemsObj) {
        try {
            List<CaseItemInputDto> inputList = objectMapper.convertValue(
                    itemsObj,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CaseItemInputDto.class)
            );

            // Mevcut kalemleri temizle ve yenilerini fiyatlandir
            dentalCase.getItems().clear();
            itemRepository.findAllByDentalCaseId(dentalCase.getId()).forEach(itemRepository::delete);

            List<CaseItem> newItems = pricingService.buildCaseItems(
                    dentalCase, inputList, LocalDate.now(), true);

            for (CaseItem ci : newItems) {
                ci.setDentalCase(dentalCase);
                dentalCase.getItems().add(ci);
            }

            BigDecimal newTotal = pricingService.calculateTotal(newItems);
            dentalCase.setToplamTutar(newTotal);

        } catch (Exception e) {
            if (e instanceof ApiException ae) {
                throw ae;
            }
            throw ApiException.validationError("Kalem verisi gecersiz: " + e.getMessage(), null);
        }
    }

    private Integer extractTargetVersion(Map<String, Object> payload) {
        if (payload != null && payload.containsKey("fiyatVersiyon")) {
            Object val = payload.get("fiyatVersiyon");
            if (val instanceof Number n) {
                return n.intValue();
            }
        }
        return null;
    }

    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Set<RolTipi> getUserRolesInCase(DentalCase dentalCase, UUID actorUserId) {
        Set<RolTipi> roles = new HashSet<>();
        List<Membership> memberships = membershipRepository.findAll();
        for (Membership m : memberships) {
            if (m.getUser().getId().equals(actorUserId)) {
                if (m.getOrganization().getId().equals(dentalCase.getPartnership().getLab().getId()) ||
                        m.getOrganization().getId().equals(dentalCase.getPartnership().getClinic().getId())) {
                    roles.add(m.getRol());
                }
            }
        }
        return roles;
    }

    private void validateRoles(Set<RolTipi> actorRoles, Set<RolTipi> allowedRoles, String aksiyon) {
        boolean authorized = actorRoles.stream().anyMatch(allowedRoles::contains);
        if (!authorized) {
            throw ApiException.forbidden("Bu aksiyon (" + aksiyon + ") icin yetkiniz bulunmamaktadir.");
        }
    }
}
