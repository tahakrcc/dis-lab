package tr.kopru.domain.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Panel (dashboard) ozet metrikleri. Tum degerler aktif organizasyonun (X-Org-Id)
 * ortakliklarina gore ve RLS altinda hesaplanir.
 */
@Builder
public record AnalyticsSummaryResponse(
        long toplamVaka,
        long devamEden,
        long teslimEdilen,
        long geciken,
        BigDecimal toplamCiro,
        BigDecimal toplamTahsilat,
        BigDecimal acikBakiye,
        Double ortalamaUretimGun,
        long zamanindaTeslim,
        long gecTeslim,
        long revizyonluVaka,
        Double revizyonOrani,
        List<DurumSayi> durumDagilimi,
        List<AySayi> aylikVaka,
        List<HizmetAdet> enCokHizmetler,
        List<TarafCiro> karsiTarafCiro,
        List<TeknisyenSayi> teknisyenPerformans
) {
    public record DurumSayi(String durum, long sayi) {}
    public record AySayi(String ay, long sayi) {}
    public record HizmetAdet(String ad, long adet) {}
    public record TarafCiro(String ad, String tip, BigDecimal ciro) {}
    public record TeknisyenSayi(String ad, long tamamlanan) {}
}
