# 源码中文讲解

> 这一份 README 已经按“项目功能说明”的视角整理，方便你快速理解 1.0 版本的整体结构与运行方式。

# 呼叫录音质检系统 1.0（Erupt）

基于 **Spring Boot 3 + Erupt + JPA + MySQL** 的 1.0 可用版骨架，已覆盖：
- 录音接入
- 本地免费 ASR 转写（FunASR 微服务）
- 录音上传后自动异步转写
- 基础规则质检
- 更完整的质检详情工作台页面
- 通话记录表格导入（Excel / CSV）
- 人工复核
- 基础报表

## 1. 技术栈
- Java 17
- Spring Boot 3.5.6
- Erupt 1.13.3
- Spring Data JPA
- MySQL 8.x
- Thymeleaf
- Spring Async
- Python FastAPI + FunASR（本地免费转写服务）

## 2. 启动步骤
### 2.1 启动本地 ASR 服务
进入 `tools/asr`：

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
Linux: ./start.sh
# Windows: start_asr.bat
```

需提前安装 `ffmpeg`。

### 2.2 启动 Java 项目
```bash
mvn spring-boot:run
```

默认使用 MySQL，连接信息见 `src/main/resources/application.yml`。

## 2.3 初始化 MySQL 数据库
先创建数据库：

```sql
CREATE DATABASE call_qc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

再根据实际环境修改 `application.yml` 中的数据库地址、用户名和密码。

## 3. 默认能力
- 导入通话后，如果已有 `recordingUrl`，可自动加入异步转写队列
- 上传录音文件后，可自动异步转写并自动触发基础质检
- `qc.auto-execute-after-transcription=true` 时，转写完成后自动跑基础规则质检
- 启动时自动注入 demo 规则和 demo 通话 `demo-call-001`
- 可访问页面：`/pages/qc-detail/demo-call-001`

## 4. 关键配置
数据库默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/call_qc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:change-me}
```

`src/main/resources/application.yml`

```yaml
qc:
  auto-execute-after-transcription: true
  async:
    enabled: true
    auto-submit-after-import: true
    auto-submit-after-recording-upload: true
    recording-upload-dir: ./uploads/recordings
  transcription:
    enabled: true
    base-url: http://127.0.0.1:18081
    transcribe-path: /transcribe
    temp-dir: ./temp/audio
```

## 5. 主要接口
- `POST /api/v1/calls/import`
- `POST /api/v1/calls/import/file`
- `GET /api/v1/calls/import/template`
- `POST /api/v1/calls/{callId}/recording/upload`
- `GET /api/v1/calls/{callId}/recording/content`
- `POST /api/v1/calls/{callId}/transcribe`
- `POST /api/v1/calls/{callId}/transcribe/async`
- `POST /api/v1/calls/{callId}/transcribe/upload`
- `GET /api/v1/qc/page`
- `GET /api/v1/qc/{callId}/detail`
- `POST /api/v1/qc/{callId}/review`
- `GET /pages/qc-detail/{callId}`
- `GET /api/v1/reports/overview`
- `GET /api/v1/reports/agents`
- `GET /api/v1/reports/rules`

## 6. 规则说明
当前基础规则支持以下判定模式：
- `KEYWORD_PRESENT`：文本中出现关键词即命中
- `KEYWORD_MISSING`：文本中未出现关键词即命中
- `REGEX_PRESENT`：正则命中
- `CLOSING_MISSING`：末尾未检测到结束语即命中

## 7. 目录说明
- `src/main/java/com/oai/callqc/entity`：实体与 Erupt 后台对象
- `src/main/java/com/oai/callqc/controller`：API 接口
- `src/main/java/com/oai/callqc/service/impl`：业务实现
- `src/main/resources/templates/qc-detail.html`：质检详情工作台模板
- `src/main/resources/templates/call-record-import.html`：通话记录表格导入页模板
- `tools/asr`：本地免费 ASR 服务脚本
- `docs/api-v1.md`：接口清单
- `docs/schema-mysql.sql`：业务建表 SQL
- `docs/upgrade-add-call-record-fields.sql`：已有数据库升级脚本（补齐 Excel 台账字段）
- `docs/import-template.csv`：导入模板示例
- `docs/init-menu.sql`：Erupt 菜单初始化 SQL


## 当前默认部署配置说明

- 数据库默认使用 MySQL：`127.0.0.1:3306/call_qc`
- 数据库账号密码：`root / 自行配置`
- Erupt 默认管理员账号密码：`erupt / 自行配置`
- 本地 ASR 服务地址：`http://127.0.0.1:18081`
- 启动前请先创建数据库：`call_qc`
- 如果访问 Erupt 登录页，请确认启动类中已包含 `@EruptScan({"com.oai.callqc", "xyz.erupt"})`



