package tr.kopru.domain.caze.dto;

import lombok.Builder;
import lombok.Getter;
import tr.kopru.domain.caze.CaseStatus;
import tr.kopru.domain.caze.MeasureType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CaseClinicResponse {
    private UUID id;
    private UUID partnershipId;
    private String kod;
    private String hastaRumuzu;
    private UUID patientId; // Klinik hasta bilgisini gorur
    private String patientAd;
    private UUID createdBy;
    private UUID assignedTo;
    private MeasureType olcuTipi;
    private CaseStatus durum;
    private LocalDate teslimTarihi;
    private BigDecimal toplamTutar;
    private int fiyatVersiyon;
    private Integer onayKlinikVersiyon;
    private Integer onayLabVersiyon;
    private OffsetDateTime onayKlinikAt;
    private OffsetDateTime onayLabAt;
    private int revizyonSayisi;
    private String iptalNeden;
    private String genelNot;
    private List<CaseItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
