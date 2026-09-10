// Backend DTO tipleri (yalnızca frontend'in kullandığı alanlar).

export type OrgTip = "LAB" | "KLINIK";
export type Rol =
  | "LAB_ADMIN"
  | "LAB_TEKNISYEN"
  | "KLINIK_ADMIN"
  | "HEKIM"
  | "ASISTAN";

export interface AuthResponse {
  access: string;
  refresh: string;
}

export interface Membership {
  orgId: string;
  orgAd: string;
  orgTip: OrgTip;
  rol: Rol;
}

export interface Me {
  id: string;
  kullaniciAdi: string;
  ad: string;
  email: string | null;
  telefon: string | null;
  memberships: Membership[];
}

export const ROL_ETIKET: Record<Rol, string> = {
  LAB_ADMIN: "Lab Yöneticisi",
  LAB_TEKNISYEN: "Teknisyen",
  KLINIK_ADMIN: "Klinik Yöneticisi",
  HEKIM: "Hekim",
  ASISTAN: "Asistan",
};
