package tr.kopru.domain.caze.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class CaseItemResponse {
    private UUID id;
    private UUID serviceItemId;
    private String serviceItemAd;
    private int[] disNumaralari;
    private String materyal;
    private String renk;
    private int adet;
    private BigDecimal birimFiyat;
    private BigDecimal araToplam;
    private String specs;
}
