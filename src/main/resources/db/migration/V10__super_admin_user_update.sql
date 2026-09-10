-- V10__super_admin_user_update.sql
-- V9'da app_user SELECT'e carve-out eklendi ama UPDATE atlanmıştı; süper-admin
-- başka kullanıcının (ad/telefon/parola) satırını güncelleyemiyordu (RLS 0 satır
-- -> Hibernate StaleObjectState -> 409). UPDATE politikasına da carve-out eklenir.

DROP POLICY user_update ON app_user;
CREATE POLICY user_update ON app_user FOR UPDATE USING (
    id = app.current_user_id() OR app.is_super_admin()
) WITH CHECK (
    id = app.current_user_id() OR app.is_super_admin()
);
