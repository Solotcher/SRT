@echo off
chcp 65001 > nul
echo SRT 바로가기를 생성합니다...

cd %~dp0

echo 바로가기 타입을 선택하세요:
echo 1. 일반 실행 (권장, fatJar 필요)
echo 2. Gradle 숨김 실행 (설정 캐시 오류 방지)
set /p choice="선택 (1 또는 2): "

if "%choice%"=="1" (
    set SCRIPT_PATH=%CD%\start_without_console.vbs
) else if "%choice%"=="2" (
    set SCRIPT_PATH=%CD%\run_hidden.vbs
) else (
    echo 잘못된 선택입니다. 기본값인 1번으로 설정합니다.
    set SCRIPT_PATH=%CD%\start_without_console.vbs
)

set DESKTOP_PATH=%USERPROFILE%\Desktop

echo Set oWS = WScript.CreateObject("WScript.Shell") > CreateShortcut.vbs
echo sLinkFile = "%DESKTOP_PATH%\SRT 스트리머 녹화 도구.lnk" >> CreateShortcut.vbs
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> CreateShortcut.vbs
echo oLink.TargetPath = "%SCRIPT_PATH%" >> CreateShortcut.vbs
echo oLink.WorkingDirectory = "%CD%" >> CreateShortcut.vbs
echo oLink.Description = "SRT 스트리머 녹화 도구" >> CreateShortcut.vbs
echo oLink.IconLocation = "%CD%\app\src\main\resources\icon.ico, 0" >> CreateShortcut.vbs
echo oLink.Save >> CreateShortcut.vbs

cscript //nologo CreateShortcut.vbs
del CreateShortcut.vbs

echo 바로가기가 바탕화면에 생성되었습니다.
pause 