import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  editMessage,
  deleteMessage,
  transitionCase,
  getAttachments,
  uploadAttachment,
  downloadAttachment,
  fetchAttachmentObjectUrl,
  formatBoyut,
  type CaseEvent,
  type CaseMessage,
  type CaseAttachment,
  type DentalCase,
} from "../api/cases";
import { availableActions, type ActionDef } from "../api/caseActions";
import { StatusBadge } from "./CasesPage";
import { useCaseLive } from "../api/live";
import { VoiceRecorder } from "../components/VoiceRecorder";

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

function EkSatiri({ a, onIndir }: { a: CaseAttachment; onIndir: (x: CaseAttachment) => void }) {
  const [url, setUrl] = useState<string | null>(null);
  const [yukleniyor, setYukleniyor] = useState(false);
  const mime = a.mime ?? "";
  const isAudio = mime.startsWith("audio/") || a.dosyaAdi.startsWith("sesli-not");
  const isImage = mime.startsWith("image/");
  useEffect(() => {
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [url]);
  async function onizle() {
    if (url || yukleniyor) return;
    setYukleniyor(true);
    try {
      setUrl(await fetchAttachmentObjectUrl(a.id));
    } catch {
      /* yut */
    } finally {
      setYukleniyor(false);
    }
  }
  return (
    <li>
      <div className="file-main">
        <span className="file-icon">{isAudio ? "🎤" : isImage ? "🖼️" : "📎"}</span>
        <div>
          <div className="file-name">{a.dosyaAdi}</div>
          <div className="file-meta">
            <span className={`badge ${a.uploaderTaraf === "LAB" ? "badge-lab" : "badge-klinik"}`}>
              {a.uploaderTaraf === "LAB" ? "LAB" : "KLİNİK"}
            </span>
            {a.uploaderAd} · {formatBoyut(a.boyut)} · {formatTarihSaat(a.createdAt)}
          </div>
          {url && isAudio && <audio className="ek-audio" src={url} controls autoPlay />}
          {url && isImage && <img className="ek-img" src={url} alt={a.dosyaAdi} />}
        </div>
      </div>
      <div className="file-actions">
        {(isAudio || isImage) && !url && (
          <button className="mini-btn" type="button" onClick={onizle} disabled={yukleniyor}>
            {yukleniyor ? "…" : isAudio ? "▶ Dinle" : "Önizle"}
          </button>
        )}
        <button className="mini-btn" type="button" onClick={() => onIndir(a)}>
          İndir
        </button>
      </div>
    </li>
  );
}

function SohbetMedya({ a, benim }: { a: CaseAttachment; benim: boolean }) {
  const [url, setUrl] = useState<string | null>(null);
  const mime = a.mime ?? "";
  const isAudio = mime.startsWith("audio/") || a.dosyaAdi.startsWith("sesli-not");
  useEffect(() => {
    let aktif = true;
    let obj: string | null = null;
    fetchAttachmentObjectUrl(a.id)
      .then((u) => {
        if (aktif) {
          obj = u;
          setUrl(u);
        } else {
          URL.revokeObjectURL(u);
        }
      })
      .catch(() => {});
    return () => {
      aktif = false;
      if (obj) URL.revokeObjectURL(obj);
    };
  }, [a.id]);
  return (
    <div className={`chat-msg ${benim ? "mine" : ""}`}>
      <div className="chat-bubble">
        <div className="chat-meta">
          <span className={`badge ${a.uploaderTaraf === "LAB" ? "badge-lab" : "badge-klinik"}`}>
            {a.uploaderTaraf === "LAB" ? "LAB" : "KLİNİK"}
          </span>
          <span className="chat-sender">{a.uploaderAd}</span>
          <span className="chat-time">{formatTarihSaat(a.createdAt)}</span>
        </div>
        {isAudio ? (
          url ? (
            <audio className="ek-audio" src={url} controls />
          ) : (
            <div className="chat-text muted-cell">🎤 ses yükleniyor…</div>
          )
        ) : url ? (
          <img className="chat-img" src={url} alt={a.dosyaAdi} />
        ) : (
          <div className="chat-text muted-cell">🖼️ görsel yükleniyor…</div>
        )}
      </div>
    </div>
  );
}

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { activeMembership, me } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";

  const [mesajlar, setMesajlar] = useState<CaseMessage[]>([]);
  const [yeniMesaj, setYeniMesaj] = useState("");
  const [mesajGonder, setMesajGonder] = useState(false);
  const [mesajHata, setMesajHata] = useState<string | null>(null);
  const [duzenId, setDuzenId] = useState<string | null>(null);
  const [duzenMetin, setDuzenMetin] = useState("");

  const [ekler, setEkler] = useState<CaseAttachment[]>([]);
  const [dosyaBusy, setDosyaBusy] = useState(false);
  const [dosyaHata, setDosyaHata] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const chatListRef = useRef<HTMLDivElement>(null);
  const atBottomRef = useRef(true);
  const chatFotoRef = useRef<HTMLInputElement>(null);

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

  // Yeniden yükleme fonksiyonları (hem efektler hem canlı güncelleme kullanır)
  const yukleMesajlar = useCallback(() => {
    if (!id) return;
    getMessages(id)
      .then((m) => {
        setMesajlar(m);
        markMessagesRead(id).catch(() => {});
      })
      .catch(() => {});
  }, [id]);

  const yukleVakaSessiz = useCallback(() => {
    if (!id) return;
    Promise.all([getCase(id), getCaseEvents(id).catch(() => [])])
      .then(([v, e]) => {
        setVaka(v);
        setOlaylar(e);
      })
      .catch(() => {});
  }, [id]);

  const yukleEkler = useCallback(() => {
    if (!id) return;
    getAttachments(id)
      .then(setEkler)
      .catch(() => {});
  }, [id]);

  // Sohbet: ilk yükleme + 20sn yedek tazeleme (canlı push WebSocket ile gelir)
  useEffect(() => {
    if (!id) return;
    yukleMesajlar();
    const t = setInterval(yukleMesajlar, 20000);
    return () => clearInterval(t);
  }, [id, yukleMesajlar]);

  // Canlı güncelleme (WebSocket): olay tipine göre ilgili veriyi tazele
  useCaseLive(
    id,
    useCallback(
      (ev) => {
        if (ev.type === "message") yukleMesajlar();
        else if (ev.type === "status") yukleVakaSessiz();
        else if (ev.type === "attachment") yukleEkler();
      },
      [yukleMesajlar, yukleVakaSessiz, yukleEkler]
    )
  );

  // Mesaj + medya eklerini tek sohbet akışında zaman sırasına göre birleştir
  type AkisOgesi =
    | { kind: "msg"; at: string; m: CaseMessage }
    | { kind: "file"; at: string; a: CaseAttachment };
  const akis: AkisOgesi[] = useMemo(() => {
    const list: AkisOgesi[] = [];
    for (const m of mesajlar) list.push({ kind: "msg", at: m.createdAt, m });
    for (const a of ekler) {
      const mime = a.mime ?? "";
      if (mime.startsWith("audio/") || mime.startsWith("image/") || a.dosyaAdi.startsWith("sesli-not")) {
        list.push({ kind: "file", at: a.createdAt, a });
      }
    }
    list.sort((x, y) => (x.at < y.at ? -1 : x.at > y.at ? 1 : 0));
    return list;
  }, [mesajlar, ekler]);

  // Sohbeti en alta sabitle: kullanıcı zaten alttaysa yeni mesajda otomatik kaydır
  const onChatScroll = () => {
    const el = chatListRef.current;
    if (!el) return;
    atBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 60;
  };
  useEffect(() => {
    const el = chatListRef.current;
    if (el && atBottomRef.current) el.scrollTop = el.scrollHeight;
  }, [akis]);

  function duzenleBasla(m: CaseMessage) {
    setDuzenId(m.id);
    setDuzenMetin(m.metin ?? "");
  }
  async function duzenleKaydet() {
    if (!id || !duzenId || !duzenMetin.trim()) return;
    try {
      const yeni = await editMessage(id, duzenId, duzenMetin.trim());
      setMesajlar((prev) => prev.map((x) => (x.id === yeni.id ? yeni : x)));
      setDuzenId(null);
      setDuzenMetin("");
    } catch (err) {
      setMesajHata(err instanceof ApiError ? err.message : "Mesaj düzenlenemedi.");
    }
  }
  async function mesajSil(m: CaseMessage) {
    if (!id) return;
    if (!window.confirm("Bu mesaj silinsin mi?")) return;
    try {
      await deleteMessage(id, m.id);
      setMesajlar((prev) =>
        prev.map((x) => (x.id === m.id ? { ...x, silindiAt: new Date().toISOString(), metin: null } : x))
      );
    } catch (err) {
      setMesajHata(err instanceof ApiError ? err.message : "Mesaj silinemedi.");
    }
  }

  async function sesYukle(file: File) {
    if (!id) return;
    setDosyaHata(null);
    setDosyaBusy(true);
    try {
      const yeni = await uploadAttachment(id, file);
      setEkler((prev) => [...prev, yeni]);
    } catch (err) {
      setDosyaHata(err instanceof Error ? err.message : "Ses yüklenemedi.");
    } finally {
      setDosyaBusy(false);
    }
  }

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

  // Ekler: ilk yükleme
  useEffect(() => {
    yukleEkler();
  }, [yukleEkler]);

  async function onDosyaSec(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !id) return;
    setDosyaHata(null);
    setDosyaBusy(true);
    try {
      const yeni = await uploadAttachment(id, file);
      setEkler((prev) => [...prev, yeni]);
    } catch (err) {
      setDosyaHata(err instanceof Error ? err.message : "Dosya yüklenemedi.");
    } finally {
      setDosyaBusy(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  }

  async function indir(att: CaseAttachment) {
    try {
      await downloadAttachment(att);
    } catch (err) {
      setDosyaHata(err instanceof Error ? err.message : "İndirilemedi.");
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

          <section className="card block">
            <div className="card-title cardtitle-row">
              <span>Dosyalar ({ekler.length})</span>
              <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                <input ref={fileRef} type="file" onChange={onDosyaSec} hidden />
                <VoiceRecorder onRecorded={sesYukle} disabled={dosyaBusy} />
                <button
                  className="btn btn-ghost"
                  type="button"
                  onClick={() => fileRef.current?.click()}
                  disabled={dosyaBusy}
                >
                  {dosyaBusy ? "Yükleniyor…" : "+ Dosya ekle"}
                </button>
              </div>
            </div>
            {dosyaHata && <div className="alert alert-error">{dosyaHata}</div>}
            {ekler.length === 0 ? (
              <div className="empty sm">Henüz dosya yok. Ölçü (STL), fotoğraf vb. ekleyebilirsin.</div>
            ) : (
              <ul className="file-list">
                {ekler.map((a) => (
                  <EkSatiri key={a.id} a={a} onIndir={indir} />
                ))}
              </ul>
            )}
          </section>

          <section className="card block chat-card">
            <div className="card-title">Sohbet ({mesajlar.length})</div>
            <div className="chat-list" ref={chatListRef} onScroll={onChatScroll}>
              {akis.length === 0 ? (
                <div className="empty sm">Henüz mesaj yok. Laboratuvar ↔ klinik yazışmasını burada yapın.</div>
              ) : (
                akis.map((o) => {
                  if (o.kind === "file") {
                    return <SohbetMedya key={`f-${o.a.id}`} a={o.a} benim={o.a.uploaderUserId === me?.id} />;
                  }
                  const m = o.m;
                  const benim = m.senderUserId === me?.id;
                  return (
                    <div key={`m-${m.id}`} className={`chat-msg ${benim ? "mine" : ""}`}>
                      <div className="chat-bubble">
                        <div className="chat-meta">
                          <span className={`badge ${m.senderTaraf === "LAB" ? "badge-lab" : "badge-klinik"}`}>
                            {m.senderTaraf === "LAB" ? "LAB" : "KLİNİK"}
                          </span>
                          <span className="chat-sender">{m.senderAd}</span>
                          <span className="chat-time">{formatTarihSaat(m.createdAt)}</span>
                        </div>
                        {m.silindiAt ? (
                          <div className="chat-text chat-silindi">Bu mesaj silindi</div>
                        ) : duzenId === m.id ? (
                          <div className="chat-edit">
                            <input
                              value={duzenMetin}
                              onChange={(e) => setDuzenMetin(e.target.value)}
                              maxLength={4000}
                              autoFocus
                            />
                            <div className="chat-edit-actions">
                              <button
                                className="mini-btn"
                                type="button"
                                onClick={duzenleKaydet}
                                disabled={!duzenMetin.trim()}
                              >
                                Kaydet
                              </button>
                              <button className="mini-btn" type="button" onClick={() => setDuzenId(null)}>
                                İptal
                              </button>
                            </div>
                          </div>
                        ) : (
                          <>
                            <div className="chat-text">
                              {m.metin}
                              {m.duzenlendiAt && <span className="chat-edited"> (düzenlendi)</span>}
                            </div>
                            {benim && (
                              <div className="chat-actions">
                                <button className="chat-act" type="button" onClick={() => duzenleBasla(m)}>
                                  Düzenle
                                </button>
                                <button className="chat-act" type="button" onClick={() => mesajSil(m)}>
                                  Sil
                                </button>
                                <span className="chat-read">{m.okunduAt ? "✓✓ okundu" : "✓ gönderildi"}</span>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
            <form className="chat-input" onSubmit={mesajYolla}>
              <VoiceRecorder onRecorded={sesYukle} disabled={dosyaBusy} etiket="🎤" />
              <input
                ref={chatFotoRef}
                type="file"
                accept="image/*"
                multiple
                hidden
                onChange={(e) => {
                  Array.from(e.target.files ?? []).forEach((f) => sesYukle(f));
                  e.target.value = "";
                }}
              />
              <button
                type="button"
                className="btn btn-ghost"
                title="Fotoğraf gönder"
                onClick={() => chatFotoRef.current?.click()}
                disabled={dosyaBusy}
              >
                🖼️
              </button>
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
