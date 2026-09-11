package tr.kopru.domain.attachment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tr.kopru.domain.org.OrgTipi;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachment")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "uploader_user_id", nullable = false)
    private UUID uploaderUserId;

    @Column(name = "uploader_ad", nullable = false)
    private String uploaderAd;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "uploader_taraf", nullable = false, columnDefinition = "org_tipi")
    private OrgTipi uploaderTaraf;

    @Column(name = "dosya_adi", nullable = false)
    private String dosyaAdi;

    private String mime;

    @Column(nullable = false)
    private Long boyut;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "icerik", nullable = false, columnDefinition = "bytea")
    private byte[] icerik;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
