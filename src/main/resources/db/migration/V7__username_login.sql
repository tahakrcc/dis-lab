-- V7__username_login.sql
-- Giris kimligi kullanici adina cevrildi (email opsiyonel oldu) ve
-- auth akisinin FORCE RLS altinda calismasi icin pre-auth lookup eklendi.

-- 1. Kullanici adi (sistem geneli benzersiz). citext => buyuk/kucuk harf duyarsiz.
ALTER TABLE app_user ADD COLUMN kullanici_adi citext;

-- Seed kullanicilarina kullanici adi ata (V6 ile gelen sabit id'ler)
UPDATE app_user SET kullanici_adi = 'labadmin'    WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
UPDATE app_user SET kullanici_adi = 'teknisyen'   WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
UPDATE app_user SET kullanici_adi = 'klinikadmin' WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
UPDATE app_user SET kullanici_adi = 'hekim'       WHERE id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
UPDATE app_user SET kullanici_adi = 'asistan'     WHERE id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';

-- Beklenmeyen (kullanici adsiz) satirlar icin savunma amacli dolgu
UPDATE app_user SET kullanici_adi = 'user_' || left(id::text, 8)
 WHERE kullanici_adi IS NULL;

ALTER TABLE app_user ALTER COLUMN kullanici_adi SET NOT NULL;
ALTER TABLE app_user ADD CONSTRAINT app_user_kullanici_adi_key UNIQUE (kullanici_adi);

-- 2. Email artik opsiyonel (unique kaldi; PostgreSQL birden fazla NULL'a izin verir)
ALTER TABLE app_user ALTER COLUMN email DROP NOT NULL;

-- 3. Pre-auth login lookup.
--    app_user tablosunda FORCE RLS var; login aninda henuz app.user_id yok,
--    bu yuzden normal SELECT 0 satir donerdi. SECURITY DEFINER fonksiyon
--    (superuser sahipli) RLS'i guvenli ve DAR kapsamli sekilde baypas eder:
--    yalnizca tek kullanici adina ait (id, parola_hash) doner.
CREATE OR REPLACE FUNCTION app.find_login(p_kullanici_adi citext)
RETURNS TABLE(id uuid, parola_hash text)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT u.id, u.parola_hash FROM app_user u WHERE u.kullanici_adi = p_kullanici_adi
$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT EXECUTE ON FUNCTION app.find_login(citext) TO kopru_app;
  END IF;
END
$$;
