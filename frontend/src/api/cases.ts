import { apiFetch, tokenStore } from "./client";

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

export interface CaseMessage {
  id: string;
  caseId: string;
  senderUserId: string;
  senderAd: string;
  senderTaraf: "LAB" | "KLINIK";
  metin: string | null;
  okunduAt: string | null;
  duzenlendiAt: string | null;
  silindiAt: string | null;
  createdAt: string;
}

export function getMessages(id: string): Promise<CaseMessage[]> {
  return apiFetch<CaseMessage[]>(`/cases/${id}/messages`);
}

export function sendMessage(id: string, metin: string): Promise<CaseMessage> {
  return apiFetch<CaseMessage>(`/cases/${id}/messages`, { method: "POST", body: { metin } });
}

export function markMessagesRead(id: string): Promise<void> {
  return apiFetch<void>(`/cases/${id}/messages/read`, { method: "POST" });
}

export function editMessage(caseId: string, messageId: string, metin: string): Promise<CaseMessage> {
  return apiFetch<CaseMessage>(`/cases/${caseId}/messages/${messageId}`, { method: "PATCH", body: { metin } });
}

export function deleteMessage(caseId: string, messageId: string): Promise<void> {
  return apiFetch<void>(`/cases/${caseId}/messages/${messageId}`, { method: "DELETE" });
}

export interface CaseAttachment {
  id: string;
  caseId: string;
  uploaderUserId: string;
  uploaderAd: string;
  uploaderTaraf: "LAB" | "KLINIK";
  dosyaAdi: string;
  mime: string | null;
  boyut: number;
  createdAt: string;
}

function authHeaders(): Record<string, string> {
  const h: Record<string, string> = {};
  if (tokenStore.access) h["Authorization"] = `Bearer ${tokenStore.access}`;
  if (tokenStore.orgId) h["X-Org-Id"] = tokenStore.orgId;
  return h;
}

export function getAttachments(id: string): Promise<CaseAttachment[]> {
  return apiFetch<CaseAttachment[]>(`/cases/${id}/attachments`);
}

export async function uploadAttachment(id: string, file: File): Promise<CaseAttachment> {
  const fd = new FormData();
  fd.append("file", file);
  const res = await fetch(`/api/v1/cases/${id}/attachments`, {
    method: "POST",
    headers: authHeaders(), // Content-Type'ı FormData otomatik ayarlar
    body: fd,
  });
  if (!res.ok) {
    let msg = `Yükleme başarısız (${res.status})`;
    try {
      const d = await res.json();
      msg = d?.error?.message ?? msg;
    } catch {
      /* boş */
    }
    throw new Error(msg);
  }
  return res.json();
}

export async function fetchAttachmentObjectUrl(id: string): Promise<string> {
  const res = await fetch(`/api/v1/attachments/${id}/download`, { headers: authHeaders() });
  if (!res.ok) throw new Error("Önizleme yüklenemedi.");
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

export async function downloadAttachment(att: CaseAttachment): Promise<void> {
  const res = await fetch(`/api/v1/attachments/${att.id}/download`, { headers: authHeaders() });
  if (!res.ok) throw new Error("Dosya indirilemedi.");
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = att.dosyaAdi;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export function formatBoyut(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(1)} MB`;
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
