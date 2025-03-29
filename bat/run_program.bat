@echo off
:: 콘솔 없이 프로그램 실행하기 위한 배치 파일
title SRT 숨김 실행기
echo SRT 프로그램을 콘솔 창 없이 실행합니다...

:: 현재 디렉토리에서 JAR 파일 경로 설정
set JAR_PATH=%~dp0app\build\libs\srt-with-dependencies.jar

:: JAR 파일 존재 확인
if not exist "%JAR_PATH%" (
    echo JAR 파일이 존재하지 않습니다: %JAR_PATH%
    echo 프로그램이 빌드되었는지 확인하세요.
    pause
    exit /b 1
)

:: javaw 명령어로 콘솔 없이 실행
echo 프로그램을 시작합니다...
start "" javaw -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"

:: 3초 기다린 후 배치 창도 종료
timeout /t 3 /nobreak > nul
exit 