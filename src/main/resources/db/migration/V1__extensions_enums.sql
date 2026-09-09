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
CREATE OR REPLACE FUNCTION app.current_user_id() RETURNS uuid
LANGUAGE sql STABLE AS $$
  SELECT nullif(current_setting('app.user_id', true), '')::uuid
$$;

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
