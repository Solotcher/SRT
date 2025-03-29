Set WshShell = CreateObject("WScript.Shell")
currentPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

' Gradle 설정 캐시 없이 실행
command = "cmd /c cd " & currentPath & " && gradlew run --no-configuration-cache"

' 콘솔 창을 보이지 않게 실행
WshShell.Run command, 0, False 