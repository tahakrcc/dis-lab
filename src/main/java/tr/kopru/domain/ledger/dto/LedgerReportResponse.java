package tr.kopru.domain.ledger.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LedgerReportResponse {
    private UUID partnershipId;
    private BigDecimal toplamBorc;
    private BigDecimal toplamTahsilat;
    private BigDecimal bakiye;
    private List<LedgerEntryResponse> movements;
}
