create table if not exists taskinventory (
    task_id varchar(64) not null,
    tenant_id varchar(64) not null,
    task_name varchar(128) not null,
    app_id varchar(64),
    app_name varchar(255),
    task_type varchar(32) not null,
    created_time timestamp default current_timestamp,
    modified_time timestamp default current_timestamp,
    user_name varchar(255),
    user_role varchar(64),
    primary key (task_id)
);

create index if not exists idx_taskinventory_tenant_created
    on taskinventory (tenant_id, created_time desc);
