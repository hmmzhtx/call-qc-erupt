> 当前版本默认以 MySQL 作为正式联调/部署数据库。

# 源码中文讲解

> 本文档说明 1.0 版本对外提供的主要接口，以及这些接口在业务链路中的作用。

# 呼叫录音质检系统 API v1

## 1. 导入通话
- `POST /api/v1/calls/import`

说明：`recordingUrl` 现在可为空。若导入时已带录音地址，且 `qc.async.auto-submit-after-import=true`，系统会自动把该通话提交到异步转写队列。

## 2. 录音与转写
- `POST /api/v1/calls/{callId}/recording/upload`：上传录音并自动加入异步转写队列
- `GET /api/v1/calls/{callId}/recording/content`：后端流式播放录音
- `POST /api/v1/calls/{callId}/transcribe`：同步执行自动转写
- `POST /api/v1/calls/{callId}/transcribe/async`：手动加入异步转写队列
- `POST /api/v1/calls/{callId}/transcribe/upload`：上传录音并立即同步转写
- `POST /api/v1/calls/{callId}/transcripts`：写入外部转写结果

说明：当 `qc.auto-execute-after-transcription=true` 时，转写落库后会自动执行一次基础质检。

## 3. 质检接口
- `POST /api/v1/qc/{callId}/execute`：手动触发基础质检
- `GET /api/v1/qc/{callId}/detail`：查看详情
- `POST /api/v1/qc/{callId}/review`：提交人工复核
- `GET /api/v1/qc/page`：分页查询质检列表
- `GET /pages/qc-detail/{callId}`：完整质检详情工作台页面

### 3.1 质检列表参数
- `pageNo`：页码，默认 1
- `pageSize`：每页大小，默认 10
- `callId`：通话 ID 模糊搜索
- `agentName`：坐席姓名模糊搜索
- `businessLine`：业务线模糊搜索
- `processStatus`：处理状态精确匹配
- `riskLevel`：风险等级精确匹配
- `needManualReview`：是否需人工复核

### 3.2 质检列表返回字段
- `callId`
- `agentId`
- `agentName`
- `businessLine`
- `skillGroup`
- `durationSeconds`
- `processStatus`
- `totalScore`
- `riskLevel`
- `needManualReview`
- `hitCount`
- `reviewCount`
- `startTime`

## 4. 报表接口
- `GET /api/v1/reports/overview`：概览
- `GET /api/v1/reports/agents`：坐席维度统计
- `GET /api/v1/reports/rules`：规则命中排行


## 新增：通话记录表格导入

### 1. 表格导入
- 请求方式：`POST /api/v1/calls/import/file`
- Content-Type：`multipart/form-data`
- 表单字段：`file`
- 支持文件：`.xlsx`、`.xls`、`.csv`

### 2. 模板下载
- 请求方式：`GET /api/v1/calls/import/template`
- 返回：CSV 模板文件

### 3. 页面入口
- 页面地址：`GET /pages/call-record-import`


## 导入录音 ZIP

- **接口**：`POST /api/v1/calls/recordings/import/zip`
- **说明**：上传 ZIP，自动解压到 `temp/audio`，并尝试按 `recordingFileName` 或 `callId` 匹配通话记录。
