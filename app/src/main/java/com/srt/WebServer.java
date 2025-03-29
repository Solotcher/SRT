package com.srt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 웹 서버 클래스
 * 스트리머 목록과 녹화 상태를 웹에서 확인하고 제어할 수 있는 인터페이스를 제공합니다.
 */
public class WebServer {
    private final Recorder recorder;
    private HttpServer server;
    private int port;
    private boolean running;
    private String lastError;
    
    public WebServer(Recorder recorder, int port) {
        this.recorder = recorder;
        this.port = port;
        this.running = false;
        this.lastError = "";
    }
    
    /**
     * 웹 서버 초기화
     */
    public boolean initialize() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RootHandler());
            server.createContext("/api/streamers", new StreamersApiHandler());
            server.createContext("/api/start", new StartRecordingHandler());
            server.createContext("/api/stop", new StopRecordingHandler());
            server.createContext("/api/startAll", new StartAllHandler());
            server.createContext("/api/stopAll", new StopAllHandler());
            server.createContext("/api/openSettings", new OpenSettingsHandler());
            server.createContext("/api/delete", new DeleteStreamerHandler());
            server.createContext("/api/settings", new SettingsHandler());
            server.createContext("/api/add", new AddStreamerHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            return true;
        } catch (IOException e) {
            lastError = "웹 서버 초기화 오류: " + e.getMessage();
            return false;
        }
    }
    
    /**
     * 웹 서버 시작
     */
    public boolean start() {
        if (server != null && !running) {
            server.start();
            running = true;
            return true;
        }
        return false;
    }
    
    /**
     * 웹 서버 중지
     */
    public boolean stop() {
        if (server != null && running) {
            server.stop(0);
            running = false;
            return true;
        }
        return false;
    }
    
    /**
     * 메인 페이지 핸들러
     */
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // 상태 화면 생성
                StringBuilder response = new StringBuilder();
                String html = "<!DOCTYPE html>\n" +
                        "<html lang=\"ko\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>스트리머 관리</title>\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                        "            background-color: #f4f6f9;\n" +
                        "            margin: 0;\n" +
                        "            padding: 20px;\n" +
                        "            color: #333;\n" +
                        "        }\n" +
                        "        .card {\n" +
                        "            background-color: white;\n" +
                        "            border-radius: 8px;\n" +
                        "            box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);\n" +
                        "            padding: 20px;\n" +
                        "            margin-bottom: 20px;\n" +
                        "        }\n" +
                        "        h1, h2 {\n" +
                        "            color: #2c3e50;\n" +
                        "            margin-top: 0;\n" +
                        "        }\n" +
                        "        table {\n" +
                        "            width: 100%;\n" +
                        "            border-collapse: collapse;\n" +
                        "        }\n" +
                        "        th, td {\n" +
                        "            border: 1px solid #ddd;\n" +
                        "            padding: 10px;\n" +
                        "            text-align: left;\n" +
                        "        }\n" +
                        "        th {\n" +
                        "            background-color: #f5f5f5;\n" +
                        "            font-weight: bold;\n" +
                        "        }\n" +
                        "        tr:nth-child(even) {\n" +
                        "            background-color: #f9f9f9;\n" +
                        "        }\n" +
                        "        .status-online { color: #2ecc71; }\n" +
                        "        .status-offline { color: #7f8c8d; }\n" +
                        "        .status-recording { color: #e67e22; }\n" +
                        "        .status-error { color: #e74c3c; }\n" +
                        "        .status-checking { color: #3498db; }\n" +
                        "        .btn {\n" +
                        "            display: inline-block;\n" +
                        "            padding: 6px 12px;\n" +
                        "            margin-bottom: 0;\n" +
                        "            font-size: 14px;\n" +
                        "            font-weight: 400;\n" +
                        "            line-height: 1.42857143;\n" +
                        "            text-align: center;\n" +
                        "            white-space: nowrap;\n" +
                        "            vertical-align: middle;\n" +
                        "            cursor: pointer;\n" +
                        "            border: 1px solid transparent;\n" +
                        "            border-radius: 4px;\n" +
                        "            text-decoration: none;\n" +
                        "        }\n" +
                        "        .btn-start { background-color: #2ecc71; color: white; }\n" +
                        "        .btn-stop { background-color: #e67e22; color: white; }\n" +
                        "        .btn-delete { background-color: #e74c3c; color: white; }\n" +
                        "        .btn-primary { background-color: #3498db; color: white; }\n" +
                        "        .btn-add { \n" +
                        "            background-color: #27ae60; \n" +
                        "            color: white; \n" +
                        "            font-weight: bold; \n" +
                        "            font-size: 16px; \n" +
                        "            padding: 10px 20px; \n" +
                        "            box-shadow: 0 4px 6px rgba(0,0,0,0.1); \n" +
                        "            border: none;\n" +
                        "            letter-spacing: 1px;\n" +
                        "        }\n" +
                        "        .btn-add:hover { \n" +
                        "            background-color: #2ecc71; \n" +
                        "            box-shadow: 0 6px 8px rgba(0,0,0,0.15); \n" +
                        "        }\n" +
                        "        .settings-group { margin-bottom: 15px; }\n" +
                        "        label { display: block; margin-bottom: 5px; font-weight: bold; }\n" +
                        "        select, input { padding: 8px; width: 100%; max-width: 300px; }\n" +
                        "        .modal {\n" +
                        "            display: none;\n" +
                        "            position: fixed;\n" +
                        "            z-index: 1000;\n" +
                        "            left: 0;\n" +
                        "            top: 0;\n" +
                        "            width: 100%;\n" +
                        "            height: 100%;\n" +
                        "            background-color: rgba(0,0,0,0.5);\n" +
                        "        }\n" +
                        "        .modal-content {\n" +
                        "            background-color: white;\n" +
                        "            margin: 10% auto;\n" +
                        "            padding: 20px;\n" +
                        "            width: 80%;\n" +
                        "            max-width: 600px;\n" +
                        "            border-radius: 8px;\n" +
                        "        }\n" +
                        "        .close {\n" +
                        "            float: right;\n" +
                        "            font-size: 28px;\n" +
                        "            font-weight: bold;\n" +
                        "            cursor: pointer;\n" +
                        "        }\n" +
                        "    </style>\n" +
                        "    <script>\n" +
                        "        function startRecording(name) {\n" +
                        "            fetch('/api/start?name=' + encodeURIComponent(name))\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (data.success) {\n" +
                        "                        alert('녹화가 시작되었습니다.');\n" +
                        "                        location.reload();\n" +
                        "                    } else {\n" +
                        "                        alert('녹화 시작 실패: ' + data.error);\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }\n" +
                        "        \n" +
                        "        function stopRecording(name) {\n" +
                        "            fetch('/api/stop?name=' + encodeURIComponent(name))\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (data.success) {\n" +
                        "                        alert('녹화가 중지되었습니다.');\n" +
                        "                        location.reload();\n" +
                        "                    } else {\n" +
                        "                        alert('녹화 중지 실패: ' + data.error);\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }\n" +
                        "        \n" +
                        "        function startAllRecordings() {\n" +
                        "            fetch('/api/startAll')\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (data.success) {\n" +
                        "                        alert('모든 녹화가 시작되었습니다.');\n" +
                        "                        location.reload();\n" +
                        "                    } else {\n" +
                        "                        alert('녹화 시작 실패: ' + data.error);\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }\n" +
                        "        \n" +
                        "        function stopAllRecordings() {\n" +
                        "            fetch('/api/stopAll')\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (data.success) {\n" +
                        "                        alert('모든 녹화가 중지되었습니다.');\n" +
                        "                        location.reload();\n" +
                        "                    } else {\n" +
                        "                        alert('녹화 중지 실패: ' + data.error);\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }\n" +
                        "        \n" +
                        "        function openSettings() {\n" +
                        "            fetch('/api/openSettings')\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (data.success) {\n" +
                        "                        alert('환경설정 창이 열렸습니다. 데스크톱에서 확인하세요.');\n" +
                        "                    } else {\n" +
                        "                        alert('환경설정 창 열기 실패');\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }\n" +
                        "        \n" +
                        "        function deleteStreamer(name) {\n" +
                        "            if (confirm('정말로 ' + name + ' 스트리머를 삭제하시겠습니까?')) {\n" +
                        "                fetch('/api/delete?name=' + encodeURIComponent(name))\n" +
                        "                    .then(response => response.json())\n" +
                        "                    .then(data => {\n" +
                        "                        if (data.success) {\n" +
                        "                            alert('스트리머가 삭제되었습니다.');\n" +
                        "                            location.reload();\n" +
                        "                        } else {\n" +
                        "                            alert('스트리머 삭제 실패: ' + data.error);\n" +
                        "                        }\n" +
                        "                    });\n" +
                        "            }\n" +
                        "        }\n" +
                        "        \n" +
                        "        function refreshPage() {\n" +
                        "            location.reload();\n" +
                        "        }\n" +
                        "        \n" +
                        "        function showWebSettings() {\n" +
                        "            document.getElementById('settingsModal').style.display = 'block';\n" +
                        "        }\n" +
                        "        \n" +
                        "        function hideWebSettings() {\n" +
                        "            document.getElementById('settingsModal').style.display = 'none';\n" +
                        "        }\n" +
                        "        \n" +
                        "        function showAddStreamerForm() {\n" +
                        "            document.getElementById('addStreamerModal').style.display = 'block';\n" +
                        "        }\n" +
                        "        \n" +
                        "        function hideAddStreamerForm() {\n" +
                        "            document.getElementById('addStreamerModal').style.display = 'none';\n" +
                        "        }\n" +
                        "        \n" +
                        "        function submitAddStreamer() {\n" +
                        "            var name = document.getElementById('streamerName').value;\n" +
                        "            var url = document.getElementById('streamerUrl').value;\n" +
                        "            var quality = document.getElementById('streamerQuality').value;\n" +
                        "            \n" +
                        "            if (!name || !url) {\n" +
                        "                alert('스트리머 이름과 URL을 입력해주세요.');\n" +
                        "                return;\n" +
                        "            }\n" +
                        "            \n" +
                        "            fetch('/api/add', {\n" +
                        "                method: 'POST',\n" +
                        "                headers: {\n" +
                        "                    'Content-Type': 'application/x-www-form-urlencoded',\n" +
                        "                },\n" +
                        "                body: 'name=' + encodeURIComponent(name) + '&url=' + encodeURIComponent(url) + '&quality=' + encodeURIComponent(quality)\n" +
                        "            })\n" +
                        "            .then(response => response.json())\n" +
                        "            .then(data => {\n" +
                        "                if (data.success) {\n" +
                        "                    alert('스트리머가 추가되었습니다.');\n" +
                        "                    hideAddStreamerForm();\n" +
                        "                    location.reload();\n" +
                        "                } else {\n" +
                        "                    alert('스트리머 추가 실패: ' + data.error);\n" +
                        "                }\n" +
                        "            });\n" +
                        "        }\n" +
                        "        \n" +
                        "        // 자동 새로고침\n" +
                        "        setInterval(function() {\n" +
                        "            fetch('/api/streamers')\n" +
                        "                .then(response => response.json())\n" +
                        "                .then(data => {\n" +
                        "                    if (document.getElementById('settingsModal').style.display !== 'block') {\n" +
                        "                        location.reload();\n" +
                        "                    }\n" +
                        "                });\n" +
                        "        }, 30000); // 30초마다 새로고침\n" +
                        "    </script>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"card\">\n" +
                        "        <h1>스트리머 관리 패널</h1>\n" +
                        "    </div>\n" +
                        "    \n" +
                        "    <div class=\"card\">\n" +
                        "        <!-- 전체 작업 버튼 스타일 개선 -->\n" +
                        "        <div style=\"display: flex; justify-content: flex-start; gap: 10px; margin-bottom: 15px; flex-wrap: wrap;\">\n" +
                        "            <button onclick=\"showAddStreamerForm()\" class=\"btn btn-primary\">스트리머 추가</button>\n" +
                        "            <button onclick=\"startAllRecordings()\" class=\"btn btn-start\">전체 녹화 시작</button>\n" +
                        "            <button onclick=\"stopAllRecordings()\" class=\"btn btn-stop\">전체 녹화 중지</button>\n" +
                        "            <button onclick=\"openSettings()\" class=\"btn btn-primary\">환경설정</button>\n" +
                        "            <button onclick=\"refreshPage()\" class=\"btn\">새로고침</button>\n" +
                        "        </div>\n" +
                        "        \n" +
                        "        <!-- 스트리머 수, 녹화 중인 스트리머 수, 온라인 스트리머 수 표시 -->\n" +
                        "        <div style=\"display: flex; justify-content: space-around; text-align: center;\">\n";
                
                // 카운트 계산
                int totalStreamers = recorder.getStreamers().size();
                int recordingStreamers = 0;
                int onlineStreamers = 0;
                
                for (Recorder.StreamerInfo info : recorder.getStreamers()) {
                    if (info.isRecording()) {
                        recordingStreamers++;
                    }
                    if ("online".equals(info.getStatus())) {
                        onlineStreamers++;
                    }
                }
                
                html += "            <div><strong>전체 스트리머:</strong> <span>" + totalStreamers + "명</span></div>\n" +
                        "            <div><strong>온라인:</strong> <span class=\"status-online\">" + onlineStreamers + "명</span></div>\n" +
                        "            <div><strong>녹화중:</strong> <span class=\"status-recording\">" + recordingStreamers + "명</span></div>\n" +
                        "            <div><strong>현재 녹화 프로그램:</strong> <span>" + recorder.getRecorderProgram() + "</span></div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "    \n" +
                        "    <div class=\"card\">\n" +
                        "        <table>\n" +
                        "            <thead>\n" +
                        "                <tr>\n" +
                        "                    <th>스트리머명</th>\n" +
                        "                    <th>방송제목</th>\n" +
                        "                    <th>상태</th>\n" +
                        "                    <th>현재 녹화시간</th>\n" +
                        "                    <th>작업</th>\n" +
                        "                </tr>\n" +
                        "            </thead>\n" +
                        "            <tbody>\n";
                
                // 스트리머 정보 행 생성
                List<Recorder.StreamerInfo> streamers = recorder.getStreamers();
                
                if (streamers.isEmpty()) {
                    html += "                <tr>\n" +
                            "                    <td colspan=\"5\" style=\"text-align: center;\">등록된 스트리머가 없습니다.</td>\n" +
                            "                </tr>\n";
                } else {
                    for (Recorder.StreamerInfo info : streamers) {
                        html += "                <tr>\n" +
                                "                    <td><strong>" + escapeHtml(info.getName()) + "</strong></td>\n";
                        
                        // 방송제목
                        String title = info.getTitle();
                        title = (title != null && !title.isEmpty()) ? title : "<i>제목 없음</i>";
                        html += "                    <td>" + escapeHtml(title) + "</td>\n";
                        
                        // 녹화상태 (색상 코드 사용)
                        String statusClass = "status-" + info.getStatus();
                        String statusText = Recorder.getStatusDisplayText(info.getStatus());
                        html += "                    <td><span class=\"" + statusClass + "\">" + statusText + "</span></td>\n";
                        
                        // 녹화시간 - 표시 향상
                        String durationDisplay = info.isRecording() ? 
                                "<span class=\"status-recording\">" + escapeHtml(info.getDuration()) + "</span>" : 
                                escapeHtml(info.getDuration());
                        html += "                    <td>" + durationDisplay + "</td>\n";
                        
                        // 작업 버튼
                        html += "                <td>\n";
                        
                        // 시작 버튼
                        if (info.isRecording()) {
                            html += "<button disabled class=\"btn btn-start\">시작</button>\n";
                        } else if ("online".equals(info.getStatus())) {
                            html += "<button onclick=\"startRecording('" + escapeHtml(info.getName()) + "')\" class=\"btn btn-start\">시작</button>\n";
                        } else {
                            html += "<button disabled class=\"btn btn-start\">시작</button>\n";
                        }
                        
                        // 중지 버튼
                        if (info.isRecording()) {
                            html += "<button onclick=\"stopRecording('" + escapeHtml(info.getName()) + "')\" class=\"btn btn-stop\">중지</button>\n";
                        } else {
                            html += "<button disabled class=\"btn btn-stop\">중지</button>\n";
                        }
                        
                        // 삭제 버튼
                        html += "<button onclick=\"deleteStreamer('" + escapeHtml(info.getName()) + "')\" class=\"btn btn-delete\">삭제</button>";
                        
                        html += "                </td>\n" +
                                "                </tr>\n";
                    }
                }
                
                html += "            </tbody>\n" +
                        "        </table>\n" +
                        "    </div>\n" +
                        "    \n" +
                        "    <!-- 웹 설정 모달 -->\n" +
                        "    <div id=\"settingsModal\" class=\"modal\">\n" +
                        "        <div class=\"modal-content\">\n" +
                        "            <span class=\"close\" onclick=\"hideWebSettings()\">&times;</span>\n" +
                        "            <h2>웹 설정</h2>\n" +
                        "            <form action=\"/settings\" method=\"post\">\n" +
                        "                <div class=\"settings-group\">\n" +
                        "                    <label for=\"recorderProgram\">녹화 프로그램:</label>\n" +
                        "                    <select name=\"recorderProgram\" id=\"recorderProgram\">\n" +
                        "                        <option value=\"streamlink\" " + 
                        (recorder.getRecorderProgram().equals("streamlink") ? "selected" : "") + 
                        ">Streamlink</option>\n" +
                        "                        <option value=\"yt-dlp\" " + 
                        (recorder.getRecorderProgram().equals("yt-dlp") ? "selected" : "") + 
                        ">yt-dlp</option>\n" +
                        "                        <option value=\"ffmpeg\" " + 
                        (recorder.getRecorderProgram().equals("ffmpeg") ? "selected" : "") + 
                        ">FFmpeg</option>\n" +
                        "                    </select>\n" +
                        "                </div>\n" +
                        "                <div class=\"settings-group\">\n" +
                        "                    <label for=\"maxThreads\">최대 스레드:</label>\n" +
                        "                    <input type=\"number\" name=\"maxThreads\" id=\"maxThreads\" value=\"" + 
                        recorder.getMaxThreads() + "\" min=\"1\" max=\"16\">\n" +
                        "                </div>\n" +
                        "                <input type=\"submit\" value=\"설정 저장\" class=\"btn btn-primary\">\n" +
                        "            </form>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "    \n" +
                        "    <!-- 스트리머 추가 모달 -->\n" +
                        "    <div id=\"addStreamerModal\" class=\"modal\" style=\"display: none;\">\n" +
                        "        <div class=\"modal-content\">\n" +
                        "            <span class=\"close\" onclick=\"hideAddStreamerForm()\">&times;</span>\n" +
                        "            <h2>스트리머 추가</h2>\n" +
                        "            <div style=\"margin-top: 20px;\">\n" +
                        "                <div style=\"margin-bottom: 10px;\">\n" +
                        "                    <label for=\"streamerName\">스트리머 이름:</label>\n" +
                        "                    <input type=\"text\" id=\"streamerName\" style=\"width: 100%; padding: 5px; margin-top: 5px;\">\n" +
                        "                </div>\n" +
                        "                <div style=\"margin-bottom: 10px;\">\n" +
                        "                    <label for=\"streamerUrl\">스트리머 URL:</label>\n" +
                        "                    <input type=\"text\" id=\"streamerUrl\" style=\"width: 100%; padding: 5px; margin-top: 5px;\">\n" +
                        "                </div>\n" +
                        "                <div style=\"margin-bottom: 20px;\">\n" +
                        "                    <label for=\"streamerQuality\">화질:</label>\n" +
                        "                    <select id=\"streamerQuality\" style=\"width: 100%; padding: 5px; margin-top: 5px;\">\n" +
                        "                        <option value=\"best\">최고 화질 (best)</option>\n" +
                        "                        <option value=\"1080p\">1080p</option>\n" +
                        "                        <option value=\"720p\">720p</option>\n" +
                        "                        <option value=\"480p\">480p</option>\n" +
                        "                        <option value=\"360p\">360p</option>\n" +
                        "                    </select>\n" +
                        "                </div>\n" +
                        "                <div style=\"text-align: center;\">\n" +
                        "                    <button onclick=\"submitAddStreamer()\" class=\"btn btn-primary\">추가하기</button>\n" +
                        "                    <button onclick=\"hideAddStreamerForm()\" class=\"btn\">취소</button>\n" +
                        "                </div>\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "    \n" +
                        "    <!-- 푸터 정보 추가 -->\n" +
                        "    <div style=\"text-align: center; margin-top: 20px; color: #7f8c8d; font-size: 12px;\">\n" +
                        "        <p>SRT 스트리머 녹화 도구 &copy; " + java.time.Year.now().getValue() + "</p>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>\n";
                
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(html.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        
        /**
         * HTML 이스케이프 처리
         */
        private String escapeHtml(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
    
    /**
     * 스트리머 목록 API 핸들러
     */
    private class StreamersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Recorder.StreamerInfo> streamers = recorder.getStreamers();
            StringBuilder json = new StringBuilder();
            json.append("{\"streamers\":[");
            
            boolean first = true;
            for (Recorder.StreamerInfo info : streamers) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                
                json.append("{");
                json.append("\"name\":\"").append(escapeJson(info.getName())).append("\",");
                json.append("\"url\":\"").append(escapeJson(info.getUrl())).append("\",");
                json.append("\"title\":\"").append(escapeJson(info.getTitle() != null ? info.getTitle() : "")).append("\",");
                json.append("\"status\":\"").append(escapeJson(info.getStatus())).append("\",");
                json.append("\"isRecording\":").append(info.isRecording()).append(",");
                json.append("\"duration\":\"").append(escapeJson(info.getDuration())).append("\"");
                json.append("}");
            }
            
            json.append("]}");
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 녹화 시작 API 핸들러
     */
    private class StartRecordingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String name = getQueryParam(query, "name");
            
            String response;
            if (name != null && !name.isEmpty()) {
                boolean success = recorder.startRecording(name);
                if (success) {
                    response = "{\"success\":true}";
                } else {
                    response = "{\"success\":false,\"error\":\"녹화 시작 실패\"}";
                }
            } else {
                response = "{\"success\":false,\"error\":\"스트리머 이름이 필요합니다\"}";
            }
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 녹화 중지 API 핸들러
     */
    private class StopRecordingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String name = getQueryParam(query, "name");
            
            String response;
            if (name != null && !name.isEmpty()) {
                boolean success = recorder.stopRecording(name);
                if (success) {
                    response = "{\"success\":true}";
                } else {
                    response = "{\"success\":false,\"error\":\"녹화 중지 실패\"}";
                }
            } else {
                response = "{\"success\":false,\"error\":\"스트리머 이름이 필요합니다\"}";
            }
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 전체 녹화 시작 API 핸들러
     */
    private class StartAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            recorder.startAllRecordings();
            
            String response = "{\"success\":true}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 전체 녹화 중지 API 핸들러
     */
    private class StopAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            recorder.stopAllRecordings();
            
            String response = "{\"success\":true}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 환경설정 열기 API 핸들러
     */
    private class OpenSettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 메인 UI 스레드에서 설정 대화상자 열기
            Platform.runLater(() -> {
                if (recorder instanceof Recorder) {
                    try {
                        // 환경설정 대화상자 생성
                        SettingsDialog settingsDialog = new SettingsDialog(recorder);
                        settingsDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            
            String response = "{\"success\":true}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 스트리머 삭제 API 핸들러
     */
    private class DeleteStreamerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String streamerName = getQueryParam(query, "name");
            
            boolean success = false;
            String error = "";
            
            if (streamerName != null && !streamerName.isEmpty()) {
                try {
                    // JavaFX 스레드에서 삭제 작업 실행
                    Platform.runLater(() -> {
                        recorder.removeStreamer(streamerName);
                    });
                    success = true;
                } catch (Exception e) {
                    error = "스트리머 삭제 중 오류 발생: " + e.getMessage();
                }
            } else {
                error = "스트리머 이름이 지정되지 않았습니다.";
            }
            
            String response = "{\"success\":" + success + ", \"error\":\"" + escapeJson(error) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    /**
     * 쿼리 파라미터 추출
     */
    private String getQueryParam(String query, String paramName) {
        if (query == null) {
            return null;
        }
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        
        return null;
    }
    
    /**
     * JSON 이스케이프 처리
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * 웹 서버 설정 로드
     */
    public boolean loadWebServerSettings() {
        port = recorder.getWebServerPort();
        return true;
    }
    
    /**
     * 마지막 오류 메시지 반환
     */
    public String getLastError() {
        return lastError;
    }
    
    /**
     * 포트 설정
     */
    public void setPort(int port) {
        if (port > 0 && port < 65536) {
            this.port = port;
        }
    }
    
    /**
     * 현재 포트 반환
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 자동 활성화 여부 반환
     */
    public boolean getAutoActivate() {
        return recorder.isWebServerEnabled();
    }
    
    /**
     * 실행 중 여부 반환
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 설정 핸들러
     */
    private class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uri = exchange.getRequestURI().toString();
            
            if (uri.startsWith("/settings")) {
                // 설정 업데이트
                if ("POST".equals(exchange.getRequestMethod())) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String query = br.readLine();
                    
                    if (query != null) {
                        Map<String, String> params = parseQueryString(query);
                        
                        // 최대 스레드 설정
                        if (params.containsKey("maxThreads")) {
                            try {
                                int maxThreads = Integer.parseInt(params.get("maxThreads"));
                                if (maxThreads > 0) {
                                    recorder.setMaxThreads(maxThreads);
                                }
                            } catch (NumberFormatException e) {
                                // 숫자 변환 실패 시 무시
                            }
                        }
                        
                        // 녹화 프로그램 설정
                        if (params.containsKey("recorderProgram")) {
                            String recorderProgram = params.get("recorderProgram");
                            recorder.setRecorderProgram(recorderProgram);
                        }
                        
                        // 설정 저장
                        recorder.saveSettings();
                    }
                    
                    // 홈 페이지로 리다이렉트
                    exchange.getResponseHeaders().add("Location", "/");
                    exchange.sendResponseHeaders(302, -1);
                }
            }
        }
    }
    
    /**
     * 쿼리 문자열을 파싱하는 유틸리티 메서드
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    // AddStreamerHandler 클래스 추가
    private class AddStreamerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"success\":false, \"error\":\"Unknown error\"}";
            
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // POST 데이터 읽기
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String formData = br.readLine();
                    
                    // 쿼리 파라미터 파싱
                    Map<String, String> params = parseQueryString(formData);
                    String name = params.get("name");
                    String url = params.get("url");
                    String quality = params.get("quality");
                    
                    if (quality == null || quality.isEmpty()) {
                        quality = "best";
                    }
                    
                    if (name != null && url != null && !name.isEmpty() && !url.isEmpty()) {
                        boolean success = recorder.addStreamer(name, url, quality);
                        if (success) {
                            response = "{\"success\":true}";
                        } else {
                            response = "{\"success\":false, \"error\":\"스트리머 추가에 실패했습니다. 이름이 중복되거나 URL이 올바르지 않을 수 있습니다.\"}";
                        }
                    } else {
                        response = "{\"success\":false, \"error\":\"스트리머 이름과 URL을 모두 입력해야 합니다.\"}";
                    }
                } else {
                    response = "{\"success\":false, \"error\":\"잘못된 요청 메서드입니다. POST 메서드가 필요합니다.\"}";
                }
            } catch (Exception e) {
                response = "{\"success\":false, \"error\":\"" + escapeJson(e.getMessage()) + "\"}";
            }
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
} 