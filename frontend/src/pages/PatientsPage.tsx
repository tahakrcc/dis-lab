import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { formatTarih } from "../api/cases";
import { createPatient, getPatients, type Patient } from "../api/catalog";

export default function PatientsPage() {
  const { activeMembership, activeOrgId } = useAuth();
  const isKlinik = activeMembership?.orgTip === "KLINIK";

  const [hastalar, setHastalar] = useState<Patient[] | null>(null);
  const [hata, setHata] = useState<string | null>(null);

  const [form, setForm] = useState(false);
  const [ad, setAd] = useState("");
  const [telefon, setTelefon] = useState("");
  const [dogumTarihi, setDogumTarihi] = useState("");
  const [not, setNot] = useState("");
  const [busy, setBusy] = useState(false);
  const [formHata, setFormHata] = useState<string | null>(null);

  async function yukle() {
    setHata(null);
    try {
      setHastalar(await getPatients());
    } catch (e) {
      setHata(e instanceof ApiError ? e.message : "Hastalar yüklenemedi.");
      setHastalar([]);
    }
  }

  useEffect(() => {
    if (isKlinik) yukle();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeOrgId, isKlinik]);

  async function ekle(e: React.FormEvent) {
    e.preventDefault();
    setFormHata(null);
    if (!ad.trim()) {
      setFormHata("Hasta adı zorunludur.");
      return;
    }
    setBusy(true);
    try {
      await createPatient({
        ad: ad.trim(),
        telefon: telefon || null,
        dogumTarihi: dogumTarihi || null,
        not: not || null,
      });
      setAd("");
      setTelefon("");
      setDogumTarihi("");
      setNot("");
      setForm(false);
      await yukle();
    } catch (err) {
      setFormHata(err instanceof ApiError ? err.message : "Hasta eklenemedi.");
    } finally {
      setBusy(false);
    }
  }

  if (!isKlinik) {
    return (
      <div className="page">
        <div className="empty">Hasta kayıtları yalnızca klinik kullanıcılarına açıktır.</div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-head list-head">
        <div>
          <div className="eyebrow">Hastalar</div>
          <h1 className="page-title">Hastalar</h1>
          <p className="page-desc">
            Hasta kayıtların kliniğinde kalır — laboratuvara asla gitmez. Vakada laba yalnızca
            rumuz iletilir.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setForm((v) => !v)}>
          {form ? "Kapat" : "+ Yeni hasta"}
        </button>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      {form && (
        <form className="card block inline-form" onSubmit={ekle}>
          <div className="card-title">Yeni hasta</div>
          <div className="grid-2">
            <label className="field">
              <span>Ad soyad</span>
              <input value={ad} onChange={(e) => setAd(e.target.value)} placeholder="Ad Soyad" autoFocus />
            </label>
            <label className="field">
              <span>Telefon</span>
              <input value={telefon} onChange={(e) => setTelefon(e.target.value)} placeholder="05xx…" />
            </label>
            <label className="field">
              <span>Doğum tarihi</span>
              <input type="date" value={dogumTarihi} onChange={(e) => setDogumTarihi(e.target.value)} />
            </label>
            <label className="field">
              <span>Not</span>
              <input value={not} onChange={(e) => setNot(e.target.value)} placeholder="opsiyonel" />
            </label>
          </div>
          {formHata && <div className="alert alert-error">{formHata}</div>}
          <div className="inline-form-bar">
            <button type="button" className="btn btn-ghost" onClick={() => setForm(false)}>
              Vazgeç
            </button>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? "Kaydediliyor…" : "Kaydet"}
            </button>
          </div>
        </form>
      )}

      {hastalar === null && !hata && (
        <div className="skeleton-list">
          <div className="skeleton-row" />
          <div className="skeleton-row" />
        </div>
      )}

      {hastalar !== null && hastalar.length === 0 && !hata && (
        <div className="empty">Henüz hasta yok. “+ Yeni hasta” ile ekleyebilirsin.</div>
      )}

      {hastalar && hastalar.length > 0 && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ad soyad</th>
                <th>Telefon</th>
                <th>Doğum tarihi</th>
                <th>Not</th>
              </tr>
            </thead>
            <tbody>
              {hastalar.map((p) => (
                <tr key={p.id}>
                  <td>{p.ad}</td>
                  <td className="muted-cell mono">{p.telefon ?? "—"}</td>
                  <td className="muted-cell">{formatTarih(p.dogumTarihi)}</td>
                  <td className="muted-cell">{p.not ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
