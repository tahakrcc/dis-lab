import { apiFetch } from "./client";
import type { DentalCase, MeasureType } from "./cases";

export type BirimTipi = "DIS" | "ADET";

export interface Partnership {
  id: string;
  labId: string;
  labAd: string;
  clinicId: string;
  clinicAd: string;
  durum: "AKTIF" | "PASIF";
  vadeGun: number;
}

export interface PriceEntry {
  id: string;
  partnershipId: string;
  serviceItemId: string;
  serviceItemAd: string;
  serviceItemBirim: BirimTipi;
  fiyat: number;
  gecerliBaslangic: string;
  gecerliBitis: string | null;
  aktif: boolean;
}

export interface Patient {
  id: string;
  clinicId: string;
  ad: string;
  telefon: string | null;
  dogumTarihi: string | null;
  not: string | null;
}

export interface CreateCaseItemInput {
  serviceItemId: string;
  disNumaralari: number[];
  adet: number;
  materyal?: string | null;
  renk?: string | null;
}

export interface CreateCaseInput {
  partnershipId: string;
  hastaRumuzu: string;
  patientId?: string | null;
  olcuTipi: MeasureType;
  teslimTarihi?: string | null;
  genelNot?: string | null;
  items: CreateCaseItemInput[];
}

export function getPartnerships(): Promise<Partnership[]> {
  return apiFetch<Partnership[]>("/partnerships");
}

export function getPartnershipPrices(partnershipId: string): Promise<PriceEntry[]> {
  return apiFetch<PriceEntry[]>(`/partnerships/${partnershipId}/prices`);
}

export function getPatients(): Promise<Patient[]> {
  return apiFetch<Patient[]>("/patients");
}

export function createCase(input: CreateCaseInput): Promise<DentalCase> {
  return apiFetch<DentalCase>("/cases", { method: "POST", body: input });
}

// FDI diş numarası doğrulaması (backend ile aynı aralıklar)
export function isValidFdi(n: number): boolean {
  const inRange = (a: number, b: number) => n >= a && n <= b;
  return (
    inRange(11, 18) ||
    inRange(21, 28) ||
    inRange(31, 38) ||
    inRange(41, 48) ||
    inRange(51, 55) ||
    inRange(61, 65) ||
    inRange(71, 75) ||
    inRange(81, 85)
  );
}

export function parseDisler(text: string): number[] {
  return text
    .split(/[^0-9]+/)
    .map((t) => t.trim())
    .filter(Boolean)
    .map(Number);
}
