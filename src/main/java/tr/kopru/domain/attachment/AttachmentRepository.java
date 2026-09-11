package tr.kopru.domain.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tr.kopru.domain.attachment.dto.AttachmentResponse;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    // Listede icerik (bytea) yüklenmez — sadece metadata.
    @Query("SELECT new tr.kopru.domain.attachment.dto.AttachmentResponse(" +
            "a.id, a.caseId, a.uploaderUserId, a.uploaderAd, a.uploaderTaraf, " +
            "a.dosyaAdi, a.mime, a.boyut, a.createdAt) " +
            "FROM Attachment a WHERE a.caseId = :caseId ORDER BY a.createdAt ASC")
    List<AttachmentResponse> listByCaseId(@Param("caseId") UUID caseId);
}
