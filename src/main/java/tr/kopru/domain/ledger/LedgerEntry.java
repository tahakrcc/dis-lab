package tr.kopru.domain.ledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tr.kopru.domain.caze.DentalCase;
import tr.kopru.domain.partnership.Partnership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
@Getter
@Setter
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", nullable = false)
    private Partnership partnership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private DentalCase dentalCase;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "ledger_type")
    private LedgerType tur;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal matrah;

    @Column(name = "kdv_orani", nullable = false, precision = 5, scale = 2)
    private BigDecimal kdvOrani = new BigDecimal("20.00");

    @Column(name = "kdv_tutari", nullable = false, precision = 12, scale = 2)
    private BigDecimal kdvTutari;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tutar;

    private String aciklama;

    @Column(name = "fatura_no")
    private String faturaNo;

    private String yontem;

    @Column(name = "belge_tarihi", nullable = false)
    private LocalDate belgeTarihi;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
