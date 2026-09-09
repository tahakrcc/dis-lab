package tr.kopru.domain.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePaymentRequest {
    @NotNull(message = "tutar zorunludur")
    @DecimalMin(value = "0.01", message = "Tutar 0 dan buyuk olmalidir")
    private BigDecimal tutar;

    private String yontem; // Havale, EFT, Nakit, Kredi Karti vs.

    @NotNull(message = "belgeTarihi zorunludur")
    private LocalDate belgeTarihi;

    private String aciklama;
}
