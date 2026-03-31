@echo off
setlocal
if "%FFMPEG_EXE%"=="" set FFMPEG_EXE=D:\Program\ffmpeg\bin\ffmpeg.exe
for %%I in ("%FFMPEG_EXE%") do set FFMPEG_DIR=%%~dpI
set PATH=%FFMPEG_DIR%;%PATH%
if "%ASR_PORT%"=="" set ASR_PORT=18081
python -m uvicorn asr_server:app --host 0.0.0.0 --port %ASR_PORT%
pause
