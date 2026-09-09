package tr.kopru.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
    List<ServiceItem> findAllByLabId(UUID labId);
    Optional<ServiceItem> findByIdAndLabId(UUID id, UUID labId);
    boolean existsByLabIdAndAd(UUID labId, String ad);
}
