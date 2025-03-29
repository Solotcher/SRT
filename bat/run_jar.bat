@echo off
chcp 65001 > nul
cls
echo =====================================
echo  SRT 스트리머 녹화 도구를 시작합니다 (JAR)
echo =====================================
echo.

REM 환경 변수 설정
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Xmx512m"
set "PYTHONIOENCODING=utf-8"
set "PYTHONUNBUFFERED=1"
set "FFMPEG_LOGLEVEL=warning"
set "STREAMLINK_LOGLEVEL=info"
set "PYTHONUTF8=1"

REM Chzzk 관련 설정
set "HTTP_HEADERS=User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,Referer:https://chzzk.naver.com/,Origin:https://chzzk.naver.com"

REM Java가 설치되어 있는지 확인
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Java가 설치되어 있지 않습니다. Java 17 이상을 설치해주세요.
    echo https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

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

REM JAR 파일 확인
set "JAR_FILE=app\build\libs\app.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=app\build\libs\srt-with-dependencies.jar"
    if not exist "%JAR_FILE%" (
        echo JAR 파일을 찾을 수 없습니다.
        echo 먼저 gradlew assemble 명령을 실행하세요.
        pause
        exit /b 1
    )
)

REM 프로그램 환경 확인
echo === 설치된 프로그램 확인 중... ===
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
echo 애플리케이션을 시작합니다...
echo JAR 파일: %JAR_FILE%
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.
echo.

REM PowerShell에서 실행 시
if defined PSModulePath (
    REM PowerShell에서는 tee 명령 대신 파이프라인 사용
    powershell -Command "& {java %JAVA_OPTS% -jar %JAR_FILE% | Tee-Object -FilePath logs\app.log}"
) else (
    REM CMD에서는 표준 출력을 파일로 리디렉션하지만 콘솔에도 표시
    java %JAVA_OPTS% -jar %JAR_FILE% > logs\app.log 2>&1
)

echo.
echo 프로그램이 종료되었습니다.

pause 