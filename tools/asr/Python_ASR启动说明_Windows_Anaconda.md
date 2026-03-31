# Python ASR 启动说明（Windows / Anaconda / Python 3.10）

本文只说明 **Python 语音识别服务（ASR）** 的启动方式，不包含 Java 项目启动。

当前建议使用 **Anaconda 新建 `asr310` 环境** 来运行 ASR，避免 `py39` 环境在安装 `torch` / `torchaudio` 时出现兼容性和下载异常问题。

---

## 1. 适用场景

适用于当前项目中的本地 ASR 服务目录：

```text
call-qc-erupt/tools/asr
```

Python 服务启动后，Java 项目会通过下面这个地址调用转写接口：

```text
http://127.0.0.1:18080/transcribe
```

健康检查地址：

```text
http://127.0.0.1:18080/health
```

---

## 2. 环境要求

建议环境如下：

- Windows
- Anaconda / Miniconda
- Python 3.10
- ffmpeg
- 项目目录中已存在 `tools/asr/asr_server.py`

---

## 3. 第一次安装与启动

### 3.1 打开 Anaconda Prompt

不要使用普通 `cmd`，建议直接打开 **Anaconda Prompt**。

---

### 3.2 创建新的 Python 3.10 环境

```bat
conda create -n asr310 python=3.10 -y
```

激活环境：

```bat
conda activate asr310
```

检查 Python 版本：

```bat
python --version
```

应看到类似输出：

```text
Python 3.10.x
```

---

### 3.3 升级 pip

```bat
python -m pip install --upgrade pip
```

---

### 3.4 安装 PyTorch

```bat
pip install torch torchaudio
```

验证安装结果：

```bat
python -c "import torch; print(torch.__version__)"
```

能输出版本号，说明安装成功。

---

### 3.5 安装其余 ASR 依赖

```bat
pip install fastapi uvicorn python-multipart soundfile funasr modelscope huggingface_hub
```

验证：

```bat
python -c "import fastapi, uvicorn, funasr; print('ok')"
```

如果输出 `ok`，说明核心依赖安装成功。

---

### 3.6 进入 ASR 服务目录

```bat
cd 你的项目路径\call-qc-erupt\tools\asr
```

例如：

```bat
cd D:\workspace\call-qc-erupt\tools\asr
```

---

### 3.7 启动 ASR 服务

推荐使用 `uvicorn` 启动：

```bat
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18080
```

正常启动后会看到类似输出：

```text
Uvicorn running on http://0.0.0.0:18080
Application startup complete
```

---

### 3.8 验证服务是否启动成功

浏览器打开：

```text
http://127.0.0.1:18080/health
```

如果返回健康检查结果，说明 Python ASR 已成功启动。

---

## 4. 后续每次启动的命令

以后不需要重复安装依赖，只需要执行：

```bat
conda activate asr310
cd 你的项目路径\call-qc-erupt\tools\asr
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18080
```

---

## 5. 常见问题处理

### 5.1 pip 安装 torch 时出现 WinError 32

典型报错：

```text
WinError 32 另一个程序正在使用此文件，进程无法访问
```

处理方法：

```bat
taskkill /F /IM python.exe
taskkill /F /IM pip.exe
python -m pip cache purge
```

然后重新安装：

```bat
pip install torch torchaudio
```

如果还是不稳定，继续使用新的 `asr310` 环境，不要回到旧的 `py39` 环境。

---

### 5.2 conda install pytorch torchaudio cpuonly 报 Content-Length 不匹配

典型报错：

```text
CondaError: Downloaded bytes did not match Content-Length
```

说明 Conda 下载包时中断或缓存损坏。

处理方式：

```bat
conda clean --all -y
```

但更推荐的做法是：

- 不再用当前 `py39` 环境继续折腾
- 直接新建 `asr310`
- 使用 `pip install torch torchaudio`

---

### 5.3 启动时提示缺少模块

例如：

```text
ModuleNotFoundError: No module named 'fastapi'
```

重新安装依赖：

```bat
pip install fastapi uvicorn python-multipart soundfile funasr modelscope huggingface_hub
```

---

### 5.4 服务启动了，但转写失败

优先检查 `ffmpeg` 是否可用：

```bat
ffmpeg -version
```

如果提示找不到命令，需要先安装 ffmpeg 并加入系统环境变量。

---

### 5.5 端口 18080 被占用

如果看到端口占用报错，可以临时改成其他端口：

```bat
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18081
```

但如果改端口，Java 项目配置也要同步修改：

```yaml
qc:
  transcription:
    local-http-url: http://127.0.0.1:18081/transcribe
```

---

## 6. 启动成功的判断标准

只要下面这个地址能访问：

```text
http://127.0.0.1:18080/health
```

并返回正常结果，就说明 Python 部分已经启动好，Java 项目也可以调用本地转写服务。

---

## 7. 最推荐的完整命令清单

```bat
conda create -n asr310 python=3.10 -y
conda activate asr310
python -m pip install --upgrade pip
pip install torch torchaudio
pip install fastapi uvicorn python-multipart soundfile funasr modelscope huggingface_hub
cd 你的项目路径\call-qc-erupt\tools\asr
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18080
```

---

## 8. 备注

当前项目建议保留 Python ASR 服务，不迁移到 Java。  
业务主系统继续由 Java 调用本地 ASR HTTP 服务即可。
