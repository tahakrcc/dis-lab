-- V3__constraints_triggers.sql

-- 1. Exclusion constraint on price_list_entry
ALTER TABLE price_list_entry
ADD CONSTRAINT price_list_entry_no_overlap
EXCLUDE USING gist (
  partnership_id WITH =,
  service_item_id WITH =,
  daterange(gecerli_baslangic, gecerli_bitis, '[)') WITH &&
) WHERE (aktif);

-- 2. Trigger: Price list entry service_item must belong to partnership.lab_id
CREATE OR REPLACE FUNCTION trg_check_price_list_service_item_lab()
RETURNS TRIGGER AS $$
DECLARE
    v_lab_id uuid;
    v_part_lab_id uuid;
BEGIN
    SELECT lab_id INTO v_lab_id FROM service_item WHERE id = NEW.service_item_id;
    SELECT lab_id INTO v_part_lab_id FROM partnership WHERE id = NEW.partnership_id;
    
    IF v_lab_id IS NULL OR v_part_lab_id IS NULL OR v_lab_id <> v_part_lab_id THEN
        RAISE EXCEPTION 'service_item belongs to a different lab or does not exist';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_price_list_entry_lab_match
BEFORE INSERT OR UPDATE ON price_list_entry
FOR EACH ROW EXECUTE FUNCTION trg_check_price_list_service_item_lab();

-- 3. Trigger: Membership role and organization type validation
CREATE OR REPLACE FUNCTION trg_check_membership_org_role()
RETURNS TRIGGER AS $$
DECLARE
    v_tip org_tipi;
BEGIN
    SELECT tip INTO v_tip FROM organization WHERE id = NEW.org_id;
    IF v_tip IS NULL THEN
        RAISE EXCEPTION 'Organization does not exist';
    END IF;

    IF v_tip = 'LAB' AND NEW.rol NOT IN ('LAB_ADMIN', 'LAB_TEKNISYEN') THEN
        RAISE EXCEPTION 'Role % is not valid for organization type LAB', NEW.rol;
    END IF;

    IF v_tip = 'KLINIK' AND NEW.rol NOT IN ('KLINIK_ADMIN', 'HEKIM', 'ASISTAN') THEN
        RAISE EXCEPTION 'Role % is not valid for organization type KLINIK', NEW.rol;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_membership_org_role_match
BEFORE INSERT OR UPDATE ON membership
FOR EACH ROW EXECUTE FUNCTION trg_check_membership_org_role();

-- 4. Case Item FDI Check Function and Constraint
CREATE OR REPLACE FUNCTION is_valid_fdi_array(arr int[])
RETURNS boolean AS $$
DECLARE
    elem int;
BEGIN
    IF arr IS NULL THEN
        RETURN true;
    END IF;
    FOREACH elem IN ARRAY arr
    LOOP
        IF NOT (
            (elem BETWEEN 11 AND 18) OR
            (elem BETWEEN 21 AND 28) OR
            (elem BETWEEN 31 AND 38) OR
            (elem BETWEEN 41 AND 48) OR
            (elem BETWEEN 51 AND 55) OR
            (elem BETWEEN 61 AND 65) OR
            (elem BETWEEN 71 AND 75) OR
            (elem BETWEEN 81 AND 85)
        ) THEN
            RETURN false;
        END IF;
    END LOOP;
    RETURN true;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

ALTER TABLE case_item
ADD CONSTRAINT chk_case_item_fdi_numbers
CHECK (is_valid_fdi_array(dis_numaralari));

-- 5. Trigger: Case item unit consistency (DIS vs ADET)
CREATE OR REPLACE FUNCTION trg_check_case_item_unit_consistency()
RETURNS TRIGGER AS $$
DECLARE
    v_birim birim_tipi;
BEGIN
    SELECT birim INTO v_birim FROM service_item WHERE id = NEW.service_item_id;
    IF v_birim IS NULL THEN
        RAISE EXCEPTION 'Service item not found';
    END IF;

    IF v_birim = 'DIS' THEN
        IF NEW.dis_numaralari IS NULL OR cardinality(NEW.dis_numaralari) = 0 THEN
            RAISE EXCEPTION 'dis_numaralari cannot be empty when birim is DIS';
        END IF;
        IF NEW.adet <> cardinality(NEW.dis_numaralari) THEN
            RAISE EXCEPTION 'adet (%) must match cardinality of dis_numaralari (%) when birim is DIS', NEW.adet, cardinality(NEW.dis_numaralari);
        END IF;
    ELSIF v_birim = 'ADET' THEN
        IF NEW.dis_numaralari IS NOT NULL AND cardinality(NEW.dis_numaralari) > 0 THEN
            RAISE EXCEPTION 'dis_numaralari must be empty when birim is ADET';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_case_item_unit_consistency
BEFORE INSERT OR UPDATE ON case_item
FOR EACH ROW EXECUTE FUNCTION trg_check_case_item_unit_consistency();

-- 6. Trigger: Prevent UPDATE and DELETE on case_event
CREATE OR REPLACE FUNCTION trg_prevent_mutation_case_event()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'case_event records cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_case_event_immutable
BEFORE UPDATE OR DELETE ON case_event
FOR EACH ROW EXECUTE FUNCTION trg_prevent_mutation_case_event();

-- 7. Trigger: Prevent UPDATE and DELETE on ledger_entry
CREATE OR REPLACE FUNCTION trg_prevent_mutation_ledger_entry()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entry records cannot be updated or deleted. Use DUZELTME type to adjust.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entry_immutable
BEFORE UPDATE OR DELETE ON ledger_entry
FOR EACH ROW EXECUTE FUNCTION trg_prevent_mutation_ledger_entry();

-- 8. Ledger Entry Constraints & Partial Unique Index
CREATE UNIQUE INDEX idx_unique_case_borc ON ledger_entry (case_id) WHERE tur = 'BORC';

ALTER TABLE ledger_entry
ADD CONSTRAINT chk_ledger_borc_rules
CHECK (
    (tur <> 'BORC') OR (
        case_id IS NOT NULL AND
        tutar = matrah + kdv_tutari AND
        kdv_tutari = round(matrah * kdv_orani / 100.0, 2)
    )
);

ALTER TABLE ledger_entry
ADD CONSTRAINT chk_ledger_tahsilat_rules
CHECK (
    (tur <> 'TAHSILAT') OR (tutar < 0)
);

-- 9. Indexes from §1.3
CREATE INDEX idx_membership_org_id_aktif ON membership (org_id) WHERE durum = 'AKTIF';
CREATE INDEX idx_membership_user_id_aktif ON membership (user_id) WHERE durum = 'AKTIF';
CREATE INDEX idx_partnership_clinic_id ON partnership (clinic_id);
CREATE INDEX idx_case_partnership_durum ON "case" (partnership_id, durum);
CREATE INDEX idx_case_assigned_to ON "case" (assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_case_item_case_id ON case_item (case_id);
CREATE INDEX idx_case_event_case_id_created_at ON case_event (case_id, created_at DESC);
CREATE INDEX idx_ledger_entry_part_belge_tarihi ON ledger_entry (partnership_id, belge_tarihi);
CREATE INDEX idx_price_list_entry_part_item_aktif ON price_list_entry (partnership_id, service_item_id) WHERE aktif;
CREATE INDEX idx_patient_clinic_id ON patient (clinic_id);
