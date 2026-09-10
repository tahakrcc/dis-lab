// Köprü API istemcisi — JWT + X-Org-Id yönetimi, 401'de tek seferlik refresh.

const ACCESS_KEY = "kopru.access";
const REFRESH_KEY = "kopru.refresh";
const ORG_KEY = "kopru.activeOrg";

export const tokenStore = {
  get access() {
    return localStorage.getItem(ACCESS_KEY);
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY);
  },
  get orgId() {
    return localStorage.getItem(ORG_KEY);
  },
  setTokens(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  setOrg(orgId: string | null) {
    if (orgId) localStorage.setItem(ORG_KEY, orgId);
    else localStorage.removeItem(ORG_KEY);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(ORG_KEY);
  },
};

export type ErrorCode =
  | "UNAUTHENTICATED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "INVALID_TRANSITION"
  | "ALREADY_DELIVERED"
  | "VERSION_MISMATCH"
  | "PRICE_NOT_FOUND"
  | "VALIDATION_ERROR";

export class ApiError extends Error {
  code: string;
  status: number;
  details?: Record<string, unknown>;
  constructor(status: number, code: string, message: string, details?: Record<string, unknown>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

const BASE = "/api/v1";

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Authorization header eklensin mi (default: true) */
  auth?: boolean;
  /** X-Org-Id header eklensin mi (default: true) */
  orgScoped?: boolean;
}

async function parseError(res: Response): Promise<ApiError> {
  let code = "VALIDATION_ERROR";
  let message = `İstek başarısız (${res.status})`;
  let details: Record<string, unknown> | undefined;
  try {
    const data = await res.json();
    if (data?.error) {
      code = data.error.code ?? code;
      message = data.error.message ?? message;
      details = data.error.details ?? undefined;
    }
  } catch {
    /* body JSON değil */
  }
  return new ApiError(res.status, code, message, details);
}

let refreshing: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  const refresh = tokenStore.refresh;
  if (!refresh) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    tokenStore.setTokens(data.access, data.refresh);
    return true;
  } catch {
    return false;
  }
}

export async function apiFetch<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true, orgScoped = true } = opts;

  const buildHeaders = (): Record<string, string> => {
    const h: Record<string, string> = {};
    if (body !== undefined) h["Content-Type"] = "application/json";
    if (auth && tokenStore.access) h["Authorization"] = `Bearer ${tokenStore.access}`;
    if (orgScoped && tokenStore.orgId) h["X-Org-Id"] = tokenStore.orgId;
    return h;
  };

  const send = () =>
    fetch(`${BASE}${path}`, {
      method,
      headers: buildHeaders(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

  let res = await send();

  // 401 -> tek seferlik refresh dene, sonra isteği tekrarla
  if (res.status === 401 && auth && tokenStore.refresh) {
    if (!refreshing) refreshing = doRefresh();
    const ok = await refreshing;
    refreshing = null;
    if (ok) {
      res = await send();
    } else {
      tokenStore.clear();
      throw await parseError(res);
    }
  }

  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
