import logging
import os
import shutil
import subprocess
import tempfile
import traceback
import uuid
from pathlib import Path
from typing import Any, Optional

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import soundfile as sf

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("call-qc-asr")

APP_NAME = "call-qc-asr"
ASR_DEVICE = os.getenv("ASR_DEVICE", "cpu")
ASR_MODEL = os.getenv("ASR_MODEL", "paraformer-zh")
ASR_VAD_MODEL = os.getenv("ASR_VAD_MODEL", "fsmn-vad")
ASR_PUNC_MODEL = os.getenv("ASR_PUNC_MODEL", "ct-punc")
PORT = int(os.getenv("ASR_PORT", os.getenv("PORT", "18081")))

app = FastAPI(title=APP_NAME)


class TranscribePathRequest(BaseModel):
    audio_path: str
    hotword: Optional[str] = ""


def resolve_ffmpeg_exe() -> str:
    """
    解析 ffmpeg 可执行文件路径，兼容 Windows / Linux / macOS。

    优先级：
    1. 环境变量 FFMPEG_EXE
    2. 常见安装路径
    3. 系统 PATH 中的 ffmpeg
    """
    candidates = []

    env_path = os.getenv("FFMPEG_EXE", "").strip()
    if env_path:
        candidates.append(env_path)

    if os.name == "nt":
        candidates.extend([
            r"D:\Program\ffmpeg\bin\ffmpeg.exe",
            r"D:\ffmpeg\bin\ffmpeg.exe",
            r"C:\ffmpeg\bin\ffmpeg.exe",
            r"C:\Program Files\ffmpeg\bin\ffmpeg.exe",
            r"C:\Program Files (x86)\ffmpeg\bin\ffmpeg.exe",
        ])
    else:
        candidates.extend([
            "/usr/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/opt/homebrew/bin/ffmpeg",
        ])

    for path in candidates:
        if path and os.path.isfile(path):
            return path

    path_cmd = shutil.which("ffmpeg")
    if path_cmd:
        return path_cmd

    raise RuntimeError(
        "未找到 ffmpeg，无法转换音频格式。请安装 ffmpeg 并加入 PATH，或设置环境变量 FFMPEG_EXE。"
    )


def ensure_ffmpeg_on_path() -> Optional[str]:
    """
    确保当前 Python 进程的 PATH 中包含 ffmpeg 所在目录。
    这样即便第三方库内部直接调用 `ffmpeg`，也能找到可执行文件。
    """
    try:
        ffmpeg_exe = resolve_ffmpeg_exe()
    except Exception as e:
        logger.warning("ffmpeg not found during startup: %s", e)
        return None

    ffmpeg_dir = str(Path(ffmpeg_exe).parent)
    current_path = os.environ.get("PATH", "")
    path_items = current_path.split(os.pathsep) if current_path else []
    normalized = {os.path.normcase(os.path.normpath(p)) for p in path_items if p}
    ffmpeg_dir_normalized = os.path.normcase(os.path.normpath(ffmpeg_dir))
    if ffmpeg_dir_normalized not in normalized:
        os.environ["PATH"] = ffmpeg_dir + os.pathsep + current_path if current_path else ffmpeg_dir
    os.environ["FFMPEG_EXE"] = ffmpeg_exe
    logger.info("ffmpeg ready: %s", ffmpeg_exe)
    return ffmpeg_exe


# 在导入 FunASR 之前先尽量把 ffmpeg 注入 PATH，避免第三方库内部直接调用 ffmpeg 失败
BOOTSTRAP_FFMPEG = ensure_ffmpeg_on_path()

from funasr import AutoModel


def run_cmd(cmd: list[str]) -> None:
    """执行外部命令并在失败时抛出更清晰的异常。"""
    logger.info("run command: %s", cmd)
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError as e:
        raise RuntimeError(
            "未找到 ffmpeg，无法转换音频格式。请安装 ffmpeg 并加入 PATH，或设置环境变量 FFMPEG_EXE。"
        ) from e
    if proc.returncode != 0:
        logger.error("command failed, stdout=%s, stderr=%s", proc.stdout, proc.stderr)
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip() or "command failed")


def is_standard_wav(path: str) -> bool:
    return str(path).lower().endswith(".wav")


def convert_to_wav(src: str, dst: str) -> None:
    ffmpeg_exe = resolve_ffmpeg_exe()
    cmd = [
        ffmpeg_exe,
        "-y",
        "-i", src,
        "-vn",
        "-ac", "1",
        "-ar", "16000",
        dst,
    ]
    run_cmd(cmd)


