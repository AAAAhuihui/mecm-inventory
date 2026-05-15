CREATE TABLE IF NOT EXISTS core_network_config (
    core_network_type VARCHAR(32) NOT NULL,
    core_network_name VARCHAR(64) NOT NULL,
    nef_ip VARCHAR(64) NOT NULL DEFAULT '192.168.254.154',
    nef_port INTEGER NOT NULL DEFAULT 8000,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (core_network_type)
);

ALTER TABLE core_network_config ADD COLUMN IF NOT EXISTS nef_ip VARCHAR(64);
ALTER TABLE core_network_config ADD COLUMN IF NOT EXISTS nef_port INTEGER;

UPDATE core_network_config
SET nef_ip = COALESCE(nef_ip, '192.168.254.154'),
    nef_port = COALESCE(nef_port, 8000);

ALTER TABLE core_network_config DROP COLUMN IF EXISTS route_ipv4_addr;
ALTER TABLE core_network_config DROP COLUMN IF EXISTS route_port;
ALTER TABLE core_network_config DROP COLUMN IF EXISTS active;

ALTER TABLE core_network_config ALTER COLUMN nef_ip SET NOT NULL;
ALTER TABLE core_network_config ALTER COLUMN nef_port SET NOT NULL;

INSERT INTO core_network_config (core_network_type, core_network_name, nef_ip, nef_port)
VALUES ('default', '默认核心网', '192.168.254.154', 8000)
ON CONFLICT (core_network_type) DO NOTHING;

INSERT INTO core_network_config (core_network_type, core_network_name, nef_ip, nef_port)
VALUES ('qiantong', '商业核心网-千通', '192.168.254.154', 8000)
ON CONFLICT (core_network_type) DO NOTHING;
