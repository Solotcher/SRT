@echo off
setlocal enabledelayedexpansion

echo ===== SRT 애플리케이션 문제 해결 유틸리티 =====
echo.
echo 이 스크립트는 다음 작업을 수행합니다:
echo 1. Java 인코딩 설정 확인 및 업데이트
echo 2. Streamlink 설치 및 확인
echo 3. 다운로드 가속화 설정 적용
echo 4. 앱 캐시 정리
echo.

REM ===== Java 인코딩 설정 확인 =====
echo [진행] Java 인코딩 설정을 확인하고 업데이트합니다...

REM gradle.properties 백업
if exist "gradle.properties" (
    echo [정보] gradle.properties 파일을 백업합니다.
    copy "gradle.properties" "gradle.properties.bak" > nul
) else (
    echo [정보] gradle.properties 파일이 없습니다. 새로 생성합니다.
)

REM gradle.properties 업데이트
echo org.gradle.jvmargs=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8> gradle.properties
echo [완료] gradle.properties 파일이 업데이트되었습니다.

REM ===== Streamlink 확인 및 설치 =====
echo.
echo [진행] Streamlink 설치 여부를 확인합니다...

streamlink --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [정보] Streamlink가 설치되어 있지 않습니다. 설치를 시도합니다...
    
    REM pip 설치 확인
    pip --version > nul 2>&1
    if %errorlevel% neq 0 (
        echo [오류] pip가 설치되어 있지 않습니다. Python을 먼저 설치해주세요.
        echo [안내] https://www.python.org/downloads/ 에서 Python을 다운로드할 수 있습니다.
        goto :error
    )
    
    echo [진행] Streamlink를 설치합니다...
    pip install streamlink
    
    if %errorlevel% neq 0 (
        echo [오류] Streamlink 설치에 실패했습니다.
        goto :error
    ) else (
        echo [완료] Streamlink가 성공적으로 설치되었습니다.
    )
) else (
    echo [완료] Streamlink가 이미 설치되어 있습니다.
)

REM ===== 다운로드 가속화 설정 =====
echo.
echo [진행] 다운로드 가속화 설정을 확인합니다...

REM settings.json 파일이 있는지 확인
if not exist "settings.json" (
    echo [정보] settings.json 파일이 없습니다. 새로 생성합니다.
    echo {> settings.json
    echo   "maxThreads": 8,>> settings.json
    echo   "speedLimit": 0,>> settings.json
    echo   "autoRecord": false,>> settings.json
    echo   "cachePath": "cache">> settings.json
    echo }>> settings.json
    echo [완료] settings.json 파일이 생성되었습니다.
) else (
    echo [정보] settings.json 파일이 이미 존재합니다.
)

REM ===== 앱 캐시 정리 =====
echo.
echo [진행] 애플리케이션 캐시를 정리합니다...

REM build 디렉토리 확인
if exist "build" (
    echo [정보] build 디렉토리를 정리합니다.
    rd /s /q build
    echo [완료] build 디렉토리가 정리되었습니다.
)

REM .gradle 캐시 정리
if exist ".gradle" (
    echo [정보] .gradle 캐시를 정리합니다.
    rd /s /q .gradle
    echo [완료] .gradle 캐시가 정리되었습니다.
)

REM cache 디렉토리 확인 및 생성
if not exist "cache" (
    echo [정보] cache 디렉토리가 없습니다. 새로 생성합니다.
    mkdir cache
    echo [완료] cache 디렉토리가 생성되었습니다.
)

REM ===== 프로젝트 빌드 =====
echo.
echo [진행] 프로젝트를 빌드합니다...
call gradlew clean build --no-daemon

if %errorlevel% neq 0 (
    echo [오류] 프로젝트 빌드에 실패했습니다.
    goto :error
) else (
    echo [완료] 프로젝트가 성공적으로 빌드되었습니다.
)

echo.
echo ===== 모든 설정이 완료되었습니다 =====
echo run.bat를 실행하여 프로그램을 시작하세요.
goto :end

:error
echo.
echo [오류] 설정 중 문제가 발생했습니다.
exit /b 1

:end
echo.
echo 작업이 완료되었습니다.
exit /b 0
 