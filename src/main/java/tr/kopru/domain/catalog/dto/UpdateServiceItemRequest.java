package tr.kopru.domain.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateServiceItemRequest {
    private String ad;
    private Boolean aktif;
}
