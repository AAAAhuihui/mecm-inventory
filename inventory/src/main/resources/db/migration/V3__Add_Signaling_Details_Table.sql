-- Create table if not exists
CREATE TABLE IF NOT EXISTS signaling_details (
    id BIGINT PRIMARY KEY,
    transaction_id VARCHAR(255),
    app_instance_id VARCHAR(64) NOT NULL,
    target_ip VARCHAR(45) NOT NULL,
    target_dnai VARCHAR(255) NOT NULL,
    request_payload TEXT,
    response_code INTEGER,
    response_body TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add missing columns if they don't exist
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS ue_type VARCHAR(10);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS ue_ip VARCHAR(45);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS dnn VARCHAR(255);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS sst VARCHAR(10);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS sd VARCHAR(15);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS network_segment VARCHAR(45);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS upf VARCHAR(255);
ALTER TABLE signaling_details ADD COLUMN IF NOT EXISTS route_prof_id VARCHAR(255);

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_signaling_app_instance ON signaling_details(app_instance_id);
CREATE INDEX IF NOT EXISTS idx_signaling_status ON signaling_details(status);
CREATE INDEX IF NOT EXISTS idx_signaling_create_time ON signaling_details(create_time);
CREATE INDEX IF NOT EXISTS idx_signaling_ue_type ON signaling_details(ue_type);
CREATE INDEX IF NOT EXISTS idx_signaling_dnn ON signaling_details(dnn);