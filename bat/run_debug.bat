@echo off
chcp 65001 > nul
echo SRT - 스트리머 녹화 도구 디버그 모드를 시작합니다...
echo ======================================================
cd %~dp0
echo 실행 경로: %CD%
echo 이전 설정: %JAVA_TOOL_OPTIONS%
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dprism.lcdtext=false -Dprism.text=t2k -Djavafx.font=System
echo 새 설정: %JAVA_TOOL_OPTIONS%
echo ======================================================
echo 프로그램 실행 중...
call gradlew --console=plain run --no-configuration-cache --info
echo ======================================================
echo 프로그램이 종료되었습니다.
pause 