def parse_result(raw: Any) -> dict:
    item = raw[0] if isinstance(raw, list) and raw else raw
    if not isinstance(item, dict):
        return {"text": str(item), "segments": [], "raw": raw}

    text = item.get("text", "")
    timestamps = (
        item.get("sentence_info")
        or item.get("sentences")
        or item.get("timestamp")
        or item.get("timestamps")
        or []
    )

    segments = []
    if isinstance(timestamps, list):
        for idx, seg in enumerate(timestamps):
            if isinstance(seg, dict):
                seg_text = seg.get("text") or seg.get("sentence") or ""
                start_ms = seg.get("start", seg.get("begin", 0))
                end_ms = seg.get("end", seg.get("stop", 0))
                if isinstance(start_ms, float):
                    start_ms = int(start_ms * 1000)
                if isinstance(end_ms, float):
                    end_ms = int(end_ms * 1000)
                segments.append({
                    "index": idx + 1,
                    "start_ms": int(start_ms or 0),
                    "end_ms": int(end_ms or 0),
                    "text": seg_text,
                })
            elif isinstance(seg, (list, tuple)) and len(seg) >= 3:
                start_ms = int(seg[0] * 1000) if isinstance(seg[0], float) else int(seg[0])
                end_ms = int(seg[1] * 1000) if isinstance(seg[1], float) else int(seg[1])
                segments.append({
                    "index": idx + 1,
                    "start_ms": start_ms,
                    "end_ms": end_ms,
                    "text": str(seg[2]),
                })

    if not segments and text:
        segments = [{"index": 1, "start_ms": 0, "end_ms": 0, "text": text}]

    return {"text": text, "segments": segments, "raw": raw}


logger.info("loading model, model=%s, device=%s", ASR_MODEL, ASR_DEVICE)
model = AutoModel(
    model=ASR_MODEL,
    vad_model=ASR_VAD_MODEL,
    punc_model=ASR_PUNC_MODEL,
    device=ASR_DEVICE,
)
logger.info("model loaded")


def generate_by_model_input(audio_input: Any, hotword: str = "") -> dict:
    logger.info("start generate by input object")
    raw = model.generate(input=audio_input, batch_size_s=300, hotword=hotword or "")
    parsed = parse_result(raw)
    logger.info("generate success, segments=%s", len(parsed["segments"]))
    return {
        "success": True,
        "text": parsed["text"],
        "segments": parsed["segments"],
        "raw": parsed["raw"],
    }


def load_audio_array_from_wav(audio_file: str):
    speech, sample_rate = sf.read(audio_file, dtype="float32")
    if getattr(speech, "ndim", 1) > 1:
        speech = speech.mean(axis=1)
    return speech, int(sample_rate)


def transcribe_audio_file(audio_path: str, hotword: str = "") -> dict:
    logger.info("transcribe begin, audio_path=%s", audio_path)
    if not audio_path:
        raise RuntimeError("audio_path 不能为空")

    src_path = Path(audio_path)
    logger.info("exists=%s is_file=%s suffix=%s", src_path.exists(), src_path.is_file(), src_path.suffix)
    if not src_path.exists() or not src_path.is_file():
        raise RuntimeError(f"音频文件不存在: {audio_path}")

    ensure_ffmpeg_on_path()

    with tempfile.TemporaryDirectory() as tmpdir:
        normalized_wav = os.path.join(tmpdir, f"{uuid.uuid4().hex}.wav")
        if is_standard_wav(audio_path):
            try:
                # 先尝试直接读取 wav，避免进入 torchaudio/torchcodec 链路
                speech, sample_rate = load_audio_array_from_wav(str(src_path))
                logger.info("direct soundfile wav load success, sample_rate=%s", sample_rate)
                if sample_rate == 16000:
                    return generate_by_model_input(speech, hotword)
            except Exception as e:
                logger.exception("direct soundfile wav load failed, fallback to ffmpeg normalize: %s", e)

        logger.info("normalize audio by ffmpeg: src=%s dst=%s", src_path, normalized_wav)
        convert_to_wav(str(src_path), normalized_wav)
        speech, sample_rate = load_audio_array_from_wav(normalized_wav)
        logger.info("normalized wav load success, sample_rate=%s", sample_rate)
        return generate_by_model_input(speech, hotword)


@app.get("/health")
def health():
    try:
        ffmpeg_path = ensure_ffmpeg_on_path() or resolve_ffmpeg_exe()
        ffmpeg_found = True
    except Exception:
        ffmpeg_path = None
        ffmpeg_found = False

    return {
        "success": True,
        "service": APP_NAME,
        "port": PORT,
        "ffmpeg_found": ffmpeg_found,
        "ffmpeg_path": ffmpeg_path,
        "model": ASR_MODEL,
        "device": ASR_DEVICE,
    }


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...), hotword: Optional[str] = Form(default="")):
    suffix = Path(file.filename or "audio.wav").suffix or ".wav"
    with tempfile.TemporaryDirectory() as tmpdir:
        src_path = os.path.join(tmpdir, f"{uuid.uuid4().hex}{suffix}")
        with open(src_path, "wb") as f:
            shutil.copyfileobj(file.file, f)
        try:
            return JSONResponse(transcribe_audio_file(src_path, hotword or ""))
        except Exception as e:
            traceback.print_exc()
            logger.exception("transcribe failed")
            raise HTTPException(status_code=500, detail=f"transcribe failed: {repr(e)}")


@app.post("/transcribe_path")
async def transcribe_path(request: TranscribePathRequest):
    try:
        return JSONResponse(transcribe_audio_file(request.audio_path, request.hotword or ""))
    except Exception as e:
        traceback.print_exc()
        logger.exception("transcribe_path failed")
        raise HTTPException(status_code=500, detail=f"transcribe_path failed: {repr(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("asr_server:app", host="0.0.0.0", port=PORT, reload=False)
