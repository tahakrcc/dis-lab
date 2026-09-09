-- V8__concurrency_and_membership.sql

-- 1. Optimistic locking: eszamanli onay/karsi-teklif yarisinda kayip guncellemeyi onler.
--    JPA @Version bu kolonu kullanir; catisan ikinci transaction 409 alir.
ALTER TABLE "case" ADD COLUMN version bigint NOT NULL DEFAULT 0;

-- 2. Uye ekleme icin kullanici adiyla lookup.
--    Hedef kullanici henuz org uyesi olmadigindan app_user RLS altinda gorunmez;
--    bu SECURITY DEFINER fonksiyon yalnizca id'yi dar kapsamda dondurur.
CREATE OR REPLACE FUNCTION app.find_user_id_by_username(p_kullanici_adi citext)
RETURNS TABLE(id uuid)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT u.id FROM app_user u WHERE u.kullanici_adi = p_kullanici_adi
$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT EXECUTE ON FUNCTION app.find_user_id_by_username(citext) TO kopru_app;
  END IF;
END
$$;
