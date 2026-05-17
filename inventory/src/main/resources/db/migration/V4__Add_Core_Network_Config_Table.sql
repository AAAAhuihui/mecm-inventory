CREATE TABLE IF NOT EXISTS core_network_config (
    core_network_type VARCHAR(32) NOT NULL,
    core_network_name VARCHAR(64) NOT NULL,
    nef_ip VARCHAR(64) NOT NULL,
    nef_port INTEGER NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (core_network_type)
);
