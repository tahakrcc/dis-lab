import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { formatTarih, formatTutar } from "../api/cases";
import { getPartnerships, type Partnership } from "../api/catalog";
import {
  createAdjustment,
  createPayment,
  getLedger,
  LEDGER_META,
  type LedgerReport,
} from "../api/ledger";

const bugun = () => new Date().toISOString().slice(0, 10);

export default function LedgerPage() {
  const { activeMembership, activeOrgId } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";
  const yonetici = activeMembership?.rol === "LAB_ADMIN" || activeMembership?.rol === "KLINIK_ADMIN";

  const [partnerships, setPartnerships] = useState<Partnership[]>([]);
  const [partnershipId, setPartnershipId] = useState("");
  const [rapor, setRapor] = useState<LedgerReport | null>(null);
  const [hata, setHata] = useState<string | null>(null);

  const [form, setForm] = useState<"none" | "tahsilat" | "duzeltme">("none");
  const [tutar, setTutar] = useState("");
  const [yontem, setYontem] = useState("Havale");
  const [belgeTarihi, setBelgeTarihi] = useState(bugun());
  const [aciklama, setAciklama] = useState("");
  const [gonderiliyor, setGonderiliyor] = useState(false);
  const [formHata, setFormHata] = useState<string | null>(null);

  useEffect(() => {
    let iptal = false;
    getPartnerships()
      .then((ps) => {
        if (iptal) return;
        setPartnerships(ps);
        setPartnershipId((cur) => cur || ps[0]?.id || "");
      })
      .catch((e) => !iptal && setHata(e instanceof ApiError ? e.message : "Ortaklıklar yüklenemedi."));
    return () => {
      iptal = true;
    };
  }, [activeOrgId]);

  async function yukle(pid: string) {
    setRapor(null);
    setHata(null);
    try {
      setRapor(await getLedger(pid));
    } catch (e) {
      setHata(e instanceof ApiError ? e.message : "Cari hesap yüklenemedi.");
    }
  }

  useEffect(() => {
    if (partnershipId) yukle(partnershipId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [partnershipId]);

  const karsiTaraf = useMemo(() => {
    const p = partnerships.find((x) => x.id === partnershipId);
    if (!p) return "";
    return isLab ? p.clinicAd : p.labAd;
  }, [partnerships, partnershipId, isLab]);

  function formAc(tip: "tahsilat" | "duzeltme") {
    setForm(tip);
    setFormHata(null);
    setTutar("");
    setAciklama("");
    setYontem("Havale");
    setBelgeTarihi(bugun());
  }

  async function gonder(e: React.FormEvent) {
    e.preventDefault();
    setFormHata(null);
    const t = Number(tutar.replace(",", "."));
    if (!t || (form === "tahsilat" && t <= 0)) {
      setFormHata("Geçerli bir tutar gir.");
      return;
    }
    setGonderiliyor(true);
    try {
      if (form === "tahsilat") {
        await createPayment(partnershipId, { tutar: t, yontem, belgeTarihi, aciklama: aciklama || null });
      } else {
        await createAdjustment(partnershipId, { tutar: t, aciklama: aciklama || null });
      }
      setForm("none");
      await yukle(partnershipId);
    } catch (err) {
      setFormHata(err instanceof ApiError ? err.message : "İşlem başarısız.");
    } finally {
      setGonderiliyor(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head list-head">
        <div>
          <div className="eyebrow">Cari hesap</div>
          <h1 className="page-title">Cari hesap</h1>
          <p className="page-desc">
            {isLab ? "Kliniklerden alacakların ve tahsilatların." : "Laboratuvarlara borcun ve ödemelerin."}
          </p>
        </div>
        {partnerships.length > 1 && (
          <label className="field inline-select">
            <span>{isLab ? "Klinik" : "Laboratuvar"}</span>
            <select value={partnershipId} onChange={(e) => setPartnershipId(e.target.value)}>
              {partnerships.map((p) => (
                <option key={p.id} value={p.id}>
                  {isLab ? p.clinicAd : p.labAd}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      {rapor && (
        <>
          <div className="tiles">
            <div className="tile">
              <div className="tile-k">Toplam borç</div>
              <div className="tile-v">{formatTutar(rapor.toplamBorc)}</div>
            </div>
            <div className="tile">
              <div className="tile-k">Toplam tahsilat</div>
              <div className="tile-v pos">{formatTutar(Math.abs(rapor.toplamTahsilat))}</div>
            </div>
            <div className={`tile ${rapor.bakiye > 0 ? "warn-tile" : "ok-tile"}`}>
              <div className="tile-k">Bakiye {rapor.bakiye > 0 ? (isLab ? "(alacak)" : "(borç)") : ""}</div>
              <div className="tile-v big">{formatTutar(rapor.bakiye)}</div>
              <div className="tile-sub">{karsiTaraf}</div>
            </div>
          </div>

          {yonetici && (
            <div className="ledger-actions">
              <button className="btn btn-ghost" onClick={() => formAc("tahsilat")}>
                + Tahsilat
              </button>
              <button className="btn btn-ghost" onClick={() => formAc("duzeltme")}>
                + Düzeltme
              </button>
            </div>
          )}

          {form !== "none" && (
            <form className="card block inline-form" onSubmit={gonder}>
              <div className="card-title">{form === "tahsilat" ? "Tahsilat ekle" : "Düzeltme ekle"}</div>
              <div className="grid-2">
                <label className="field">
                  <span>Tutar {form === "duzeltme" ? "(+/-)" : "(₺)"}</span>
                  <input value={tutar} onChange={(e) => setTutar(e.target.value)} placeholder="0" inputMode="decimal" />
                </label>
                {form === "tahsilat" && (
                  <>
                    <label className="field">
                      <span>Yöntem</span>
                      <select value={yontem} onChange={(e) => setYontem(e.target.value)}>
                        <option>Havale</option>
                        <option>EFT</option>
                        <option>Nakit</option>
                        <option>Kredi Kartı</option>
                        <option>Çek</option>
                      </select>
                    </label>
                    <label className="field">
                      <span>Belge tarihi</span>
                      <input type="date" value={belgeTarihi} onChange={(e) => setBelgeTarihi(e.target.value)} />
                    </label>
                  </>
                )}
                <label className="field">
                  <span>Açıklama</span>
                  <input value={aciklama} onChange={(e) => setAciklama(e.target.value)} placeholder="opsiyonel" />
                </label>
              </div>
              {formHata && <div className="alert alert-error">{formHata}</div>}
              <div className="inline-form-bar">
                <button type="button" className="btn btn-ghost" onClick={() => setForm("none")}>
                  Vazgeç
                </button>
                <button type="submit" className="btn btn-primary" disabled={gonderiliyor}>
                  {gonderiliyor ? "Kaydediliyor…" : "Kaydet"}
                </button>
              </div>
            </form>
          )}

          <section className="card block">
            <div className="card-title">Hareketler ({rapor.movements.length})</div>
            {rapor.movements.length === 0 ? (
              <div className="empty sm">Henüz hareket yok.</div>
            ) : (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Tarih</th>
                      <th>Tür</th>
                      <th>Açıklama</th>
                      <th className="num">Matrah</th>
                      <th className="num">KDV</th>
                      <th className="num">Tutar</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rapor.movements.map((m) => (
                      <tr key={m.id}>
                        <td className="muted-cell">{formatTarih(m.belgeTarihi)}</td>
                        <td>
                          <span className={`status-badge ${LEDGER_META[m.tur].cls}`}>{LEDGER_META[m.tur].label}</span>
                        </td>
                        <td>
                          {m.aciklama ?? "—"}
                          {m.caseId && (
                            <>
                              {" "}
                              <Link className="mini-link" to={`/cases/${m.caseId}`}>
                                vaka →
                              </Link>
                            </>
                          )}
                        </td>
                        <td className="num mono muted-cell">{m.tur === "BORC" ? formatTutar(m.matrah) : "—"}</td>
                        <td className="num mono muted-cell">{m.tur === "BORC" ? formatTutar(m.kdvTutari) : "—"}</td>
                        <td className={`num mono ${m.tutar < 0 ? "neg" : ""}`}>{formatTutar(m.tutar)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
