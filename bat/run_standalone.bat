@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Xmx512m -XX:+UseG1GC"
set "PYTHONIOENCODING=utf-8"
set "PYTHONUNBUFFERED=1"
set "FFMPEG_LOGLEVEL=warning"
set "STREAMLINK_LOGLEVEL=info"
set "PYTHONUTF8=1"
set "HTTP_HEADERS=User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,Referer:https://chzzk.naver.com/,Origin:https://chzzk.naver.com"

echo SRT 스트리머 녹화 도구를 시작합니다...
echo 인코딩 설정: UTF-8
echo 현재 사용 가능한 녹화 프로그램: streamlink, yt-dlp, ffmpeg
echo.

REM 현재 디렉토리 표시
echo 현재 디렉토리: %CD%

REM 캐시 디렉토리 확인
if not exist cache (
    echo 캐시 디렉토리 생성 중...
    mkdir cache
)

REM 로그 디렉토리 확인
if not exist logs (
    echo 로그 디렉토리 생성 중...
    mkdir logs
)

REM Java 확인
echo Java 버전 확인 중...
java -version 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 오류: Java가 설치되지 않았거나 PATH에 없습니다.
    echo Java를 설치하고 다시 시도하세요.
    goto :exit
)

REM JAR 파일 찾기
set "JAR_FILE="
for %%f in (app\build\libs\*.jar) do (
    set "JAR_FILE=%%f"
)

if "!JAR_FILE!" == "" (
    echo JAR 파일을 찾을 수 없습니다. gradlew.bat build 명령을 실행하여 애플리케이션을 빌드하세요.
    goto :exit
) else (
    echo 실행할 JAR 파일: !JAR_FILE!
)

echo.
echo 애플리케이션을 시작합니다...
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.
echo.

REM 기존 로그 파일 백업
if exist logs\app.log (
    echo 이전 로그 파일 백업 중...
    ren logs\app.log app_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log 2>nul
)

REM 로그 파일 생성
echo %date% %time% - 애플리케이션 시작 > logs\app.log

REM Java 애플리케이션 실행
java %JAVA_OPTS% -jar "!JAR_FILE!" >>logs\app.log 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo 애플리케이션 실행 중 오류가 발생했습니다. 오류 코드: %ERRORLEVEL%
    echo 자세한 로그는 logs\app.log 파일을 확인하세요.
) else (
    echo 프로그램이 정상적으로 종료되었습니다.
)

:exit
endlocal
pause 