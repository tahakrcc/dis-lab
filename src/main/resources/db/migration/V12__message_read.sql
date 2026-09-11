-- V12__message_read.sql
-- Okundu bilgisi: bir mesaj karşı tarafça ilk okunduğunda okundu_at set edilir.

ALTER TABLE message ADD COLUMN okundu_at timestamptz;

-- Ortaklık üyeleri okundu_at güncelleyebilir (karşı tarafın mesajlarını okurken)
CREATE POLICY message_update ON message FOR UPDATE USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
) WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT UPDATE ON message TO kopru_app;
  END IF;
END
$$;
