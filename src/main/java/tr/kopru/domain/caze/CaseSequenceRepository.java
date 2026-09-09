package tr.kopru.domain.caze;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CaseSequenceRepository extends JpaRepository<CaseSequence, UUID> {

    @Query(value = "INSERT INTO case_sequence (partnership_id, son_no) VALUES (:pId, 1) " +
            "ON CONFLICT (partnership_id) DO UPDATE SET son_no = case_sequence.son_no + 1 RETURNING son_no",
            nativeQuery = true)
    Long nextCaseNumber(@Param("pId") UUID partnershipId);
}
