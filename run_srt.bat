@echo off
:: 콘솔 창 없이 실행하기 위한 VBS 생성
echo Set WshShell = CreateObject("WScript.Shell") > "%TEMP%\run_srt_invisible.vbs"
echo WshShell.Run """%~dp0bat\run_javafx_custom.bat""", 0, False >> "%TEMP%\run_srt_invisible.vbs"

:: VBS 실행 (콘솔 창 없이 실행)
start "" /b "%TEMP%\run_srt_invisible.vbs"

:: 현재 콘솔 창 즉시 종료
exit 