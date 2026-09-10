-- V2__tables.sql

-- 1. organization
CREATE TABLE organization (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tip org_tipi NOT NULL,
    ad text NOT NULL,
    telefon text,
    vergi_no text,
    adres text,
    ayarlar jsonb NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, tip)
);

-- 2. app_user
CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ad text NOT NULL,
    email text NOT NULL UNIQUE,
    telefon text,
    parola_hash text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3. refresh_token
CREATE TABLE refresh_token (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 4. membership
CREATE TABLE membership (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    org_id uuid NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    rol rol_tipi NOT NULL,
    durum aktiflik NOT NULL DEFAULT 'AKTIF',
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, org_id)
);

-- 5. partnership
CREATE TABLE partnership (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL,
    lab_tip org_tipi GENERATED ALWAYS AS ('LAB'::org_tipi) STORED,
    clinic_id uuid NOT NULL,
    clinic_tip org_tipi GENERATED ALWAYS AS ('KLINIK'::org_tipi) STORED,
    durum aktiflik NOT NULL DEFAULT 'AKTIF',
    vade_gun int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (lab_id, clinic_id),
    FOREIGN KEY (lab_id, lab_tip) REFERENCES organization(id, tip),
    FOREIGN KEY (clinic_id, clinic_tip) REFERENCES organization(id, tip)
);

-- 6. case_sequence
CREATE TABLE case_sequence (
    partnership_id uuid PRIMARY KEY REFERENCES partnership(id) ON DELETE CASCADE,
    son_no bigint NOT NULL DEFAULT 0
);

-- 7. service_item
CREATE TABLE service_item (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id uuid NOT NULL REFERENCES organization(id),
    ad text NOT NULL,
    birim birim_tipi NOT NULL,
    aktif boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (lab_id, ad)
);

-- 8. price_list_entry
CREATE TABLE price_list_entry (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    partnership_id uuid NOT NULL REFERENCES partnership(id) ON DELETE CASCADE,
    service_item_id uuid NOT NULL REFERENCES service_item(id),
    fiyat numeric(12,2) NOT NULL CHECK (fiyat >= 0),
    gecerli_baslangic date NOT NULL,
    gecerli_bitis date,
    aktif boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (gecerli_bitis IS NULL OR gecerli_bitis > gecerli_baslangic)
);

-- 9. patient (laba ASLA gitmez)
CREATE TABLE patient (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id uuid NOT NULL REFERENCES organization(id),
    ad text NOT NULL,
    telefon text,
    dogum_tarihi date,
    notlar text,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 10. "case"
CREATE TABLE "case" (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    partnership_id uuid NOT NULL REFERENCES partnership(id),
    kod text NOT NULL,
    hasta_rumuzu text NOT NULL,
    created_by uuid NOT NULL REFERENCES app_user(id),
    assigned_to uuid REFERENCES app_user(id),
    olcu_tipi measure_type NOT NULL,
    durum case_status NOT NULL DEFAULT 'TASLAK',
    teslim_tarihi date,
    toplam_tutar numeric(12,2) NOT NULL DEFAULT 0,
    fiyat_versiyon int NOT NULL DEFAULT 1,
    onay_klinik_versiyon int,
    onay_lab_versiyon int,
    onay_klinik_at timestamptz,
    onay_lab_at timestamptz,
    revizyon_sayisi int NOT NULL DEFAULT 0,
    iptal_neden text,
    genel_not text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (partnership_id, kod)
);

-- 11. case_patient_link
CREATE TABLE case_patient_link (
    case_id uuid PRIMARY KEY REFERENCES "case"(id) ON DELETE CASCADE,
    patient_id uuid NOT NULL REFERENCES patient(id),
    clinic_id uuid NOT NULL REFERENCES organization(id)
);

-- 12. case_item
CREATE TABLE case_item (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id uuid NOT NULL REFERENCES "case"(id) ON DELETE CASCADE,
    service_item_id uuid NOT NULL REFERENCES service_item(id),
    dis_numaralari int[] NOT NULL DEFAULT '{}',
    materyal text,
    renk text,
    adet int NOT NULL CHECK (adet > 0),
    birim_fiyat numeric(12,2) NOT NULL,
    ara_toplam numeric(12,2) NOT NULL,
    specs jsonb NOT NULL DEFAULT '{}',
    CHECK (ara_toplam = birim_fiyat * adet)
);

-- 13. case_event
CREATE TABLE case_event (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id uuid NOT NULL REFERENCES "case"(id) ON DELETE CASCADE,
    actor_user_id uuid NOT NULL REFERENCES app_user(id),
    tur event_type NOT NULL,
    eski_durum case_status,
    yeni_durum case_status,
    detay jsonb NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 14. shipment
CREATE TABLE shipment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id uuid NOT NULL REFERENCES "case"(id) ON DELETE CASCADE,
    yon shipment_yon NOT NULL,
    kurye text,
    takip_no text,
    gonderim_at timestamptz,
    teslim_at timestamptz
);

-- 15. ledger_entry
CREATE TABLE ledger_entry (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    partnership_id uuid NOT NULL REFERENCES partnership(id),
    case_id uuid REFERENCES "case"(id),
    tur ledger_type NOT NULL,
    matrah numeric(12,2) NOT NULL,
    kdv_orani numeric(5,2) NOT NULL DEFAULT 20.00,
    kdv_tutari numeric(12,2) NOT NULL,
    tutar numeric(12,2) NOT NULL,
    aciklama text,
    fatura_no text,
    yontem text,
    belge_tarihi date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
