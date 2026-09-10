-- V9__super_admin.sql
-- Platform (süper-admin) rolü: org'ların üstünde; lab/klinik/kullanıcı oluşturur,
-- üye ekler, ortaklık kurar, katalog & fiyat yönetir. HASTA verisine erişmez
-- (patient / case_patient_link politikalarına carve-out EKLENMEZ).

ALTER TABLE app_user ADD COLUMN is_super_admin boolean NOT NULL DEFAULT false;

-- Seed süper-admin (parola: password123)
INSERT INTO app_user (id, ad, email, telefon, parola_hash, kullanici_adi, is_super_admin) VALUES
('99999999-9999-9999-9999-999999999999', 'Platform Yoneticisi', NULL, NULL,
 '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu', 'superadmin', true);

-- RLS yardımcı: aktif kullanıcı süper-admin mi?
CREATE OR REPLACE FUNCTION app.is_super_admin() RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT coalesce((SELECT u.is_super_admin FROM app_user u WHERE u.id = app.current_user_id()), false)
$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT EXECUTE ON FUNCTION app.is_super_admin() TO kopru_app;
  END IF;
END
$$;

-- ---- RLS politikalarına süper-admin carve-out'u ----

-- organization
DROP POLICY org_select ON organization;
CREATE POLICY org_select ON organization FOR SELECT USING (
    id IN (SELECT app.user_orgs()) OR id IN (SELECT app.user_partner_orgs()) OR app.is_super_admin()
);
DROP POLICY org_update ON organization;
CREATE POLICY org_update ON organization FOR UPDATE USING (
    id IN (SELECT app.user_orgs()) OR app.is_super_admin()
) WITH CHECK (
    id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);

-- app_user (süper-admin tüm kullanıcıları listeleyebilir)
DROP POLICY user_select ON app_user;
CREATE POLICY user_select ON app_user FOR SELECT USING (
    id = app.current_user_id()
    OR id IN (SELECT user_id FROM membership WHERE org_id IN (SELECT app.user_orgs()))
    OR app.is_super_admin()
);

-- membership
DROP POLICY membership_select ON membership;
CREATE POLICY membership_select ON membership FOR SELECT USING (
    org_id IN (SELECT app.user_orgs()) OR user_id = app.current_user_id() OR app.is_super_admin()
);
DROP POLICY membership_insert ON membership;
CREATE POLICY membership_insert ON membership FOR INSERT WITH CHECK (
    org_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);
DROP POLICY membership_update ON membership;
CREATE POLICY membership_update ON membership FOR UPDATE USING (
    org_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
) WITH CHECK (
    org_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);

-- partnership
DROP POLICY partnership_select ON partnership;
CREATE POLICY partnership_select ON partnership FOR SELECT USING (
    id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);
DROP POLICY partnership_insert ON partnership;
CREATE POLICY partnership_insert ON partnership FOR INSERT WITH CHECK (
    lab_id IN (SELECT app.user_orgs()) OR clinic_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);
DROP POLICY partnership_update ON partnership;
CREATE POLICY partnership_update ON partnership FOR UPDATE USING (
    id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
) WITH CHECK (
    id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);

-- case_sequence (ortaklık oluştururken sıra tablosu)
DROP POLICY case_seq_insert ON case_sequence;
CREATE POLICY case_seq_insert ON case_sequence FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);

-- service_item
DROP POLICY service_item_select ON service_item;
CREATE POLICY service_item_select ON service_item FOR SELECT USING (
    lab_id IN (SELECT app.user_orgs())
    OR lab_id IN (
        SELECT lab_id FROM partnership
        WHERE durum = 'AKTIF' AND clinic_id IN (SELECT app.user_clinic_orgs())
    )
    OR app.is_super_admin()
);
DROP POLICY service_item_insert ON service_item;
CREATE POLICY service_item_insert ON service_item FOR INSERT WITH CHECK (
    lab_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);
DROP POLICY service_item_update ON service_item;
CREATE POLICY service_item_update ON service_item FOR UPDATE USING (
    lab_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
) WITH CHECK (
    lab_id IN (SELECT app.user_orgs()) OR app.is_super_admin()
);

-- price_list_entry
DROP POLICY price_list_select ON price_list_entry;
CREATE POLICY price_list_select ON price_list_entry FOR SELECT USING (
    partnership_id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);
DROP POLICY price_list_insert ON price_list_entry;
CREATE POLICY price_list_insert ON price_list_entry FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);
DROP POLICY price_list_update ON price_list_entry;
CREATE POLICY price_list_update ON price_list_entry FOR UPDATE USING (
    partnership_id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
) WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships()) OR app.is_super_admin()
);
