package tr.kopru.domain.message;

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
@Table(name = "message")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @Column(name = "sender_ad", nullable = false)
    private String senderAd;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sender_taraf", nullable = false, columnDefinition = "org_tipi")
    private OrgTipi senderTaraf;

    @Column(nullable = false)
    private String metin;

    @Column(name = "okundu_at")
    private OffsetDateTime okunduAt;

    @Column(name = "duzenlendi_at")
    private OffsetDateTime duzenlendiAt;

    @Column(name = "silindi_at")
    private OffsetDateTime silindiAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