## 8.1 表格导入能力说明
当前项目已经补充了和你现有基础台账一致的字段：
- 主叫号码 -> `callerNumber`
- 客户号码 -> `customerPhone`
- 坐席姓名 -> `agentName`
- 坐席工号 -> `agentId`
- 项目名称 -> `projectName`
- 任务名称 -> `taskName`
- 客户姓名 -> `customerName`
- 客户状态 -> `customerStatus`
- 录音文件名 -> `recordingFileName`

表格导入时，系统会自动生成：
- `callId = 坐席工号_客户号码_开始时间`
- `recordingFileName = 坐席工号_客户号码_开始时间.wav`

这样能和你的录音命名规范保持一致，例如：
- `20260001_13543010534_20260326170407.wav`

导入页面入口：
- Erupt 后台 -> 质检管理 -> 通话记录 -> 顶部按钮 `导入表格`
- 或直接访问：`/pages/call-record-import`

## 8. Erupt 后台菜单入口（方案 A）
当前项目已经把 `CallRecord` 强化为 Erupt 后台资源，并增加了行按钮 `质检详情`，点击后会直接跳到 `/pages/qc-detail/{callId}`。

当前版本已经补充了菜单初始化 SQL，优先建议你直接执行：

- `docs/init-menu.sql`
- 或 `src/main/resources/db/init-menu.sql`

执行完成后，刷新后台即可看到：
- 质检管理
  - 通话记录

如果你不想执行 SQL，也可以继续走【系统管理 → 菜单维护】手工新增：
1. 新建一级菜单：`质检管理`
2. 在一级菜单下新建二级菜单：`通话记录`
3. 菜单类型选择：`table`
4. 类型值填写：`CallRecord`
5. 保存后刷新页面

进入 `通话记录` 列表页后，可以：
- 按 `callId`、坐席姓名、业务线、通话类型、处理状态进行搜索
- 查看录音地址、处理状态、转写分段数等字段
- 点击每行的 `质检详情` 按钮，在新标签页打开质检详情工作台

> 注意：Erupt 新增资源后，通常仍需要在“菜单维护”里做一次菜单映射，类型值填写实体类类名即可。


## 通话记录导入入口

当前版本已经补齐两个入口：

1. 在 **质检管理 -> 通话记录** 页面顶部点击 **导入表格** 按钮
2. 在左侧菜单直接点击 **质检管理 -> 通话记录导入**

如果你点击按钮时只看到 Erupt 默认确认弹窗，那是因为旧版本按钮没有关闭默认提示。本版本已经在按钮配置里关闭默认提示，点击后会直接打开导入页。


## 新增：录音 ZIP 导入

- 页面：`/pages/call-record-import`
- 接口：`POST /api/v1/calls/recordings/import/zip`
- 行为：上传 ZIP 后自动解压到 `qc.transcription.temp-dir`（默认 `./temp/audio`），并按录音文件名回写通话记录。

## 默认入口说明

当前版本为了免登录直接进入业务页面，访问根地址：

```text
http://127.0.0.1:8080/
```

会直接跳转到：

```text
/pages/call-record-import
```

也就是说，**不再要求先进入 Erupt 登录页再操作业务页面**。

如仍手工访问 `/erupt`，则还是 Erupt 自带后台入口。


## 8. Python ASR 启动脚本
- Linux：`tools/asr/start.sh`
- Windows：`tools/asr/start_asr.bat`
- 默认端口：`18081`
- 如需显式指定 ffmpeg：
  - Windows：先 `set FFMPEG_EXE=D:\\Program\\ffmpeg\\bin\\ffmpeg.exe` 再运行脚本
  - Linux：先 `export FFMPEG_EXE=/usr/bin/ffmpeg` 再运行脚本


## MySQL 初始化修复说明

如果启动日志出现 `modify column `qc_call_transcript`.`transcript_text` `LONGTEXT`` 相关 DDL 警告，说明旧版本启用了全局字段引用符并给 LONGTEXT 生成了错误 SQL。

当前版本已修复：
- 去掉 `application.yml` 中的 `globally_quoted_identifiers: true`
- `CallTranscript.transcriptText` 不再使用 `columnDefinition = "LONGTEXT"`

如果你的数据库已经是旧结构，请额外执行：

```sql
ALTER TABLE qc_call_transcript MODIFY COLUMN transcript_text LONGTEXT NULL COMMENT '转写文本';
```
