# 源码中文讲解

> 本文档说明如何启动本地免费 ASR 服务，以及为什么本项目采用 FunASR 方案。

# 本地免费 ASR 方案（FunASR）

## 1. 方案说明

本项目默认使用本地 Python 微服务完成录音转文字：

- Python FastAPI 提供 HTTP 接口
- FunASR 负责中文 ASR
- ffmpeg 负责音频统一转为 16k / mono / wav
- Java 项目通过 `/api/v1/calls/{callId}/transcribe` 调用本地 ASR 并自动落库

## 2. 安装依赖

```bash
cd tools/asr
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

> 另需安装系统级 `ffmpeg`

## 3. 启动服务

```bash
bash start.sh
```

## 4. 测试接口

```bash
curl -X POST "http://127.0.0.1:18080/transcribe" \
  -F "file=@/data/test-call.wav" \
  -F "hotword=订单号 客服 工单 售后"
```

返回示例：

```json
{
  "success": true,
  "text": "您好，这里是售后客服中心。",
  "segments": [
    {
      "index": 0,
      "start_ms": 0,
      "end_ms": 3200,
      "text": "您好，这里是售后客服中心。"
    }
  ]
}
```

## 5. Java 配置

```yaml
qc:
  transcription:
    enabled: true
    base-url: http://127.0.0.1:18080
    transcribe-path: /transcribe
    health-path: /health
```
