import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ThemeToggle } from "./ThemeToggle";

export default function AdminLayout() {
  const { me, logout } = useAuth();
  if (!me) return null;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-tooth">🦷</span>
          <span className="brand-name">Köprü</span>
        </div>

        <nav className="nav">
          <NavLink to="/admin" end className="nav-link">
            <span className="nav-dot" /> Kullanıcılar
          </NavLink>
          <NavLink to="/admin/orgs" className="nav-link">
            <span className="nav-dot" /> Organizasyonlar
          </NavLink>
          <NavLink to="/admin/partnerships" className="nav-link">
            <span className="nav-dot" /> Ortaklıklar
          </NavLink>
          <NavLink to="/admin/catalog" className="nav-link">
            <span className="nav-dot" /> Katalog &amp; fiyat
          </NavLink>
        </nav>

        <div className="sidebar-foot">
          <div className="ctx-role">Platform yönetimi</div>
        </div>
      </aside>

      <div className="main-col">
        <header className="topbar">
          <div className="admin-badge">Platform Yönetimi</div>
          <div className="topbar-right">
            <ThemeToggle />
            <div className="user-chip">
              <div className="user-ad">{me.ad}</div>
              <div className="user-rol">Süper Admin</div>
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
