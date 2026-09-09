package tr.kopru.domain.caze;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CasePatientLinkRepository extends JpaRepository<CasePatientLink, UUID> {

    @Query("SELECT cpl FROM CasePatientLink cpl JOIN FETCH cpl.patient WHERE cpl.caseId = :caseId")
    Optional<CasePatientLink> findByCaseIdWithPatient(@Param("caseId") UUID caseId);
}
