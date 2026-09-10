import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ROL_ETIKET } from "../api/types";

export default function Layout() {
  const { me, activeOrgId, activeMembership, setActiveOrg, logout } = useAuth();
  if (!me) return null;

  const isLab = activeMembership?.orgTip === "LAB";

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-tooth">🦷</span>
          <span className="brand-name">Köprü</span>
        </div>

        <nav className="nav">
          <NavLink to="/" end className="nav-link">
            <span className="nav-dot" /> Panel
          </NavLink>
          <NavLink to="/cases" className="nav-link">
            <span className="nav-dot" /> Vakalar
          </NavLink>
          {/* Sonraki sayfalar sırayla buraya eklenecek */}
        </nav>

        <div className="sidebar-foot">
          <div className="ctx-role">{isLab ? "Laboratuvar" : "Klinik"} paneli</div>
        </div>
      </aside>

      <div className="main-col">
        <header className="topbar">
          <div className="org-switch">
            <label htmlFor="org">Aktif organizasyon</label>
            <select
              id="org"
              value={activeOrgId ?? ""}
              onChange={(e) => setActiveOrg(e.target.value)}
            >
              {me.memberships.map((m) => (
                <option key={m.orgId} value={m.orgId}>
                  {m.orgAd} · {m.orgTip === "LAB" ? "Lab" : "Klinik"}
                </option>
              ))}
            </select>
          </div>

          <div className="topbar-right">
            <div className="user-chip">
              <div className="user-ad">{me.ad}</div>
              <div className="user-rol">
                {activeMembership ? ROL_ETIKET[activeMembership.rol] : "—"}
              </div>
            </div>
            <button className="btn btn-ghost" onClick={() => logout()}>
              Çıkış
            </button>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
