-- =============================================================
-- Köprü — PostgreSQL Initialization Script
-- Runs as superuser (postgres) when the container first starts.
-- =============================================================

-- Restricted application role: subject to RLS, no BYPASSRLS
CREATE ROLE kopru_app WITH LOGIN PASSWORD 'kopru_app_pass';
GRANT CONNECT ON DATABASE kopru TO kopru_app;

-- Default privileges: tables/sequences created by postgres (Flyway)
-- in the public schema are automatically accessible to kopru_app.
-- No DELETE granted — records are never deleted, only soft-deactivated.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO kopru_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO kopru_app;
