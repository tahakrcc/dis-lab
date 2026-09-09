package tr.kopru.domain.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class UpsertPriceRequest {
    @NotNull(message = "serviceItemId zorunludur")
    private UUID serviceItemId;

    @NotNull(message = "fiyat zorunludur")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fiyat negatif olamaz")
    private BigDecimal fiyat;

    @NotNull(message = "gecerliBaslangic zorunludur")
    private LocalDate gecerliBaslangic;
}
