# Python ASR 排错说明

## 查看健康检查
访问 `http://127.0.0.1:18081/health`，确认 `ffmpeg_found=true`。

## 查看详细日志
当前 `asr_server.py` 已经输出完整 traceback。
若转写失败，请看 Python 控制台里的异常堆栈。

## 常见问题
- `音频文件不存在`：检查 Java 传入的 `temp/audio` 路径是否真实存在
- `未找到 ffmpeg`：设置环境变量 `FFMPEG_EXE` 或将 ffmpeg 加入 PATH
- `direct wav generate failed`：说明 wav 文件规格不兼容，服务会自动尝试 ffmpeg 转换后再识别
