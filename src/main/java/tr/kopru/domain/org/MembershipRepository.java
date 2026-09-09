package tr.kopru.domain.org;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tr.kopru.common.Aktiflik;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.user.id = :userId AND m.durum = :durum")
    List<Membership> findByUserIdAndDurumWithOrg(@Param("userId") UUID userId, @Param("durum") Aktiflik durum);

    Optional<Membership> findByUserIdAndOrganizationIdAndDurum(UUID userId, UUID orgId, Aktiflik durum);

    Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID orgId);

    List<Membership> findByOrganizationIdAndDurum(UUID orgId, Aktiflik durum);
}
