# call-qc-erupt

一个基于 **Spring Boot 3 + Erupt + JPA + MySQL** 的呼叫录音质检系统示例项目，支持：

- 通话记录导入（Excel / CSV）
- 录音文件上传与 ZIP 导入
- 本地 ASR 转写（FunASR 微服务）
- 异步转写任务队列
- 基础规则质检
- 人工复核
- 报表统计
- Erupt 后台管理页面

## 技术栈

- Java 17
- Spring Boot 3.5.6
- Erupt 1.13.3
- Spring Data JPA
- MySQL 8.x
- Thymeleaf
- Spring Async
- Python FastAPI + FunASR（本地 ASR 服务）

## 项目结构

```text
call-qc-erupt/
├── src/main/java/com/oai/callqc      # Java 业务代码
├── src/main/resources                # 配置、SQL、模板
├── docs/                             # API、数据库脚本、排错文档
├── tools/asr/                        # 本地 ASR 服务脚本
├── .env.example                      # 环境变量示例
└── pom.xml
```

## 功能概览

### 1. 通话记录管理
- 导入通话记录表格
- 维护通话基础信息
- 关联录音文件与录音地址

### 2. 转写能力
- 上传录音后自动入队转写
- 支持本地 FunASR 服务
- 支持异步任务调度与失败重试

### 3. 质检能力
- 转写完成后自动执行基础规则质检
- 支持人工复核与结果确认
- 支持规则命中明细展示

### 4. 页面与接口
- Erupt 后台列表页
- 质检详情页
- 导入页面
- REST API

## 快速开始

### 1. 准备数据库
先创建数据库：

```sql
CREATE DATABASE call_qc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 2. 配置环境变量
复制 `.env.example`，按你的环境修改：

```bash
cp .env.example .env
```

然后至少准备这些变量：

- `DB_USERNAME`
- `DB_PASSWORD`
- `ERUPT_DEFAULT_PASSWORD`
- `QC_TRANSCRIPTION_BASE_URL`

> 说明：当前项目默认仍从 `application.yml` 读取配置，`.env.example` 主要用于说明推荐配置项与占位值；你可以通过系统环境变量或 IDE / 启动脚本注入这些变量。

### 3. 启动本地 ASR 服务（可选但推荐）
进入 `tools/asr`：

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
./start.sh
```

Windows 可使用：

```bat
start_asr.bat
```

启动前请确保已安装 `ffmpeg`。

### 4. 启动 Java 项目

```bash
mvn spring-boot:run
```

默认访问：

- 应用首页：`http://127.0.0.1:8080/`
- 质检详情页示例：`/pages/qc-detail/demo-call-001`

## 关键配置

配置文件位置：

- `src/main/resources/application.yml`

当前改为通过环境变量覆盖敏感项，例如：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/call_qc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:change-me}

erupt:
  upms:
    default-account: erupt
    default-password: ${ERUPT_DEFAULT_PASSWORD:change-me}

qc:
  transcription:
    base-url: ${QC_TRANSCRIPTION_BASE_URL:http://127.0.0.1:18081}
```

## 主要接口

- `POST /api/v1/calls/import`
- `POST /api/v1/calls/import/file`
- `GET /api/v1/calls/import/template`
- `POST /api/v1/calls/{callId}/recording/upload`
- `POST /api/v1/calls/{callId}/transcribe`
- `POST /api/v1/calls/{callId}/transcribe/async`
- `GET /api/v1/qc/{callId}/detail`
- `POST /api/v1/qc/{callId}/review`
- `GET /api/v1/reports/overview`

更详细的接口说明见：

- `docs/api-v1.md`

## 数据库与初始化脚本

可参考：

- `docs/schema-mysql.sql`
- `docs/init-menu.sql`
- `docs/create-qc-transcribe-task.sql`
- `docs/upgrade-add-call-record-fields.sql`
- `docs/upgrade-transcript-text-longtext.sql`
- `docs/fix-qc-call-transcript-longtext.sql`

## 安全说明

这个仓库已做基础清理：

- 不上传运行产物、IDE 文件、数据库文件、临时目录
- 默认密码已改为环境变量占位
- 建议不要把生产数据库地址、真实账号密码、内部接口地址直接提交到仓库

如果你打算公开发布，请在提交前再次检查：

- `application.yml`
- `.env`
- 各类脚本与文档中的账号、密码、URL

## 开发建议

推荐本地忽略以下内容：

- `target/`
- `.idea/`
- `tools/asr/.venv/`
- `temp/`
- `.erupt/`
- `*.mv.db`

## License

当前仓库未显式声明 License。如需开源分发，建议补充 LICENSE 文件。
