@echo off
echo 스크립트 시작 확인

REM 콘솔 글꼴 설정 (TrueType 글꼴 사용)
reg add "HKEY_CURRENT_USER\Console" /v "TrueTypeFont" /t REG_SZ /d "Consolas" /f > nul 2>&1
reg add "HKEY_CURRENT_USER\Console" /v "FontFamily" /t REG_DWORD /d 0x00000036 /f > nul 2>&1

REM 한글 표시를 위한 코드 페이지 설정
chcp 65001 > nul
title SRT 스트리머 녹화 도구 (UTF-8 지원)
setlocal EnableDelayedExpansion

REM Define base directory relative to this script location (bat folder)
set "SCRIPT_DIR=%~dp0"
set "BASE_DIR=%SCRIPT_DIR%..\"

REM Change working directory to BASE_DIR (J/SRT/)
echo 작업 디렉토리를 %BASE_DIR% (으)로 변경합니다.
pushd "%BASE_DIR%"

cls
echo =====================================
echo  SRT 스트리머 녹화 도구를 시작합니다 (JavaFX Custom)
echo =====================================
echo.

REM 환경 변수 설정 - UTF-8로 변경
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Djavafx.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Duser.language=ko -Duser.country=KR -Xmx512m"
set "PYTHONIOENCODING=utf-8"
set "PYTHONUNBUFFERED=1"
set "FFMPEG_LOGLEVEL=warning"
set "STREAMLINK_LOGLEVEL=info"
set "PYTHONUTF8=1"

REM 웹 인터페이스 설정
set "SRT_ADMIN_MODE=true"
set "SRT_WEB_FULLACCESS=true"

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

REM 디버그 로그 시작
echo ===== 디버깅 로그 시작 ===== > debug_log.txt
echo 현재 작업 디렉토리: %CD% >> debug_log.txt

REM 명시적으로 자바 경로 설정
set "JAVA_EXE=C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\java.exe"
echo Java 실행 파일: %JAVA_EXE% >> debug_log.txt

REM JavaFX 모듈 경로 설정 - SDK 설치 경로 사용
set "JAVAFX_SDK_PATH=C:\Program Files\Microsoft\javafx-sdk-23.0.2"
set "JAVAFX_MODULE_PATH=%JAVAFX_SDK_PATH%\lib"

if exist "%JAVAFX_MODULE_PATH%" (
    echo JavaFX SDK 경로를 사용합니다: %JAVAFX_MODULE_PATH%
    echo JavaFX SDK 경로 확인됨: %JAVAFX_MODULE_PATH% >> debug_log.txt
) else (
    echo 오류: 지정된 JavaFX SDK 경로를 찾을 수 없습니다.
    echo 경로: %JAVAFX_MODULE_PATH%
    echo JavaFX SDK를 다운로드하고 압축을 해제한 후, 이 스크립트의 JAVAFX_SDK_PATH를 수정해주세요.
    echo JavaFX SDK 경로 없음: %JAVAFX_MODULE_PATH% >> debug_log.txt
    pause
    goto :end
)

REM JAR 파일 확인
set "JAR_FILE=app\build\libs\srt-with-dependencies.jar"
echo JAR 파일 경로: %JAR_FILE% >> debug_log.txt
echo 전체 JAR 경로: %CD%\%JAR_FILE% >> debug_log.txt

if not exist "%JAR_FILE%" (
    echo JAR 파일을 찾을 수 없습니다: %JAR_FILE% >> debug_log.txt
    echo JAR 파일을 찾을 수 없습니다. 다시 빌드합니다...
    echo.
    call gradlew.bat fatJar --quiet
    if not exist "%JAR_FILE%" (
        echo JAR 빌드에 실패했습니다. >> debug_log.txt
        echo JAR 빌드에 실패했습니다.
        goto use_gradle
    )
)

echo JAR 파일 확인됨 >> debug_log.txt

REM 기존 로그 파일 백업
if exist logs\app.log (
    echo 이전 로그 파일 백업 중...
    ren logs\app.log app_%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log 2>nul
)

echo.
echo === 애플리케이션 시작 ===
echo JAR 파일: %JAR_FILE%
echo 웹 브라우저에서 http://localhost:8080 으로 접속하세요.
echo.

REM 설정 파일 수정 (필요한 경우)
if exist settings.json (
    echo 설정 파일 확인 중...
    findstr /C:"allowAdd" settings.json > nul
    if errorlevel 1 (
        echo 설정 파일 업데이트 필요: 스트리머 추가 옵션 활성화
        set "TEMP_FILE=settings_temp.json"
        copy settings.json %TEMP_FILE% > nul
        echo   "allowAdd": true, >> %TEMP_FILE%
        echo   "allowEdit": true, >> %TEMP_FILE%
        echo   "allowDelete": true >> %TEMP_FILE%
        echo } >> %TEMP_FILE%
        move /Y %TEMP_FILE% settings.json > nul
    )
)

REM JavaFX 모듈 옵션 설정 (큰따옴표 주의)
set JAVAFX_MODULES=--module-path "%JAVAFX_MODULE_PATH%" --add-modules=javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media,javafx.graphics

REM 추가 옵션 설정
set "EXTRA_OPTS=-Dapp.web.admin=true -Dapp.web.fullaccess=true -Dapp.allow.add=true -Dapp.allow.edit=true -Dapp.allow.delete=true -Dapp.web.features=add,edit,delete,record,stop -Dapp.mode=advanced"

REM Java 실행 명령어 구성 (큰따옴표 주의)
set JAVA_CMD="%JAVA_EXE%" %JAVA_OPTS% %EXTRA_OPTS% %JAVAFX_MODULES% -jar "%CD%\%JAR_FILE%"

echo 실행 명령어: %JAVA_CMD% >> debug_log.txt
echo 실행 명령어: %JAVA_CMD% > logs\app.log

echo.
echo === 애플리케이션 실행 중... ===
echo 실행 명령어: %JAVA_CMD%
echo.

REM 디버그를 위해 Java 실행 명령을 로그로 저장
echo cd %CD% > run_java_debug.bat
echo %JAVA_CMD% >> run_java_debug.bat

echo ----------------------------------------------
echo 프로그램 UI 창과 웹 인터페이스가 모두 활성화됩니다.
echo 웹 브라우저: http://localhost:8080
echo ----------------------------------------------
echo.

REM 애플리케이션 실행 (UI 창 표시)
echo %date% %time% - 프로그램 시작 > logs\console_output.log
%JAVA_CMD% 2>&1 | findstr /V /C:"SLF4J" >> logs\console_output.log

echo 프로그램이 종료되었습니다.
echo 콘솔 출력은 logs\console_output.log 파일에서 확인할 수 있습니다.
echo.

goto :end

:use_gradle
echo.
echo Gradle로 실행합니다...
call gradlew.bat run --console=plain
set EXIT_CODE=%ERRORLEVEL%
echo Gradle 종료 코드: %EXIT_CODE% >> debug_log.txt

:end_script
REM Restore original directory
popd
:end
pause 