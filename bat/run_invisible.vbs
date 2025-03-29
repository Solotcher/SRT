Set WshShell = CreateObject("WScript.Shell")
currentPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

' Gradle을 통해 실행하는 명령으로 변경
command = "cmd /c cd " & currentPath & " && gradlew run -Dfile.encoding=UTF-8 -q"
WshShell.Run command, 0, False 