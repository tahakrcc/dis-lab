-- V5__views.sql

CREATE OR REPLACE VIEW balance WITH (security_invoker = true) AS
SELECT partnership_id,
       coalesce(sum(tutar) FILTER (WHERE tur='BORC'), 0)      AS toplam_borc,
       coalesce(-sum(tutar) FILTER (WHERE tur='TAHSILAT'), 0) AS toplam_tahsilat,
       coalesce(sum(tutar), 0)                                 AS bakiye
FROM ledger_entry 
GROUP BY partnership_id;

-- Grant select on view to kopru_app
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kopru_app') THEN
        GRANT SELECT ON balance TO kopru_app;
    END IF;
END
$$;
