@echo off
:: UTF-8 인코딩 설정
chcp 65001 > nul
title SRT 간단 실행기
echo SRT 프로그램을 실행합니다...

:: 현재 디렉토리
set CURRENT_DIR=%~dp0
set JAR_PATH=%CURRENT_DIR%app\build\libs\srt-with-dependencies.jar

:: JAR 파일 확인
if not exist "%JAR_PATH%" (
    echo JAR 파일을 찾을 수 없습니다: %JAR_PATH%
    echo 먼저 빌드를 실행해야 합니다.
    pause
    exit /b 1
)

:: javaw.exe 직접 지정
set JAVAW_EXE=C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\javaw.exe

:: 실행
echo JAR 경로: %JAR_PATH%
echo JavaW 경로: %JAVAW_EXE%
echo 프로그램 시작 중...

start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"

:: 2초 후 종료
echo 프로그램이 백그라운드에서 실행되었습니다.
timeout /t 2 /nobreak > nul
exit 