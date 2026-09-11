package tr.kopru.domain.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kopru.common.ApiException;
import tr.kopru.domain.analytics.dto.AnalyticsSummaryResponse;
import tr.kopru.domain.analytics.dto.AnalyticsSummaryResponse.*;
import tr.kopru.tenant.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Panel metrikleri. @Transactional(readOnly=true) sayesinde TenantAspect app.user_id'yi
 * set eder; tum native sorgular RLS altinda calisir (kullanici yalnizca kendi
 * ortakliklarinin verisini gorur). Ek olarak aktif org'un (X-Org-Id) ortakliklarina
 * daraltilir.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    @PersistenceContext
    private final EntityManager em;

    // Aktif org'un ortakliklari (RLS zaten sinirlar; bu filtre org'u netlestirir)
    private static final String PF =
            " partnership_id IN (SELECT id FROM partnership WHERE lab_id = :orgId OR clinic_id = :orgId)";

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(LocalDate from, LocalDate to) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId == null) {
            throw ApiException.validationError("Panel icin aktif organizasyon (X-Org-Id) gereklidir.", null);
        }
        LocalDate f = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate t = to != null ? to : LocalDate.of(2100, 1, 1);

        // 1) Vaka KPI sayaclari
        Object[] kpi = (Object[]) em.createNativeQuery("""
                SELECT
                  COUNT(*),
                  COUNT(*) FILTER (WHERE durum NOT IN ('TESLIM_EDILDI','IPTAL')),
                  COUNT(*) FILTER (WHERE durum = 'TESLIM_EDILDI'),
                  COUNT(*) FILTER (WHERE durum NOT IN ('TESLIM_EDILDI','IPTAL')
                                   AND teslim_tarihi IS NOT NULL AND teslim_tarihi < CURRENT_DATE),
                  COUNT(*) FILTER (WHERE revizyon_sayisi > 0)
                FROM "case"
                WHERE """ + PF + " AND created_at::date BETWEEN :from AND :to")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getSingleResult();

        long toplam = toLong(kpi[0]);
        long devam = toLong(kpi[1]);
        long teslim = toLong(kpi[2]);
        long geciken = toLong(kpi[3]);
        long revizyonlu = toLong(kpi[4]);

        // 2) Cari toplamlar
        Object[] fin = (Object[]) em.createNativeQuery("""
                SELECT
                  COALESCE(SUM(matrah) FILTER (WHERE tur='BORC'),0),
                  COALESCE(SUM(ABS(tutar)) FILTER (WHERE tur='TAHSILAT'),0),
                  COALESCE(SUM(tutar),0)
                FROM ledger_entry
                WHERE """ + PF + " AND belge_tarihi BETWEEN :from AND :to")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getSingleResult();

        BigDecimal ciro = toDecimal(fin[0]);
        BigDecimal tahsilat = toDecimal(fin[1]);
        BigDecimal bakiye = toDecimal(fin[2]);

        // 3) Ortalama uretim suresi (URETIMDE -> TAMAMLANDI), gun
        Object avg = em.createNativeQuery("""
                SELECT AVG(d) FROM (
                  SELECT EXTRACT(EPOCH FROM (
                            MIN(created_at) FILTER (WHERE yeni_durum='TAMAMLANDI')
                          - MIN(created_at) FILTER (WHERE yeni_durum='URETIMDE')))/86400.0 AS d
                  FROM case_event
                  WHERE case_id IN (SELECT id FROM "case" WHERE """ + PF + """
                  )
                  GROUP BY case_id
                ) s WHERE d IS NOT NULL AND d >= 0
                """)
                .setParameter("orgId", orgId)
                .getSingleResult();
        Double ortUretim = toDoubleOrNull(avg);

        // 4) Zamaninda / gec teslim
        Object[] onTime = (Object[]) em.createNativeQuery("""
                SELECT
                  COUNT(*) FILTER (WHERE teslim_at::date <= teslim_tarihi),
                  COUNT(*) FILTER (WHERE teslim_at::date >  teslim_tarihi)
                FROM (
                  SELECT c.teslim_tarihi,
                    (SELECT MIN(e.created_at) FROM case_event e
                      WHERE e.case_id = c.id AND e.yeni_durum='TESLIM_EDILDI') AS teslim_at
                  FROM "case" c
                  WHERE """ + PF.replace("partnership_id", "c.partnership_id") + """
                    AND c.durum='TESLIM_EDILDI' AND c.teslim_tarihi IS NOT NULL
                ) t WHERE teslim_at IS NOT NULL
                """)
                .setParameter("orgId", orgId)
                .getSingleResult();
        long zamaninda = toLong(onTime[0]);
        long gec = toLong(onTime[1]);

        // 5) Durum dagilimi
        List<DurumSayi> durumDagilimi = new ArrayList<>();
        for (Object row : em.createNativeQuery("""
                SELECT durum::text, COUNT(*) FROM "case"
                WHERE """ + PF + " AND created_at::date BETWEEN :from AND :to GROUP BY durum")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getResultList()) {
            Object[] r = (Object[]) row;
            durumDagilimi.add(new DurumSayi((String) r[0], toLong(r[1])));
        }

        // 6) Aylik vaka hacmi (son 6 ay)
        List<AySayi> aylik = new ArrayList<>();
        for (Object row : em.createNativeQuery("""
                SELECT to_char(date_trunc('month', created_at),'YYYY-MM') ay, COUNT(*)
                FROM "case"
                WHERE """ + PF + """
                  AND created_at >= (date_trunc('month', CURRENT_DATE) - INTERVAL '5 months')
                GROUP BY 1 ORDER BY 1""")
                .setParameter("orgId", orgId)
                .getResultList()) {
            Object[] r = (Object[]) row;
            aylik.add(new AySayi((String) r[0], toLong(r[1])));
        }

        // 7) En cok kullanilan hizmetler
        List<HizmetAdet> hizmetler = new ArrayList<>();
        for (Object row : em.createNativeQuery("""
                SELECT si.ad, COALESCE(SUM(ci.adet),0) adet
                FROM case_item ci
                JOIN "case" c ON c.id = ci.case_id
                JOIN service_item si ON si.id = ci.service_item_id
                WHERE c.partnership_id IN (SELECT id FROM partnership WHERE lab_id=:orgId OR clinic_id=:orgId)
                  AND c.created_at::date BETWEEN :from AND :to
                GROUP BY si.ad ORDER BY adet DESC LIMIT 8""")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getResultList()) {
            Object[] r = (Object[]) row;
            hizmetler.add(new HizmetAdet((String) r[0], toLong(r[1])));
        }

        // 8) Karsi taraf bazli ciro (lab icin klinik, klinik icin lab)
        List<TarafCiro> taraflar = new ArrayList<>();
        for (Object row : em.createNativeQuery("""
                SELECT o.ad, o.tip::text, COALESCE(SUM(le.matrah),0) ciro
                FROM ledger_entry le
                JOIN partnership p ON p.id = le.partnership_id
                JOIN organization o ON o.id = CASE WHEN p.lab_id=:orgId THEN p.clinic_id ELSE p.lab_id END
                WHERE le.tur='BORC' AND (p.lab_id=:orgId OR p.clinic_id=:orgId)
                  AND le.belge_tarihi BETWEEN :from AND :to
                GROUP BY o.ad, o.tip ORDER BY ciro DESC LIMIT 8""")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getResultList()) {
            Object[] r = (Object[]) row;
            taraflar.add(new TarafCiro((String) r[0], (String) r[1], toDecimal(r[2])));
        }

        // 9) Teknisyen performansi (tamamlanan vaka sayisi)
        List<TeknisyenSayi> teknisyenler = new ArrayList<>();
        for (Object row : em.createNativeQuery("""
                SELECT au.ad, COUNT(*) tamam
                FROM case_event e
                JOIN app_user au ON au.id = e.actor_user_id
                WHERE e.yeni_durum='TAMAMLANDI'
                  AND e.created_at::date BETWEEN :from AND :to
                  AND e.case_id IN (SELECT id FROM "case"
                        WHERE partnership_id IN (SELECT id FROM partnership WHERE lab_id=:orgId OR clinic_id=:orgId))
                GROUP BY au.ad ORDER BY tamam DESC LIMIT 8""")
                .setParameter("orgId", orgId).setParameter("from", f).setParameter("to", t)
                .getResultList()) {
            Object[] r = (Object[]) row;
            teknisyenler.add(new TeknisyenSayi((String) r[0], toLong(r[1])));
        }

        Double revizyonOrani = toplam > 0 ? (revizyonlu * 100.0 / toplam) : 0.0;

        return AnalyticsSummaryResponse.builder()
                .toplamVaka(toplam).devamEden(devam).teslimEdilen(teslim).geciken(geciken)
                .toplamCiro(ciro).toplamTahsilat(tahsilat).acikBakiye(bakiye)
                .ortalamaUretimGun(ortUretim)
                .zamanindaTeslim(zamaninda).gecTeslim(gec)
                .revizyonluVaka(revizyonlu).revizyonOrani(revizyonOrani)
                .durumDagilimi(durumDagilimi).aylikVaka(aylik)
                .enCokHizmetler(hizmetler).karsiTarafCiro(taraflar)
                .teknisyenPerformans(teknisyenler)
                .build();
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        return new BigDecimal(o.toString());
    }

    private static Double toDoubleOrNull(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }
}
