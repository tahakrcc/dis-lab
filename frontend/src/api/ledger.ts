import { apiFetch } from "./client";

export type LedgerType = "BORC" | "TAHSILAT" | "DUZELTME";

export interface LedgerEntry {
  id: string;
  partnershipId: string;
  caseId: string | null;
  tur: LedgerType;
  matrah: number;
  kdvOrani: number;
  kdvTutari: number;
  tutar: number;
  aciklama: string | null;
  faturaNo: string | null;
  yontem: string | null;
  belgeTarihi: string;
  createdAt: string;
}

export interface LedgerReport {
  partnershipId: string;
  toplamBorc: number;
  toplamTahsilat: number;
  bakiye: number;
  movements: LedgerEntry[];
}

export const LEDGER_META: Record<LedgerType, { label: string; cls: string }> = {
  BORC: { label: "Borç", cls: "lt-borc" },
  TAHSILAT: { label: "Tahsilat", cls: "lt-tahsilat" },
  DUZELTME: { label: "Düzeltme", cls: "lt-duzeltme" },
};

export function getLedger(partnershipId: string): Promise<LedgerReport> {
  return apiFetch<LedgerReport>(`/partnerships/${partnershipId}/ledger`);
}

export function createPayment(
  partnershipId: string,
  body: { tutar: number; yontem?: string | null; belgeTarihi: string; aciklama?: string | null }
): Promise<LedgerEntry> {
  return apiFetch<LedgerEntry>(`/partnerships/${partnershipId}/payments`, { method: "POST", body });
}

export function createAdjustment(
  partnershipId: string,
  body: { tutar: number; aciklama?: string | null }
): Promise<LedgerEntry> {
  return apiFetch<LedgerEntry>(`/partnerships/${partnershipId}/adjustments`, { method: "POST", body });
}
