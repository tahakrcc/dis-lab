import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { formatTutar } from "../api/cases";
import {
  getAnalyticsSummary,
  DURUM_ETIKET,
  type AnalyticsSummary,
} from "../api/analytics";

function BarRow({ label, value, max, valueText }: { label: string; value: number; max: number; valueText?: string }) {
  const pct = max > 0 ? Math.max(2, Math.round((value / max) * 100)) : 0;
  return (
    <div className="bar-row">
      <div className="bar-label" title={label}>{label}</div>
      <div className="bar-track">
        <div className="bar-fill" style={{ width: `${pct}%` }} />
      </div>
      <div className="bar-val">{valueText ?? value}</div>
    </div>
  );
}

const AY_KISA = ["Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"];
function ayEtiket(ym: string): string {
  const [y, m] = ym.split("-");
  const idx = Number(m) - 1;
  return `${AY_KISA[idx] ?? m} ${y?.slice(2) ?? ""}`;
}

export default function DashboardPage() {
  const { me, activeMembership, activeOrgId } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";

  const [data, setData] = useState<AnalyticsSummary | null>(null);
  const [hata, setHata] = useState<string | null>(null);
  const [yukleniyor, setYukleniyor] = useState(true);

  useEffect(() => {
    let iptal = false;
    setYukleniyor(true);
    setHata(null);
    setData(null);
    getAnalyticsSummary()
      .then((d) => !iptal && setData(d))
      .catch((e) => !iptal && setHata(e instanceof ApiError ? e.message : "Panel verisi yüklenemedi."))
      .finally(() => !iptal && setYukleniyor(false));
    return () => {
      iptal = true;
    };
  }, [activeOrgId]);

  const zamanindaOrani = useMemo(() => {
    if (!data) return null;
    const t = data.zamanindaTeslim + data.gecTeslim;
    return t > 0 ? Math.round((data.zamanindaTeslim / t) * 100) : null;
  }, [data]);

  const maxAy = useMemo(() => Math.max(1, ...(data?.aylikVaka.map((a) => a.sayi) ?? [0])), [data]);
  const maxDurum = useMemo(() => Math.max(1, ...(data?.durumDagilimi.map((a) => a.sayi) ?? [0])), [data]);
  const maxHizmet = useMemo(() => Math.max(1, ...(data?.enCokHizmetler.map((a) => a.adet) ?? [0])), [data]);
  const maxTaraf = useMemo(() => Math.max(1, ...(data?.karsiTarafCiro.map((a) => a.ciro) ?? [0])), [data]);
  const maxTek = useMemo(() => Math.max(1, ...(data?.teknisyenPerformans.map((a) => a.tamamlanan) ?? [0])), [data]);

  if (!me) return null;

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Panel</div>
        <h1 className="page-title">Merhaba, {me.ad.split(" ")[0]}</h1>
        <p className="page-desc">
          {activeMembership
            ? `${activeMembership.orgAd} · ${isLab ? "Laboratuvar" : "Klinik"} bağlamındasın.`
            : "Henüz bir organizasyona bağlı değilsin."}
        </p>
      </div>

      {yukleniyor && (
        <div className="screen-center" style={{ minHeight: 160 }}>
          <div className="spinner" aria-label="Yükleniyor" />
        </div>
      )}

      {hata && !yukleniyor && <div className="note">{hata}</div>}

      {data && !yukleniyor && (
        <>
          {/* Vaka KPI'ları */}
          <div className="kpi-grid">
            <div className="kpi">
              <div className="kpi-k">Toplam vaka</div>
              <div className="kpi-v">{data.toplamVaka}</div>
            </div>
            <div className="kpi">
              <div className="kpi-k">Devam eden</div>
              <div className="kpi-v">{data.devamEden}</div>
            </div>
            <div className={`kpi ${data.geciken > 0 ? "kpi-alert" : ""}`}>
              <div className="kpi-k">Geciken</div>
              <div className="kpi-v">{data.geciken}</div>
              <div className="kpi-sub">Teslim tarihi geçmiş, açık</div>
            </div>
            <div className="kpi">
              <div className="kpi-k">Teslim edilen</div>
              <div className="kpi-v">{data.teslimEdilen}</div>
            </div>
          </div>

          {/* Finans */}
          <div className="kpi-grid">
            <div className="kpi">
              <div className="kpi-k">Toplam ciro (KDV hariç)</div>
              <div className="kpi-v">{formatTutar(data.toplamCiro)}</div>
            </div>
            <div className="kpi">
              <div className="kpi-k">Toplam tahsilat</div>
              <div className="kpi-v">{formatTutar(data.toplamTahsilat)}</div>
            </div>
            <div className={`kpi ${data.acikBakiye > 0 ? "kpi-warn" : ""}`}>
              <div className="kpi-k">Açık bakiye</div>
              <div className="kpi-v">{formatTutar(data.acikBakiye)}</div>
              <div className="kpi-sub">{isLab ? "Kliniklerden alacak" : "Laba borç"}</div>
            </div>
          </div>

          {/* Operasyon / kalite */}
          <div className="kpi-grid">
            <div className="kpi">
              <div className="kpi-k">Ort. üretim süresi</div>
              <div className="kpi-v">
                {data.ortalamaUretimGun != null ? `${data.ortalamaUretimGun.toFixed(1)} gün` : "—"}
              </div>
              <div className="kpi-sub">Üretime alış → tamamlanma</div>
            </div>
            <div className="kpi">
              <div className="kpi-k">Zamanında teslim</div>
              <div className="kpi-v">{zamanindaOrani != null ? `%${zamanindaOrani}` : "—"}</div>
              <div className="kpi-sub">{data.zamanindaTeslim}/{data.zamanindaTeslim + data.gecTeslim} teslim</div>
            </div>
            <div className="kpi">
              <div className="kpi-k">Revizyon oranı</div>
              <div className="kpi-v">{data.revizyonOrani != null ? `%${data.revizyonOrani.toFixed(0)}` : "—"}</div>
              <div className="kpi-sub">{data.revizyonluVaka} vakada revizyon</div>
            </div>
          </div>

          <div className="card-grid">
            {/* Aylık vaka hacmi */}
            <section className="card">
              <div className="card-title">Aylık vaka hacmi (son 6 ay)</div>
              {data.aylikVaka.length === 0 ? (
                <div className="empty">Henüz vaka yok.</div>
              ) : (
                <div className="vbars">
                  {data.aylikVaka.map((a) => (
                    <div className="vbar" key={a.ay}>
                      <div className="vbar-num">{a.sayi}</div>
                      <div className="vbar-track">
                        <div
                          className="vbar-fill"
                          style={{ height: `${Math.max(4, Math.round((a.sayi / maxAy) * 100))}%` }}
                        />
                      </div>
                      <div className="vbar-cap">{ayEtiket(a.ay)}</div>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* Durum dağılımı */}
            <section className="card">
              <div className="card-title">Durum dağılımı</div>
              {data.durumDagilimi.length === 0 ? (
                <div className="empty">Veri yok.</div>
              ) : (
                <div className="bars">
                  {data.durumDagilimi.map((d) => (
                    <BarRow
                      key={d.durum}
                      label={DURUM_ETIKET[d.durum] ?? d.durum}
                      value={d.sayi}
                      max={maxDurum}
                    />
                  ))}
                </div>
              )}
            </section>

            {/* En çok kullanılan hizmetler */}
            <section className="card">
              <div className="card-title">En çok kullanılan hizmetler</div>
              {data.enCokHizmetler.length === 0 ? (
                <div className="empty">Veri yok.</div>
              ) : (
                <div className="bars">
                  {data.enCokHizmetler.map((h) => (
                    <BarRow key={h.ad} label={h.ad} value={h.adet} max={maxHizmet} valueText={`${h.adet} adet`} />
                  ))}
                </div>
              )}
            </section>

            {/* Karşı taraf ciro */}
            <section className="card">
              <div className="card-title">{isLab ? "Klinik bazlı ciro" : "Lab bazlı harcama"}</div>
              {data.karsiTarafCiro.length === 0 ? (
                <div className="empty">Veri yok.</div>
              ) : (
                <div className="bars">
                  {data.karsiTarafCiro.map((c) => (
                    <BarRow key={c.ad} label={c.ad} value={c.ciro} max={maxTaraf} valueText={formatTutar(c.ciro)} />
                  ))}
                </div>
              )}
            </section>

            {/* Teknisyen performansı */}
            {data.teknisyenPerformans.length > 0 && (
              <section className="card">
                <div className="card-title">Teknisyen performansı (tamamlanan vaka)</div>
                <div className="bars">
                  {data.teknisyenPerformans.map((t) => (
                    <BarRow key={t.ad} label={t.ad} value={t.tamamlanan} max={maxTek} />
                  ))}
                </div>
              </section>
            )}
          </div>
        </>
      )}
    </div>
  );
}
