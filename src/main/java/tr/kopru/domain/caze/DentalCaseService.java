package tr.kopru.domain.caze;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.caze.dto.*;
import tr.kopru.domain.org.OrgTipi;
import tr.kopru.domain.org.Organization;
import tr.kopru.domain.org.OrganizationRepository;
import tr.kopru.domain.partnership.Partnership;
import tr.kopru.domain.partnership.PartnershipRepository;
import tr.kopru.domain.user.AppUser;
import tr.kopru.domain.user.AppUserRepository;
import tr.kopru.tenant.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DentalCaseService {

    private final DentalCaseRepository caseRepository;
    private final CaseItemRepository itemRepository;
    private final CaseEventRepository eventRepository;
    private final ShipmentRepository shipmentRepository;
    private final PatientRepository patientRepository;
    private final CasePatientLinkRepository patientLinkRepository;
    private final PartnershipRepository partnershipRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final CaseSequenceRepository caseSequenceRepository;
    private final PricingService pricingService;
    private final CaseStateMachine stateMachine;
    private final CaseLabMapper labMapper;
    private final CaseClinicMapper clinicMapper;

    @Transactional
    public Object createCase(CreateCaseRequest request) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw ApiException.unauthenticated("Oturum acilmamis.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Kullanici bulunamadi."));

        Partnership partnership = partnershipRepository.findByIdWithOrgs(request.getPartnershipId())
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        // Generate case code: {YIL}-{6 haneli sifir dolgulu}
        Long seqNo = caseSequenceRepository.nextCaseNumber(partnership.getId());
        int year = LocalDate.now().getYear();
        String kod = String.format("%d-%06d", year, seqNo);

        DentalCase dentalCase = new DentalCase();
        dentalCase.setPartnership(partnership);
        dentalCase.setKod(kod);
        dentalCase.setHastaRumuzu(request.getHastaRumuzu());
        dentalCase.setCreatedBy(user);
        dentalCase.setOlcuTipi(request.getOlcuTipi());
        dentalCase.setTeslimTarihi(request.getTeslimTarihi());
        dentalCase.setGenelNot(request.getGenelNot());
        dentalCase.setDurum(CaseStatus.TASLAK);
        dentalCase.setFiyatVersiyon(1);

        // Price snapshot calculation
        List<CaseItem> items = pricingService.buildCaseItems(
                dentalCase, request.getItems(), LocalDate.now(), false);
        BigDecimal total = pricingService.calculateTotal(items);
        dentalCase.setToplamTutar(total);

        dentalCase = caseRepository.save(dentalCase);

        for (CaseItem item : items) {
            item.setDentalCase(dentalCase);
            itemRepository.save(item);
        }
        dentalCase.setItems(items);

        // Patient privacy link (Klinik tarafi hasta secmisse)
        Patient patient = null;
        if (request.getPatientId() != null) {
            patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> ApiException.notFound("Hasta bulunamadi."));

            CasePatientLink link = new CasePatientLink();
            link.setDentalCase(dentalCase);
            link.setPatient(patient);
            link.setClinic(partnership.getClinic());
            patientLinkRepository.save(link);
        }

        return serializeCaseForCurrentOrg(dentalCase, patient);
    }

    @Transactional(readOnly = true)
    public Object getCaseById(UUID id) {
        DentalCase dentalCase = caseRepository.findByIdWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Vaka bulunamadi."));

        Optional<CasePatientLink> linkOpt = patientLinkRepository.findByCaseIdWithPatient(id);
        Patient patient = linkOpt.map(CasePatientLink::getPatient).orElse(null);

        return serializeCaseForCurrentOrg(dentalCase, patient);
    }

    @Transactional(readOnly = true)
    public List<?> getCases(UUID partnershipId, CaseStatus durum) {
        List<DentalCase> cases;
        if (partnershipId != null && durum != null) {
            cases = caseRepository.findByPartnership_IdAndDurumOrderByCreatedAtDesc(partnershipId, durum);
        } else if (partnershipId != null) {
            cases = caseRepository.findByPartnership_IdOrderByCreatedAtDesc(partnershipId);
        } else if (durum != null) {
            cases = caseRepository.findByDurumOrderByCreatedAtDesc(durum);
        } else {
            cases = caseRepository.findAllByOrderByCreatedAtDesc();
        }
        OrgTipi activeOrgType = getActiveOrgType();

        if (activeOrgType == OrgTipi.LAB) {
            return cases.stream().map(labMapper::toResponse).toList();
        } else {
            return cases.stream().map(c -> {
                Optional<CasePatientLink> linkOpt = patientLinkRepository.findByCaseIdWithPatient(c.getId());
                return clinicMapper.toResponse(c, linkOpt.map(CasePatientLink::getPatient).orElse(null));
            }).toList();
        }
    }

    @Transactional
    public Object updateItems(UUID caseId, UpdateCaseItemsRequest request) {
        DentalCase dentalCase = caseRepository.findByIdWithItems(caseId)
                .orElseThrow(() -> ApiException.notFound("Vaka bulunamadi."));

        if (dentalCase.getDurum() != CaseStatus.TASLAK && dentalCase.getDurum() != CaseStatus.FIYAT_MUTABAKATI) {
            throw ApiException.validationError("Kalemler sadece TASLAK veya FIYAT_MUTABAKATI durumlarinda duzenlenebilir.", null);
        }

        itemRepository.deleteAll(dentalCase.getItems());
        dentalCase.getItems().clear();

        List<CaseItem> newItems = pricingService.buildCaseItems(
                dentalCase, request.getItems(), LocalDate.now(), false);
        BigDecimal newTotal = pricingService.calculateTotal(newItems);

        dentalCase.setToplamTutar(newTotal);
        dentalCase.setFiyatVersiyon(dentalCase.getFiyatVersiyon() + 1);
        dentalCase.setOnayKlinikVersiyon(null);
        dentalCase.setOnayLabVersiyon(null);
        dentalCase.setOnayKlinikAt(null);
        dentalCase.setOnayLabAt(null);

        dentalCase = caseRepository.save(dentalCase);

        for (CaseItem ci : newItems) {
            ci.setDentalCase(dentalCase);
            itemRepository.save(ci);
        }
        dentalCase.setItems(newItems);

        Optional<CasePatientLink> linkOpt = patientLinkRepository.findByCaseIdWithPatient(caseId);
        return serializeCaseForCurrentOrg(dentalCase, linkOpt.map(CasePatientLink::getPatient).orElse(null));
    }

    @Transactional
    public Object transitionCase(UUID caseId, CaseTransitionRequest request) {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw ApiException.unauthenticated("Oturum acilmamis.");
        }

        DentalCase dentalCase = stateMachine.transition(caseId, request.getAksiyon(), userId, request.getPayload());
        Optional<CasePatientLink> linkOpt = patientLinkRepository.findByCaseIdWithPatient(caseId);

        return serializeCaseForCurrentOrg(dentalCase, linkOpt.map(CasePatientLink::getPatient).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<CaseEventResponse> getCaseEvents(UUID caseId) {
        return eventRepository.findAllByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(e -> CaseEventResponse.builder()
                        .id(e.getId())
                        .caseId(e.getDentalCase().getId())
                        .actorUserId(e.getActorUser().getId())
                        .actorUserAd(e.getActorUser().getAd())
                        .tur(e.getTur())
                        .eskiDurum(e.getEskiDurum())
                        .yeniDurum(e.getYeniDurum())
                        .detay(e.getDetay())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public ShipmentResponse createShipment(UUID caseId, CreateShipmentRequest request) {
        DentalCase dentalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> ApiException.notFound("Vaka bulunamadi."));

        Shipment shipment = new Shipment();
        shipment.setDentalCase(dentalCase);
        shipment.setYon(request.getYon());
        shipment.setKurye(request.getKurye());
        shipment.setTakipNo(request.getTakipNo());
        shipment.setGonderimAt(request.getGonderimAt());
        shipment = shipmentRepository.save(shipment);

        return mapToShipmentResponse(shipment);
    }

    @Transactional
    public ShipmentResponse updateShipment(UUID shipmentId, UpdateShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> ApiException.notFound("Kargo takibi bulunamadi."));

        if (request.getGonderimAt() != null) {
            shipment.setGonderimAt(request.getGonderimAt());
        }
        if (request.getTeslimAt() != null) {
            shipment.setTeslimAt(request.getTeslimAt());
        }
        shipment = shipmentRepository.save(shipment);

        return mapToShipmentResponse(shipment);
    }

    private Object serializeCaseForCurrentOrg(DentalCase dentalCase, Patient patient) {
        OrgTipi orgType = getActiveOrgType();
        if (orgType == OrgTipi.LAB) {
            return labMapper.toResponse(dentalCase);
        } else {
            return clinicMapper.toResponse(dentalCase, patient);
        }
    }

    private OrgTipi getActiveOrgType() {
        UUID activeOrgId = TenantContext.getOrgId();
        if (activeOrgId != null) {
            return organizationRepository.findById(activeOrgId)
                    .map(Organization::getTip)
                    .orElse(OrgTipi.KLINIK);
        }
        return OrgTipi.KLINIK;
    }

    private ShipmentResponse mapToShipmentResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .caseId(s.getDentalCase().getId())
                .yon(s.getYon())
                .kurye(s.getKurye())
                .takipNo(s.getTakipNo())
                .gonderimAt(s.getGonderimAt())
                .teslimAt(s.getTeslimAt())
                .build();
    }
}
