Option Explicit

' 현재 스크립트의 경로 구하기
Dim fso, scriptPath, srtPath, javaPath, javafxPath, jarPath
Set fso = CreateObject("Scripting.FileSystemObject")
scriptPath = fso.GetParentFolderName(WScript.ScriptFullName)

' 경로 설정
javaPath = "C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\java.exe"
javafxPath = "C:\Program Files\Microsoft\javafx-sdk-23.0.2\lib"
jarPath = scriptPath & "\app\build\libs\srt-with-dependencies.jar"

' 추가 옵션 설정
Dim webPort, cacheDir
webPort = "8080"
cacheDir = scriptPath & "\cache"

' 환경 변수 설정
Dim WshShell, WshEnv
Set WshShell = CreateObject("WScript.Shell")
Set WshEnv = WshShell.Environment("PROCESS")
WshEnv("PYTHONIOENCODING") = "utf-8"
WshEnv("PYTHONUNBUFFERED") = "1"
WshEnv("FFMPEG_LOGLEVEL") = "warning"
WshEnv("STREAMLINK_LOGLEVEL") = "info"
WshEnv("PYTHONUTF8") = "1"
WshEnv("SRT_ADMIN_MODE") = "true"
WshEnv("SRT_WEB_FULLACCESS") = "true"
WshEnv("JAVA_TOOL_OPTIONS") = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

' 실행 명령 구성
Dim command
command = """" & javaPath & """ " & _
         "-Dfile.encoding=UTF-8 " & _
         "-Dsun.stdout.encoding=UTF-8 " & _
         "-Dsun.stderr.encoding=UTF-8 " & _
         "-Djavafx.encoding=UTF-8 " & _
         "-Dconsole.encoding=UTF-8 " & _
         "-Dstdout.encoding=UTF-8 " & _
         "-Dstderr.encoding=UTF-8 " & _
         "-Duser.language=ko " & _
         "-Duser.country=KR " & _
         "-Dapp.web.port=" & webPort & " " & _
         "-Dapp.cache.dir=""" & cacheDir & """ " & _
         "-Dapp.allow.add=true " & _
         "-Dapp.allow.edit=true " & _
         "-Dapp.allow.delete=true " & _
         "-Dapp.web.admin=true " & _
         "-Dapp.web.fullaccess=true " & _
         "-Dapp.web.features=add,edit,delete,record,stop " & _
         "-Dapp.settings.file=""" & scriptPath & "\settings.json"" " & _
         "-Dapp.mode=advanced " & _
         "-Xmx512m " & _
         "--module-path """ & javafxPath & """ " & _
         "--add-modules=javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media,javafx.graphics " & _
         "-jar """ & jarPath & """"

' 캐시 디렉토리 확인/생성
If Not fso.FolderExists(cacheDir) Then
    fso.CreateFolder(cacheDir)
End If

' 로그 디렉토리 확인/생성
If Not fso.FolderExists(scriptPath & "\logs") Then
    fso.CreateFolder(scriptPath & "\logs")
End If

' 설정 파일 수정 (스트리머 추가 기능 활성화)
Dim settingsFile
settingsFile = scriptPath & "\settings.json"

' 기존 설정 파일 백업
If fso.FileExists(settingsFile) Then
    ' 백업 생성
    Dim backupFile
    backupFile = scriptPath & "\settings_backup_" & _
                 Year(Now) & Right("0" & Month(Now), 2) & Right("0" & Day(Now), 2) & "_" & _
                 Right("0" & Hour(Now), 2) & Right("0" & Minute(Now), 2) & Right("0" & Second(Now), 2) & ".json"
    
    ' 기존 파일이 없는 경우에만 백업
    If Not fso.FileExists(backupFile) Then
        fso.CopyFile settingsFile, backupFile
    End If
    
    ' 기존 파일 읽기
    Dim settingsContent
    Dim settingsStream
    Set settingsStream = fso.OpenTextFile(settingsFile, 1, False)
    settingsContent = settingsStream.ReadAll()
    settingsStream.Close
    
    ' 설정 추가
    If InStr(settingsContent, """allowAdd""") = 0 Then
        settingsContent = Replace(settingsContent, "}", _
        "  ""allowAdd"" : true," & vbCrLf & _
        "  ""allowEdit"" : true," & vbCrLf & _
        "  ""allowDelete"" : true" & vbCrLf & _
        "}")
    End If
    
    ' 업데이트된 설정 저장
    Set settingsStream = fso.CreateTextFile(settingsFile, True)
    settingsStream.Write(settingsContent)
    settingsStream.Close
Else
    ' 설정 파일이 없는 경우 기본 설정 파일 생성
    Dim jsonStream
    Set jsonStream = fso.CreateTextFile(settingsFile, True)
    jsonStream.WriteLine "{" 
    jsonStream.WriteLine "  ""version"": ""1.0""," 
    jsonStream.WriteLine "  ""settingsVersion"": 1," 
    jsonStream.WriteLine "  ""cachePath"": """ & Replace(cacheDir, "\", "\\") & """," 
    jsonStream.WriteLine "  ""webServerPort"": " & webPort & "," 
    jsonStream.WriteLine "  ""webServerEnabled"": true," 
    jsonStream.WriteLine "  ""allowAdd"": true," 
    jsonStream.WriteLine "  ""allowEdit"": true," 
    jsonStream.WriteLine "  ""allowDelete"": true," 
    jsonStream.WriteLine "  ""streamers"": []" 
    jsonStream.WriteLine "}" 
    jsonStream.Close
    Set jsonStream = Nothing
End If

' 프로그램 실행 (UI 창 표시)
WshShell.Run command, 1, False

' 브라우저 열기 (약간 지연시켜서 서버가 시작된 후 열기)
WScript.Sleep 3000 ' 서버가 시작될 때까지 3초 대기
WshShell.Run "http://localhost:" & webPort, 1, False

' 정리
Set WshShell = Nothing
Set WshEnv = Nothing
Set fso = Nothing 