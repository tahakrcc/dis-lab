import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { formatTutar, uploadAttachment, type MeasureType } from "../api/cases";
import {
  createCase,
  currentPrices,
  getPartnerships,
  getPartnershipPrices,
  getPatients,
  isValidFdi,
  parseDisler,
  type Partnership,
  type Patient,
  type PriceEntry,
} from "../api/catalog";
import { VoiceRecorder } from "../components/VoiceRecorder";

interface ItemRow {
  key: number;
  serviceItemId: string;
  disText: string;
  adet: number;
  materyal: string;
  renk: string;
  aciklama: string;
}

let rowSeq = 1;
const bosRow = (): ItemRow => ({
  key: rowSeq++,
  serviceItemId: "",
  disText: "",
  adet: 1,
  materyal: "",
  renk: "",
  aciklama: "",
});

function baslikHarfleri(ad: string): string {
  return ad
    .split(/\s+/)
    .filter(Boolean)
    .map((p) => p[0]?.toUpperCase() + ".")
    .join("");
}

export default function NewCasePage() {
  const { activeMembership, activeOrgId } = useAuth();
  const navigate = useNavigate();
  const isKlinik = activeMembership?.orgTip === "KLINIK";

  const [partnerships, setPartnerships] = useState<Partnership[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [prices, setPrices] = useState<PriceEntry[]>([]);
  const [yukleniyor, setYukleniyor] = useState(true);

  const [partnershipId, setPartnershipId] = useState("");
  const [olcuTipi, setOlcuTipi] = useState<MeasureType>("STL");
  const [patientId, setPatientId] = useState("");
  const [hastaRumuzu, setHastaRumuzu] = useState("");
  const [teslimTarihi, setTeslimTarihi] = useState("");
  const [genelNot, setGenelNot] = useState("");
  const [items, setItems] = useState<ItemRow[]>([bosRow()]);

  const [hata, setHata] = useState<string | null>(null);
  const [gonderiliyor, setGonderiliyor] = useState(false);
  const [sesFile, setSesFile] = useState<File | null>(null);
  const [fotolar, setFotolar] = useState<File[]>([]);
  const fotoRef = useRef<HTMLInputElement>(null);

  // Ortaklıklar + hastalar
  useEffect(() => {
    let iptal = false;
    setYukleniyor(true);
    Promise.all([getPartnerships(), isKlinik ? getPatients() : Promise.resolve([])])
      .then(([ps, pts]) => {
        if (iptal) return;
        setPartnerships(ps);
        setPatients(pts);
        if (ps.length > 0) setPartnershipId((cur) => cur || ps[0].id);
        setYukleniyor(false);
      })
      .catch((e) => {
        if (iptal) return;
        setHata(e instanceof ApiError ? e.message : "Veriler yüklenemedi.");
        setYukleniyor(false);
      });
    return () => {
      iptal = true;
    };
  }, [activeOrgId, isKlinik]);

  // Seçilen ortaklığın fiyat listesi
  useEffect(() => {
    if (!partnershipId) return;
    let iptal = false;
    getPartnershipPrices(partnershipId)
      .then((p) => !iptal && setPrices(currentPrices(p.filter((x) => x.aktif))))
      .catch(() => !iptal && setPrices([]));
    return () => {
      iptal = true;
    };
  }, [partnershipId]);

  const priceById = useMemo(() => {
    const m = new Map<string, PriceEntry>();
    prices.forEach((p) => m.set(p.serviceItemId, p));
    return m;
  }, [prices]);

  function rowHesap(r: ItemRow) {
    const pe = priceById.get(r.serviceItemId);
    if (!pe) return { birim: null as null | "DIS" | "ADET", adet: 0, araToplam: 0, gecersizDis: false };
    if (pe.serviceItemBirim === "DIS") {
      const disler = parseDisler(r.disText);
      const gecersizDis = disler.length === 0 || disler.some((d) => !isValidFdi(d));
      const adet = disler.length;
      return { birim: "DIS" as const, adet, araToplam: pe.fiyat * adet, gecersizDis };
    }
    return { birim: "ADET" as const, adet: r.adet, araToplam: pe.fiyat * r.adet, gecersizDis: false };
  }

  const toplam = useMemo(
    () => items.reduce((s, r) => s + rowHesap(r).araToplam, 0),
    [items, priceById]
  );

  function updateRow(key: number, patch: Partial<ItemRow>) {
    setItems((rows) => rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  }
  function removeRow(key: number) {
    setItems((rows) => (rows.length > 1 ? rows.filter((r) => r.key !== key) : rows));
  }

  function seatientDegis(id: string) {
    setPatientId(id);
    const p = patients.find((x) => x.id === id);
    if (p && !hastaRumuzu.trim()) setHastaRumuzu(baslikHarfleri(p.ad));
  }

  async function gonder(e: React.FormEvent) {
    e.preventDefault();
    setHata(null);

    if (!partnershipId) return setHata("Bir laboratuvar (ortaklık) seçmelisin.");
    if (!hastaRumuzu.trim()) return setHata("Hasta rumuzu zorunludur.");

    const specsOf = (r: ItemRow) =>
      r.aciklama.trim() ? JSON.stringify({ aciklama: r.aciklama.trim() }) : undefined;

    const payloadItems = [];
    for (const r of items) {
      if (!r.serviceItemId) return setHata("Her kalemde bir hizmet seçilmeli.");
      const h = rowHesap(r);
      if (h.birim === "DIS") {
        if (h.gecersizDis) return setHata("Diş numaraları geçersiz (FDI). Örn: 11 12 13.");
        payloadItems.push({
          serviceItemId: r.serviceItemId,
          disNumaralari: parseDisler(r.disText),
          adet: h.adet,
          materyal: r.materyal || null,
          renk: r.renk || null,
          specs: specsOf(r),
        });
      } else {
        if (r.adet < 1) return setHata("Adet en az 1 olmalı.");
        payloadItems.push({
          serviceItemId: r.serviceItemId,
          disNumaralari: [],
          adet: r.adet,
          materyal: r.materyal || null,
          renk: r.renk || null,
          specs: specsOf(r),
        });
      }
    }

    setGonderiliyor(true);
    try {
      const vaka = await createCase({
        partnershipId,
        hastaRumuzu: hastaRumuzu.trim(),
        patientId: patientId || null,
        olcuTipi,
        teslimTarihi: teslimTarihi || null,
        genelNot: genelNot || null,
        items: payloadItems,
      });
      // Sesli not + fotoğrafları yeni vakaya ek olarak yükle (opsiyonel; hata vaka oluşturmayı bozmaz)
      const ekDosyalar = [sesFile, ...fotolar].filter((f): f is File => !!f);
      for (const f of ekDosyalar) {
        try {
          await uploadAttachment(vaka.id, f);
        } catch {
          /* ek yüklenemedi, yut */
        }
      }
      navigate(`/cases/${vaka.id}`);
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Vaka oluşturulamadı.");
      setGonderiliyor(false);
    }
  }

  if (!isKlinik) {
    return (
      <div className="page">
        <div className="empty">Vaka oluşturmak yalnızca klinik kullanıcılarına açıktır.</div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="crumbs">
        <Link to="/cases">← Vakalar</Link>
      </div>
      <div className="page-head">
        <div className="eyebrow">Yeni vaka</div>
        <h1 className="page-title">Yeni iş oluştur</h1>
        <p className="page-desc">
          Katalogdan hizmet seç, fiyat kliniğine özel listeden otomatik gelir. Kaydedince
          taslak olarak açılır; sonra fiyat mutabakatına gönderirsin.
        </p>
      </div>

      {yukleniyor ? (
        <div className="skeleton-list">
          <div className="skeleton-row" />
          <div className="skeleton-row" />
        </div>
      ) : (
        <form className="form-grid" onSubmit={gonder}>
          <section className="card block">
            <div className="card-title">Genel</div>
            <div className="grid-2">
              <label className="field">
                <span>Laboratuvar</span>
                <select value={partnershipId} onChange={(e) => setPartnershipId(e.target.value)}>
                  {partnerships.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.labAd}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>Ölçü tipi</span>
                <select value={olcuTipi} onChange={(e) => setOlcuTipi(e.target.value as MeasureType)}>
                  <option value="STL">STL (dijital)</option>
                  <option value="FIZIKSEL">Fiziksel</option>
                </select>
              </label>
              <label className="field">
                <span>Hasta (kliniğinde kalır)</span>
                <select value={patientId} onChange={(e) => seatientDegis(e.target.value)}>
                  <option value="">— Seçme —</option>
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.ad}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>Hasta rumuzu (laba gider)</span>
                <input
                  value={hastaRumuzu}
                  onChange={(e) => setHastaRumuzu(e.target.value)}
                  placeholder="ör. C.C."
                  required
                />
              </label>
              <label className="field">
                <span>Teslim tarihi</span>
                <input type="date" value={teslimTarihi} onChange={(e) => setTeslimTarihi(e.target.value)} />
              </label>
              <label className="field full">
                <span>Açıklama</span>
                <textarea
                  value={genelNot}
                  onChange={(e) => setGenelNot(e.target.value)}
                  placeholder="Vaka için genel açıklama / laba iletmek istediğin notlar (opsiyonel)"
                  rows={3}
                />
              </label>
            </div>
          </section>

          <section className="card block">
            <div className="card-title">Kalemler</div>
            <div className="items-edit">
              {items.map((r) => {
                const h = rowHesap(r);
                return (
                  <div className="item-row" key={r.key}>
                    <label className="field grow">
                      <span>Hizmet</span>
                      <select
                        value={r.serviceItemId}
                        onChange={(e) => updateRow(r.key, { serviceItemId: e.target.value })}
                      >
                        <option value="">— Seç —</option>
                        {prices.map((p) => (
                          <option key={p.serviceItemId} value={p.serviceItemId}>
                            {p.serviceItemAd} — {formatTutar(p.fiyat)}/{p.serviceItemBirim === "DIS" ? "diş" : "adet"}
                          </option>
                        ))}
                      </select>
                    </label>

                    {h.birim === "ADET" ? (
                      <label className="field w-adet">
                        <span>Adet</span>
                        <input
                          type="number"
                          min={1}
                          value={r.adet}
                          onChange={(e) => updateRow(r.key, { adet: Math.max(1, Number(e.target.value) || 1) })}
                        />
                      </label>
                    ) : (
                      <label className="field w-dis">
                        <span>Diş no (FDI)</span>
                        <input
                          value={r.disText}
                          onChange={(e) => updateRow(r.key, { disText: e.target.value })}
                          placeholder="11 12 13"
                          className={h.gecersizDis && r.disText ? "input-error" : ""}
                          disabled={!r.serviceItemId}
                        />
                      </label>
                    )}

                    <label className="field w-mat">
                      <span>Materyal</span>
                      <input value={r.materyal} onChange={(e) => updateRow(r.key, { materyal: e.target.value })} />
                    </label>
                    <label className="field w-renk">
                      <span>Renk</span>
                      <input value={r.renk} onChange={(e) => updateRow(r.key, { renk: e.target.value })} placeholder="A2" />
                    </label>

                    <div className="row-total">
                      <span className="rt-k">Ara toplam</span>
                      <span className="rt-v mono">{formatTutar(h.araToplam)}</span>
                    </div>

                    <button
                      type="button"
                      className="row-del"
                      onClick={() => removeRow(r.key)}
                      disabled={items.length === 1}
                      aria-label="Kalemi sil"
                    >
                      ✕
                    </button>

                    <label className="field item-aciklama">
                      <span>Açıklama (bu kalem)</span>
                      <input
                        value={r.aciklama}
                        onChange={(e) => updateRow(r.key, { aciklama: e.target.value })}
                        placeholder="ör. Kesim çizgisine dikkat, komşu diş rengiyle uyumlu"
                      />
                    </label>
                  </div>
                );
              })}
            </div>

            <button type="button" className="btn btn-ghost add-item" onClick={() => setItems((r) => [...r, bosRow()])}>
              + Kalem ekle
            </button>
          </section>

          <section className="card block">
            <div className="card-title">Sesli not & fotoğraflar (opsiyonel)</div>
            <p className="page-desc" style={{ marginTop: 0 }}>
              Yazmak yerine sesli not bırakabilir, ağız içi fotoğraf ekleyebilirsin. Vaka oluşunca eklenir.
            </p>
            <div className="media-box">
              <div className="media-row">
                <VoiceRecorder
                  onRecorded={(f) => setSesFile(f)}
                  etiket={sesFile ? "🎤 Yeniden kaydet" : "🎤 Sesli not kaydet"}
                />
                {sesFile && (
                  <span className="media-chip">
                    🎤 {(sesFile.size / 1024).toFixed(0)} KB
                    <button type="button" title="Kaldır" onClick={() => setSesFile(null)}>
                      ×
                    </button>
                  </span>
                )}
              </div>
              <div className="media-row">
                <input
                  ref={fotoRef}
                  type="file"
                  accept="image/*"
                  multiple
                  hidden
                  onChange={(e) => {
                    const secilen = Array.from(e.target.files ?? []);
                    if (secilen.length) setFotolar((prev) => [...prev, ...secilen]);
                    e.target.value = "";
                  }}
                />
                <button type="button" className="btn btn-ghost" onClick={() => fotoRef.current?.click()}>
                  🖼️ Fotoğraf ekle
                </button>
                {fotolar.length > 0 && (
                  <span className="media-chip">
                    {fotolar.length} fotoğraf
                    <button type="button" title="Temizle" onClick={() => setFotolar([])}>
                      ×
                    </button>
                  </span>
                )}
              </div>
              {fotolar.length > 0 && (
                <div className="foto-thumbs">
                  {fotolar.map((f, i) => (
                    <img key={i} src={URL.createObjectURL(f)} alt={f.name} />
                  ))}
                </div>
              )}
            </div>
          </section>

          {hata && <div className="alert alert-error">{hata}</div>}

          <div className="submit-bar">
            <div className="submit-total">
              <span>Toplam</span>
              <strong className="mono">{formatTutar(toplam)}</strong>
            </div>
            <button className="btn btn-primary" type="submit" disabled={gonderiliyor}>
              {gonderiliyor ? "Oluşturuluyor…" : "Vakayı oluştur"}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
