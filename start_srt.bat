@echo off
chcp 65001 > nul
title SRT 간편 실행기

echo ===================================
echo    SRT 스트리머 녹화 도구 실행
echo ===================================
echo.

REM 경로 설정
set "JAVA_PATH=C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\java.exe"
set "JAVAFX_PATH=C:\Program Files\Microsoft\javafx-sdk-23.0.2\lib"
set "JAR_PATH=%~dp0app\build\libs\srt-with-dependencies.jar"

REM 환경 변수 설정
set "SRT_ADMIN_MODE=true"
set "SRT_WEB_FULLACCESS=true"
set "JAVA_OPTS=-Dapp.allow.add=true -Dapp.allow.edit=true -Dapp.allow.delete=true -Dapp.web.admin=true -Dapp.web.fullaccess=true"

REM JAR 파일 확인
if not exist "%JAR_PATH%" (
    echo 오류: JAR 파일을 찾을 수 없습니다.
    echo 경로: %JAR_PATH%
    echo.
    echo gradlew를 실행하여 JAR 파일을 빌드합니다...
    call gradlew.bat fatJar
    if not exist "%JAR_PATH%" (
        echo JAR 빌드에 실패했습니다.
        pause
        exit /b 1
    )
)

REM 디렉토리 생성
if not exist cache mkdir cache
if not exist logs mkdir logs

REM settings.json 파일 확인 및 수정
if exist settings.json (
    echo 설정 파일 확인 중...
    type settings.json | findstr "\"nidAut\" : \"\"," > nul
    if errorlevel 1 (
        echo 설정 파일 형식 수정 중...
        powershell -Command "(Get-Content settings.json) -replace '\"nidAut\" : \"\"([^,])', '\"nidAut\" : \"\",$1' | Set-Content settings.json"
    )
)

REM 브라우저 시작 프로세스
start cmd /c "timeout /t 3 && start http://localhost:8080 && exit"

REM 자바 프로그램 실행
echo 프로그램을 시작합니다...
echo 시작 시간: %TIME%
echo.
echo [자바 프로그램 출력]
echo ---------------------

"%JAVA_PATH%" -Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR ^
    -Dapp.allow.add=true ^
    -Dapp.allow.edit=true ^
    -Dapp.allow.delete=true ^
    -Dapp.web.admin=true ^
    -Dapp.web.fullaccess=true ^
    --module-path "%JAVAFX_PATH%" ^
    --add-modules=javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media,javafx.graphics ^
    -jar "%JAR_PATH%" ^
    -web=true -admin=true -allow=add,edit,delete -webadmin=true

echo.
echo 프로그램이 종료되었습니다.
echo 종료 시간: %TIME%
pause
