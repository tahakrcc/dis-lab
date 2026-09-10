import type { CaseStatus } from "./cases";
import type { Rol } from "./types";

export interface ActionDef {
  aksiyon: string;
  label: string;
  roles: Rol[];
  statuses: CaseStatus[];
  variant: "primary" | "ghost" | "danger";
  /** payload'a güncel fiyatVersiyon eklenir (optimistik onay guard'ı) */
  sendVersion?: boolean;
  /** kullanıcıdan neden istenir; payload[nedenKey] = metin */
  nedenKey?: string;
  /** tıklamadan önce onay diyalogu */
  confirm?: string;
}

// Sıra, butonların görünme önceliğini belirler.
export const CASE_ACTIONS: ActionDef[] = [
  {
    aksiyon: "gonder",
    label: "Fiyat mutabakatına gönder",
    roles: ["HEKIM", "KLINIK_ADMIN"],
    statuses: ["TASLAK"],
    variant: "primary",
  },
  {
    aksiyon: "klinikOnayla",
    label: "Klinik onayla",
    roles: ["HEKIM", "KLINIK_ADMIN"],
    statuses: ["FIYAT_MUTABAKATI"],
    variant: "primary",
    sendVersion: true,
  },
  {
    aksiyon: "labOnayla",
    label: "Lab onayla",
    roles: ["LAB_ADMIN"],
    statuses: ["FIYAT_MUTABAKATI"],
    variant: "primary",
    sendVersion: true,
  },
  {
    aksiyon: "karsiTeklif",
    label: "Karşı teklif (mutabakatı sıfırla)",
    roles: ["LAB_ADMIN", "HEKIM", "KLINIK_ADMIN"],
    statuses: ["FIYAT_MUTABAKATI"],
    variant: "ghost",
    confirm: "Karşı teklif verilince iki tarafın onayı sıfırlanır. Devam?",
  },
  {
    aksiyon: "uretimeAl",
    label: "Üretime al",
    roles: ["LAB_ADMIN", "LAB_TEKNISYEN"],
    statuses: ["ONAYLANDI"],
    variant: "primary",
  },
  {
    aksiyon: "provayaGonder",
    label: "Provaya gönder",
    roles: ["LAB_ADMIN", "LAB_TEKNISYEN"],
    statuses: ["URETIMDE"],
    variant: "primary",
  },
  {
    aksiyon: "revizyonIste",
    label: "Revizyon iste",
    roles: ["HEKIM", "KLINIK_ADMIN"],
    statuses: ["PROVA"],
    variant: "ghost",
    nedenKey: "neden",
  },
  {
    aksiyon: "tamamla",
    label: "Tamamla",
    roles: ["LAB_ADMIN", "LAB_TEKNISYEN"],
    statuses: ["PROVA", "URETIMDE"],
    variant: "primary",
  },
  {
    aksiyon: "teslimEt",
    label: "Teslim et (borç oluşur)",
    roles: ["LAB_ADMIN"],
    statuses: ["TAMAMLANDI"],
    variant: "primary",
    confirm: "Teslim edilince cari hesaba borç kaydı oluşur. Onaylıyor musun?",
  },
  {
    aksiyon: "iptal",
    label: "İptal et",
    roles: ["KLINIK_ADMIN", "LAB_ADMIN"],
    statuses: ["TASLAK", "FIYAT_MUTABAKATI", "ONAYLANDI"],
    variant: "danger",
    nedenKey: "iptalNeden",
    confirm: "Vaka iptal edilecek. Emin misin?",
  },
];

export function availableActions(rol: Rol | undefined, durum: CaseStatus): ActionDef[] {
  if (!rol) return [];
  return CASE_ACTIONS.filter((a) => a.roles.includes(rol) && a.statuses.includes(durum));
}
