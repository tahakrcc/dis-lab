package tr.kopru.domain.caze.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.caze.CaseStatus;
import tr.kopru.domain.caze.EventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class CaseEventResponse {
    private UUID id;
    private UUID caseId;
    private UUID actorUserId;
    private String actorUserAd;
    private EventType tur;
    private CaseStatus eskiDurum;
    private CaseStatus yeniDurum;
    private String detay;
    private OffsetDateTime createdAt;
}
