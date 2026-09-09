-- V6__seed_data.sql
-- Seed Data according to Specification §8:
-- 1 Lab, 2 Kliniks, 2 Partnerships, 5 Service Items, Price Lists,
-- Users for each role (LAB_ADMIN, LAB_TEKNISYEN, KLINIK_ADMIN, HEKIM, ASISTAN),
-- 3 Sample Cases in different statuses (TASLAK, URETIMDE, TAMAMLANDI)

-- Parola for all users: 'password123'
-- BCrypt hash: $2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a

-- 1. Organizations
INSERT INTO organization (id, tip, ad, telefon, vergi_no, adres, ayarlar) VALUES
('11111111-1111-1111-1111-111111111111', 'LAB', 'DentArt Laboratuvar', '02125550101', '1234567890', 'Kadikoy, Istanbul', '{"calisma_saatleri": "08:30-18:00"}'),
('22222222-2222-2222-2222-222222222222', 'KLINIK', 'Inci Dis Klinigi', '02125550202', '2345678901', 'Besiktas, Istanbul', '{"yetkili": "Dr. Ahmet Yilmaz"}'),
('33333333-3333-3333-3333-333333333333', 'KLINIK', 'Beyaz Gul Agiz ve Dis Sagligi', '02165550303', '3456789012', 'Uskudar, Istanbul', '{"yetkili": "Dr. Ayse Demir"}');

-- 2. App Users (5 distinct roles)
INSERT INTO app_user (id, ad, email, telefon, parola_hash) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Mehmet Oz (Lab Admin)', 'labadmin@dentart.com', '05321110001', '$2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Ali Vural (Teknisyen)', 'teknisyen@dentart.com', '05321110002', '$2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Fatma Kaya (Klinik Admin)', 'admin@incidis.com', '05321110003', '$2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a'),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Dt. Ahmet Yilmaz (Hekim)', 'hekim@incidis.com', '05321110004', '$2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Zeynep Celik (Asistan)', 'asistan@incidis.com', '05321110005', '$2a$10$w8.3fCshkL9h.fQvO3q0heWv31N8K5yIeHj7lV.rWzJ4cT4KzHj0a');

-- 3. Memberships
INSERT INTO membership (id, user_id, org_id, rol, durum) VALUES
('m1111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'LAB_ADMIN', 'AKTIF'),
('m2222222-2222-2222-2222-222222222222', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'LAB_TEKNISYEN', 'AKTIF'),
('m3333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'KLINIK_ADMIN', 'AKTIF'),
('m4444444-4444-4444-4444-444444444444', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'HEKIM', 'AKTIF'),
('m5555555-5555-5555-5555-555555555555', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '22222222-2222-2222-2222-222222222222', 'ASISTAN', 'AKTIF');

-- 4. Partnerships (DentArt <-> Inci, DentArt <-> Beyaz Gul)
INSERT INTO partnership (id, lab_id, clinic_id, durum, vade_gun) VALUES
('p1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'AKTIF', 30),
('p2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'AKTIF', 45);

INSERT INTO case_sequence (partnership_id, son_no) VALUES
('p1111111-1111-1111-1111-111111111111', 3),
('p2222222-2222-2222-2222-222222222222', 0);

-- 5. Service Items (DentArt Catalog - 5 items)
INSERT INTO service_item (id, lab_id, ad, birim, aktif) VALUES
('s1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Zirkonyum Kron', 'DIS', true),
('s2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Porselen Kron', 'DIS', true),
('s3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'E-Max Laminate', 'DIS', true),
('s4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'Gece Plagi', 'ADET', true),
('s5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', 'Total Protez', 'ADET', true);

-- 6. Price List Entries (DentArt <-> Inci)
INSERT INTO price_list_entry (id, partnership_id, service_item_id, fiyat, gecerli_baslangic, gecerli_bitis, aktif) VALUES
('ple11111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 's1111111-1111-1111-1111-111111111111', 1500.00, '2026-01-01', NULL, true),
('ple22222-2222-2222-2222-222222222222', 'p1111111-1111-1111-1111-111111111111', 's2222222-2222-2222-2222-222222222222', 1000.00, '2026-01-01', NULL, true),
('ple33333-3333-3333-3333-333333333333', 'p1111111-1111-1111-1111-111111111111', 's3333333-3333-3333-3333-333333333333', 2000.00, '2026-01-01', NULL, true),
('ple44444-4444-4444-4444-444444444444', 'p1111111-1111-1111-1111-111111111111', 's4444444-4444-4444-4444-444444444444', 800.00,  '2026-01-01', NULL, true),
('ple55555-5555-5555-5555-555555555555', 'p1111111-1111-1111-1111-111111111111', 's5555555-5555-5555-5555-555555555555', 4000.00, '2026-01-01', NULL, true);

-- 7. Patient (Inci Dis Klinigi)
INSERT INTO patient (id, clinic_id, ad, telefon, dogum_tarihi, not) VALUES
('pat11111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Canan Can', '05441234567', '1990-05-15', 'Alerjik reaksiyon yok');

-- 8. 3 Sample Cases in Different Statuses
-- Case 1: TASLAK
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, olcu_tipi, durum, toplam_tutar, fiyat_versiyon) VALUES
('c1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', '2026-000001', 'C.C.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'STL', 'TASLAK', 3000.00, 1);

INSERT INTO case_patient_link (case_id, patient_id, clinic_id) VALUES
('c1111111-1111-1111-1111-111111111111', 'pat11111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam, materyal, renk) VALUES
('ci111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 's1111111-1111-1111-1111-111111111111', '{11, 12}', 2, 1500.00, 3000.00, 'Zirkon', 'A2');

-- Case 2: URETIMDE
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, assigned_to, olcu_tipi, durum, toplam_tutar, fiyat_versiyon, onay_klinik_versiyon, onay_lab_versiyon, onay_klinik_at, onay_lab_at) VALUES
('c2222222-2222-2222-2222-222222222222', 'p1111111-1111-1111-1111-111111111111', '2026-000002', 'M.B.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'FIZIKSEL', 'URETIMDE', 800.00, 1, 1, 1, now() - interval '2 days', now() - interval '2 days');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam) VALUES
('ci222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', 's4444444-4444-4444-4444-444444444444', '{}', 1, 800.00, 800.00);

-- Case 3: TAMAMLANDI
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, assigned_to, olcu_tipi, durum, toplam_tutar, fiyat_versiyon, onay_klinik_versiyon, onay_lab_versiyon, onay_klinik_at, onay_lab_at) VALUES
('c3333333-3333-3333-3333-333333333333', 'p1111111-1111-1111-1111-111111111111', '2026-000003', 'H.K.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'STL', 'TAMAMLANDI', 2000.00, 1, 1, 1, now() - interval '5 days', now() - interval '5 days');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam, materyal, renk) VALUES
('ci333333-3333-3333-3333-333333333333', 'c3333333-3333-3333-3333-333333333333', 's3333333-3333-3333-3333-333333333333', '{21}', 1, 2000.00, 2000.00, 'E-Max', 'BL1');
