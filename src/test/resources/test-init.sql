-- =============================================================
-- Test container initialization — mirrors docker/init.sql
-- Executed by Testcontainers before Flyway migrations run.
-- =============================================================

-- Restricted application role (same as production setup)
CREATE ROLE kopru_app WITH LOGIN PASSWORD 'kopru_app_pass';
GRANT CONNECT ON DATABASE kopru TO kopru_app;

-- Default privileges for tables/sequences created by postgres
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO kopru_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO kopru_app;
