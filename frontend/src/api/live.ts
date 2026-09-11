import { useCallback, useEffect, useRef } from "react";
import { tokenStore } from "./client";

export interface CaseLiveEvent {
  type: "message" | "status" | "attachment";
  caseId: string;
  durum?: string;
}

/**
 * Bir vakaya ham WebSocket ile abone olur; sunucudan gelen olayları onEvent'e iletir.
 * Auth ve caseId query param olarak gider (tarayıcı WebSocket'i header gönderemez).
 * Bağlantı koparsa 5 sn'de bir yeniden dener.
 */
export function useCaseLive(caseId: string | undefined, onEvent: (e: CaseLiveEvent) => void) {
  const cbRef = useRef(onEvent);
  cbRef.current = onEvent;

  useEffect(() => {
    if (!caseId) return;
    const token = tokenStore.access;
    if (!token) return;

    let ws: WebSocket | null = null;
    let closed = false;
    let retry: ReturnType<typeof setTimeout> | null = null;

    const connect = () => {
      if (closed) return;
      const base = location.origin.replace(/^http/, "ws");
      const url = `${base}/ws/case?caseId=${encodeURIComponent(caseId)}&token=${encodeURIComponent(token)}`;
      ws = new WebSocket(url);
      ws.onmessage = (ev) => {
        try {
          cbRef.current(JSON.parse(ev.data) as CaseLiveEvent);
        } catch {
          /* geçersiz mesaj yut */
        }
      };
      ws.onclose = () => {
        if (closed) return;
        retry = setTimeout(connect, 5000);
      };
      ws.onerror = () => {
        try {
          ws?.close();
        } catch {
          /* yut */
        }
      };
    };
    connect();

    return () => {
      closed = true;
      if (retry) clearTimeout(retry);
      try {
        ws?.close();
      } catch {
        /* yut */
      }
    };
  }, [caseId]);
}

/** onEvent'i sabit referansta tutmak için küçük yardımcı (isteğe bağlı). */
export function useStableCallback<T extends (...a: never[]) => void>(fn: T): T {
  const ref = useRef(fn);
  ref.current = fn;
  return useCallback(((...a: Parameters<T>) => ref.current(...a)) as T, []);
}
