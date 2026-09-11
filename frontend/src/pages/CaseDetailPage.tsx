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
  getMessages,
  markMessagesRead,
  sendMessage,
  transitionCase,
  type CaseEvent,
  type CaseMessage,
  type DentalCase,
} from "../api/cases";
import { availableActions, type ActionDef } from "../api/caseActions";
import { StatusBadge } from "./CasesPage";

function disList(d: number[]): string {
  return d && d.length ? d.join(" · ") : "—";
}

function specAciklama(specs: string | null): string {
  if (!specs) return "";
  try {
    const o = JSON.parse(specs);
    return o && typeof o.aciklama === "string" ? o.aciklama : "";
  } catch {
    return "";
  }
}

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { activeMembership, me } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";

  const [mesajlar, setMesajlar] = useState<CaseMessage[]>([]);
  const [yeniMesaj, setYeniMesaj] = useState("");
  const [mesajGonder, setMesajGonder] = useState(false);
  const [mesajHata, setMesajHata] = useState<string | null>(null);

  const [vaka, setVaka] = useState<DentalCase | null>(null);
  const [olaylar, setOlaylar] = useState<CaseEvent[]>([]);
  const [hata, setHata] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [aksiyonHata, setAksiyonHata] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let iptal = false;
    setVaka(null);
    setHata(null);
    setAksiyonHata(null);
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

  // Sohbet: ilk yükleme + 8sn'de bir tazeleme
  useEffect(() => {
    if (!id) return;
    let iptal = false;
    const yukle = () =>
      getMessages(id)
        .then((m) => {
          if (iptal) return;
          setMesajlar(m);
          // Karşı tarafın mesajlarını okundu işaretle
          markMessagesRead(id).catch(() => {});
        })
        .catch(() => {});
    yukle();
    const t = setInterval(yukle, 8000);
    return () => {
      iptal = true;
      clearInterval(t);
    };
  }, [id]);

  async function mesajYolla(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !yeniMesaj.trim()) return;
    setMesajHata(null);
    setMesajGonder(true);
    try {
      const m = await sendMessage(id, yeniMesaj.trim());
      setMesajlar((prev) => [...prev, m]);
      setYeniMesaj("");
    } catch (err) {
      setMesajHata(err instanceof ApiError ? err.message : "Mesaj gönderilemedi.");
    } finally {
      setMesajGonder(false);
    }
  }

  async function aksiyonCalistir(a: ActionDef) {
    if (!vaka) return;
    setAksiyonHata(null);

    if (a.confirm && !window.confirm(a.confirm)) return;

    const payload: Record<string, unknown> = {};
    if (a.sendVersion) payload.fiyatVersiyon = vaka.fiyatVersiyon;
    if (a.nedenKey) {
      const neden = window.prompt("Neden / açıklama:", "");
      if (neden === null) return; // vazgeçildi
      payload[a.nedenKey] = neden;
    }

    setBusy(a.aksiyon);
    try {
      const guncel = await transitionCase(vaka.id, a.aksiyon, payload);
      setVaka(guncel);
      const ev = await getCaseEvents(vaka.id).catch(() => olaylar);
      setOlaylar(ev);
    } catch (e) {
      setAksiyonHata(e instanceof ApiError ? e.message : "İşlem başarısız.");
    } finally {
      setBusy(null);
    }
  }

  const aksiyonlar = vaka ? availableActions(activeMembership?.rol, vaka.durum) : [];

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

          <section className="card block actions-card">
            <div className="card-title">Aksiyonlar</div>
            {aksiyonlar.length === 0 ? (
              <div className="empty sm">
                Bu durumda ({CASE_STATUS_META[vaka.durum].label}) rolünle yapılabilecek bir işlem yok.
              </div>
            ) : (
              <div className="action-btns">
                {aksiyonlar.map((a) => (
                  <button
                    key={a.aksiyon}
                    className={`btn btn-${a.variant === "danger" ? "danger" : a.variant === "ghost" ? "ghost" : "primary"}`}
                    disabled={busy !== null}
                    onClick={() => aksiyonCalistir(a)}
                  >
                    {busy === a.aksiyon ? "İşleniyor…" : a.label}
                  </button>
                ))}
              </div>
            )}
            {aksiyonHata && (
              <div className="alert alert-error" style={{ marginTop: 12 }}>
                {aksiyonHata}
              </div>
            )}
          </section>

          {vaka.genelNot && (
            <section className="card block">
              <div className="card-title">Açıklama</div>
              <p className="aciklama-text">{vaka.genelNot}</p>
            </section>
          )}

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
                      <td>
                        {it.serviceItemAd}
                        {specAciklama(it.specs) && (
                          <div className="item-note">{specAciklama(it.specs)}</div>
                        )}
                      </td>
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

          <section className="card block chat-card">
            <div className="card-title">Sohbet ({mesajlar.length})</div>
            <div className="chat-list">
              {mesajlar.length === 0 ? (
                <div className="empty sm">Henüz mesaj yok. Laboratuvar ↔ klinik yazışmasını burada yapın.</div>
              ) : (
                mesajlar.map((m) => {
                  const benim = m.senderUserId === me?.id;
                  return (
                    <div key={m.id} className={`chat-msg ${benim ? "mine" : ""}`}>
                      <div className="chat-bubble">
                        <div className="chat-meta">
                          <span className={`badge ${m.senderTaraf === "LAB" ? "badge-lab" : "badge-klinik"}`}>
                            {m.senderTaraf === "LAB" ? "LAB" : "KLİNİK"}
                          </span>
                          <span className="chat-sender">{m.senderAd}</span>
                          <span className="chat-time">{formatTarihSaat(m.createdAt)}</span>
                        </div>
                        <div className="chat-text">{m.metin}</div>
                        {benim && (
                          <div className="chat-read">{m.okunduAt ? "✓✓ okundu" : "✓ gönderildi"}</div>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
            <form className="chat-input" onSubmit={mesajYolla}>
              <input
                value={yeniMesaj}
                onChange={(e) => setYeniMesaj(e.target.value)}
                placeholder="Mesaj yaz…"
                maxLength={4000}
              />
              <button className="btn btn-primary" type="submit" disabled={mesajGonder || !yeniMesaj.trim()}>
                {mesajGonder ? "…" : "Gönder"}
              </button>
            </form>
            {mesajHata && <div className="alert alert-error">{mesajHata}</div>}
          </section>
        </>
      )}
    </div>
  );
}
