@echo off
:: 콘솔 없이 프로그램 실행하기 위한 Windows 배치 파일
title SRT 숨김 실행기
echo SRT 프로그램 시작 중...

:: 현재 디렉토리 저장
set CURRENT_DIR=%~dp0
set JAR_PATH=%CURRENT_DIR%app\build\libs\srt-with-dependencies.jar

:: JAVA_HOME 확인
if not defined JAVA_HOME (
    echo JAVA_HOME이 설정되지 않았습니다. 
    echo 자바 환경변수를 확인해주세요.
    pause
    exit /b 1
)

:: JAR 파일 존재 확인
if not exist "%JAR_PATH%" (
    echo JAR 파일을 찾을 수 없습니다: %JAR_PATH%
    echo 프로그램을 먼저 빌드해야 합니다.
    pause
    exit /b 1
)

:: 전체 경로로 JavaW 지정
set JAVAW_EXE=%JAVA_HOME%\bin\javaw.exe

:: JavaW 파일 존재 확인
if not exist "%JAVAW_EXE%" (
    echo JavaW를 찾을 수 없습니다: %JAVAW_EXE%
    echo 자바 환경변수를 확인해주세요.
    pause
    exit /b 1
)

echo JavaW 경로: %JAVAW_EXE%
echo JAR 경로: %JAR_PATH%
echo 프로그램을 시작합니다...

:: 시작 명령어
start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"

:: 2초 후 배치 창도 종료
echo 프로그램이 시작되었습니다.
timeout /t 2 /nobreak > nul
exit 