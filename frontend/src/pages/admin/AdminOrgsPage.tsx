import { useEffect, useState } from "react";
import { ApiError } from "../../api/client";
import { ROL_ETIKET, type OrgTip, type Rol } from "../../api/types";
import { ROLLER_BY_TIP, type Member, type Organization } from "../../api/org";
import {
  addMember,
  createOrganization,
  listMembers,
  listOrganizations,
  removeMember,
} from "../../api/admin";

export default function AdminOrgsPage() {
  const [orgs, setOrgs] = useState<Organization[] | null>(null);
  const [hata, setHata] = useState<string | null>(null);

  // yeni org
  const [tip, setTip] = useState<OrgTip>("KLINIK");
  const [ad, setAd] = useState("");
  const [telefon, setTelefon] = useState("");
  const [orgBusy, setOrgBusy] = useState(false);
  const [orgHata, setOrgHata] = useState<string | null>(null);

  // seçili org + üyeler
  const [selected, setSelected] = useState<Organization | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [kullaniciAdi, setKullaniciAdi] = useState("");
  const [rol, setRol] = useState<Rol>("HEKIM");
  const [uyeBusy, setUyeBusy] = useState(false);
  const [uyeHata, setUyeHata] = useState<string | null>(null);

  async function yukle() {
    setHata(null);
    try {
      setOrgs(await listOrganizations());
    } catch (e) {
      setHata(e instanceof ApiError ? e.message : "Yüklenemedi.");
      setOrgs([]);
    }
  }
  useEffect(() => {
    yukle();
  }, []);

  async function orgSec(o: Organization) {
    setSelected(o);
    setUyeHata(null);
    setRol(o.tip === "LAB" ? "LAB_TEKNISYEN" : "HEKIM");
    setMembers([]);
    try {
      setMembers(await listMembers(o.id));
    } catch {
      setMembers([]);
    }
  }

  async function orgEkle(e: React.FormEvent) {
    e.preventDefault();
    setOrgHata(null);
    if (!ad.trim()) return setOrgHata("Ad zorunlu.");
    setOrgBusy(true);
    try {
      await createOrganization({ tip, ad: ad.trim(), telefon: telefon || null });
      setAd("");
      setTelefon("");
      await yukle();
    } catch (err) {
      setOrgHata(err instanceof ApiError ? err.message : "Oluşturulamadı.");
    } finally {
      setOrgBusy(false);
    }
  }

  async function uyeCikar(membershipId: string) {
    if (!selected) return;
    if (!window.confirm("Bu üye organizasyondan çıkarılacak (pasifleştirilecek). Devam?")) return;
    try {
      await removeMember(membershipId);
      setMembers(await listMembers(selected.id));
    } catch (err) {
      setUyeHata(err instanceof ApiError ? err.message : "Üye çıkarılamadı.");
    }
  }

  async function uyeEkle(e: React.FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setUyeHata(null);
    if (!kullaniciAdi.trim()) return setUyeHata("Kullanıcı adı gir.");
    setUyeBusy(true);
    try {
      await addMember(selected.id, { kullaniciAdi: kullaniciAdi.trim(), rol });
      setKullaniciAdi("");
      setMembers(await listMembers(selected.id));
    } catch (err) {
      setUyeHata(err instanceof ApiError ? err.message : "Üye eklenemedi.");
    } finally {
      setUyeBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Platform</div>
        <h1 className="page-title">Organizasyonlar</h1>
        <p className="page-desc">Laboratuvar ve klinik oluştur; içlerine kullanıcı (görevli) ata.</p>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      <section className="card block">
        <div className="card-title">Yeni organizasyon</div>
        <form className="member-form" onSubmit={orgEkle}>
          <label className="field">
            <span>Tip</span>
            <select value={tip} onChange={(e) => setTip(e.target.value as OrgTip)}>
              <option value="LAB">Laboratuvar</option>
              <option value="KLINIK">Klinik</option>
            </select>
          </label>
          <label className="field grow">
            <span>Ad</span>
            <input value={ad} onChange={(e) => setAd(e.target.value)} placeholder="ör. Şen Diş Laboratuvarı" />
          </label>
          <label className="field">
            <span>Telefon</span>
            <input value={telefon} onChange={(e) => setTelefon(e.target.value)} placeholder="opsiyonel" />
          </label>
          <button className="btn btn-primary" type="submit" disabled={orgBusy}>
            {orgBusy ? "…" : "+ Oluştur"}
          </button>
        </form>
        {orgHata && <div className="alert alert-error">{orgHata}</div>}
      </section>

      {orgs && orgs.length > 0 && (
        <div className="table-wrap block">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ad</th>
                <th>Tip</th>
                <th>Telefon</th>
                <th aria-label="yönet"></th>
              </tr>
            </thead>
            <tbody>
              {orgs.map((o) => (
                <tr key={o.id} className={selected?.id === o.id ? "row-active" : ""}>
                  <td>{o.ad}</td>
                  <td>
                    <span className={`badge ${o.tip === "LAB" ? "badge-lab" : "badge-klinik"}`}>
                      {o.tip === "LAB" ? "LAB" : "KLİNİK"}
                    </span>
                  </td>
                  <td className="muted-cell mono">{o.telefon ?? "—"}</td>
                  <td className="num">
                    <button className="mini-btn" onClick={() => orgSec(o)}>
                      Üyeler
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && (
        <section className="card block">
          <div className="card-title">
            {selected.ad} · üyeler ({members.length})
          </div>
          {members.length === 0 ? (
            <div className="empty sm">Henüz üye yok.</div>
          ) : (
            <ul className="member-list">
              {members.map((m) => (
                <li key={m.id}>
                  <div className="mem-main">
                    <span className="mem-ad">{m.userAd}</span>
                  </div>
                  <span className="mem-rol">{ROL_ETIKET[m.rol]}</span>
                  <button className="mini-btn danger-btn" onClick={() => uyeCikar(m.id)}>
                    Çıkar
                  </button>
                </li>
              ))}
            </ul>
          )}

          <form className="member-form" onSubmit={uyeEkle}>
            <label className="field grow">
              <span>Kullanıcı adı</span>
              <input value={kullaniciAdi} onChange={(e) => setKullaniciAdi(e.target.value)} placeholder="mevcut kullanıcı adı" />
            </label>
            <label className="field">
              <span>Rol</span>
              <select value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
                {ROLLER_BY_TIP[selected.tip].map((r) => (
                  <option key={r} value={r}>
                    {ROL_ETIKET[r]}
                  </option>
                ))}
              </select>
            </label>
            <button className="btn btn-primary" type="submit" disabled={uyeBusy}>
              {uyeBusy ? "…" : "Üye ekle"}
            </button>
          </form>
          {uyeHata && <div className="alert alert-error">{uyeHata}</div>}
        </section>
      )}
    </div>
  );
}
