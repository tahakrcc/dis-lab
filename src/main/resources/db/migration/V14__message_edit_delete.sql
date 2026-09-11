-- V14__message_edit_delete.sql
-- Mesaj düzenleme (duzenlendi_at) ve yumuşak silme (silindi_at). İkisi de UPDATE'tir;
-- V12'deki message_update politikası ve UPDATE grant'i yeterli (yeni izin gerekmez).
-- Yetki (yalnızca gönderen kendi mesajını) uygulama katmanında zorlanır.

ALTER TABLE message ADD COLUMN duzenlendi_at timestamptz;
ALTER TABLE message ADD COLUMN silindi_at timestamptz;
