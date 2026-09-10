import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { ROL_ETIKET, type Rol } from "../api/types";
import { getPartnerships, type Partnership } from "../api/catalog";
import {
  addMember,
  createPartnership,
  getOrganization,
  ROLLER_BY_TIP,
  type Member,
  type Organization,
} from "../api/org";

export default function OrgPage() {
  const { activeMembership, activeOrgId } = useAuth();
  const isLab = activeMembership?.orgTip === "LAB";
  const admin = activeMembership?.rol === "LAB_ADMIN" || activeMembership?.rol === "KLINIK_ADMIN";

  const [org, setOrg] = useState<Organization | null>(null);
  const [partnerships, setPartnerships] = useState<Partnership[]>([]);
  const [hata, setHata] = useState<string | null>(null);

  // üye ekle
  const [kullaniciAdi, setKullaniciAdi] = useState("");
  const [rol, setRol] = useState<Rol>(isLab ? "LAB_TEKNISYEN" : "HEKIM");
  const [uyeBusy, setUyeBusy] = useState(false);
  const [uyeSonuc, setUyeSonuc] = useState<Member | null>(null);
  const [uyeHata, setUyeHata] = useState<string | null>(null);

  // ortaklık ekle
  const [karsiId, setKarsiId] = useState("");
  const [vade, setVade] = useState("30");
  const [ortBusy, setOrtBusy] = useState(false);
  const [ortHata, setOrtHata] = useState<string | null>(null);

  async function ortakliklariYukle() {
    setPartnerships(await getPartnerships());
  }

  useEffect(() => {
    if (!activeOrgId) return;
    let iptal = false;
    Promise.all([getOrganization(activeOrgId), getPartnerships()])
      .then(([o, ps]) => {
        if (iptal) return;
        setOrg(o);
        setPartnerships(ps);
      })
      .catch((e) => !iptal && setHata(e instanceof ApiError ? e.message : "Yüklenemedi."));
    return () => {
      iptal = true;
    };
  }, [activeOrgId]);

  async function uyeEkle(e: React.FormEvent) {
    e.preventDefault();
    setUyeHata(null);
    setUyeSonuc(null);
    if (!kullaniciAdi.trim()) {
      setUyeHata("Kullanıcı adı gir.");
      return;
    }
    setUyeBusy(true);
    try {
      const m = await addMember(activeOrgId!, { kullaniciAdi: kullaniciAdi.trim(), rol });
      setUyeSonuc(m);
      setKullaniciAdi("");
    } catch (err) {
      setUyeHata(err instanceof ApiError ? err.message : "Üye eklenemedi.");
    } finally {
      setUyeBusy(false);
    }
  }

  async function ortaklikEkle(e: React.FormEvent) {
    e.preventDefault();
    setOrtHata(null);
    if (!karsiId.trim()) {
      setOrtHata(`${isLab ? "Klinik" : "Laboratuvar"} organizasyon kimliği (UUID) gir.`);
      return;
    }
    setOrtBusy(true);
    try {
      const body = isLab
        ? { labId: activeOrgId!, clinicId: karsiId.trim(), vadeGun: Number(vade) || 0 }
        : { labId: karsiId.trim(), clinicId: activeOrgId!, vadeGun: Number(vade) || 0 };
      await createPartnership(body);
      setKarsiId("");
      await ortakliklariYukle();
    } catch (err) {
      setOrtHata(err instanceof ApiError ? err.message : "Ortaklık kurulamadı.");
    } finally {
      setOrtBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Yönetim</div>
        <h1 className="page-title">Organizasyon</h1>
        <p className="page-desc">Organizasyon bilgileri, üyeler ve ortaklıklar.</p>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      {org && (
        <section className="card block">
          <div className="card-title">Bilgiler</div>
          <dl className="kv">
            <div>
              <dt>Ad</dt>
              <dd>{org.ad}</dd>
            </div>
            <div>
              <dt>Tip</dt>
              <dd>
                <span className={`badge ${org.tip === "LAB" ? "badge-lab" : "badge-klinik"}`}>
                  {org.tip === "LAB" ? "LAB" : "KLİNİK"}
                </span>
              </dd>
            </div>
            <div>
              <dt>Telefon</dt>
              <dd>{org.telefon ?? "—"}</dd>
            </div>
            <div>
              <dt>Vergi no</dt>
              <dd className="mono">{org.vergiNo ?? "—"}</dd>
            </div>
            <div>
              <dt>Adres</dt>
              <dd>{org.adres ?? "—"}</dd>
            </div>
            <div>
              <dt>Kimlik (UUID)</dt>
              <dd className="mono small-id">{org.id}</dd>
            </div>
          </dl>
        </section>
      )}

      {admin && (
        <section className="card block">
          <div className="card-title">Üye ekle</div>
          <form className="member-form" onSubmit={uyeEkle}>
            <label className="field grow">
              <span>Kullanıcı adı</span>
              <input
                value={kullaniciAdi}
                onChange={(e) => setKullaniciAdi(e.target.value)}
                placeholder="ör. teknisyen2"
              />
            </label>
            <label className="field">
              <span>Rol</span>
              <select value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
                {(org ? ROLLER_BY_TIP[org.tip] : []).map((r) => (
                  <option key={r} value={r}>
                    {ROL_ETIKET[r]}
                  </option>
                ))}
              </select>
            </label>
            <button className="btn btn-primary" type="submit" disabled={uyeBusy}>
              {uyeBusy ? "Ekleniyor…" : "Ekle"}
            </button>
          </form>
          {uyeSonuc && (
            <div className="alert alert-ok">
              Eklendi: <strong>{uyeSonuc.userAd}</strong> — {ROL_ETIKET[uyeSonuc.rol]}
            </div>
          )}
          {uyeHata && <div className="alert alert-error">{uyeHata}</div>}
          <p className="hint">
            Kullanıcı sistemde kayıtlı olmalı. (Üye listeleme API'si henüz yok; ekleme sonucu
            burada onaylanır.)
          </p>
        </section>
      )}

      <section className="card block">
        <div className="card-title">Ortaklıklar ({partnerships.length})</div>
        {partnerships.length === 0 ? (
          <div className="empty sm">Ortaklık yok.</div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>{isLab ? "Klinik" : "Laboratuvar"}</th>
                  <th>Durum</th>
                  <th className="num">Vade (gün)</th>
                </tr>
              </thead>
              <tbody>
                {partnerships.map((p) => (
                  <tr key={p.id}>
                    <td>{isLab ? p.clinicAd : p.labAd}</td>
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

        {admin && (
          <form className="member-form" onSubmit={ortaklikEkle}>
            <label className="field grow">
              <span>{isLab ? "Klinik" : "Laboratuvar"} kimliği (UUID)</span>
              <input value={karsiId} onChange={(e) => setKarsiId(e.target.value)} placeholder="00000000-0000-…" />
            </label>
            <label className="field w-adet">
              <span>Vade</span>
              <input type="number" min={0} value={vade} onChange={(e) => setVade(e.target.value)} />
            </label>
            <button className="btn btn-ghost" type="submit" disabled={ortBusy}>
              {ortBusy ? "Kuruluyor…" : "+ Ortaklık"}
            </button>
          </form>
        )}
        {ortHata && <div className="alert alert-error">{ortHata}</div>}
      </section>
    </div>
  );
}
