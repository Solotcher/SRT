@echo off
chcp 65001 > nul
cls
echo =====================================
echo  SRT 스트리머 녹화 도구를 시작합니다 (Gradle)
echo =====================================
echo.

REM 환경 변수 설정
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
set "PYTHONIOENCODING=utf-8"
set "PYTHONUNBUFFERED=1"
set "FFMPEG_LOGLEVEL=warning"
set "STREAMLINK_LOGLEVEL=info"
set "PYTHONUTF8=1"

REM Chzzk 관련 설정
set "HTTP_HEADERS=User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,Referer:https://chzzk.naver.com/,Origin:https://chzzk.naver.com"

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

echo.
echo === 프로그램 환경 확인 중... ===
echo.

REM 현재 명령어 확인
where streamlink >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo streamlink 확인: 설치됨
    streamlink --version 2>nul | findstr version
) else (
    echo streamlink 확인: 설치되지 않음 - 녹화가 작동하지 않을 수 있습니다
)

where yt-dlp >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo yt-dlp 확인: 설치됨
    yt-dlp --version 2>nul
) else (
    echo yt-dlp 확인: 설치되지 않음
)

where ffmpeg >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ffmpeg 확인: 설치됨
    ffmpeg -version 2>nul | findstr version
) else (
    echo ffmpeg 확인: 설치되지 않음
)

where aria2c >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo aria2c 확인: 설치됨 (다운로드 가속화 사용 가능)
    aria2c --version 2>nul | findstr version
) else (
    echo aria2c 확인: 설치되지 않음
)
echo.

REM 기존 로그 파일 백업
if exist logs\app.log (
    echo 이전 로그 파일 백업 중...
    ren logs\app.log app_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log 2>nul
)

echo.
echo === 애플리케이션 시작 ===
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.
echo.

REM Gradle로 직접 실행
echo Gradle 명령어 실행 중...
call gradlew.bat -Dorg.gradle.java.home="%JAVA_HOME%" run --console=plain --no-daemon --info

echo.
echo 프로그램이 종료되었습니다.
pause 