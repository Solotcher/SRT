@echo off
:: UTF-8 인코딩 설정
chcp 65001 > nul
title SRT 실행기

echo SRT 프로그램을 실행합니다...
echo.

:: 현재 디렉토리 설정
set CURRENT_DIR=%~dp0
set JAR_PATH=%CURRENT_DIR%app\build\libs\srt-with-dependencies.jar

:: JAR 파일 확인
if not exist "%JAR_PATH%" (
    echo 오류: JAR 파일을 찾을 수 없습니다.
    echo 파일 경로: %JAR_PATH%
    echo 먼저 프로그램을 빌드해야 합니다.
    pause
    exit /b 1
)

:: 자바 홈 디렉토리 설정 - 미리 알려진 경로 시도
set JAVA_PATHS=^
"C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot"^
"C:\Program Files\Eclipse Adoptium\jdk-23.0.2.7-hotspot"

set FOUND_JAVA=0

:: 자바 홈 먼저 확인
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        set JAVA_DIR=%JAVA_HOME%
        set FOUND_JAVA=1
        echo 시스템 JAVA_HOME에서 Java를 찾았습니다: %JAVA_DIR%
    )
)

:: 미리 알려진 경로 시도
if %FOUND_JAVA%==0 (
    for %%p in (%JAVA_PATHS%) do (
        if exist %%p\bin\javaw.exe (
            set JAVA_DIR=%%p
            set FOUND_JAVA=1
            echo 알려진 경로에서 Java를 찾았습니다: !JAVA_DIR!
            goto :found_java
        )
    )
)

:found_java
if %FOUND_JAVA%==0 (
    echo 오류: 시스템에서 자바를 찾을 수 없습니다.
    echo Java를 설치한 후 다시 시도하세요.
    pause
    exit /b 1
)

:: JavaW.exe 경로 설정
set JAVAW_EXE=%JAVA_DIR%\bin\javaw.exe

echo 실행할 JAR 파일: %JAR_PATH%
echo 사용할 Java 경로: %JAVAW_EXE%
echo.
echo 프로그램을 시작합니다...

:: 자바 시스템 속성 설정
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Djava.library.path=%JAVA_DIR%\bin

:: 프로그램 실행
start "" "%JAVAW_EXE%" %JAVA_OPTS% -jar "%JAR_PATH%"

:: 짧은 대기 후 종료
echo 프로그램이 백그라운드에서 실행되었습니다.
timeout /t 2 /nobreak > nul
exit 