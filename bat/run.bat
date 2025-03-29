@echo off
chcp 65001 > nul
setlocal

REM 환경 변수 설정
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
set "PYTHONIOENCODING=utf-8"
set "PYTHONUNBUFFERED=1"
set "FFMPEG_LOGLEVEL=warning"
set "STREAMLINK_LOGLEVEL=info"
set "PYTHONUTF8=1"

REM Chzzk 관련 설정
set "HTTP_HEADERS=User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,Referer:https://chzzk.naver.com/,Origin:https://chzzk.naver.com"

echo =====================================
echo  SRT 스트리머 녹화 도구를 시작합니다
echo =====================================
echo.

REM 필요한 폴더 생성
if not exist cache mkdir cache
if not exist logs mkdir logs

echo 프로그램을 실행합니다...
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.
echo.
echo 종료하려면 Ctrl+C를 누르거나 이 창을 닫으세요.
echo.

REM Gradle로 앱 실행
call gradlew.bat run --console=plain

echo.
echo 프로그램이 종료되었습니다.
endlocal
pause 
