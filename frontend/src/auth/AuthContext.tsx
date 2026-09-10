import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, tokenStore } from "../api/client";
import type { AuthResponse, Me, Membership } from "../api/types";

type Status = "loading" | "anon" | "authed";

interface AuthState {
  status: Status;
  me: Me | null;
  activeOrgId: string | null;
  activeMembership: Membership | null;
  login: (kullaniciAdi: string, parola: string) => Promise<void>;
  logout: () => Promise<void>;
  setActiveOrg: (orgId: string) => void;
  reloadMe: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [me, setMe] = useState<Me | null>(null);
  const [activeOrgId, setActiveOrgId] = useState<string | null>(tokenStore.orgId);

  const applyMe = useCallback((data: Me) => {
    setMe(data);
    // Aktif org geçerli değilse ilk üyeliğe düş
    const stored = tokenStore.orgId;
    const valid = data.memberships.find((m) => m.orgId === stored);
    const chosen = valid ?? data.memberships[0] ?? null;
    if (chosen) {
      tokenStore.setOrg(chosen.orgId);
      setActiveOrgId(chosen.orgId);
    } else {
      tokenStore.setOrg(null);
      setActiveOrgId(null);
    }
  }, []);

  const reloadMe = useCallback(async () => {
    const data = await apiFetch<Me>("/me");
    applyMe(data);
  }, [applyMe]);

  // İlk yüklemede token varsa oturumu canlandır
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!tokenStore.access) {
        setStatus("anon");
        return;
      }
      try {
        const data = await apiFetch<Me>("/me");
        if (cancelled) return;
        applyMe(data);
        setStatus("authed");
      } catch {
        if (cancelled) return;
        tokenStore.clear();
        setStatus("anon");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applyMe]);

  const login = useCallback(
    async (kullaniciAdi: string, parola: string) => {
      const res = await apiFetch<AuthResponse>("/auth/login", {
        method: "POST",
        body: { kullaniciAdi, parola },
        auth: false,
        orgScoped: false,
      });
      tokenStore.setTokens(res.access, res.refresh);
      const data = await apiFetch<Me>("/me");
      applyMe(data);
      setStatus("authed");
    },
    [applyMe]
  );

  const logout = useCallback(async () => {
    const refresh = tokenStore.refresh;
    try {
      if (refresh) {
        await apiFetch<void>("/auth/logout", {
          method: "POST",
          body: { refresh },
          auth: false,
          orgScoped: false,
        });
      }
    } catch {
      /* logout hatası kritik değil */
    }
    tokenStore.clear();
    setMe(null);
    setActiveOrgId(null);
    setStatus("anon");
  }, []);

  const setActiveOrg = useCallback((orgId: string) => {
    tokenStore.setOrg(orgId);
    setActiveOrgId(orgId);
  }, []);

  const activeMembership = useMemo(
    () => me?.memberships.find((m) => m.orgId === activeOrgId) ?? null,
    [me, activeOrgId]
  );

  const value: AuthState = {
    status,
    me,
    activeOrgId,
    activeMembership,
    login,
    logout,
    setActiveOrg,
    reloadMe,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
