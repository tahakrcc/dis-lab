package tr.kopru.domain.attachment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tr.kopru.domain.org.OrgTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AttachmentResponse {
    private UUID id;
    private UUID caseId;
    private UUID uploaderUserId;
    private String uploaderAd;
    private OrgTipi uploaderTaraf;
    private String dosyaAdi;
    private String mime;
    private Long boyut;
    private OffsetDateTime createdAt;
}
