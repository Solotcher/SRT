@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
set PYTHONIOENCODING=utf-8
set PYTHONUNBUFFERED=1
set FFMPEG_LOGLEVEL=warning
set HTTP_HEADERS=User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,Referer:https://chzzk.naver.com/,Origin:https://chzzk.naver.com

echo ===== SRT 스트리머 녹화 도구 =====
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.

if not exist cache mkdir cache
if not exist logs mkdir logs

call gradlew.bat run --console=plain

pause 