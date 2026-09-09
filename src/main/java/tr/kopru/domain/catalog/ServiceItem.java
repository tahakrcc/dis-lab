package tr.kopru.domain.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tr.kopru.domain.org.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lab_id", "ad"})
})
@Getter
@Setter
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false)
    private Organization lab;

    @Column(nullable = false)
    private String ad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "birim_tipi")
    private BirimTipi birim;

    @Column(nullable = false)
    private boolean aktif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
