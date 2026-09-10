import { useAuth } from "../auth/AuthContext";
import { ROL_ETIKET } from "../api/types";

export default function DashboardPage() {
  const { me, activeMembership } = useAuth();
  if (!me) return null;

  const isLab = activeMembership?.orgTip === "LAB";

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Panel</div>
        <h1 className="page-title">Merhaba, {me.ad.split(" ")[0]}</h1>
        <p className="page-desc">
          {activeMembership
            ? `${activeMembership.orgAd} · ${isLab ? "Laboratuvar" : "Klinik"} bağlamındasın.`
            : "Henüz bir organizasyona bağlı değilsin."}
        </p>
      </div>

      <div className="card-grid">
        <section className="card">
          <div className="card-title">Hesap</div>
          <dl className="kv">
            <div>
              <dt>Kullanıcı adı</dt>
              <dd className="mono">{me.kullaniciAdi}</dd>
            </div>
            <div>
              <dt>Ad</dt>
              <dd>{me.ad}</dd>
            </div>
            <div>
              <dt>E-posta</dt>
              <dd>{me.email ?? "—"}</dd>
            </div>
            <div>
              <dt>Telefon</dt>
              <dd>{me.telefon ?? "—"}</dd>
            </div>
          </dl>
        </section>

        <section className="card">
          <div className="card-title">Organizasyonların ({me.memberships.length})</div>
          <ul className="member-list">
            {me.memberships.map((m) => (
              <li key={m.orgId} className={m.orgId === activeMembership?.orgId ? "active" : ""}>
                <div className="mem-main">
                  <span className={`badge ${m.orgTip === "LAB" ? "badge-lab" : "badge-klinik"}`}>
                    {m.orgTip === "LAB" ? "LAB" : "KLİNİK"}
                  </span>
                  <span className="mem-ad">{m.orgAd}</span>
                </div>
                <span className="mem-rol">{ROL_ETIKET[m.rol]}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <div className="note">
        Sonraki sayfalar sırayla ekleniyor: vaka listesi, vaka detayı &amp; onay akışı,
        yeni vaka, katalog &amp; fiyat, cari hesap.
      </div>
    </div>
  );
}
