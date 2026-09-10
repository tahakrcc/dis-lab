import { apiFetch } from "./client";

export type CaseStatus =
  | "TASLAK"
  | "FIYAT_MUTABAKATI"
  | "ONAYLANDI"
  | "URETIMDE"
  | "PROVA"
  | "TAMAMLANDI"
  | "TESLIM_EDILDI"
  | "IPTAL";

export type MeasureType = "STL" | "FIZIKSEL";
export type EventType = "DURUM" | "FIYAT" | "ONAY";

export interface CaseItem {
  id: string;
  serviceItemId: string;
  serviceItemAd: string;
  disNumaralari: number[];
  materyal: string | null;
  renk: string | null;
  adet: number;
  birimFiyat: number;
  araToplam: number;
  specs: string | null;
}

export interface DentalCase {
  id: string;
  partnershipId: string;
  kod: string;
  hastaRumuzu: string;
  patientId?: string | null; // yalnızca klinik görünümü
  patientAd?: string | null; // yalnızca klinik görünümü
  createdBy: string;
  assignedTo: string | null;
  olcuTipi: MeasureType;
  durum: CaseStatus;
  teslimTarihi: string | null;
  toplamTutar: number;
  fiyatVersiyon: number;
  onayKlinikVersiyon: number | null;
  onayLabVersiyon: number | null;
  onayKlinikAt: string | null;
  onayLabAt: string | null;
  revizyonSayisi: number;
  iptalNeden: string | null;
  genelNot: string | null;
  items: CaseItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CaseEvent {
  id: string;
  caseId: string;
  actorUserId: string;
  actorUserAd: string;
  tur: EventType;
  eskiDurum: CaseStatus | null;
  yeniDurum: CaseStatus | null;
  detay: string | null;
  createdAt: string;
}

export const CASE_STATUS_META: Record<CaseStatus, { label: string; cls: string }> = {
  TASLAK: { label: "Taslak", cls: "st-taslak" },
  FIYAT_MUTABAKATI: { label: "Fiyat Mutabakatı", cls: "st-fiyat" },
  ONAYLANDI: { label: "Onaylandı", cls: "st-onay" },
  URETIMDE: { label: "Üretimde", cls: "st-uretim" },
  PROVA: { label: "Prova", cls: "st-prova" },
  TAMAMLANDI: { label: "Tamamlandı", cls: "st-tamam" },
  TESLIM_EDILDI: { label: "Teslim Edildi", cls: "st-teslim" },
  IPTAL: { label: "İptal", cls: "st-iptal" },
};

export const CASE_STATUS_ORDER: CaseStatus[] = [
  "TASLAK",
  "FIYAT_MUTABAKATI",
  "ONAYLANDI",
  "URETIMDE",
  "PROVA",
  "TAMAMLANDI",
  "TESLIM_EDILDI",
  "IPTAL",
];

export function getCases(durum?: CaseStatus): Promise<DentalCase[]> {
  const q = durum ? `?durum=${encodeURIComponent(durum)}` : "";
  return apiFetch<DentalCase[]>(`/cases${q}`);
}

export function getCase(id: string): Promise<DentalCase> {
  return apiFetch<DentalCase>(`/cases/${id}`);
}

export function getCaseEvents(id: string): Promise<CaseEvent[]> {
  return apiFetch<CaseEvent[]>(`/cases/${id}/events`);
}

export function transitionCase(
  id: string,
  aksiyon: string,
  payload?: Record<string, unknown>
): Promise<DentalCase> {
  return apiFetch<DentalCase>(`/cases/${id}/transitions`, {
    method: "POST",
    body: { aksiyon, payload: payload ?? {} },
  });
}

export function formatTutar(n: number): string {
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    maximumFractionDigits: 0,
  }).format(n);
}

export function formatTarih(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium" }).format(d);
}

export function formatTarihSaat(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium", timeStyle: "short" }).format(d);
}
