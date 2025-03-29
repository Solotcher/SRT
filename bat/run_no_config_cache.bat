@echo off
chcp 65001 > nul
echo SRT - 스트리머 녹화 도구를 시작합니다...

cd %~dp0
call gradlew run --no-configuration-cache

echo 프로그램이 종료되었습니다.
pause 