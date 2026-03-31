-- 数据库建表脚本
--
-- 本版针对“初始录音记录表”补充了以下字段：
-- 1）caller_number：主叫号码
-- 2）customer_name：客户姓名
-- 3）customer_status：客户状态
-- 4）project_name：项目名称
-- 5）task_name：任务名称
-- 6）recording_file_name：录音文件名（按 坐席工号_客户号码_时间戳.wav 生成）
--
-- 这样可以和现有 Excel 台账、录音文件命名规范保持一致。

CREATE TABLE qc_call_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(64) NOT NULL UNIQUE,
    caller_number VARCHAR(32),
    agent_id VARCHAR(64),
    agent_name VARCHAR(64),
    customer_id VARCHAR(64),
    customer_phone VARCHAR(32),
    customer_name VARCHAR(64),
    customer_status VARCHAR(64),
    project_name VARCHAR(128),
    task_name VARCHAR(128),
    business_line VARCHAR(64),
    skill_group VARCHAR(64),
    call_type VARCHAR(16),
    start_time DATETIME,
    end_time DATETIME,
    duration_seconds INT,
    recording_file_name VARCHAR(255),
    recording_url VARCHAR(500),
    process_status VARCHAR(32),
    process_message VARCHAR(500),
    last_process_time DATETIME,
    transcript_segment_count INT,
    INDEX idx_qc_call_record_agent(agent_id),
    INDEX idx_qc_call_record_customer_phone(customer_phone),
    INDEX idx_qc_call_record_recording_file_name(recording_file_name),
    INDEX idx_qc_call_record_start(start_time),
    INDEX idx_qc_call_record_status(process_status)
);

CREATE TABLE qc_call_transcript (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(64) NOT NULL,
    segment_index INT,
    speaker_role VARCHAR(16),
    start_ms BIGINT,
    end_ms BIGINT,
    transcript_text LONGTEXT,
    confidence DECIMAL(8,4),
    INDEX idx_qc_call_transcript_call(call_id)
);

CREATE TABLE qc_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32),
    business_line VARCHAR(64),
    expression_text LONGTEXT,
    deduct_score INT,
    severity VARCHAR(16),
    enabled TINYINT(1),
    INDEX idx_qc_rule_enabled(enabled)
);

CREATE TABLE qc_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(64) NOT NULL UNIQUE,
    total_score INT,
    compliance_score INT,
    service_score INT,
    process_score INT,
    business_score INT,
    risk_level VARCHAR(16),
    need_manual_review TINYINT(1),
    summary_text LONGTEXT,
    qc_status VARCHAR(32),
    INDEX idx_qc_result_risk(risk_level),
    INDEX idx_qc_result_status(qc_status)
);

CREATE TABLE qc_hit_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64),
    rule_name VARCHAR(128),
    hit_flag TINYINT(1),
    severity VARCHAR(16),
    deduct_score INT,
    evidence_text LONGTEXT,
    start_ms BIGINT,
    end_ms BIGINT,
    judge_source VARCHAR(16),
    INDEX idx_qc_hit_detail_call(call_id),
    INDEX idx_qc_hit_detail_rule(rule_code)
);

CREATE TABLE qc_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id VARCHAR(64) NOT NULL,
    reviewer_id VARCHAR(64),
    reviewer_name VARCHAR(64),
    review_result VARCHAR(32),
    adjusted_score INT,
    review_comment LONGTEXT,
    reviewed_at DATETIME,
    INDEX idx_qc_review_call(call_id),
    INDEX idx_qc_review_time(reviewed_at)
);

-- 如需初始化 Erupt 菜单入口，请继续执行 init-menu.sql
