package tr.kopru.domain.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateAdjustmentRequest {
    @NotNull(message = "tutar zorunludur")
    private BigDecimal tutar; // Pozitif veya negatif olabilir

    private String aciklama;
}
