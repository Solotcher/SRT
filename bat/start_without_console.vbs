Set WshShell = CreateObject("WScript.Shell")
currentPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

' 모듈 경로 설정 (JavaFX 모듈이 필요한 경우)
javaOptions = "-Dfile.encoding=UTF-8"

' JAR 파일 경로 설정
jarPath = currentPath & "\app\build\libs\srt-with-dependencies.jar"

' 파일 존재 확인
If Not CreateObject("Scripting.FileSystemObject").FileExists(jarPath) Then
    MsgBox "JAR 파일을 찾을 수 없습니다: " & jarPath & vbCrLf & "먼저 프로그램을 빌드해주세요.", 16, "오류"
    WScript.Quit
End If

' 전체 경로를 명시적으로 지정하여 실행 - 시스템 환경변수의 javaw 사용
command = "javaw " & javaOptions & " -jar """ & jarPath & """"

' 콘솔 창 없이 실행 (0=hide, false=don't wait)
WshShell.Run command, 0, False 