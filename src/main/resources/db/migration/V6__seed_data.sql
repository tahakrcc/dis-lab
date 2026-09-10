-- V6__seed_data.sql
-- Seed: 1 Lab, 2 Klinik, 2 Ortaklik, 5 Hizmet Kalemi, Fiyat Listesi,
-- 5 rolde kullanici, 3 farkli durumda ornek vaka.
-- NOT: Tum UUID'ler gecerli hex (8-4-4-4-12) olmalidir.

-- Parola (tum kullanicilar): 'password123'
-- BCrypt: $2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu

-- 1. Organizations
INSERT INTO organization (id, tip, ad, telefon, vergi_no, adres, ayarlar) VALUES
('11111111-1111-1111-1111-111111111111', 'LAB', 'DentArt Laboratuvar', '02125550101', '1234567890', 'Kadikoy, Istanbul', '{"calisma_saatleri": "08:30-18:00"}'),
('22222222-2222-2222-2222-222222222222', 'KLINIK', 'Inci Dis Klinigi', '02125550202', '2345678901', 'Besiktas, Istanbul', '{"yetkili": "Dr. Ahmet Yilmaz"}'),
('33333333-3333-3333-3333-333333333333', 'KLINIK', 'Beyaz Gul Agiz ve Dis Sagligi', '02165550303', '3456789012', 'Uskudar, Istanbul', '{"yetkili": "Dr. Ayse Demir"}');

-- 2. App Users (kullanici adlari V7 ile ayni: labadmin/teknisyen/klinikadmin/hekim/asistan)
INSERT INTO app_user (id, ad, email, telefon, parola_hash) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Mehmet Oz (Lab Admin)', 'labadmin@dentart.com', '05321110001', '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Ali Vural (Teknisyen)', 'teknisyen@dentart.com', '05321110002', '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Fatma Kaya (Klinik Admin)', 'admin@incidis.com', '05321110003', '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu'),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Dt. Ahmet Yilmaz (Hekim)', 'hekim@incidis.com', '05321110004', '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Zeynep Celik (Asistan)', 'asistan@incidis.com', '05321110005', '$2y$10$5tJJgqy3Rj/7SV6YOvVwi.UXb8jlz6RbayvH177gD76dMQiGVULfu');

-- 3. Memberships
INSERT INTO membership (id, user_id, org_id, rol, durum) VALUES
('a0000001-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'LAB_ADMIN', 'AKTIF'),
('a0000002-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'LAB_TEKNISYEN', 'AKTIF'),
('a0000003-0000-0000-0000-000000000003', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'KLINIK_ADMIN', 'AKTIF'),
('a0000004-0000-0000-0000-000000000004', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'HEKIM', 'AKTIF'),
('a0000005-0000-0000-0000-000000000005', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '22222222-2222-2222-2222-222222222222', 'ASISTAN', 'AKTIF');

-- 4. Partnerships (DentArt <-> Inci, DentArt <-> Beyaz Gul)
INSERT INTO partnership (id, lab_id, clinic_id, durum, vade_gun) VALUES
('b0000001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'AKTIF', 30),
('b0000002-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'AKTIF', 45);

INSERT INTO case_sequence (partnership_id, son_no) VALUES
('b0000001-0000-0000-0000-000000000001', 3),
('b0000002-0000-0000-0000-000000000002', 0);

-- 5. Service Items (DentArt katalogu)
INSERT INTO service_item (id, lab_id, ad, birim, aktif) VALUES
('e0000001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Zirkonyum Kron', 'DIS', true),
('e0000002-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Porselen Kron', 'DIS', true),
('e0000003-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'E-Max Laminate', 'DIS', true),
('e0000004-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'Gece Plagi', 'ADET', true),
('e0000005-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'Total Protez', 'ADET', true);

-- 6. Price List Entries (DentArt <-> Inci)
INSERT INTO price_list_entry (id, partnership_id, service_item_id, fiyat, gecerli_baslangic, gecerli_bitis, aktif) VALUES
('f0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', 'e0000001-0000-0000-0000-000000000001', 1500.00, '2026-01-01', NULL, true),
('f0000002-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000001', 'e0000002-0000-0000-0000-000000000002', 1000.00, '2026-01-01', NULL, true),
('f0000003-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000001', 'e0000003-0000-0000-0000-000000000003', 2000.00, '2026-01-01', NULL, true),
('f0000004-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000001', 'e0000004-0000-0000-0000-000000000004', 800.00,  '2026-01-01', NULL, true),
('f0000005-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000001', 'e0000005-0000-0000-0000-000000000005', 4000.00, '2026-01-01', NULL, true);

-- 7. Patient (Inci Dis Klinigi)
INSERT INTO patient (id, clinic_id, ad, telefon, dogum_tarihi, notlar) VALUES
('fa000001-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'Canan Can', '05441234567', '1990-05-15', 'Alerjik reaksiyon yok');

-- 8. 3 ornek vaka (farkli durumlarda)
-- Vaka 1: TASLAK
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, olcu_tipi, durum, toplam_tutar, fiyat_versiyon) VALUES
('c1111111-1111-1111-1111-111111111111', 'b0000001-0000-0000-0000-000000000001', '2026-000001', 'C.C.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'STL', 'TASLAK', 3000.00, 1);

INSERT INTO case_patient_link (case_id, patient_id, clinic_id) VALUES
('c1111111-1111-1111-1111-111111111111', 'fa000001-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam, materyal, renk) VALUES
('ce000001-0000-0000-0000-000000000001', 'c1111111-1111-1111-1111-111111111111', 'e0000001-0000-0000-0000-000000000001', '{11, 12}', 2, 1500.00, 3000.00, 'Zirkon', 'A2');

-- Vaka 2: URETIMDE
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, assigned_to, olcu_tipi, durum, toplam_tutar, fiyat_versiyon, onay_klinik_versiyon, onay_lab_versiyon, onay_klinik_at, onay_lab_at) VALUES
('c2222222-2222-2222-2222-222222222222', 'b0000001-0000-0000-0000-000000000001', '2026-000002', 'M.B.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'FIZIKSEL', 'URETIMDE', 800.00, 1, 1, 1, now() - interval '2 days', now() - interval '2 days');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam) VALUES
('ce000002-0000-0000-0000-000000000002', 'c2222222-2222-2222-2222-222222222222', 'e0000004-0000-0000-0000-000000000004', '{}', 1, 800.00, 800.00);

-- Vaka 3: TAMAMLANDI
INSERT INTO "case" (id, partnership_id, kod, hasta_rumuzu, created_by, assigned_to, olcu_tipi, durum, toplam_tutar, fiyat_versiyon, onay_klinik_versiyon, onay_lab_versiyon, onay_klinik_at, onay_lab_at) VALUES
('c3333333-3333-3333-3333-333333333333', 'b0000001-0000-0000-0000-000000000001', '2026-000003', 'H.K.', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'STL', 'TAMAMLANDI', 2000.00, 1, 1, 1, now() - interval '5 days', now() - interval '5 days');

INSERT INTO case_item (id, case_id, service_item_id, dis_numaralari, adet, birim_fiyat, ara_toplam, materyal, renk) VALUES
('ce000003-0000-0000-0000-000000000003', 'c3333333-3333-3333-3333-333333333333', 'e0000003-0000-0000-0000-000000000003', '{21}', 1, 2000.00, 2000.00, 'E-Max', 'BL1');
