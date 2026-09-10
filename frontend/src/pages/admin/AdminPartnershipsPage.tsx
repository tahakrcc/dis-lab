import { useEffect, useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import type { Organization } from "../../api/org";
import type { Partnership } from "../../api/catalog";
import { createPartnership, listOrganizations, listPartnerships } from "../../api/admin";

export default function AdminPartnershipsPage() {
  const [orgs, setOrgs] = useState<Organization[]>([]);
  const [partnerships, setPartnerships] = useState<Partnership[] | null>(null);
  const [hata, setHata] = useState<string | null>(null);

  const [labId, setLabId] = useState("");
  const [clinicId, setClinicId] = useState("");
  const [vade, setVade] = useState("30");
  const [busy, setBusy] = useState(false);
  const [formHata, setFormHata] = useState<string | null>(null);

  const labs = useMemo(() => orgs.filter((o) => o.tip === "LAB"), [orgs]);
  const clinics = useMemo(() => orgs.filter((o) => o.tip === "KLINIK"), [orgs]);

  async function yukle() {
    setHata(null);
    try {
      const [o, p] = await Promise.all([listOrganizations(), listPartnerships()]);
      setOrgs(o);
      setPartnerships(p);
      setLabId((c) => c || o.find((x) => x.tip === "LAB")?.id || "");
      setClinicId((c) => c || o.find((x) => x.tip === "KLINIK")?.id || "");
    } catch (e) {
      setHata(e instanceof ApiError ? e.message : "Yüklenemedi.");
      setPartnerships([]);
    }
  }
  useEffect(() => {
    yukle();
  }, []);

  async function ekle(e: React.FormEvent) {
    e.preventDefault();
    setFormHata(null);
    if (!labId || !clinicId) return setFormHata("Lab ve klinik seç.");
    setBusy(true);
    try {
      await createPartnership({ labId, clinicId, vadeGun: Number(vade) || 0 });
      await yukle();
    } catch (err) {
      setFormHata(err instanceof ApiError ? err.message : "Ortaklık kurulamadı.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Platform</div>
        <h1 className="page-title">Ortaklıklar</h1>
        <p className="page-desc">Laboratuvar ile klinik arasında bağlantı kur; iş ve fiyatlar bu ortaklık üzerinden yürür.</p>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      <section className="card block">
        <div className="card-title">Yeni ortaklık</div>
        <form className="member-form" onSubmit={ekle}>
          <label className="field grow">
            <span>Laboratuvar</span>
            <select value={labId} onChange={(e) => setLabId(e.target.value)}>
              {labs.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.ad}
                </option>
              ))}
            </select>
          </label>
          <label className="field grow">
            <span>Klinik</span>
            <select value={clinicId} onChange={(e) => setClinicId(e.target.value)}>
              {clinics.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.ad}
                </option>
              ))}
            </select>
          </label>
          <label className="field w-adet">
            <span>Vade (gün)</span>
            <input type="number" min={0} value={vade} onChange={(e) => setVade(e.target.value)} />
          </label>
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? "…" : "+ Kur"}
          </button>
        </form>
        {formHata && <div className="alert alert-error">{formHata}</div>}
      </section>

      {partnerships && partnerships.length > 0 && (
        <div className="table-wrap block">
          <table className="data-table">
            <thead>
              <tr>
                <th>Laboratuvar</th>
                <th>Klinik</th>
                <th>Durum</th>
                <th className="num">Vade</th>
              </tr>
            </thead>
            <tbody>
              {partnerships.map((p) => (
                <tr key={p.id}>
                  <td>{p.labAd}</td>
                  <td>{p.clinicAd}</td>
                  <td>
                    <span className={`status-badge ${p.durum === "AKTIF" ? "st-onay" : "st-taslak"}`}>
                      {p.durum === "AKTIF" ? "Aktif" : "Pasif"}
                    </span>
                  </td>
                  <td className="num mono">{p.vadeGun}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
