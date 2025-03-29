@echo off
chcp 65001 > nul
title SRT 간단 실행기

REM 경로 설정
set "JAVA_PATH=C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\java.exe"
set "JAVAFX_PATH=C:\Program Files\Microsoft\javafx-sdk-23.0.2\lib"
set "JAR_PATH=%~dp0app\build\libs\srt-with-dependencies.jar"

REM 필수 옵션만 설정
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Duser.language=ko -Duser.country=KR"
set "JAVAFX_MODULES=--module-path "%JAVAFX_PATH%" --add-modules=javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media,javafx.graphics"

echo === SRT 스트리머 녹화 도구 간단 실행기 ===
echo 실행 파일: %JAR_PATH%
echo.

REM 캐시 및 로그 디렉토리 생성
if not exist cache mkdir cache
if not exist logs mkdir logs

REM 자바 프로그램 실행
echo 프로그램을 시작합니다...
"%JAVA_PATH%" %JAVA_OPTS% %JAVAFX_MODULES% -jar "%JAR_PATH%" -web=true -admin=true -allow=add,edit,delete

echo.
echo 프로그램이 종료되었습니다.
pause 