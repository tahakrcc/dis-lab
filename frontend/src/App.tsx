import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import Layout from "./components/Layout";
import AdminLayout from "./components/AdminLayout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import CasesPage from "./pages/CasesPage";
import NewCasePage from "./pages/NewCasePage";
import CaseDetailPage from "./pages/CaseDetailPage";
import LedgerPage from "./pages/LedgerPage";
import CatalogPage from "./pages/CatalogPage";
import PatientsPage from "./pages/PatientsPage";
import OrgPage from "./pages/OrgPage";
import AdminUsersPage from "./pages/admin/AdminUsersPage";
import AdminOrgsPage from "./pages/admin/AdminOrgsPage";
import AdminPartnershipsPage from "./pages/admin/AdminPartnershipsPage";
import AdminCatalogPage from "./pages/admin/AdminCatalogPage";

function FullScreenLoader() {
  return (
    <div className="screen-center">
      <div className="spinner" aria-label="Yükleniyor" />
    </div>
  );
}

export default function App() {
  const { status, me } = useAuth();

  if (status === "loading") return <FullScreenLoader />;

  if (status !== "authed") {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  // Platform süper-admin: ayrı yönetim uygulaması
  if (me?.superAdmin) {
    return (
      <Routes>
        <Route path="/login" element={<Navigate to="/admin" replace />} />
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminUsersPage />} />
          <Route path="orgs" element={<AdminOrgsPage />} />
          <Route path="partnerships" element={<AdminPartnershipsPage />} />
          <Route path="catalog" element={<AdminCatalogPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Routes>
    );
  }

  // Org kullanıcıları (lab/klinik)
  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="cases" element={<CasesPage />} />
        <Route path="cases/new" element={<NewCasePage />} />
        <Route path="cases/:id" element={<CaseDetailPage />} />
        <Route path="ledger" element={<LedgerPage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="patients" element={<PatientsPage />} />
        <Route path="org" element={<OrgPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
