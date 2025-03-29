@echo off
chcp 65001 > nul
echo 모든 종속성이 포함된 JAR 파일을 생성합니다...

cd %~dp0
call gradlew fatJar --no-configuration-cache

if %ERRORLEVEL% equ 0 (
    echo JAR 파일이 성공적으로 생성되었습니다.
    echo 위치: %CD%\app\build\libs\srt-with-dependencies.jar
) else (
    echo JAR 파일 생성에 실패했습니다.
)

pause 