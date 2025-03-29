@echo off
chcp 65001 > nul
echo streamlink 설치 스크립트를 실행합니다...

REM streamlink가 이미 설치되어 있는지 확인
where streamlink >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo streamlink가 이미 설치되어 있습니다.
    streamlink --version
    pause
    exit /b 0
)

REM pip이 설치되어 있는지 확인
where pip >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo pip이 설치되어 있지 않습니다. Python을 먼저 설치해주세요.
    echo https://www.python.org/downloads/
    pause
    exit /b 1
)

REM streamlink 설치
echo streamlink를 설치합니다...
pip install streamlink

if %ERRORLEVEL% equ 0 (
    echo streamlink가 성공적으로 설치되었습니다.
    streamlink --version
) else (
    echo streamlink 설치에 실패했습니다.
)

pause 