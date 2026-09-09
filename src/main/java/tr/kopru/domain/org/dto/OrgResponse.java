package tr.kopru.domain.org.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.org.OrgTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class OrgResponse {
    private UUID id;
    private OrgTipi tip;
    private String ad;
    private String telefon;
    private String vergiNo;
    private String adres;
    private String ayarlar;
    private OffsetDateTime createdAt;
}
