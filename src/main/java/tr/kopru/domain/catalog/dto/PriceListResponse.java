package tr.kopru.domain.catalog.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.catalog.BirimTipi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class PriceListResponse {
    private UUID id;
    private UUID partnershipId;
    private UUID serviceItemId;
    private String serviceItemAd;
    private BirimTipi serviceItemBirim;
    private BigDecimal fiyat;
    private LocalDate gecerliBaslangic;
    private LocalDate gecerliBitis;
    private boolean aktif;
    private OffsetDateTime createdAt;
}
