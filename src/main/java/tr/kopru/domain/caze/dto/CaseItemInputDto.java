package tr.kopru.domain.caze.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CaseItemInputDto {
    @NotNull(message = "serviceItemId zorunludur")
    private UUID serviceItemId;

    private int[] disNumaralari = new int[0];
    private String materyal;
    private String renk;

    @Min(value = 1, message = "Adet en az 1 olmalidir")
    private int adet = 1;

    private BigDecimal customBirimFiyat; // Opsiyonel (karsi teklif icin)
    private String specs;
}
