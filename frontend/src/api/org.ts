import { apiFetch } from "./client";
import type { OrgTip, Rol } from "./types";
import type { Partnership } from "./catalog";

export interface Organization {
  id: string;
  tip: OrgTip;
  ad: string;
  telefon: string | null;
  vergiNo: string | null;
  adres: string | null;
  ayarlar: string | null;
  createdAt: string;
}

export interface Member {
  id: string;
  userId: string;
  userAd: string;
  userEmail: string | null;
  orgId: string;
  rol: Rol;
  durum: "AKTIF" | "PASIF";
  createdAt: string;
}

export const ROLLER_BY_TIP: Record<OrgTip, Rol[]> = {
  LAB: ["LAB_ADMIN", "LAB_TEKNISYEN"],
  KLINIK: ["KLINIK_ADMIN", "HEKIM", "ASISTAN"],
};

export function getOrganization(id: string): Promise<Organization> {
  return apiFetch<Organization>(`/organizations/${id}`);
}

export function addMember(orgId: string, body: { kullaniciAdi: string; rol: Rol }): Promise<Member> {
  return apiFetch<Member>(`/organizations/${orgId}/members`, { method: "POST", body });
}

export function createPartnership(body: {
  labId: string;
  clinicId: string;
  vadeGun?: number;
}): Promise<Partnership> {
  return apiFetch<Partnership>("/partnerships", { method: "POST", body });
}
