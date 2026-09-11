package tr.kopru.domain.message.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.org.OrgTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class MessageResponse {
    private UUID id;
    private UUID caseId;
    private UUID senderUserId;
    private String senderAd;
    private OrgTipi senderTaraf;
    private String metin;
    private OffsetDateTime okunduAt;
    private OffsetDateTime createdAt;
}
