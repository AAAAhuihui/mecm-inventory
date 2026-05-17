-- app_ports was added to V1 after many environments had already applied V1;
-- Flyway does not re-run V1, so existing DBs need this standalone migration.
alter table if exists mecapplicationinventory
    add column if not exists app_ports jsonb;
