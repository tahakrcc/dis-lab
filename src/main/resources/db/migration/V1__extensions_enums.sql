-- V1__extensions_enums.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "citext";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- Schema for RLS functions and context
CREATE SCHEMA IF NOT EXISTS app;

-- Enums
CREATE TYPE org_tipi AS ENUM ('LAB', 'KLINIK');
CREATE TYPE rol_tipi AS ENUM ('LAB_ADMIN', 'LAB_TEKNISYEN', 'KLINIK_ADMIN', 'HEKIM', 'ASISTAN');
CREATE TYPE aktiflik AS ENUM ('AKTIF', 'PASIF');
CREATE TYPE case_status AS ENUM (
    'TASLAK', 'FIYAT_MUTABAKATI', 'ONAYLANDI', 'URETIMDE', 'PROVA',
    'TAMAMLANDI', 'TESLIM_EDILDI', 'IPTAL'
);
CREATE TYPE measure_type AS ENUM ('STL', 'FIZIKSEL');
CREATE TYPE event_type AS ENUM ('DURUM', 'FIYAT', 'ONAY');
CREATE TYPE ledger_type AS ENUM ('BORC', 'TAHSILAT', 'DUZELTME');
CREATE TYPE birim_tipi AS ENUM ('DIS', 'ADET');
CREATE TYPE shipment_yon AS ENUM ('KLINIKTEN_LABA', 'LABDAN_KLINIGE');

-- RLS Helper Functions
-- NOT: Tabloya baglı olmayan yardımcı burada; tabloya (membership/partnership/
-- organization) baglı fonksiyonlar V4'te (tablolar olustuktan SONRA) tanımlanir.
-- Aksi halde Postgres fonksiyon govdesini CREATE aninda dogruladigi icin
-- "relation membership does not exist" hatasi olusur.
CREATE OR REPLACE FUNCTION app.current_user_id() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT nullif(current_setting('app.user_id', true), '')::uuid
$$;
