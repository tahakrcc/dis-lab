package tr.kopru.domain.caze;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.partnership.Partnership;

import java.util.UUID;

@Entity
@Table(name = "case_sequence")
@Getter
@Setter
public class CaseSequence {

    @Id
    @Column(name = "partnership_id")
    private UUID partnershipId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;

    @Column(name = "son_no", nullable = false)
    private long sonNo = 0;
}
