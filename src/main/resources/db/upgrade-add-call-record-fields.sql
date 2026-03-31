-- 已有数据库升级脚本：给 qc_call_record 补充与 Excel 台账一致的字段

ALTER TABLE qc_call_record ADD COLUMN caller_number VARCHAR(32) NULL COMMENT '主叫号码';
ALTER TABLE qc_call_record ADD COLUMN customer_name VARCHAR(64) NULL COMMENT '客户姓名';
ALTER TABLE qc_call_record ADD COLUMN customer_status VARCHAR(64) NULL COMMENT '客户状态';
ALTER TABLE qc_call_record ADD COLUMN project_name VARCHAR(128) NULL COMMENT '项目名称';
ALTER TABLE qc_call_record ADD COLUMN task_name VARCHAR(128) NULL COMMENT '任务名称';
ALTER TABLE qc_call_record ADD COLUMN recording_file_name VARCHAR(255) NULL COMMENT '录音文件名';

CREATE INDEX idx_qc_call_record_customer_phone ON qc_call_record(customer_phone);
CREATE INDEX idx_qc_call_record_recording_file_name ON qc_call_record(recording_file_name);
