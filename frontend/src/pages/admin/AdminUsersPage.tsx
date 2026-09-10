import { useEffect, useState } from "react";
import { ApiError } from "../../api/client";
import { formatTarih } from "../../api/cases";
import { createUser, listUsers, type AdminUser } from "../../api/admin";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[] | null>(null);
  const [hata, setHata] = useState<string | null>(null);

  const [form, setForm] = useState(false);
  const [kullaniciAdi, setKullaniciAdi] = useState("");
  const [ad, setAd] = useState("");
  const [parola, setParola] = useState("");
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [formHata, setFormHata] = useState<string | null>(null);

  async function yukle() {
    setHata(null);
    try {
      setUsers(await listUsers());
    } catch (e) {
      setHata(e instanceof ApiError ? e.message : "Kullanıcılar yüklenemedi.");
      setUsers([]);
    }
  }

  useEffect(() => {
    yukle();
  }, []);

  async function ekle(e: React.FormEvent) {
    e.preventDefault();
    setFormHata(null);
    if (!kullaniciAdi.trim() || !ad.trim() || !parola) {
      setFormHata("Kullanıcı adı, ad ve parola zorunlu.");
      return;
    }
    setBusy(true);
    try {
      await createUser({ kullaniciAdi: kullaniciAdi.trim(), ad: ad.trim(), parola, email: email || null });
      setKullaniciAdi("");
      setAd("");
      setParola("");
      setEmail("");
      setForm(false);
      await yukle();
    } catch (err) {
      setFormHata(err instanceof ApiError ? err.message : "Kullanıcı oluşturulamadı.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="page-head list-head">
        <div>
          <div className="eyebrow">Platform</div>
          <h1 className="page-title">Kullanıcılar</h1>
          <p className="page-desc">Sistemdeki tüm kullanıcılar. Yeni kullanıcı oluştur, sonra organizasyona ata.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setForm((v) => !v)}>
          {form ? "Kapat" : "+ Yeni kullanıcı"}
        </button>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      {form && (
        <form className="card block inline-form" onSubmit={ekle}>
          <div className="card-title">Yeni kullanıcı</div>
          <div className="grid-2">
            <label className="field">
              <span>Kullanıcı adı</span>
              <input value={kullaniciAdi} onChange={(e) => setKullaniciAdi(e.target.value)} placeholder="ör. drmehmet" autoFocus />
            </label>
            <label className="field">
              <span>Ad soyad</span>
              <input value={ad} onChange={(e) => setAd(e.target.value)} placeholder="Dr. Mehmet Ak" />
            </label>
            <label className="field">
              <span>Parola</span>
              <input type="password" value={parola} onChange={(e) => setParola(e.target.value)} placeholder="••••••••" />
            </label>
            <label className="field">
              <span>E-posta (opsiyonel)</span>
              <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="opsiyonel" />
            </label>
          </div>
          {formHata && <div className="alert alert-error">{formHata}</div>}
          <div className="inline-form-bar">
            <button type="button" className="btn btn-ghost" onClick={() => setForm(false)}>
              Vazgeç
            </button>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? "Oluşturuluyor…" : "Oluştur"}
            </button>
          </div>
        </form>
      )}

      {users === null && !hata && (
        <div className="skeleton-list">
          <div className="skeleton-row" />
          <div className="skeleton-row" />
        </div>
      )}

      {users && users.length > 0 && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Kullanıcı adı</th>
                <th>Ad soyad</th>
                <th>E-posta</th>
                <th>Rol</th>
                <th>Oluşturma</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td className="mono">{u.kullaniciAdi}</td>
                  <td>{u.ad}</td>
                  <td className="muted-cell">{u.email ?? "—"}</td>
                  <td>
                    {u.superAdmin ? (
                      <span className="status-badge st-tamam">Süper Admin</span>
                    ) : (
                      <span className="muted-cell">—</span>
                    )}
                  </td>
                  <td className="muted-cell">{formatTarih(u.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
