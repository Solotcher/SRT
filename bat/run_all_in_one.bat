@echo off
:: UTF-8 인코딩 설정
chcp 65001 > nul
:: 모든 종속성을 자체적으로 해결하는 올인원 실행 배치 파일
title SRT 통합 실행기
echo SRT 프로그램 시작 준비 중...

:: 현재 디렉토리 저장
set CURRENT_DIR=%~dp0
set JAR_PATH=%CURRENT_DIR%app\build\libs\srt-with-dependencies.jar

:: JAR 파일 존재 확인
if not exist "%JAR_PATH%" (
    echo JAR 파일을 찾을 수 없습니다: %JAR_PATH%
    echo 먼저 빌드를 실행합니다...
    
    :: gradlew 파일 확인
    if exist "%CURRENT_DIR%gradlew.bat" (
        echo Gradle로 빌드 중...
        call "%CURRENT_DIR%gradlew.bat" :app:shadowJar
        
        :: 빌드 성공 확인
        if not exist "%JAR_PATH%" (
            echo 빌드 실패! JAR 파일이 생성되지 않았습니다.
            pause
            exit /b 1
        )
        echo 빌드 완료!
    ) else (
        echo Gradle 빌드 파일을 찾을 수 없습니다.
        pause
        exit /b 1
    )
)

:: 콘솔 없이 실행 방식 선택
echo:
echo 실행 방식을 선택하세요:
echo 1. 시스템 JavaW 사용 (환경변수에 Java가 설정되어 있어야 함)
echo 2. 로컬 JavaW 복사 후 사용 (권장)
echo 3. 프로젝트 폴더에서 자바 경로 검색 후 사용
echo:
set /p CHOICE="선택 (기본값: 2): "

if "%CHOICE%"=="" set CHOICE=2

if "%CHOICE%"=="1" (
    call :USE_SYSTEM_JAVAW
) else if "%CHOICE%"=="2" (
    call :USE_LOCAL_JAVAW
) else if "%CHOICE%"=="3" (
    call :SEARCH_JAVAW
) else (
    echo 잘못된 선택입니다. 기본값(2)으로 진행합니다.
    call :USE_LOCAL_JAVAW
)

exit /b 0

:USE_SYSTEM_JAVAW
echo 시스템 JavaW 사용...
if not defined JAVA_HOME (
    echo JAVA_HOME이 설정되지 않았습니다.
    echo 다른 방식으로 시도합니다...
    call :USE_LOCAL_JAVAW
    exit /b
)

set JAVAW_EXE=%JAVA_HOME%\bin\javaw.exe
if not exist "%JAVAW_EXE%" (
    echo JavaW를 찾을 수 없습니다: %JAVAW_EXE%
    echo 다른 방식으로 시도합니다...
    call :USE_LOCAL_JAVAW
    exit /b
)

echo JavaW 경로: %JAVAW_EXE%
echo JAR 경로: %JAR_PATH%
echo 프로그램을 시작합니다...
start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"
echo 프로그램이 시작되었습니다.
timeout /t 2 /nobreak > nul
exit /b

:USE_LOCAL_JAVAW
echo 로컬 JavaW 설정 중...
set LOCAL_JAVA_DIR=%CURRENT_DIR%local_java
set LOCAL_BIN_DIR=%LOCAL_JAVA_DIR%\bin

:: 로컬 자바 디렉토리 생성
if not exist "%LOCAL_JAVA_DIR%" mkdir "%LOCAL_JAVA_DIR%" 2>nul
if not exist "%LOCAL_BIN_DIR%" mkdir "%LOCAL_BIN_DIR%" 2>nul

:: 이미 로컬에 javaw가 있는지 확인
if exist "%LOCAL_BIN_DIR%\javaw.exe" (
    echo 로컬 자바 환경이 이미 설정되어 있습니다.
) else (
    :: JAVA_HOME이 설정되어 있는지 확인
    if defined JAVA_HOME (
        echo JAVA_HOME에서 필수 자바 파일을 복사합니다...
        
        :: 필수 파일 복사
        if exist "%JAVA_HOME%\bin\javaw.exe" (
            copy "%JAVA_HOME%\bin\javaw.exe" "%LOCAL_BIN_DIR%\" /Y >nul
            if exist "%JAVA_HOME%\bin\java.exe" copy "%JAVA_HOME%\bin\java.exe" "%LOCAL_BIN_DIR%\" /Y >nul
            
            :: 필요한 DLL 복사
            if exist "%JAVA_HOME%\bin\server\jvm.dll" (
                mkdir "%LOCAL_BIN_DIR%\server" 2>nul
                copy "%JAVA_HOME%\bin\server\jvm.dll" "%LOCAL_BIN_DIR%\server\" /Y >nul
            )
            
            :: 기본 DLL 복사
            for %%F in (vcruntime140.dll msvcp140.dll) do (
                if exist "%JAVA_HOME%\bin\%%F" copy "%JAVA_HOME%\bin\%%F" "%LOCAL_BIN_DIR%\" /Y >nul
            )
            
            echo 자바 필수 파일 복사 완료!
        ) else (
            echo JAVA_HOME에 javaw.exe가 없습니다: %JAVA_HOME%\bin\javaw.exe
            echo 다른 방식으로 시도합니다...
            call :SEARCH_JAVAW
            exit /b
        )
    ) else (
        echo JAVA_HOME이 설정되어 있지 않습니다.
        echo 다른 방식으로 시도합니다...
        call :SEARCH_JAVAW
        exit /b
    )
)

:: 로컬 JAVAW 사용
set JAVAW_EXE=%LOCAL_BIN_DIR%\javaw.exe
if not exist "%JAVAW_EXE%" (
    echo 로컬 JavaW를 찾을 수 없습니다: %JAVAW_EXE%
    echo 다른 방식으로 시도합니다...
    call :SEARCH_JAVAW
    exit /b
)

echo 로컬 JavaW 경로: %JAVAW_EXE%
echo JAR 경로: %JAR_PATH%
echo 프로그램을 시작합니다...
start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"
echo 프로그램이 시작되었습니다.
timeout /t 2 /nobreak > nul
exit /b

:SEARCH_JAVAW
echo 시스템에서 JavaW 검색 중...

:: 일반적인 자바 설치 위치 확인
set COMMON_JAVA_PATHS=^
"C:\Program Files\Java\*" ^
"C:\Program Files\Microsoft\jdk-*" ^
"C:\Program Files\Eclipse Adoptium\*" ^
"C:\Program Files (x86)\Java\*" ^
"%CURRENT_DIR%jdk\*" ^
"%CURRENT_DIR%java\*"

for %%P in (%COMMON_JAVA_PATHS%) do (
    for /d %%J in (%%P) do (
        if exist "%%J\bin\javaw.exe" (
            set JAVAW_EXE=%%J\bin\javaw.exe
            echo JavaW를 발견했습니다: !JAVAW_EXE!
            goto :FOUND_JAVAW
        )
    )
)

echo 시스템에서 JavaW를 찾을 수 없습니다.
echo SRT를 실행하려면 Java를 설치해야 합니다.
echo https://adoptium.net/에서 Java를 다운로드하세요.
pause
exit /b 1

:FOUND_JAVAW
echo JavaW 경로: %JAVAW_EXE%
echo JAR 경로: %JAR_PATH%
echo 프로그램을 시작합니다...
start "" "%JAVAW_EXE%" -Dfile.encoding=UTF-8 -jar "%JAR_PATH%"
echo 프로그램이 시작되었습니다.
timeout /t 2 /nobreak > nul
exit /b 