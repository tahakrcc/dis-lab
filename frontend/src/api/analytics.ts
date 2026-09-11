import { apiFetch } from "./client";

export interface DurumSayi {
  durum: string;
  sayi: number;
}
export interface AySayi {
  ay: string;
  sayi: number;
}
export interface HizmetAdet {
  ad: string;
  adet: number;
}
export interface TarafCiro {
  ad: string;
  tip: "LAB" | "KLINIK";
  ciro: number;
}
export interface TeknisyenSayi {
  ad: string;
  tamamlanan: number;
}

export interface AnalyticsSummary {
  toplamVaka: number;
  devamEden: number;
  teslimEdilen: number;
  geciken: number;
  toplamCiro: number;
  toplamTahsilat: number;
  acikBakiye: number;
  ortalamaUretimGun: number | null;
  zamanindaTeslim: number;
  gecTeslim: number;
  revizyonluVaka: number;
  revizyonOrani: number | null;
  durumDagilimi: DurumSayi[];
  aylikVaka: AySayi[];
  enCokHizmetler: HizmetAdet[];
  karsiTarafCiro: TarafCiro[];
  teknisyenPerformans: TeknisyenSayi[];
}

export const DURUM_ETIKET: Record<string, string> = {
  TASLAK: "Taslak",
  FIYAT_MUTABAKATI: "Fiyat mutabakatı",
  ONAYLANDI: "Onaylandı",
  URETIMDE: "Üretimde",
  PROVA: "Prova",
  TAMAMLANDI: "Tamamlandı",
  TESLIM_EDILDI: "Teslim edildi",
  IPTAL: "İptal",
};

export function getAnalyticsSummary(params?: { from?: string; to?: string }): Promise<AnalyticsSummary> {
  const q = new URLSearchParams();
  if (params?.from) q.set("from", params.from);
  if (params?.to) q.set("to", params.to);
  const qs = q.toString();
  return apiFetch<AnalyticsSummary>(`/analytics/summary${qs ? `?${qs}` : ""}`);
}
