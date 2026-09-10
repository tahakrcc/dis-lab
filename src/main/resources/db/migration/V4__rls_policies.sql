-- V4__rls_policies.sql

-- Tabloya baglı RLS yardımcı fonksiyonları (tablolar V2'de olustu, burada tanımlanir)
CREATE OR REPLACE FUNCTION app.user_orgs() RETURNS setof uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT org_id FROM membership
   WHERE user_id = app.current_user_id() AND durum = 'AKTIF'
$$;

CREATE OR REPLACE FUNCTION app.user_partnerships() RETURNS setof uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT p.id FROM partnership p
   WHERE p.durum = 'AKTIF'
     AND (p.lab_id IN (SELECT app.user_orgs())
       OR p.clinic_id IN (SELECT app.user_orgs()))
$$;

CREATE OR REPLACE FUNCTION app.user_clinic_orgs() RETURNS setof uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT m.org_id FROM membership m JOIN organization o ON o.id = m.org_id
   WHERE m.user_id = app.current_user_id() AND m.durum='AKTIF' AND o.tip='KLINIK'
$$;

-- Helper function for active partner orgs
CREATE OR REPLACE FUNCTION app.user_partner_orgs() RETURNS setof uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT CASE 
    WHEN p.lab_id IN (SELECT app.user_orgs()) THEN p.clinic_id
    ELSE p.lab_id
  END
  FROM partnership p
  WHERE p.durum = 'AKTIF'
    AND (p.lab_id IN (SELECT app.user_orgs()) OR p.clinic_id IN (SELECT app.user_orgs()))
$$;

-- ENABLE and FORCE RLS on ALL tables
ALTER TABLE organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization FORCE ROW LEVEL SECURITY;

ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_user FORCE ROW LEVEL SECURITY;

ALTER TABLE refresh_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_token FORCE ROW LEVEL SECURITY;

ALTER TABLE membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE membership FORCE ROW LEVEL SECURITY;

ALTER TABLE partnership ENABLE ROW LEVEL SECURITY;
ALTER TABLE partnership FORCE ROW LEVEL SECURITY;

ALTER TABLE case_sequence ENABLE ROW LEVEL SECURITY;
ALTER TABLE case_sequence FORCE ROW LEVEL SECURITY;

ALTER TABLE service_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_item FORCE ROW LEVEL SECURITY;

ALTER TABLE price_list_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE price_list_entry FORCE ROW LEVEL SECURITY;

ALTER TABLE patient ENABLE ROW LEVEL SECURITY;
ALTER TABLE patient FORCE ROW LEVEL SECURITY;

ALTER TABLE "case" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "case" FORCE ROW LEVEL SECURITY;

ALTER TABLE case_patient_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE case_patient_link FORCE ROW LEVEL SECURITY;

ALTER TABLE case_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE case_item FORCE ROW LEVEL SECURITY;

ALTER TABLE case_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE case_event FORCE ROW LEVEL SECURITY;

ALTER TABLE shipment ENABLE ROW LEVEL SECURITY;
ALTER TABLE shipment FORCE ROW LEVEL SECURITY;

ALTER TABLE ledger_entry ENABLE ROW LEVEL SECURITY;
ALTER TABLE ledger_entry FORCE ROW LEVEL SECURITY;

-- 1. organization policies
CREATE POLICY org_select ON organization
FOR SELECT USING (
    id IN (SELECT app.user_orgs()) OR id IN (SELECT app.user_partner_orgs())
);

CREATE POLICY org_insert ON organization
FOR INSERT WITH CHECK (true); -- User creating org will become admin

CREATE POLICY org_update ON organization
FOR UPDATE USING (
    id IN (SELECT app.user_orgs())
) WITH CHECK (
    id IN (SELECT app.user_orgs())
);

-- 2. app_user policies (users can see themselves, or members of same orgs)
CREATE POLICY user_select ON app_user
FOR SELECT USING (
    id = app.current_user_id() OR
    id IN (SELECT user_id FROM membership WHERE org_id IN (SELECT app.user_orgs()))
);

CREATE POLICY user_insert ON app_user
FOR INSERT WITH CHECK (true); -- Registration

CREATE POLICY user_update ON app_user
FOR UPDATE USING (
    id = app.current_user_id()
) WITH CHECK (
    id = app.current_user_id()
);

-- 3. refresh_token policies
CREATE POLICY refresh_token_all ON refresh_token
FOR ALL USING (
    user_id = app.current_user_id()
) WITH CHECK (
    user_id = app.current_user_id()
);

-- 4. membership policies
CREATE POLICY membership_select ON membership
FOR SELECT USING (
    org_id IN (SELECT app.user_orgs()) OR user_id = app.current_user_id()
);

CREATE POLICY membership_insert ON membership
FOR INSERT WITH CHECK (
    org_id IN (SELECT app.user_orgs())
);

CREATE POLICY membership_update ON membership
FOR UPDATE USING (
    org_id IN (SELECT app.user_orgs())
) WITH CHECK (
    org_id IN (SELECT app.user_orgs())
);

-- 5. partnership policies
CREATE POLICY partnership_select ON partnership
FOR SELECT USING (
    id IN (SELECT app.user_partnerships())
);

CREATE POLICY partnership_insert ON partnership
FOR INSERT WITH CHECK (
    lab_id IN (SELECT app.user_orgs()) OR clinic_id IN (SELECT app.user_orgs())
);

CREATE POLICY partnership_update ON partnership
FOR UPDATE USING (
    id IN (SELECT app.user_partnerships())
) WITH CHECK (
    id IN (SELECT app.user_partnerships())
);

-- 6. case_sequence policies
CREATE POLICY case_seq_select ON case_sequence
FOR SELECT USING (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY case_seq_insert ON case_sequence
FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY case_seq_update ON case_sequence
FOR UPDATE USING (
    partnership_id IN (SELECT app.user_partnerships())
) WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

-- 7. service_item policies
CREATE POLICY service_item_select ON service_item
FOR SELECT USING (
    lab_id IN (SELECT app.user_orgs())
    OR lab_id IN (
        SELECT lab_id FROM partnership 
        WHERE durum = 'AKTIF' AND clinic_id IN (SELECT app.user_clinic_orgs())
    )
);

CREATE POLICY service_item_insert ON service_item
FOR INSERT WITH CHECK (
    lab_id IN (SELECT app.user_orgs())
);

CREATE POLICY service_item_update ON service_item
FOR UPDATE USING (
    lab_id IN (SELECT app.user_orgs())
) WITH CHECK (
    lab_id IN (SELECT app.user_orgs())
);

-- 8. price_list_entry policies
CREATE POLICY price_list_select ON price_list_entry
FOR SELECT USING (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY price_list_insert ON price_list_entry
FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY price_list_update ON price_list_entry
FOR UPDATE USING (
    partnership_id IN (SELECT app.user_partnerships())
) WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

-- 9. patient policies (LAB NEVER SEES PATIENTS)
CREATE POLICY patient_select ON patient
FOR SELECT USING (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

CREATE POLICY patient_insert ON patient
FOR INSERT WITH CHECK (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

CREATE POLICY patient_update ON patient
FOR UPDATE USING (
    clinic_id IN (SELECT app.user_clinic_orgs())
) WITH CHECK (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

-- 10. case_patient_link policies (LAB NEVER SEES CASE_PATIENT_LINK)
CREATE POLICY case_patient_link_select ON case_patient_link
FOR SELECT USING (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

CREATE POLICY case_patient_link_insert ON case_patient_link
FOR INSERT WITH CHECK (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

CREATE POLICY case_patient_link_update ON case_patient_link
FOR UPDATE USING (
    clinic_id IN (SELECT app.user_clinic_orgs())
) WITH CHECK (
    clinic_id IN (SELECT app.user_clinic_orgs())
);

-- 11. "case" policies
CREATE POLICY case_select ON "case"
FOR SELECT USING (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY case_insert ON "case"
FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY case_update ON "case"
FOR UPDATE USING (
    partnership_id IN (SELECT app.user_partnerships())
) WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

-- 12. case_item policies
CREATE POLICY case_item_select ON case_item
FOR SELECT USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

CREATE POLICY case_item_insert ON case_item
FOR INSERT WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

CREATE POLICY case_item_update ON case_item
FOR UPDATE USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
) WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

-- 13. case_event policies
CREATE POLICY case_event_select ON case_event
FOR SELECT USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

CREATE POLICY case_event_insert ON case_event
FOR INSERT WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

-- 14. shipment policies
CREATE POLICY shipment_select ON shipment
FOR SELECT USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

CREATE POLICY shipment_insert ON shipment
FOR INSERT WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

CREATE POLICY shipment_update ON shipment
FOR UPDATE USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
) WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

-- 15. ledger_entry policies
CREATE POLICY ledger_entry_select ON ledger_entry
FOR SELECT USING (
    partnership_id IN (SELECT app.user_partnerships())
);

CREATE POLICY ledger_entry_insert ON ledger_entry
FOR INSERT WITH CHECK (
    partnership_id IN (SELECT app.user_partnerships())
);

-- Grant privileges to application role
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
        GRANT USAGE ON SCHEMA app TO kopru_app;
        GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO kopru_app;
        GRANT USAGE ON SCHEMA public TO kopru_app;
        GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO kopru_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO kopru_app;
    END IF;
END
$$;
