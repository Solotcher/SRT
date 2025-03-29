@echo off
:: UTF-8 인코딩 설정
chcp 65001 > nul
:: 필수 파일을 로컬로 복사하고 실행하는 배치 파일
title SRT 로컬 자바 환경 설정
echo SRT 로컬 실행 환경을 설정합니다...

:: 현재 디렉토리 저장
set CURRENT_DIR=%~dp0
set JAR_PATH=%CURRENT_DIR%app\build\libs\srt-with-dependencies.jar

:: JDK 파일 복사를 위한 디렉토리
set LOCAL_JAVA_DIR=%CURRENT_DIR%local_java
set LOCAL_BIN_DIR=%LOCAL_JAVA_DIR%\bin
set LOCAL_SERVER_DIR=%LOCAL_BIN_DIR%\server

:: JAR 파일 존재 확인
if not exist "%JAR_PATH%" (
    echo JAR 파일을 찾을 수 없습니다: %JAR_PATH%
    echo 프로그램을 먼저 빌드해야 합니다.
    pause
    exit /b 1
)

:: 로컬 자바 디렉토리 생성
if not exist "%LOCAL_JAVA_DIR%" (
    echo 로컬 자바 디렉토리를 생성합니다...
    mkdir "%LOCAL_JAVA_DIR%"
)

if not exist "%LOCAL_BIN_DIR%" (
    mkdir "%LOCAL_BIN_DIR%"
)

if not exist "%LOCAL_SERVER_DIR%" (
    mkdir "%LOCAL_SERVER_DIR%"
)

:: 필수 자바 파일 복사
if not exist "%LOCAL_BIN_DIR%\javaw.exe" (
    if not defined JAVA_HOME (
        echo JAVA_HOME이 설정되지 않았습니다. 
        echo 자바 환경변수를 확인해주세요.
        pause
        exit /b 1
    )
    
    echo 필수 자바 파일을 복사합니다...
    copy "%JAVA_HOME%\bin\javaw.exe" "%LOCAL_BIN_DIR%\" /Y
    copy "%JAVA_HOME%\bin\java.exe" "%LOCAL_BIN_DIR%\" /Y
    
    :: 서버 폴더와 JVM DLL 복사
    if exist "%JAVA_HOME%\bin\server\jvm.dll" (
        echo server\jvm.dll 복사 중...
        if not exist "%LOCAL_SERVER_DIR%" mkdir "%LOCAL_SERVER_DIR%" 2>nul
        copy "%JAVA_HOME%\bin\server\jvm.dll" "%LOCAL_SERVER_DIR%\" /Y
    ) else if exist "%JAVA_HOME%\jre\bin\server\jvm.dll" (
        echo jre\bin\server\jvm.dll 복사 중...
        if not exist "%LOCAL_SERVER_DIR%" mkdir "%LOCAL_SERVER_DIR%" 2>nul
        copy "%JAVA_HOME%\jre\bin\server\jvm.dll" "%LOCAL_SERVER_DIR%\" /Y
    ) else if exist "%JAVA_HOME%\lib\server\jvm.dll" (
        echo lib\server\jvm.dll 복사 중...
        if not exist "%LOCAL_SERVER_DIR%" mkdir "%LOCAL_SERVER_DIR%" 2>nul
        copy "%JAVA_HOME%\lib\server\jvm.dll" "%LOCAL_SERVER_DIR%\" /Y
    ) else (
        echo jvm.dll 파일을 찾을 수 없습니다.
        echo 자바 실행에 필요한 파일을 찾을 수 없습니다.
        pause
        exit /b 1
    )
    
    :: 필요한 DLL 파일들
    echo 추가 DLL 파일 복사 중...
    if exist "%JAVA_HOME%\bin\*.dll" (
        copy "%JAVA_HOME%\bin\*.dll" "%LOCAL_BIN_DIR%\" /Y >nul
    )
    
    echo 자바 필수 파일 복사 완료!
) else (
    echo 로컬 자바 환경이 이미 설정되어 있습니다.
)

:: 로컬 JAVAW 사용
set LOCAL_JAVAW=%LOCAL_BIN_DIR%\javaw.exe

:: JAVAW 파일 존재 확인
if not exist "%LOCAL_JAVAW%" (
    echo 로컬 JavaW를 찾을 수 없습니다: %LOCAL_JAVAW%
    echo 파일 복사 과정에 문제가 발생했습니다.
    pause
    exit /b 1
)

echo 로컬 JavaW 경로: %LOCAL_JAVAW%
echo JAR 경로: %JAR_PATH%
echo 프로그램을 시작합니다...

:: 시작 명령어
start "" "%LOCAL_JAVAW%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"

:: 2초 후 배치 창도 종료
echo 프로그램이 시작되었습니다.
timeout /t 2 /nobreak > nul
exit 