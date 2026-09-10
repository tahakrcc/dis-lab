package tr.kopru.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrgRequest {
    private String ad;
    private String telefon;
    private String vergiNo;
    private String adres;
}
