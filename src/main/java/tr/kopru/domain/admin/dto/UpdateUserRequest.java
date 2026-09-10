package tr.kopru.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String ad;
    private String telefon;
    /** Doldurulursa parola sıfırlanır; boş/null ise değişmez. */
    private String parola;
}
