package tr.kopru.domain.ledger.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.ledger.LedgerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class LedgerEntryResponse {
    private UUID id;
    private UUID partnershipId;
    private UUID caseId;
    private LedgerType tur;
    private BigDecimal matrah;
    private BigDecimal kdvOrani;
    private BigDecimal kdvTutari;
    private BigDecimal tutar;
    private String aciklama;
    private String faturaNo;
    private String yontem;
    private LocalDate belgeTarihi;
    private OffsetDateTime createdAt;
}
