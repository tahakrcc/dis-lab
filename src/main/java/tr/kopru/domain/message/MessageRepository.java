package tr.kopru.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByCaseIdOrderByCreatedAtAsc(UUID caseId);

    /** Karşı tarafın (gönderen != okuyan) henüz okunmamış mesajlarını okundu işaretle. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.okunduAt = CURRENT_TIMESTAMP " +
            "WHERE m.caseId = :caseId AND m.senderUserId <> :userId AND m.okunduAt IS NULL")
    int markOthersRead(@Param("caseId") UUID caseId, @Param("userId") UUID userId);
}
