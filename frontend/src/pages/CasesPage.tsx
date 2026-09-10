import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import {
  CASE_STATUS_META,
  CASE_STATUS_ORDER,
  formatTarih,
  formatTutar,
  getCases,
  type CaseStatus,
  type DentalCase,
} from "../api/cases";

export function StatusBadge({ durum }: { durum: CaseStatus }) {
  const m = CASE_STATUS_META[durum];
  return <span className={`status-badge ${m.cls}`}>{m.label}</span>;
}

export default function CasesPage() {
  const { activeMembership, activeOrgId } = useAuth();
  const navigate = useNavigate();
  const isLab = activeMembership?.orgTip === "LAB";

  const [cases, setCases] = useState<DentalCase[] | null>(null);
  const [hata, setHata] = useState<string | null>(null);
  const [filtre, setFiltre] = useState<CaseStatus | "HEPSI">("HEPSI");

  useEffect(() => {
    let iptal = false;
    setCases(null);
    setHata(null);
    getCases()
      .then((d) => !iptal && setCases(d))
      .catch((e) => {
        if (iptal) return;
        setHata(e instanceof ApiError ? e.message : "Vakalar yüklenemedi.");
        setCases([]);
      });
    return () => {
      iptal = true;
    };
  }, [activeOrgId]);

  const sayimlar = useMemo(() => {
    const m = new Map<CaseStatus, number>();
    (cases ?? []).forEach((c) => m.set(c.durum, (m.get(c.durum) ?? 0) + 1));
    return m;
  }, [cases]);

  const gorunen = useMemo(
    () => (filtre === "HEPSI" ? cases ?? [] : (cases ?? []).filter((c) => c.durum === filtre)),
    [cases, filtre]
  );

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Vakalar</div>
        <h1 className="page-title">Vaka listesi</h1>
        <p className="page-desc">
          {isLab
            ? "Laboratuvarına gelen işler. Hasta kimliği görünmez; yalnızca rumuz."
            : "Kliniğinin açtığı işler ve durumları."}
        </p>
      </div>

      <div className="filter-row">
        <button
          className={`filter-chip ${filtre === "HEPSI" ? "on" : ""}`}
          onClick={() => setFiltre("HEPSI")}
        >
          Tümü <span className="fc-count">{cases?.length ?? "…"}</span>
        </button>
        {CASE_STATUS_ORDER.filter((s) => (sayimlar.get(s) ?? 0) > 0).map((s) => (
          <button
            key={s}
            className={`filter-chip ${filtre === s ? "on" : ""}`}
            onClick={() => setFiltre(s)}
          >
            {CASE_STATUS_META[s].label} <span className="fc-count">{sayimlar.get(s)}</span>
          </button>
        ))}
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      {cases === null && !hata && (
        <div className="skeleton-list">
          {[0, 1, 2].map((i) => (
            <div className="skeleton-row" key={i} />
          ))}
        </div>
      )}

      {cases !== null && gorunen.length === 0 && !hata && (
        <div className="empty">
          {cases.length === 0 ? "Henüz vaka yok." : "Bu durumda vaka yok."}
        </div>
      )}

      {gorunen.length > 0 && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Kod</th>
                <th>Durum</th>
                <th>{isLab ? "Rumuz" : "Hasta"}</th>
                <th>Ölçü</th>
                <th className="num">Tutar</th>
                <th>Teslim</th>
                <th aria-label="aç"></th>
              </tr>
            </thead>
            <tbody>
              {gorunen.map((c) => (
                <tr key={c.id} className="row-click" onClick={() => navigate(`/cases/${c.id}`)}>
                  <td className="mono">{c.kod}</td>
                  <td>
                    <StatusBadge durum={c.durum} />
                  </td>
                  <td>{isLab ? c.hastaRumuzu : c.patientAd ?? c.hastaRumuzu}</td>
                  <td className="muted-cell">{c.olcuTipi === "STL" ? "STL" : "Fiziksel"}</td>
                  <td className="num mono">{formatTutar(c.toplamTutar)}</td>
                  <td className="muted-cell">{formatTarih(c.teslimTarihi)}</td>
                  <td className="chev">›</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
