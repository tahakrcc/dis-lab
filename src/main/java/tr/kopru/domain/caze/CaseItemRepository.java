package tr.kopru.domain.caze;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseItemRepository extends JpaRepository<CaseItem, UUID> {
    List<CaseItem> findAllByDentalCaseId(UUID caseId);
}
