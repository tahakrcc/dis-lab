import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import CasesPage from "./pages/CasesPage";
import NewCasePage from "./pages/NewCasePage";
import CaseDetailPage from "./pages/CaseDetailPage";
import LedgerPage from "./pages/LedgerPage";
import CatalogPage from "./pages/CatalogPage";
import PatientsPage from "./pages/PatientsPage";
import OrgPage from "./pages/OrgPage";

function FullScreenLoader() {
  return (
    <div className="screen-center">
      <div className="spinner" aria-label="Yükleniyor" />
    </div>
  );
}

export default function App() {
  const { status } = useAuth();

  if (status === "loading") return <FullScreenLoader />;

  return (
    <Routes>
      <Route
        path="/login"
        element={status === "authed" ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route
        path="/"
        element={status === "authed" ? <Layout /> : <Navigate to="/login" replace />}
      >
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
