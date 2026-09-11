-- V13__attachment.sql
-- Vaka ekleri (dosya). İçerik bytea olarak saklanır (demo için yeterli).
-- Yükleyenin adı+tarafı denormalize; hasta kimliği içermez. RLS: ortaklık üyeleri.

CREATE TABLE attachment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id uuid NOT NULL REFERENCES "case"(id) ON DELETE CASCADE,
    uploader_user_id uuid NOT NULL REFERENCES app_user(id),
    uploader_ad text NOT NULL,
    uploader_taraf org_tipi NOT NULL,
    dosya_adi text NOT NULL,
    mime text,
    boyut bigint NOT NULL,
    icerik bytea NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachment_case_created ON attachment (case_id, created_at);

ALTER TABLE attachment ENABLE ROW LEVEL SECURITY;
ALTER TABLE attachment FORCE ROW LEVEL SECURITY;

CREATE POLICY attachment_select ON attachment FOR SELECT USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);
CREATE POLICY attachment_insert ON attachment FOR INSERT WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
    AND uploader_user_id = app.current_user_id()
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT SELECT, INSERT ON attachment TO kopru_app;
  END IF;
END
$$;
