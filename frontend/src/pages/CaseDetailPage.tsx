import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import {
  CASE_STATUS_META,
  formatTarih,
  formatTarihSaat,
  formatTutar,
  getCase,
  getCaseEvents,
  type CaseEvent,
  type DentalCase,
} from "../api/cases";
import { StatusBadge } from "./CasesPage";

function disList(d: number[]): string {
  return d && d.length ? d.join(" · ") : "—";
}

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { activeMembership } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";

  const [vaka, setVaka] = useState<DentalCase | null>(null);
  const [olaylar, setOlaylar] = useState<CaseEvent[]>([]);
  const [hata, setHata] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let iptal = false;
    setVaka(null);
    setHata(null);
    Promise.all([getCase(id), getCaseEvents(id).catch(() => [])])
      .then(([v, e]) => {
        if (iptal) return;
        setVaka(v);
        setOlaylar(e);
      })
      .catch((e) => !iptal && setHata(e instanceof ApiError ? e.message : "Vaka yüklenemedi."));
    return () => {
      iptal = true;
    };
  }, [id]);

  return (
    <div className="page">
      <div className="crumbs">
        <Link to="/cases">← Vakalar</Link>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}
      {!vaka && !hata && (
        <div className="skeleton-list">
          <div className="skeleton-row" />
          <div className="skeleton-row" />
        </div>
      )}

      {vaka && (
        <>
          <div className="page-head detail-head">
            <div>
              <div className="eyebrow mono">{vaka.kod}</div>
              <h1 className="page-title">{isLab ? vaka.hastaRumuzu : vaka.patientAd ?? vaka.hastaRumuzu}</h1>
              <p className="page-desc">
                {isLab ? "Rumuz" : "Hasta"} · Ölçü {vaka.olcuTipi === "STL" ? "STL" : "Fiziksel"}
                {vaka.revizyonSayisi > 0 && ` · ${vaka.revizyonSayisi} revizyon`}
              </p>
            </div>
            <StatusBadge durum={vaka.durum} />
          </div>

          <div className="stat-row">
            <div className="stat">
              <div className="stat-k">Toplam</div>
              <div className="stat-v money">{formatTutar(vaka.toplamTutar)}</div>
            </div>
            <div className="stat">
              <div className="stat-k">Teslim tarihi</div>
              <div className="stat-v">{formatTarih(vaka.teslimTarihi)}</div>
            </div>
            <div className="stat">
              <div className="stat-k">Fiyat versiyonu</div>
              <div className="stat-v mono">v{vaka.fiyatVersiyon}</div>
            </div>
            <div className="stat">
              <div className="stat-k">Onaylar</div>
              <div className="stat-v onay">
                <span className={vaka.onayKlinikVersiyon === vaka.fiyatVersiyon ? "ok" : "no"}>
                  Klinik {vaka.onayKlinikVersiyon === vaka.fiyatVersiyon ? "✓" : "○"}
                </span>
                <span className={vaka.onayLabVersiyon === vaka.fiyatVersiyon ? "ok" : "no"}>
                  Lab {vaka.onayLabVersiyon === vaka.fiyatVersiyon ? "✓" : "○"}
                </span>
              </div>
            </div>
          </div>

          <section className="card block">
            <div className="card-title">Kalemler ({vaka.items.length})</div>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Hizmet</th>
                    <th>Diş (FDI)</th>
                    <th>Materyal / Renk</th>
                    <th className="num">Adet</th>
                    <th className="num">Birim</th>
                    <th className="num">Ara toplam</th>
                  </tr>
                </thead>
                <tbody>
                  {vaka.items.map((it) => (
                    <tr key={it.id}>
                      <td>{it.serviceItemAd}</td>
                      <td className="mono muted-cell">{disList(it.disNumaralari)}</td>
                      <td className="muted-cell">
                        {[it.materyal, it.renk].filter(Boolean).join(" · ") || "—"}
                      </td>
                      <td className="num">{it.adet}</td>
                      <td className="num mono">{formatTutar(it.birimFiyat)}</td>
                      <td className="num mono">{formatTutar(it.araToplam)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card block">
            <div className="card-title">Durum geçmişi ({olaylar.length})</div>
            {olaylar.length === 0 ? (
              <div className="empty sm">Henüz olay yok.</div>
            ) : (
              <ul className="timeline">
                {olaylar.map((ev) => (
                  <li key={ev.id}>
                    <span className="tl-dot" />
                    <div className="tl-body">
                      <div className="tl-main">
                        {ev.eskiDurum && ev.yeniDurum ? (
                          <>
                            {CASE_STATUS_META[ev.eskiDurum].label} →{" "}
                            <strong>{CASE_STATUS_META[ev.yeniDurum].label}</strong>
                          </>
                        ) : (
                          <span className="tl-tur">{ev.tur}</span>
                        )}
                      </div>
                      <div className="tl-meta">
                        {ev.actorUserAd} · {formatTarihSaat(ev.createdAt)}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <div className="note sm">
            Onay ve durum aksiyonları (gönder, karşı teklif, onayla, üretime al, prova,
            tamamla, teslim) bir sonraki adımda bu sayfaya eklenecek.
          </div>
        </>
      )}
    </div>
  );
}
