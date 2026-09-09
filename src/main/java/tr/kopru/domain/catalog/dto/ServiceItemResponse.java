package tr.kopru.domain.catalog.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.catalog.BirimTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ServiceItemResponse {
    private UUID id;
    private UUID labId;
    private String ad;
    private BirimTipi birim;
    private boolean aktif;
    private OffsetDateTime createdAt;
}
