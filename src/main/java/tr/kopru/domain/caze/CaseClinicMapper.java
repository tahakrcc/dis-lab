package tr.kopru.domain.caze;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tr.kopru.domain.caze.dto.CaseClinicResponse;
import tr.kopru.domain.caze.dto.CaseItemResponse;

@Mapper(componentModel = "spring")
public interface CaseClinicMapper {

    @Mapping(target = "partnershipId", source = "dentalCase.partnership.id")
    @Mapping(target = "createdBy", source = "dentalCase.createdBy.id")
    @Mapping(target = "assignedTo", source = "dentalCase.assignedTo.id")
    @Mapping(target = "items", source = "dentalCase.items")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientAd", source = "patient.ad")
    @Mapping(target = "id", source = "dentalCase.id")
    @Mapping(target = "kod", source = "dentalCase.kod")
    @Mapping(target = "hastaRumuzu", source = "dentalCase.hastaRumuzu")
    @Mapping(target = "olcuTipi", source = "dentalCase.olcuTipi")
    @Mapping(target = "durum", source = "dentalCase.durum")
    @Mapping(target = "teslimTarihi", source = "dentalCase.teslimTarihi")
    @Mapping(target = "toplamTutar", source = "dentalCase.toplamTutar")
    @Mapping(target = "fiyatVersiyon", source = "dentalCase.fiyatVersiyon")
    @Mapping(target = "onayKlinikVersiyon", source = "dentalCase.onayKlinikVersiyon")
    @Mapping(target = "onayLabVersiyon", source = "dentalCase.onayLabVersiyon")
    @Mapping(target = "onayKlinikAt", source = "dentalCase.onayKlinikAt")
    @Mapping(target = "onayLabAt", source = "dentalCase.onayLabAt")
    @Mapping(target = "revizyonSayisi", source = "dentalCase.revizyonSayisi")
    @Mapping(target = "iptalNeden", source = "dentalCase.iptalNeden")
    @Mapping(target = "genelNot", source = "dentalCase.genelNot")
    @Mapping(target = "createdAt", source = "dentalCase.createdAt")
    @Mapping(target = "updatedAt", source = "dentalCase.updatedAt")
    CaseClinicResponse toResponse(DentalCase dentalCase, Patient patient);

    @Mapping(target = "serviceItemId", source = "serviceItem.id")
    @Mapping(target = "serviceItemAd", source = "serviceItem.ad")
    CaseItemResponse toItemResponse(CaseItem item);
}
