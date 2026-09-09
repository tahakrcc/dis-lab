package tr.kopru.domain.caze;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tr.kopru.domain.caze.dto.CaseItemResponse;
import tr.kopru.domain.caze.dto.CaseLabResponse;

@Mapper(componentModel = "spring")
public interface CaseLabMapper {

    @Mapping(target = "partnershipId", source = "partnership.id")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "assignedTo", source = "assignedTo.id")
    @Mapping(target = "items", source = "items")
    CaseLabResponse toResponse(DentalCase dentalCase);

    @Mapping(target = "serviceItemId", source = "serviceItem.id")
    @Mapping(target = "serviceItemAd", source = "serviceItem.ad")
    CaseItemResponse toItemResponse(CaseItem item);
}
