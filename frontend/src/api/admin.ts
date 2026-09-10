import { apiFetch } from "./client";
import type { OrgTip, Rol } from "./types";
import type { Member, Organization } from "./org";
import type { BirimTipi, Partnership, PriceEntry, ServiceItem } from "./catalog";

export interface AdminUser {
  id: string;
  kullaniciAdi: string;
  ad: string;
  email: string | null;
  superAdmin: boolean;
  createdAt: string;
}

// Kullanıcılar
export function listUsers(): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>("/admin/users");
}
export function createUser(body: {
  kullaniciAdi: string;
  ad: string;
  parola: string;
  email?: string | null;
}): Promise<AdminUser> {
  return apiFetch<AdminUser>("/admin/users", { method: "POST", body });
}

// Organizasyonlar
export function listOrganizations(): Promise<Organization[]> {
  return apiFetch<Organization[]>("/admin/organizations");
}
export function createOrganization(body: {
  tip: OrgTip;
  ad: string;
  telefon?: string | null;
  vergiNo?: string | null;
  adres?: string | null;
}): Promise<Organization> {
  return apiFetch<Organization>("/admin/organizations", { method: "POST", body });
}
export function listMembers(orgId: string): Promise<Member[]> {
  return apiFetch<Member[]>(`/admin/organizations/${orgId}/members`);
}
export function addMember(orgId: string, body: { kullaniciAdi: string; rol: Rol }): Promise<Member> {
  return apiFetch<Member>(`/admin/organizations/${orgId}/members`, { method: "POST", body });
}
export function removeMember(membershipId: string): Promise<void> {
  return apiFetch<void>(`/admin/members/${membershipId}`, { method: "DELETE" });
}

// Ortaklıklar
export function listPartnerships(): Promise<Partnership[]> {
  return apiFetch<Partnership[]>("/admin/partnerships");
}
export function createPartnership(body: {
  labId: string;
  clinicId: string;
  vadeGun?: number;
}): Promise<Partnership> {
  return apiFetch<Partnership>("/admin/partnerships", { method: "POST", body });
}
export function updatePartnership(
  id: string,
  body: { durum?: "AKTIF" | "PASIF"; vadeGun?: number }
): Promise<Partnership> {
  return apiFetch<Partnership>(`/admin/partnerships/${id}`, { method: "PATCH", body });
}

// Katalog & fiyat
export function listServiceItems(labId: string): Promise<ServiceItem[]> {
  return apiFetch<ServiceItem[]>(`/admin/labs/${labId}/service-items`);
}
export function createServiceItem(labId: string, body: { ad: string; birim: BirimTipi }): Promise<ServiceItem> {
  return apiFetch<ServiceItem>(`/admin/labs/${labId}/service-items`, { method: "POST", body });
}
export function updateServiceItem(
  labId: string,
  id: string,
  body: { ad?: string; aktif?: boolean }
): Promise<ServiceItem> {
  return apiFetch<ServiceItem>(`/admin/labs/${labId}/service-items/${id}`, { method: "PATCH", body });
}
export function getPrices(partnershipId: string): Promise<PriceEntry[]> {
  return apiFetch<PriceEntry[]>(`/admin/partnerships/${partnershipId}/prices`);
}
export function upsertPrice(
  partnershipId: string,
  body: { serviceItemId: string; fiyat: number; gecerliBaslangic: string }
): Promise<PriceEntry> {
  return apiFetch<PriceEntry>(`/admin/partnerships/${partnershipId}/prices`, { method: "PUT", body });
}
