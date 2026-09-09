package tr.kopru.domain.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.ledger.dto.*;
import tr.kopru.domain.partnership.Partnership;
import tr.kopru.domain.partnership.PartnershipRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final PartnershipRepository partnershipRepository;

    @Transactional(readOnly = true)
    public LedgerReportResponse getLedgerReport(UUID partnershipId, LocalDate from, LocalDate to) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        List<LedgerEntry> entries = ledgerEntryRepository.findByPartnershipIdAndDateRange(partnershipId, from, to);
        List<LedgerEntryResponse> movementResponses = entries.stream()
                .map(this::mapToResponse)
                .toList();

        BigDecimal toplamBorc = BigDecimal.ZERO;
        BigDecimal toplamTahsilat = BigDecimal.ZERO;
        BigDecimal bakiye = BigDecimal.ZERO;

        Optional<Object[]> balanceRow = ledgerEntryRepository.getBalanceNative(partnershipId);
        if (balanceRow.isPresent()) {
            Object[] row = balanceRow.get();
            if (row[1] != null) toplamBorc = new BigDecimal(row[1].toString());
            if (row[2] != null) toplamTahsilat = new BigDecimal(row[2].toString());
            if (row[3] != null) bakiye = new BigDecimal(row[3].toString());
        }

        return LedgerReportResponse.builder()
                .partnershipId(partnership.getId())
                .toplamBorc(toplamBorc)
                .toplamTahsilat(toplamTahsilat)
                .bakiye(bakiye)
                .movements(movementResponses)
                .build();
    }

    @Transactional
    public LedgerEntryResponse createPayment(UUID partnershipId, CreatePaymentRequest request) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        // TAHSILAT kurali: tutar < 0 (PostgreSQL CHECK constraint)
        BigDecimal negatifTutar = request.getTutar().abs().negate();

        LedgerEntry entry = new LedgerEntry();
        entry.setPartnership(partnership);
        entry.setTur(LedgerType.TAHSILAT);
        entry.setMatrah(BigDecimal.ZERO);
        entry.setKdvOrani(BigDecimal.ZERO);
        entry.setKdvTutari(BigDecimal.ZERO);
        entry.setTutar(negatifTutar);
        entry.setYontem(request.getYontem());
        entry.setAciklama(request.getAciklama());
        entry.setBelgeTarihi(request.getBelgeTarihi());
        entry = ledgerEntryRepository.save(entry);

        return mapToResponse(entry);
    }

    @Transactional
    public LedgerEntryResponse createAdjustment(UUID partnershipId, CreateAdjustmentRequest request) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> ApiException.notFound("Ortaklik bulunamadi."));

        LedgerEntry entry = new LedgerEntry();
        entry.setPartnership(partnership);
        entry.setTur(LedgerType.DUZELTME);
        entry.setMatrah(request.getTutar());
        entry.setKdvOrani(BigDecimal.ZERO);
        entry.setKdvTutari(BigDecimal.ZERO);
        entry.setTutar(request.getTutar());
        entry.setAciklama(request.getAciklama());
        entry.setBelgeTarihi(LocalDate.now());
        entry = ledgerEntryRepository.save(entry);

        return mapToResponse(entry);
    }

    private LedgerEntryResponse mapToResponse(LedgerEntry l) {
        return LedgerEntryResponse.builder()
                .id(l.getId())
                .partnershipId(l.getPartnership().getId())
                .caseId(l.getDentalCase() != null ? l.getDentalCase().getId() : null)
                .tur(l.getTur())
                .matrah(l.getMatrah())
                .kdvOrani(l.getKdvOrani())
                .kdvTutari(l.getKdvTutari())
                .tutar(l.getTutar())
                .aciklama(l.getAciklama())
                .faturaNo(l.getFaturaNo())
                .yontem(l.getYontem())
                .belgeTarihi(l.getBelgeTarihi())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
