package tr.kopru.domain.caze;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tr.kopru.domain.org.Organization;

import java.util.UUID;

@Entity
@Table(name = "case_patient_link")
@Getter
@Setter
public class CasePatientLink {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "case_id")
    private DentalCase dentalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Organization clinic;
}
