package tr.kopru.domain.caze;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tr.kopru.domain.catalog.ServiceItem;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "case_item")
@Getter
@Setter
public class CaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private DentalCase dentalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_item_id", nullable = false)
    private ServiceItem serviceItem;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dis_numaralari", nullable = false, columnDefinition = "int[]")
    private int[] disNumaralari = new int[0];

    private String materyal;

    private String renk;

    @Column(nullable = false)
    private int adet;

    @Column(name = "birim_fiyat", nullable = false, precision = 12, scale = 2)
    private BigDecimal birimFiyat;

    @Column(name = "ara_toplam", nullable = false, precision = 12, scale = 2)
    private BigDecimal araToplam;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String specs = "{}";
}
