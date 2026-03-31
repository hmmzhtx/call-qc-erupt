CREATE TABLE IF NOT EXISTS qc_transcribe_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(128) NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    priority_num INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 3,
    locked_by VARCHAR(64) NULL,
    locked_at DATETIME NULL,
    next_run_time DATETIME NULL,
    last_error LONGTEXT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    CONSTRAINT uk_qc_transcribe_task_call_id UNIQUE (call_id)
);
