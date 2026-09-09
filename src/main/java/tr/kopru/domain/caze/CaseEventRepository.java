package tr.kopru.domain.caze;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEvent, UUID> {

    @Query("SELECT e FROM CaseEvent e JOIN FETCH e.actorUser WHERE e.dentalCase.id = :caseId ORDER BY e.createdAt DESC")
    List<CaseEvent> findAllByCaseIdOrderByCreatedAtDesc(@Param("caseId") UUID caseId);
}
