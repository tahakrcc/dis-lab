import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";

const SEED = [
  { u: "labadmin", label: "Lab Yöneticisi" },
  { u: "teknisyen", label: "Teknisyen" },
  { u: "klinikadmin", label: "Klinik Yöneticisi" },
  { u: "hekim", label: "Hekim" },
];

export default function LoginPage() {
  const { login } = useAuth();
  const [kullaniciAdi, setKullaniciAdi] = useState("");
  const [parola, setParola] = useState("");
  const [hata, setHata] = useState<string | null>(null);
  const [yukleniyor, setYukleniyor] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setHata(null);
    setYukleniyor(true);
    try {
      await login(kullaniciAdi.trim(), parola);
    } catch (err) {
      if (err instanceof ApiError) setHata(err.message);
      else setHata("Sunucuya ulaşılamadı. Backend çalışıyor mu?");
    } finally {
      setYukleniyor(false);
    }
  }

  function seedDoldur(u: string) {
    setKullaniciAdi(u);
    setParola("password123");
    setHata(null);
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-head">
          <span className="brand-tooth lg">🦷</span>
          <div>
            <div className="auth-brand">Köprü</div>
            <div className="auth-sub">Lab ↔ Klinik Platformu</div>
          </div>
        </div>

        <h1 className="auth-title">Giriş yap</h1>
        <p className="auth-lede">Kullanıcı adın ve parolanla oturum aç.</p>

        <form className="form" onSubmit={onSubmit}>
          <label className="field">
            <span>Kullanıcı adı</span>
            <input
              type="text"
              autoComplete="username"
              value={kullaniciAdi}
              onChange={(e) => setKullaniciAdi(e.target.value)}
              placeholder="ör. hekim"
              autoFocus
              required
            />
          </label>

          <label className="field">
            <span>Parola</span>
            <input
              type="password"
              autoComplete="current-password"
              value={parola}
              onChange={(e) => setParola(e.target.value)}
              placeholder="••••••••"
              required
            />
          </label>

          {hata && <div className="alert alert-error" role="alert">{hata}</div>}

          <button className="btn btn-primary btn-block" type="submit" disabled={yukleniyor}>
            {yukleniyor ? "Giriş yapılıyor…" : "Giriş yap"}
          </button>
        </form>

        <div className="seed-box">
          <div className="seed-label">Hızlı giriş (demo · parola: password123)</div>
          <div className="seed-chips">
            {SEED.map((s) => (
              <button key={s.u} type="button" className="chip" onClick={() => seedDoldur(s.u)}>
                {s.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
