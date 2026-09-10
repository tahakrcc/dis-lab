-- V11__message.sql
-- Vaka içi sohbet (lab <-> klinik). Mesaj vakaya bağlıdır; hasta kimliği içermez.
-- Gönderenin adı ve tarafı (LAB/KLINIK) mesajda saklanır (RLS'te karşı org'un
-- app_user satırı görünmediğinden isim burada denormalize edilir).

CREATE TABLE message (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id uuid NOT NULL REFERENCES "case"(id) ON DELETE CASCADE,
    sender_user_id uuid NOT NULL REFERENCES app_user(id),
    sender_ad text NOT NULL,
    sender_taraf org_tipi NOT NULL,
    metin text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_case_created ON message (case_id, created_at);

ALTER TABLE message ENABLE ROW LEVEL SECURITY;
ALTER TABLE message FORCE ROW LEVEL SECURITY;

-- Vakanın ortaklığına üye olanlar mesajları görür
CREATE POLICY message_select ON message FOR SELECT USING (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
);

-- Yalnızca ortaklık üyesi ve kendisi adına mesaj ekleyebilir
CREATE POLICY message_insert ON message FOR INSERT WITH CHECK (
    case_id IN (SELECT id FROM "case" WHERE partnership_id IN (SELECT app.user_partnerships()))
    AND sender_user_id = app.current_user_id()
);

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
    GRANT SELECT, INSERT ON message TO kopru_app;
  END IF;
END
$$;
