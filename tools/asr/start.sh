#!/usr/bin/env bash
set -e
if [ -z "$FFMPEG_EXE" ]; then
  if [ -x /usr/bin/ffmpeg ]; then
    export FFMPEG_EXE=/usr/bin/ffmpeg
  elif [ -x /usr/local/bin/ffmpeg ]; then
    export FFMPEG_EXE=/usr/local/bin/ffmpeg
  fi
fi
if [ -n "$FFMPEG_EXE" ]; then
  export PATH="$(dirname "$FFMPEG_EXE"):$PATH"
fi
export ASR_PORT="${ASR_PORT:-18081}"
python -m uvicorn asr_server:app --host 0.0.0.0 --port "$ASR_PORT"
