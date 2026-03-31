# Windows 安装 FFmpeg 教程

本文用于指导在 **Windows** 环境下安装 `ffmpeg`，并完成环境变量配置，供当前项目的 **Python ASR 服务** 使用。

---

## 1. 为什么需要安装 FFmpeg

当前项目中的 Python ASR 服务在处理录音文件时，通常会先把原始音频转换成适合识别的标准格式，例如：

- 单声道
- 16k 采样率
- wav 格式

这一步通常依赖 `ffmpeg` 来完成。

如果没有安装 `ffmpeg`，或者没有正确配置环境变量，就可能出现：

- Python 服务能启动
- 但录音转写时失败
- 日志里提示找不到 `ffmpeg`

---

## 2. 官方下载说明

FFmpeg 官网说明：

- 官网主要提供源码
- Windows 用户通常下载官网列出的 **预编译版本**
- 常见 Windows 预编译来源包括：
  - gyan.dev
  - BtbN

官方下载页：

```text
https://ffmpeg.org/download.html
```

---

## 3. 安装步骤

### 3.1 下载 FFmpeg

打开官网：

```text
https://ffmpeg.org/download.html
```

进入 **Windows EXE Files** 区域，选择一个官网列出的 Windows 预编译版本下载。

---

### 3.2 解压到固定目录

建议把 FFmpeg 解压到一个固定目录，例如：

```text
D:\ffmpeg
```

解压后应保证目录结构类似：

```text
D:\ffmpeg
  └─ bin
      ├─ ffmpeg.exe
      ├─ ffprobe.exe
      └─ ffplay.exe
```

其中最关键的是：

```text
D:\ffmpeg\bin\ffmpeg.exe
D:\ffmpeg\bin\ffprobe.exe
```

---

## 4. 配置环境变量 Path

需要把下面这个路径加入到 Windows 的系统环境变量 `Path` 中：

```text
D:\ffmpeg\bin
```

---

## 5. Windows 图形界面配置步骤

按下面顺序操作：

1. 右键 **此电脑**
2. 点击 **属性**
3. 点击 **高级系统设置**
4. 点击 **环境变量**
5. 在“系统变量”中找到 **Path**
6. 点击 **编辑**
7. 点击 **新建**
8. 填入：

```text
D:\ffmpeg\bin
```

9. 连续点击 **确定** 保存

---

## 6. 配置后如何验证

### 6.1 关闭当前终端

如果你已经打开了 `cmd`、PowerShell、Anaconda Prompt，先全部关闭。

然后重新打开一个新的终端。

---

### 6.2 验证 ffmpeg

执行：

```bat
ffmpeg -version
```

如果安装成功，会看到类似版本输出，而不是提示“不是内部或外部命令”。

---

### 6.3 验证 ffprobe

再执行：

```bat
ffprobe -version
```

如果也能输出版本信息，说明 FFmpeg 已正确安装完成。

---

## 7. 推荐的安装目录

为了后续项目排查更方便，建议固定安装在：

```text
D:\ffmpeg
```

这样项目排查时，默认都可以按这个路径检查。

---

## 8. 常见问题

### 8.1 执行 `ffmpeg -version` 提示不是内部或外部命令

原因通常是：

- 没有安装 FFmpeg
- 已安装，但没有把 `D:\ffmpeg\bin` 加入到 `Path`
- 已加 `Path`，但没有重新打开终端

处理方式：

1. 确认目录下存在：
   - `D:\ffmpeg\bin\ffmpeg.exe`
   - `D:\ffmpeg\bin\ffprobe.exe`
2. 重新检查系统环境变量 `Path`
3. 关闭终端后重新打开再试

---

### 8.2 目录下有 ffmpeg.exe，但命令行仍然找不到

先直接用完整路径测试：

```bat
D:\ffmpeg\bin\ffmpeg.exe -version
```

如果这个命令能执行，说明 FFmpeg 本身没问题，只是环境变量没配好。

---

### 8.3 Python 服务仍然提示 ffmpeg 错误

即使 `ffmpeg` 已安装，也要确认：

- 启动 Python ASR 服务的终端，是在配置环境变量之后重新打开的
- 当前 Python 运行用户能访问 `D:\ffmpeg\bin`

---

## 9. 安装完成后的建议验证

建议你在终端里执行下面两个命令，并确认都能正常输出版本号：

```bat
ffmpeg -version
ffprobe -version
```

只要这两个都能正常执行，Python ASR 这部分通常就不会再因为音频转换缺少工具而失败。

---

## 10.Windows / Linux 启动方式都兼容
Windows

如果你不想配系统 PATH，可以这样启动：
```bat
set FFMPEG_EXE=D:\Program\ffmpeg\bin\ffmpeg.exe
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18081
```

Linux

如果 ffmpeg 在 /usr/bin/ffmpeg，通常什么都不用配，直接：
```bat
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18081
```
如果不是默认路径，也可以：
```bat
export FFMPEG_EXE=/usr/local/bin/ffmpeg
python -m uvicorn asr_server:app --host 0.0.0.0 --port 18081
```

## 10. 备注

当前项目的 Python ASR 服务依赖 FFmpeg 进行音频预处理。  
所以 **FFmpeg 是否安装成功**，是 Python 语音识别服务能否正常工作的前置条件之一。
