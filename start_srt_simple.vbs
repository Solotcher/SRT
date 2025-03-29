Option Explicit

' 현재 스크립트의 경로 구하기
Dim fso, scriptPath, batchPath
Set fso = CreateObject("Scripting.FileSystemObject")
scriptPath = fso.GetParentFolderName(WScript.ScriptFullName)
batchPath = scriptPath & "\start_srt.bat"

' WScript.Shell 객체 생성
Dim WshShell, WshEnv
Set WshShell = CreateObject("WScript.Shell")
Set WshEnv = WshShell.Environment("PROCESS")

' 환경 변수 설정
WshEnv("SRT_ADMIN_MODE") = "true"
WshEnv("SRT_WEB_FULLACCESS") = "true"
WshEnv("JAVA_TOOL_OPTIONS") = "-Dapp.allow.add=true -Dapp.allow.edit=true -Dapp.allow.delete=true -Dapp.web.admin=true -Dapp.web.fullaccess=true"

' 배치 파일 실행
WshShell.Run """" & batchPath & """", 1, False

' 정리
Set WshEnv = Nothing
Set WshShell = Nothing
Set fso = Nothing 