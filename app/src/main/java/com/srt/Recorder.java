package com.srt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.time.Duration;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 스트리머 녹화 관리 클래스
 * 스트리머 목록과 녹화 상태를 관리합니다.
 */
public class Recorder {
    public interface RecorderCallback {
        // 스트리머 상태 상수
        String STATUS_ONLINE = "online";
        String STATUS_OFFLINE = "offline";
        String STATUS_RECORDING = "recording";
        String STATUS_ERROR = "error";
        String STATUS_CHECKING = "checking";
        
        /**
         * 스트리머 상태 변경 시 호출되는 콜백 메서드
         * 
         * @param streamerName 상태가 변경된 스트리머 이름
         * @param status 새로운 상태 (online, offline, recording, error, checking)
         */
        void onStatusChange(String streamerName, String status);
    }
    
    public static class StreamerInfo {
        private String name;
        private String url;
        private String status;     // "online", "offline", "error", "recording"
        private String title;
        private String outputPath;
        private String quality;
        private boolean isRecording;
        private String duration;
        private LocalDateTime startTime;
        private Thread recordThread;
        private Process recordProcess;
        private AtomicBoolean shouldStop;
        
        public StreamerInfo() {
            this.status = "online"; // 기본적으로 온라인으로 가정 (실제 상태는 곧 확인됨)
            this.quality = "best";
            this.isRecording = false;
            this.duration = "00:00:00";
            this.shouldStop = new AtomicBoolean(false);
        }
        
        // Getter 및 Setter 메서드
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        
        public boolean isRecording() { return isRecording; }
        public void setRecording(boolean recording) { isRecording = recording; }
        
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public Thread getRecordThread() { return recordThread; }
        public void setRecordThread(Thread recordThread) { this.recordThread = recordThread; }
        
        public Process getRecordProcess() { return recordProcess; }
        public void setRecordProcess(Process recordProcess) { this.recordProcess = recordProcess; }
        
        public boolean getShouldStop() { return shouldStop.get(); }
        public void setShouldStop(boolean shouldStop) { this.shouldStop.set(shouldStop); }
    }
    
    private Map<String, StreamerInfo> streamers;
    private String cachePath;
    private int maxThreads;
    private int speedLimit;
    private boolean autoRecordEnabled;
    private boolean isMonitoringClipboard;
    private RecorderCallback callback;
    private ExecutorService executorService;
    private ScheduledExecutorService statusCheckExecutor; // 상태 확인용 스케줄러
    
    // 녹화 프로그램 선택 (streamlink, yt-dlp, ffmpeg)
    private String recorderProgram;
    
    // 웹 서버 관련 설정
    private boolean webServerEnabled;
    private int webServerPort;
    private String naverId;
    private String naverPw;
    private String nidSes;
    private String nidAut;
    
    public Recorder() {
        this.streamers = new ConcurrentHashMap<>();
        this.cachePath = "cache";
        this.maxThreads = 4;
        this.speedLimit = 0; // 무제한
        this.autoRecordEnabled = false;
        this.isMonitoringClipboard = false;
        this.webServerEnabled = false;
        this.webServerPort = 8080;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        this.statusCheckExecutor = Executors.newScheduledThreadPool(1);
        this.recorderProgram = "streamlink"; // 기본 녹화 프로그램
    }
    
    /**
     * 녹화 프로그램 설정
     */
    public void setRecorderProgram(String program) {
        if(program != null && !program.isEmpty()) {
            this.recorderProgram = program;
        }
    }
    
    /**
     * 녹화 프로그램 가져오기
     */
    public String getRecorderProgram() {
        return this.recorderProgram;
    }
    
    /**
     * 대체 녹화 프로그램 사용 가능 여부 확인
     */
    private boolean isYtDlpAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("yt-dlp 확인 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * FFmpeg 사용 가능 여부 확인
     */
    private boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("FFmpeg 확인 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * streamlink 등 외부 도구 설치 여부 확인
     */
    private boolean checkExternalDependencies() {
        try {
            ProcessBuilder pb = new ProcessBuilder("streamlink", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Streamlink 확인 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 레코더 초기화
     */
    public boolean initialize() {
        // 캐시 디렉토리 생성
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // 설정 파일 로드
        boolean loaded = loadSettings();
        if (loaded) {
            System.out.println("설정 파일을 성공적으로 로드했습니다.");
            
            // 설정 파일 백업 생성
            backupSettings();
        } else {
            System.out.println("설정 파일을 찾을 수 없거나 로드할 수 없습니다. 기본 설정을 사용합니다.");
        }
        
        // 외부 의존성 확인
        boolean dependenciesInstalled = checkExternalDependencies();
        if (!dependenciesInstalled) {
            System.err.println("경고: Streamlink가 설치되어 있지 않습니다. 온라인 상태 확인 및 녹화 기능이 작동하지 않을 수 있습니다.");
        }
        
        // 스트리머 상태 확인 스케줄러 시작
        startStatusChecker();
        
        return true;
    }
    
    /**
     * 설정 파일 로드
     */
    private boolean loadSettings() {
        Path settingsPath = Paths.get("settings.json");
        if (Files.exists(settingsPath)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> settings = mapper.readValue(settingsPath.toFile(), Map.class);
                
                // 설정 적용
                if (settings.containsKey("maxThreads")) {
                    setMaxThreads((Integer) settings.get("maxThreads"));
                }
                
                if (settings.containsKey("speedLimit")) {
                    setSpeedLimit((Integer) settings.get("speedLimit"));
                }
                
                if (settings.containsKey("autoRecord")) {
                    setAutoRecordEnabled((Boolean) settings.get("autoRecord"));
                }
                
                if (settings.containsKey("cachePath")) {
                    setCachePath((String) settings.get("cachePath"));
                }
                
                if (settings.containsKey("webServerEnabled")) {
                    setWebServerEnabled((Boolean) settings.get("webServerEnabled"));
                }
                
                if (settings.containsKey("webServerPort")) {
                    setWebServerPort((Integer) settings.get("webServerPort"));
                }
                
                // 녹화 프로그램 설정
                if (settings.containsKey("recorderProgram")) {
                    setRecorderProgram((String) settings.get("recorderProgram"));
                }
                
                // 네이버 인증 정보
                if (settings.containsKey("naverId")) {
                    setNaverId((String) settings.get("naverId"));
                }
                
                if (settings.containsKey("naverPw")) {
                    setNaverPw((String) settings.get("naverPw"));
                }
                
                if (settings.containsKey("nidSes")) {
                    setNidSes((String) settings.get("nidSes"));
                }
                
                if (settings.containsKey("nidAut")) {
                    setNidAut((String) settings.get("nidAut"));
                }
                
                // 스트리머 목록 로드
                if (settings.containsKey("streamers") && settings.get("streamers") instanceof List) {
                    List<Map<String, Object>> streamersList = (List<Map<String, Object>>) settings.get("streamers");
                    
                    for (Map<String, Object> streamerMap : streamersList) {
                        String name = (String) streamerMap.get("name");
                        String url = (String) streamerMap.get("url");
                        
                        StreamerInfo info = new StreamerInfo();
                        info.setName(name);
                        info.setUrl(url);
                        
                        if (streamerMap.containsKey("quality")) {
                            info.setQuality((String) streamerMap.get("quality"));
                        }
                        
                        if (streamerMap.containsKey("outputPath")) {
                            info.setOutputPath((String) streamerMap.get("outputPath"));
                        }
                        
                        streamers.put(name, info);
                    }
                }
                
                return true;
            } catch (IOException e) {
                System.err.println("설정 파일 로드 오류: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 설정 파일 저장
     */
    public boolean saveSettings() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            ObjectNode rootNode = mapper.createObjectNode();
            
            // 기본 설정 저장
            rootNode.put("maxThreads", maxThreads);
            rootNode.put("speedLimit", speedLimit);
            rootNode.put("autoRecord", autoRecordEnabled);
            rootNode.put("cachePath", cachePath);
            rootNode.put("webServerEnabled", webServerEnabled);
            rootNode.put("webServerPort", webServerPort);
            rootNode.put("recorderProgram", recorderProgram);
            
            // 네이버 인증 정보 저장
            rootNode.put("naverId", naverId != null ? naverId : "");
            rootNode.put("naverPw", naverPw != null ? naverPw : "");
            rootNode.put("nidSes", nidSes != null ? nidSes : "");
            rootNode.put("nidAut", nidAut != null ? nidAut : "");
            
            // 스트리머 목록 저장
            List<Map<String, Object>> streamersList = new ArrayList<>();
            
            for (StreamerInfo info : streamers.values()) {
                Map<String, Object> streamerMap = new HashMap<>();
                streamerMap.put("name", info.getName());
                streamerMap.put("url", info.getUrl());
                streamerMap.put("quality", info.getQuality());
                
                if (info.getOutputPath() != null) {
                    streamerMap.put("outputPath", info.getOutputPath());
                }
                
                streamersList.add(streamerMap);
            }
            
            // 스트리머 목록을 rootNode에 추가
            rootNode.putPOJO("streamers", streamersList);
            
            mapper.writeValue(new File("settings.json"), rootNode);
            
            System.out.println("설정이 성공적으로 저장되었습니다. 스트리머 수: " + streamers.size());
            return true;
        } catch (IOException e) {
            System.err.println("설정 파일 저장 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 설정 파일을 백업합니다.
     */
    public boolean backupSettings() {
        try {
            File settingsFile = new File("settings.json");
            if (!settingsFile.exists()) {
                System.out.println("백업을 생성할 설정 파일이 없습니다.");
                return false;
            }
            
            // 백업 파일 이름에 날짜와 시간 추가
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            File backupFile = new File("settings_backup_" + timestamp + ".json");
            
            // 파일 복사
            Files.copy(settingsFile.toPath(), backupFile.toPath());
            System.out.println("설정 파일이 백업되었습니다: " + backupFile.getName());
            
            return true;
        } catch (IOException e) {
            System.err.println("설정 파일 백업 오류: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 스트리머 추가
     */
    public boolean addStreamer(String name, String url) {
        return addStreamer(name, url, "best", null);
    }
    
    /**
     * 스트리머 추가 (품질 및 경로 지정)
     */
    public boolean addStreamer(String name, String url, String quality, String outputPath) {
        if (name == null || name.isEmpty() || url == null || url.isEmpty()) {
            return false;
        }
        
        if (streamers.containsKey(name)) {
            return false;
        }
        
        StreamerInfo info = new StreamerInfo();
        info.setName(name);
        info.setUrl(url);
        
        if (quality != null && !quality.isEmpty()) {
            info.setQuality(quality);
        }
        
        if (outputPath != null && !outputPath.isEmpty()) {
            info.setOutputPath(outputPath);
        }
        
        streamers.put(name, info);
        return true;
    }
    
    /**
     * 스트리머 제거
     */
    public boolean removeStreamer(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        StreamerInfo info = streamers.get(name);
        if (info != null && info.isRecording()) {
            stopRecording(name);
        }
        
        streamers.remove(name);
        return true;
    }
    
    /**
     * 스트리머 정보 가져오기
     */
    public StreamerInfo getStreamerInfo(String name) {
        return streamers.get(name);
    }
    
    /**
     * 모든 스트리머 목록 가져오기
     */
    public List<StreamerInfo> getStreamers() {
        return new ArrayList<>(streamers.values());
    }
    
    /**
     * 녹화 시작
     */
    public boolean startRecording(String name) {
        StreamerInfo info = streamers.get(name);
        if (info != null && !info.isRecording()) {
            if (RecorderCallback.STATUS_ONLINE.equals(info.getStatus())) {
                info.setRecording(true);
                info.setStatus(RecorderCallback.STATUS_RECORDING);
                info.setStartTime(LocalDateTime.now()); // 실제 녹화 시작 시간 설정
                info.setDuration("00:00:00"); // 녹화 시작 시 녹화 시간 초기화
                
                // 녹화 스레드 시작
                Thread recordThread = new Thread(() -> recordWorker(info));
                recordThread.setDaemon(true);
                recordThread.start();
                info.setRecordThread(recordThread);
                
                // 콜백 호출
                if (callback != null) {
                    callback.onStatusChange(name, RecorderCallback.STATUS_RECORDING);
                }
                
                return true;
            } else {
                // 오프라인 스트리머는 녹화 불가
                return false;
            }
        }
        return false;
    }
    
    /**
     * 녹화 중지
     */
    public boolean stopRecording(String name) {
        StreamerInfo info = streamers.get(name);
        if (info == null || !info.isRecording()) {
            return false;
        }
        
        // 녹화 중지 신호 전송
        info.setShouldStop(true);
        
        // 녹화 프로세스 종료
        Process process = info.getRecordProcess();
        if (process != null) {
            process.destroy();
        }
        
        // 상태 업데이트
        info.setRecording(false);
        info.setDuration("00:00:00"); // 녹화 시간 초기화
        
        // 콜백 호출
        if (callback != null) {
            if (RecorderCallback.STATUS_ONLINE.equals(info.getStatus())) {
                callback.onStatusChange(name, RecorderCallback.STATUS_ONLINE);
            } else {
                callback.onStatusChange(name, RecorderCallback.STATUS_OFFLINE);
            }
        }
        
        return true;
    }
    
    /**
     * 모든 녹화 시작
     */
    public void startAllRecordings() {
        for (String name : streamers.keySet()) {
            StreamerInfo info = streamers.get(name);
            if (RecorderCallback.STATUS_ONLINE.equals(info.getStatus()) && !info.isRecording()) {
                startRecording(name);
            }
        }
    }
    
    /**
     * 모든 녹화 중지
     */
    public void stopAllRecordings() {
        for (String name : streamers.keySet()) {
            if (streamers.get(name).isRecording()) {
                stopRecording(name);
            }
        }
    }
    
    /**
     * 녹화 작업 스레드
     */
    private void recordWorker(StreamerInfo info) {
        long recordingStartTime = 0; // 실제 녹화 시작 시간
        File outputFile = null; // 출력 파일 변수를 상위로 이동
        
        try {
            // 상태는 이미 startRecording에서 설정했으므로 중복 설정 제거
            info.setShouldStop(false);
            
            // 녹화 파일 이름 생성 개선
            String filename = createOutputFileName(info);
            
            // 출력 경로 설정 및 디렉토리 생성
            String outputPath = info.getOutputPath();
            if (outputPath == null || outputPath.trim().isEmpty()) {
                outputPath = cachePath; // 기본 캐시 경로 사용
            }
            
            // 경로 정규화
            outputPath = normalizeOutputPath(outputPath);
            
            // 출력 디렉토리 생성
            File outputDir = createOutputDirectory(outputPath);
            
            // 최종 출력 파일 객체 생성
            outputFile = new File(outputDir, filename);
            System.out.println("녹화 파일 경로: " + outputFile.getAbsolutePath());
            
            // URL에 따른 녹화 프로그램 자동 선택
            String url = info.getUrl();
            String effectiveRecorderProgram = recorderProgram;
            
            // Chzzk URL인 경우 특별 처리
            boolean isChzzkUrl = url != null && url.contains("chzzk.naver.com");
            if (isChzzkUrl) {
                System.out.println("Chzzk URL 감지: " + url);
                
                // Chzzk URL인 경우 yt-dlp 우선 사용 (FFmpeg 대신)
                if ("ffmpeg".equals(effectiveRecorderProgram) && isYtDlpAvailable()) {
                    System.out.println("Chzzk 녹화를 위해 FFmpeg 대신 yt-dlp로 변경합니다");
                    effectiveRecorderProgram = "yt-dlp";
                }
            }
            
            // 실제 녹화 시작 시간 기록 (프로세스 시작 전)
            recordingStartTime = System.currentTimeMillis();
            System.out.println("녹화 준비 시간: " + new java.util.Date(recordingStartTime));
            
            // 명령어 구성 - 선택한 녹화 프로그램에 따라 다른 명령 사용
            List<String> command = new ArrayList<>();
            ProcessBuilder pb = new ProcessBuilder();
            
            // 환경 변수 설정
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");
            
            switch (effectiveRecorderProgram.toLowerCase()) {
                case "yt-dlp":
                    // yt-dlp를 사용하는 경우
                    if (!isYtDlpAvailable()) {
                        System.out.println("yt-dlp가 설치되어 있지 않아 streamlink로 대체합니다.");
                        buildStreamlinkCommand(command, info, outputFile);
                    } else {
                        buildYtDlpCommand(command, info, outputFile);
                    }
                    break;
                    
                case "ffmpeg":
                    // FFmpeg를 직접 사용하는 경우
                    if (!isFfmpegAvailable()) {
                        System.out.println("FFmpeg가 설치되어 있지 않아 streamlink로 대체합니다.");
                        buildStreamlinkCommand(command, info, outputFile);
                    } else {
                        buildFfmpegCommand(command, info, outputFile, isChzzkUrl);
                    }
                    break;
                    
                case "streamlink":
                default:
                    // 기본값: streamlink 사용
                    buildStreamlinkCommand(command, info, outputFile);
                    break;
            }
            
            // 로그에 명령어 출력
            System.out.println("실행 명령어: " + String.join(" ", command));
            
            // 프로세스 환경 설정
            pb.command(command);
            pb.redirectErrorStream(true);
            
            // 표준 출력 로깅 설정
            File logFile = new File(outputDir, filename + "_log.txt");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            // 프로세스 시작 준비
            System.out.println("녹화 프로세스 시작 중...");
            
            // 프로세스 시작
            Process process = pb.start();
            info.setRecordProcess(process);
            
            // 프로세스 시작 시간 기록
            long processStartTime = System.currentTimeMillis();
            System.out.println("프로세스 시작 시간: " + new java.util.Date(processStartTime));
            System.out.println("프로세스 시작 소요 시간: " + (processStartTime - recordingStartTime) + "ms");
            
            // 프로세스 종료 대기
            while (!info.getShouldStop()) {
                try {
                    // 프로세스 상태 확인
                    if (!process.isAlive()) {
                        int exitCode = process.exitValue();
                        System.out.println(info.getName() + " 녹화 프로세스가 종료되었습니다. 상태 코드: " + exitCode);
                        
                        // 오류 코드인 경우 로그 추가
                        if (exitCode != 0) {
                            System.err.println("오류 발생: 프로세스가 비정상 종료됨 (코드: " + exitCode + ")");
                        }
                        break;
                    }
                    
                    // 녹화 시간 업데이트 - 현재 시간 - 시작 시간 (실제 녹화 시작 시간 기준)
                    long currentTime = System.currentTimeMillis();
                    long elapsedMillis = currentTime - recordingStartTime;
                    long elapsedSeconds = elapsedMillis / 1000;
                    
                    String durationText = formatRecordingDuration(elapsedSeconds);
                    info.setDuration(durationText);
                    
                    // 파일 존재 여부와 크기 정기적으로 확인 (5초마다)
                    if (currentTime % 5000 < 1000) {
                        if (outputFile.exists()) {
                            long fileSizeMB = outputFile.length() / (1024 * 1024);
                            System.out.println(info.getName() + " 녹화 중: " + info.getDuration() + 
                                " (파일 크기: " + fileSizeMB + " MB)");
                        } else {
                            System.out.println(info.getName() + " 녹화 중이지만 파일이 아직 생성되지 않음: " + info.getDuration());
                        }
                    }
                    
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // 프로세스 종료
            if (process.isAlive()) {
                process.destroy();
                try {
                    // 최대 5초간 정상 종료 대기
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly(); // 강제 종료
                    }
                } catch (InterruptedException e) {
                    process.destroyForcibly();
                }
            }
            
            System.out.println(info.getName() + " 녹화가 완료되었습니다. 파일: " + outputFile.getName());
            
            // 치지직 URL인 경우 추가 후처리
            if (info.getUrl() != null && info.getUrl().contains("chzzk.naver.com") 
                && outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                System.out.println("치지직 녹화 파일 후처리 시작: " + outputFile.getName());
                fixVideoMetadata(outputFile);
            }
            
        } catch (Exception e) {
            System.err.println("녹화 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 녹화 상태 업데이트
            info.setRecording(false);
            info.setRecordProcess(null);
            info.setDuration("00:00:00"); // 녹화 종료 시 녹화 시간 초기화
            
            // 콜백 호출
            if (callback != null) {
                String currentStatus = RecorderCallback.STATUS_ONLINE.equals(info.getStatus()) 
                    || RecorderCallback.STATUS_RECORDING.equals(info.getStatus())
                    ? RecorderCallback.STATUS_ONLINE 
                    : RecorderCallback.STATUS_OFFLINE;
                info.setStatus(currentStatus);
                callback.onStatusChange(info.getName(), currentStatus);
            }
        }
    }
    
    /**
     * 상태 업데이트 콜백 설정
     */
    public void setCallback(RecorderCallback callback) {
        this.callback = callback;
    }
    
    // Getter 및 Setter 메서드
    public String getCachePath() {
        return cachePath;
    }
    
    public void setCachePath(String cachePath) {
        if (cachePath != null && !cachePath.isEmpty()) {
            this.cachePath = cachePath;
            File cacheDir = new File(cachePath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public void setMaxThreads(int maxThreads) {
        if (maxThreads > 0) {
            this.maxThreads = maxThreads;
            
            // 스레드 풀 재설정
            executorService.shutdown();
            executorService = Executors.newFixedThreadPool(maxThreads);
        }
    }
    
    public int getSpeedLimit() {
        return speedLimit;
    }
    
    public void setSpeedLimit(int speedLimit) {
        if (speedLimit >= 0) {
            this.speedLimit = speedLimit;
        }
    }
    
    public boolean isAutoRecordEnabled() {
        return autoRecordEnabled;
    }
    
    public void setAutoRecordEnabled(boolean autoRecordEnabled) {
        this.autoRecordEnabled = autoRecordEnabled;
    }
    
    public boolean isWebServerEnabled() {
        return webServerEnabled;
    }
    
    public void setWebServerEnabled(boolean webServerEnabled) {
        this.webServerEnabled = webServerEnabled;
    }
    
    public int getWebServerPort() {
        return webServerPort;
    }
    
    public void setWebServerPort(int webServerPort) {
        if (webServerPort > 0 && webServerPort < 65536) {
            this.webServerPort = webServerPort;
        }
    }
    
    public String getNaverId() {
        return naverId;
    }
    
    public void setNaverId(String naverId) {
        this.naverId = naverId;
    }
    
    public String getNaverPw() {
        return naverPw;
    }
    
    public void setNaverPw(String naverPw) {
        this.naverPw = naverPw;
    }
    
    public String getNidSes() {
        return nidSes;
    }
    
    public void setNidSes(String nidSes) {
        this.nidSes = nidSes;
    }
    
    public String getNidAut() {
        return nidAut;
    }
    
    public void setNidAut(String nidAut) {
        this.nidAut = nidAut;
    }
    
    /**
     * 스트리머 추가 (품질 지정)
     */
    public boolean addStreamer(String name, String url, String quality) {
        return addStreamer(name, url, quality, null);
    }
    
    /**
     * 스트리머 상태 확인 스케줄러 시작
     */
    private void startStatusChecker() {
        // 10초마다 모든 스트리머의 상태를 확인 (기존 30초에서 단축)
        statusCheckExecutor.scheduleAtFixedRate(() -> {
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] 스트리머 상태 확인 시작");
            checkAllStreamersStatus();
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 모든 스트리머 상태 확인
     */
    public void checkAllStreamersStatus() {
        try {
            System.out.println("총 " + streamers.size() + "명의 스트리머 상태 확인 중...");
            for (StreamerInfo info : streamers.values()) {
                checkStreamerStatus(info);
            }
        } catch (Exception e) {
            System.err.println("스트리머 상태 확인 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 특정 스트리머 상태 수동 갱신
     */
    public void forceUpdateStreamerStatus(String name) {
        StreamerInfo info = streamers.get(name);
        if (info != null) {
            System.out.println(name + " 스트리머 상태 수동 갱신 시도...");
            checkStreamerStatus(info);
        }
    }
    
    /**
     * 모든 스트리머 상태 수동 갱신
     */
    public void forceUpdateAllStreamers() {
        System.out.println("모든 스트리머 상태 수동 갱신 시작...");
        
        if (streamers.isEmpty()) {
            System.out.println("등록된 스트리머가 없습니다.");
            return;
        }
        
        try {
            System.out.println("총 " + streamers.size() + "명의 스트리머 상태 확인 중...");
            
            // 각 스트리머에 대해 동기적으로 상태 확인
            for (StreamerInfo info : streamers.values()) {
                // 초기에 상태를 업데이트하여 UI에 반영
                if (callback != null) {
                    callback.onStatusChange(info.getName(), "checking");
                }
                
                // 상태 확인
                checkStreamerStatus(info);
                
                // UI 업데이트를 위한 약간의 지연
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // 무시
                }
            }
            
            System.out.println("모든 스트리머 상태 갱신 완료.");
        } catch (Exception e) {
            System.err.println("스트리머 상태 확인 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 특정 스트리머의 상태 확인
     */
    private void checkStreamerStatus(StreamerInfo info) {
        String url = info.getUrl();
        if (url == null || url.isEmpty()) {
            return;
        }
        
        // 이전 상태를 저장
        String oldStatus = info.getStatus();
        String streamerName = info.getName();
        
        System.out.println("[상태 확인] " + streamerName + " 스트리머 상태 확인 중... (현재: " + oldStatus + ")");
        
        try {
            // 먼저 streamlink로 확인 시도
            boolean streamlinkAvailable = checkExternalDependencies();
            
            if (streamlinkAvailable) {
                // streamlink로 상태 확인
                System.out.println("[상태 확인] " + streamerName + " - streamlink 확인 방식 사용");
                checkWithStreamlink(info);
            } else {
                // HTTP 요청으로 상태 확인 (간단한 대체 방법)
                System.out.println("[상태 확인] " + streamerName + " - HTTP 요청 확인 방식 사용");
                checkWithHttpRequest(info);
            }
            
            // 상태 확인 결과 출력
            System.out.println("[상태 확인] " + streamerName + " 상태 확인 결과: " + oldStatus + " -> " + info.getStatus());
            
            // 상태가 변경되었으면 콜백 호출
            if (!oldStatus.equals(info.getStatus()) && callback != null) {
                System.out.println("[상태 변경] " + streamerName + " 상태가 변경됨: " + oldStatus + " -> " + info.getStatus());
                callback.onStatusChange(info.getName(), info.getStatus());
                
                // 자동 녹화가 활성화되어 있고, 상태가 온라인이 되었으면 녹화 시작
                if (autoRecordEnabled && "online".equals(info.getStatus()) && !info.isRecording()) {
                    System.out.println("[자동 녹화] " + streamerName + " 자동 녹화 시작");
                    startRecording(info.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("[오류] " + streamerName + " 스트리머 상태 확인 오류: " + e.getMessage());
            e.printStackTrace();
            
            // 에러 상태로 변경
            info.setStatus("error");
            
            // 상태가 변경되었으면 콜백 호출
            if (!oldStatus.equals("error") && callback != null) {
                callback.onStatusChange(info.getName(), "error");
            }
        }
    }
    
    /**
     * Streamlink를 사용해 스트리머 상태 확인
     */
    private void checkWithStreamlink(StreamerInfo info) {
        try {
            ProcessBuilder pb = new ProcessBuilder("streamlink", info.getUrl(), "--json");
            // 환경 변수에 UTF-8 인코딩 설정 추가
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            Process process = pb.start();
            
            // InputStreamReader에 UTF-8 인코딩 명시
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"));
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // JSON 파싱
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = mapper.readValue(output.toString(), Map.class);
                
                // 방송 제목 가져오기
                if (result.containsKey("metadata") && result.get("metadata") instanceof Map) {
                    Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                    if (metadata.containsKey("title")) {
                        info.setTitle((String) metadata.get("title"));
                    }
                }
                
                // 스트림 정보 확인
                if (result.containsKey("streams") && !((Map) result.get("streams")).isEmpty()) {
                    // 스트림이 있으면 온라인
                    info.setStatus("online");
                } else {
                    // 스트림이 없으면 오프라인
                    info.setStatus("offline");
                }
            } else {
                // 프로세스 실패
                info.setStatus("offline");
            }
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("Streamlink 확인 오류: " + e.getMessage());
        }
    }
    
    /**
     * HTTP 요청으로 스트리머 상태 확인 (단순 접속 확인)
     */
    private void checkWithHttpRequest(StreamerInfo info) {
        try {
            String url = info.getUrl();
            if (url == null || url.isEmpty()) {
                info.setStatus("error");
                return;
            }
            
            // URL 도메인에 따라 다른 확인 방법 적용
            if (url.contains("twitch.tv")) {
                checkTwitchStreamer(info);
            } else if (url.contains("youtube.com") || url.contains("youtu.be")) {
                checkYoutubeStreamer(info);
            } else if (url.contains("afreecatv.com")) {
                checkAfreecaStreamer(info);
            } else {
                // 기본 확인 방법
                checkGenericUrl(info);
            }
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("HTTP 요청 오류: " + e.getMessage());
        }
    }
    
    /**
     * 트위치 스트리머 상태 확인
     */
    private void checkTwitchStreamer(StreamerInfo info) {
        try {
            String url = info.getUrl();
            // 채널 이름 추출
            String channelName = extractTwitchChannelName(url);
            
            if (channelName == null || channelName.isEmpty()) {
                info.setStatus("error");
                System.err.println("트위치 채널 이름을 추출할 수 없습니다: " + url);
                return;
            }
            
            System.out.println("트위치 채널 확인: " + channelName);
            
            // 먼저 API를 통해 확인 시도 (GQL API 사용)
            String gqlUrl = "https://gql.twitch.tv/gql";
            java.net.URL apiUrl = new java.net.URL(gqlUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Client-ID", "kimne78kx3ncx6brgo4mv6wki5h1ko"); // Public Client ID
            
            // GraphQL 쿼리
            String gqlQuery = "{\n" +
                "  \"operationName\": \"StreamMetadata\",\n" +
                "  \"variables\": {\n" +
                "    \"channelLogin\": \"" + channelName + "\"\n" +
                "  },\n" +
                "  \"query\": \"query StreamMetadata($channelLogin: String!) {\\n  user(login: $channelLogin) {\\n    id\\n    login\\n    stream {\\n      id\\n      title\\n      type\\n      viewersCount\\n    }\\n  }\\n}\\n\"\n" +
                "}";
            
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = gqlQuery.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // API 응답 읽기
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                
                String jsonResponse = content.toString();
                System.out.println("트위치 API 응답: " + jsonResponse);
                
                // JSON 파싱
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> result = mapper.readValue(jsonResponse, Map.class);
                
                if (result.containsKey("data") && result.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    
                    if (data.containsKey("user") && data.get("user") instanceof Map) {
                        Map<String, Object> user = (Map<String, Object>) data.get("user");
                        
                        if (user.containsKey("stream") && user.get("stream") != null) {
                            // 스트림이 있으면 온라인
                            info.setStatus("online");
                            
                            // 제목 추출
                            Map<String, Object> stream = (Map<String, Object>) user.get("stream");
                            if (stream.containsKey("title")) {
                                String title = (String) stream.get("title");
                                info.setTitle(title);
                                System.out.println("트위치 채널 " + channelName + " 온라인, 제목: " + title);
                            } else {
                                System.out.println("트위치 채널 " + channelName + " 온라인, 제목 없음");
                            }
                        } else {
                            // 스트림이 없으면 오프라인
                            info.setStatus("offline");
                            System.out.println("트위치 채널 " + channelName + " 오프라인");
                        }
                    } else {
                        // 유저 정보가 없으면 오프라인
                        info.setStatus("offline");
                        System.out.println("트위치 채널 " + channelName + " 정보 없음");
                    }
                } else {
                    // 데이터가 없으면 오프라인
                    info.setStatus("offline");
                    System.out.println("트위치 API 응답에 데이터 없음");
                }
                
                reader.close();
            } else {
                // API 요청 실패 - 웹페이지로 대체 확인
                System.out.println("트위치 API 요청 실패, 웹페이지로 대체 확인: " + responseCode);
                fallbackTwitchCheck(channelName, info);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("트위치 채널 확인 오류: " + e.getMessage());
            try {
                // API 확인 실패 시 웹페이지로 대체 확인
                String channelName = extractTwitchChannelName(info.getUrl());
                System.out.println("트위치 대체 확인: " + channelName);
                fallbackTwitchCheck(channelName, info);
            } catch (Exception ex) {
                info.setStatus("error");
                System.err.println("트위치 대체 확인 오류: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 트위치 웹페이지 대체 확인 (API 실패 시)
     */
    private void fallbackTwitchCheck(String channelName, StreamerInfo info) {
        try {
            if (channelName == null || channelName.isEmpty()) {
                info.setStatus("error");
                return;
            }
            
            // 트위치 채널 페이지 접속
            java.net.URL pageUrl = new java.net.URL("https://www.twitch.tv/" + channelName);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) pageUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // 채널 페이지 컨텐츠 확인
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null && content.length() < 50000) {
                    content.append(line);
                }
                
                String htmlContent = content.toString();
                
                // 방송 중인지 확인 (isLiveBroadcast 문자열이 포함되어 있으면 방송 중)
                if (htmlContent.contains("isLiveBroadcast") || htmlContent.contains("\"isLive\":true")) {
                    info.setStatus("online");
                    System.out.println("트위치 채널 " + channelName + " 온라인 (웹페이지 확인)");
                    
                    // 제목 추출
                    int titleStart = htmlContent.indexOf("\"meta_title\":\"") + 14;
                    int titleEnd = htmlContent.indexOf("\"", titleStart);
                    
                    if (titleStart > 14 && titleEnd > titleStart) {
                        String title = htmlContent.substring(titleStart, titleEnd);
                        // JSON 이스케이프 문자 처리
                        title = title.replace("\\\"", "\"").replace("\\\\", "\\");
                        info.setTitle(title);
                        System.out.println("트위치 채널 제목: " + title);
                    }
                } else {
                    info.setStatus("offline");
                    System.out.println("트위치 채널 " + channelName + " 오프라인 (웹페이지 확인)");
                }
                
                reader.close();
            } else {
                info.setStatus("offline");
                System.out.println("트위치 채널 페이지 접속 실패: " + responseCode);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("트위치 웹페이지 확인 오류: " + e.getMessage());
        }
    }
    
    /**
     * 유튜브 스트리머 상태 확인
     */
    private void checkYoutubeStreamer(StreamerInfo info) {
        try {
            String url = info.getUrl();
            
            // 채널 또는 비디오 ID 추출
            String videoId = extractYoutubeVideoId(url);
            
            if (videoId == null || videoId.isEmpty()) {
                info.setStatus("error");
                System.err.println("유튜브 비디오 ID를 추출할 수 없습니다: " + url);
                return;
            }
            
            System.out.println("유튜브 비디오 확인: " + videoId);
            
            // 유튜브 비디오 페이지 접속
            java.net.URL videoUrl = new java.net.URL("https://www.youtube.com/watch?v=" + videoId);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) videoUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // 비디오 페이지 컨텐츠 확인
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null && content.length() < 100000) {
                    content.append(line);
                }
                
                String htmlContent = content.toString();
                
                // 라이브 스트림인지 확인 - 여러 가지 방법 시도
                boolean isLive = false;
                
                // 방법 1: videoDetails 정보에서 확인
                if (htmlContent.contains("\"isLiveNow\":true")) {
                    isLive = true;
                    System.out.println("유튜브 비디오 " + videoId + " 라이브 중 (isLiveNow 확인)");
                }
                // 방법 2: 라이브 배지 확인
                else if (htmlContent.contains("LIVE_STREAM") || htmlContent.contains("LIVE NOW")) {
                    isLive = true;
                    System.out.println("유튜브 비디오 " + videoId + " 라이브 중 (LIVE_STREAM 확인)");
                }
                // 방법 3: isLiveContent 확인
                else if (htmlContent.contains("\"isLiveContent\":true")) {
                    isLive = true;
                    System.out.println("유튜브 비디오 " + videoId + " 라이브 중 (isLiveContent 확인)");
                }
                
                if (isLive) {
                    info.setStatus("online");
                    
                    // 제목 추출
                    int titleStart = htmlContent.indexOf("\"title\":\"") + 9;
                    int titleEnd = htmlContent.indexOf("\"", titleStart);
                    
                    if (titleStart > 9 && titleEnd > titleStart) {
                        String title = htmlContent.substring(titleStart, titleEnd);
                        // JSON 이스케이프 문자 처리
                        title = title.replace("\\\"", "\"").replace("\\\\", "\\");
                        info.setTitle(title);
                        System.out.println("유튜브 비디오 제목: " + title);
                    } else {
                        // 대체 방법으로 제목 추출
                        titleStart = htmlContent.indexOf("<title>") + 7;
                        titleEnd = htmlContent.indexOf("</title>", titleStart);
                        
                        if (titleStart > 7 && titleEnd > titleStart) {
                            String title = htmlContent.substring(titleStart, titleEnd);
                            title = title.replace(" - YouTube", "");
                            info.setTitle(title);
                            System.out.println("유튜브 비디오 제목 (대체): " + title);
                        }
                    }
                } else {
                    info.setStatus("offline");
                    System.out.println("유튜브 비디오 " + videoId + " 라이브 아님");
                }
                
                reader.close();
            } else {
                info.setStatus("offline");
                System.out.println("유튜브 비디오 페이지 접속 실패: " + responseCode);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("유튜브 스트림 확인 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 아프리카TV 스트리머 상태 확인
     */
    private void checkAfreecaStreamer(StreamerInfo info) {
        try {
            String url = info.getUrl();
            
            // BJ 아이디 추출
            String bjId = extractAfreecaBjId(url);
            
            if (bjId == null || bjId.isEmpty()) {
                info.setStatus("error");
                return;
            }
            
            // 아프리카TV BJ 페이지 접속
            java.net.URL bjUrl = new java.net.URL("https://play.afreecatv.com/" + bjId);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) bjUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // BJ 페이지 컨텐츠 확인
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null && content.length() < 50000) {
                    content.append(line);
                }
                
                String htmlContent = content.toString();
                
                // 라이브 방송 중인지 확인
                if (htmlContent.contains("class=\"on\"") && !htmlContent.contains("class=\"thumb_off\"")) {
                    info.setStatus("online");
                    
                    // 제목 추출
                    int titleStart = htmlContent.indexOf("<title>") + 7;
                    int titleEnd = htmlContent.indexOf("</title>", titleStart);
                    
                    if (titleStart > 7 && titleEnd > titleStart) {
                        String title = htmlContent.substring(titleStart, titleEnd);
                        title = title.replace(" - AfreecaTV", "");
                        info.setTitle(title);
                    }
                } else {
                    info.setStatus("offline");
                }
                
                reader.close();
            } else {
                info.setStatus("offline");
            }
            
            connection.disconnect();
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("아프리카TV BJ 확인 오류: " + e.getMessage());
        }
    }
    
    /**
     * 일반 URL 상태 확인
     */
    private void checkGenericUrl(StreamerInfo info) {
        try {
            java.net.URL urlObj = new java.net.URL(info.getUrl());
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // 페이지가 정상적으로 로드됨 - 온라인으로 가정
                info.setStatus("online");
                
                // 제목 가져오기 시도
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    StringBuilder content = new StringBuilder();
                    while ((line = reader.readLine()) != null && content.length() < 10000) {
                        content.append(line);
                    }
                    
                    String htmlContent = content.toString();
                    int titleStart = htmlContent.indexOf("<title>") + 7;
                    int titleEnd = htmlContent.indexOf("</title>");
                    
                    if (titleStart > 0 && titleEnd > titleStart) {
                        String title = htmlContent.substring(titleStart, titleEnd);
                        info.setTitle(title);
                    }
                }
            } else {
                info.setStatus("offline");
            }
            
            connection.disconnect();
        } catch (Exception e) {
            info.setStatus("error");
            System.err.println("일반 URL 확인 오류: " + e.getMessage());
        }
    }
    
    /**
     * 트위치 URL에서 채널 이름 추출
     */
    private String extractTwitchChannelName(String url) {
        try {
            if (url.contains("twitch.tv/")) {
                String[] parts = url.split("twitch.tv/");
                if (parts.length > 1) {
                    String channelPart = parts[1];
                    // 추가 경로나 쿼리 파라미터 제거
                    if (channelPart.contains("/")) {
                        channelPart = channelPart.split("/")[0];
                    }
                    if (channelPart.contains("?")) {
                        channelPart = channelPart.split("\\?")[0];
                    }
                    return channelPart;
                }
            }
        } catch (Exception e) {
            System.err.println("트위치 채널 이름 추출 오류: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 유튜브 URL에서 비디오 ID 추출
     */
    private String extractYoutubeVideoId(String url) {
        try {
            if (url.contains("youtube.com/watch")) {
                java.net.URL urlObj = new java.net.URL(url);
                String query = urlObj.getQuery();
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length > 1 && keyValue[0].equals("v")) {
                        return keyValue[1];
                    }
                }
            } else if (url.contains("youtu.be/")) {
                String[] parts = url.split("youtu.be/");
                if (parts.length > 1) {
                    String videoId = parts[1];
                    if (videoId.contains("?")) {
                        videoId = videoId.split("\\?")[0];
                    }
                    return videoId;
                }
            } else if (url.contains("youtube.com/channel/")) {
                String[] parts = url.split("youtube.com/channel/");
                if (parts.length > 1) {
                    String channelId = parts[1];
                    if (channelId.contains("/")) {
                        channelId = channelId.split("/")[0];
                    }
                    return channelId; // 채널 ID 반환
                }
            }
        } catch (Exception e) {
            System.err.println("유튜브 비디오 ID 추출 오류: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 아프리카TV URL에서 BJ 아이디 추출
     */
    private String extractAfreecaBjId(String url) {
        try {
            if (url.contains("afreecatv.com/")) {
                String[] parts = url.split("afreecatv.com/");
                if (parts.length > 1) {
                    String bjId = parts[1];
                    if (bjId.contains("/")) {
                        bjId = bjId.split("/")[0];
                    }
                    if (bjId.contains("?")) {
                        bjId = bjId.split("\\?")[0];
                    }
                    return bjId;
                }
            }
        } catch (Exception e) {
            System.err.println("아프리카TV BJ 아이디 추출 오류: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 자원 정리
     */
    public void shutdown() {
        // 실행 중인 모든 작업 중지
        stopAllRecordings();
        
        // 스케줄러 종료
        if (statusCheckExecutor != null) {
            statusCheckExecutor.shutdown();
            try {
                if (!statusCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    statusCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                statusCheckExecutor.shutdownNow();
            }
        }
        
        // 스레드 풀 종료
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
    
    /**
     * 상태 텍스트 표시 변환 메서드
     */
    public static String getStatusDisplayText(String status) {
        switch (status) {
            case "online":
                return "온라인";
            case "offline":
                return "오프라인";
            case "error":
                return "오류";
            case "recording":
                return "녹화중";
            case "checking":
                return "확인중";
            default:
                return status;
        }
    }
    
    /**
     * 상태별 색상 코드 반환 메서드 (HTML/CSS 용)
     */
    public static String getStatusColorCode(String status) {
        switch (status) {
            case "online":
                return "#2ecc71"; // 녹색
            case "offline":
                return "#7f8c8d"; // 회색
            case "error":
                return "#e74c3c"; // 빨간색
            case "recording":
                return "#e67e22"; // 주황색
            case "checking":
                return "#3498db"; // 파란색
            default:
                return "#7f8c8d"; // 기본 회색
        }
    }
    
    /**
     * 상태별 CSS 클래스 반환 메서드 (웹페이지 용)
     */
    public static String getStatusCssClass(String status) {
        switch (status) {
            case "online":
                return "online";
            case "offline":
                return "offline";
            case "error":
                return "error";
            case "recording":
                return "recording";
            case "checking":
                return "checking";
            default:
                return "offline";
        }
    }
    
    /**
     * Streamlink 명령어 구성
     */
    private void buildStreamlinkCommand(List<String> command, StreamerInfo info, File outputFile) {
        command.add("streamlink");
        
        // Chzzk URL 확인
        boolean isChzzkUrl = info.getUrl() != null && info.getUrl().contains("chzzk.naver.com");
        
        // 품질 옵션 설정
        command.add(info.getUrl());
        command.add(info.getQuality());
        
        // 출력 파일 설정
        command.add("-o");
        command.add(outputFile.getAbsolutePath());
        
        // 추가 옵션 설정
        command.add("--force");           // 기존 파일 덮어쓰기
        // --hls-live-restart 옵션 제거 (이 옵션은 방송 전체를 처음부터 다운로드하도록 함)
        
        // 현재 시점부터 녹화하기 위한 옵션 추가
        command.add("--hls-start-offset");
        command.add("0");                 // 0 = 현재 시점부터 녹화
        
        command.add("--stream-segment-timeout");
        command.add("5");                 // 세그먼트 다운로드 제한시간 (초)
        
        // Chzzk 관련 설정
        if (isChzzkUrl) {
            command.add("--http-header");
            command.add("User-Agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            command.add("--http-header");
            command.add("Referer=https://chzzk.naver.com/");
            command.add("--http-header");
            command.add("Origin=https://chzzk.naver.com");
        }
        
        // FFmpeg 옵션 추가
        command.add("--ffmpeg-ffmpeg");
        command.add("ffmpeg");            // FFmpeg 경로 지정
        command.add("--ffmpeg-copyts");   // 타임스탬프 복사
        command.add("--ffmpeg-fout");     
        command.add("mp4");               // 출력 포맷 mp4로 지정
        
        // 다운로드 가속화 옵션 추가 (hls-segment-threads 대신 stream-segment-threads 사용)
        command.add("--stream-segment-threads");
        command.add(String.valueOf(this.maxThreads)); // 스레드 수 사용
        
        // 로그 레벨 설정
        command.add("--loglevel");
        command.add("info");             // 로그 레벨 (info만 표시)
        command.add("--retry-streams");
        command.add("1");                // 스트림 재시도 (1초마다)
        
        // 속도 제한 설정이 있는 경우
        if (this.speedLimit > 0) {
            command.add("--hls-segment-attempts");
            command.add("5");            // 다운로드 시도 횟수 증가
            command.add("--ringbuffer-size");
            command.add("32M");          // 링버퍼 크기 증가
        }
    }
    
    /**
     * yt-dlp 명령어 구성
     */
    private void buildYtDlpCommand(List<String> command, StreamerInfo info, File outputFile) {
        command.add("yt-dlp");
        
        // Chzzk URL 확인
        boolean isChzzkUrl = info.getUrl() != null && info.getUrl().contains("chzzk.naver.com");
        
        // 출력 파일 설정
        command.add("-o");
        command.add(outputFile.getAbsolutePath());
        
        // 품질 설정
        if (!"best".equals(info.getQuality())) {
            command.add("-f");
            command.add(info.getQuality());
        }
        
        // Chzzk 관련 설정
        if (isChzzkUrl) {
            command.add("--user-agent");
            command.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            command.add("--referer");
            command.add("https://chzzk.naver.com/");
            command.add("--add-header");
            command.add("Origin:https://chzzk.naver.com");
        }
        
        // 추가 옵션
        command.add("--live-from-now");   // 현재 시점부터 녹화 시작
        
        command.add("--no-part");          // 임시 파일 생성 안함
        command.add("--no-continue");      // 이어받기 비활성화
        command.add("--retry-sleep");      // 재시도 간격
        command.add("1");                  // 1초
        
        // 멀티스레드 다운로드 설정
        command.add("--concurrent-fragments");
        command.add(String.valueOf(this.maxThreads));
        
        // 로그 레벨 설정
        command.add("--quiet");            // 필요한 메시지만 출력
        command.add("--progress");         // 진행률 표시
        
        // 외부 다운로더 설정 (aria2 사용 가능한 경우)
        try {
            ProcessBuilder pb = new ProcessBuilder("aria2c", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("aria2c 발견: 다운로드 가속화에 사용합니다");
                command.add("--external-downloader");
                command.add("aria2c");
                command.add("--external-downloader-args");
                command.add("aria2c:-x" + this.maxThreads + " -s" + this.maxThreads + " -k1M");
            }
        } catch (Exception e) {
            // aria2c를 찾을 수 없음 - 무시
        }
        
        // 속도 제한 설정이 있는 경우
        if (this.speedLimit > 0) {
            command.add("--limit-rate");
            command.add(this.speedLimit + "K");
        }
        
        // URL 추가
        command.add(info.getUrl());
    }
    
    /**
     * FFmpeg 명령어 구성
     */
    private void buildFfmpegCommand(List<String> command, StreamerInfo info, File outputFile, boolean isChzzkUrl) {
        command.add("ffmpeg");
        
        // 추가 글로벌 옵션 (오류 무시 및 로깅 레벨 설정)
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");
        
        // 시스템 시간을 타임스탬프로 사용 (모든 파일에 적용)
        command.add("-use_wallclock_as_timestamps");
        command.add("true");
        
        // 특별한 HTTP 헤더 추가 (Chzzk 등의 스트리밍 서비스에 필요)
        if (isChzzkUrl) {
            command.add("-user_agent");
            command.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            command.add("-referer");
            command.add("https://chzzk.naver.com/");
            command.add("-headers");
            command.add("Origin: https://chzzk.naver.com");
        }
        
        // 재시도 옵션
        command.add("-reconnect");
        command.add("1");
        command.add("-reconnect_streamed");
        command.add("1");
        command.add("-reconnect_delay_max");
        command.add("5");
        
        // 입력 옵션 (HLS 스트림 처리용)
        if (isChzzkUrl) {
            // 치지직 전용 입력 옵션
            command.add("-fflags");
            command.add("+discardcorrupt+nobuffer+igndts");  // 손상된 패킷 무시, 버퍼링 없음, 타임스탬프 무시
            command.add("-flags");
            command.add("+low_delay");               // 저지연 처리
            command.add("-analyzeduration");
            command.add("1000000");                  // 분석 시간 제한 (마이크로초)
            command.add("-probesize");
            command.add("32");                       // 최소 프로브 크기
        } else {
            // 일반 스트림용 타임스탬프 옵션
            command.add("-copyts");                  // 타임스탬프 복사
            command.add("-start_at_zero");           // 시작 시간을 0으로 설정
        }
        
        // 입력 URL
        command.add("-i");
        command.add(info.getUrl());
        
        // 출력 설정 - 코덱 복사 (트랜스코딩 없음)
        command.add("-c");
        command.add("copy");
        
        // 세그먼트 처리 개선
        command.add("-max_muxing_queue_size");
        command.add("1024");                         // 큐 크기 증가
        
        if (isChzzkUrl) {
            // 치지직 전용 출력 옵션
            command.add("-fflags");
            command.add("+genpts");                  // 타임스탬프 생성
            command.add("-movflags");
            command.add("faststart+empty_moov");     // MP4 최적화
            command.add("-avoid_negative_ts");
            command.add("make_zero");                // 음수 타임스탬프 처리
            command.add("-map_metadata");
            command.add("-1");                       // 메타데이터 제거
        } else {
            // 일반 스트림용 옵션
            command.add("-use_wallclock_as_timestamps");
            command.add("1");                        // 시스템 시간을 타임스탬프로 사용
        }
        
        // 파일 포맷 설정
        command.add("-f");
        command.add("mp4");
        
        // 스트림 매핑 (모든 스트림 복사)
        command.add("-map");
        command.add("0");
        
        // 덮어쓰기 옵션
        command.add("-y");
        
        // 출력 파일
        command.add(outputFile.getAbsolutePath());
    }
    
    /**
     * 오버로딩된 메서드 (이전 버전 호환성 유지)
     */
    private void buildFfmpegCommand(List<String> command, StreamerInfo info, File outputFile) {
        buildFfmpegCommand(command, info, outputFile, false);
    }
    
    /**
     * 파일 경로 정규화 유틸리티 메서드
     */
    private String normalizeOutputPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return cachePath;
        }
        
        // 경로 정규화
        String normalized = path.replace('\\', '/'); // 모든 백슬래시를 슬래시로 변환
        if (!normalized.endsWith("/")) {
            normalized += "/"; // 경로 끝에 슬래시 추가
        }
        
        return normalized;
    }
    
    /**
     * 출력 디렉토리 생성 유틸리티 메서드
     */
    private File createOutputDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("출력 디렉토리 생성 실패: " + path);
                // 디렉토리 생성 실패 시 기본 캐시 디렉토리 사용
                dir = new File(cachePath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            }
        }
        return dir;
    }
    
    /**
     * 녹화 파일명 생성 유틸리티 메서드
     */
    private String createOutputFileName(StreamerInfo info) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = info.getStartTime().format(formatter);
        
        // 제목 정보가 없는 경우 "no_title" 대신 현재 시간을 추가로 사용
        String titlePart;
        if (info.getTitle() == null || info.getTitle().trim().isEmpty()) {
            titlePart = "stream_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        } else {
            // 제목에서 파일명으로 사용할 수 없는 문자 제거
            titlePart = info.getTitle().trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 특수문자 제거
                .replaceAll("\\s+", "_") // 공백을 언더스코어로 변경
                .replaceAll("[^a-zA-Z0-9가-힣_.-]", ""); // 영문, 숫자, 한글, 언더스코어, 점, 하이픈만 허용
        }
        
        // 제목 길이 제한
        if (titlePart.length() > 50) {
            titlePart = titlePart.substring(0, 50);
        } else if (titlePart.length() < 3) {
            // 너무 짧은 제목은 기본값 사용
            titlePart = "stream_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        }
        
        // 최종 파일명 생성 (타임스탬프_스트리머이름_제목.mp4)
        return String.format("%s_%s_%s.mp4", timestamp, info.getName(), titlePart);
    }
    
    /**
     * 녹화 시간을 형식화하는 유틸리티 메서드
     */
    private String formatRecordingDuration(long elapsedSeconds) {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * 비디오 메타데이터 수정 (치지직 녹화 파일의 재생 시간 문제 해결용)
     */
    private void fixVideoMetadata(File videoFile) {
        try {
            if (!videoFile.exists() || videoFile.length() == 0) {
                System.err.println("파일이 없거나 비어 있어 메타데이터 수정을 건너뜁니다: " + videoFile.getPath());
                return;
            }
            
            System.out.println("치지직 녹화 파일 메타데이터 수정 시작: " + videoFile.getName());
            
            // 원본 파일과 임시 파일 경로
            String originalPath = videoFile.getAbsolutePath();
            File tempFile = new File(originalPath + ".fixed.mp4");
            
            // FFmpeg를 사용하여 단순히 파일을 재묶기 (remux)
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-i");
            command.add(originalPath);
            
            // 코덱 복사 (재인코딩 없음)
            command.add("-c");
            command.add("copy");
            
            // MP4 메타데이터 수정 옵션
            command.add("-fflags");
            command.add("+genpts");                  // 타임스탬프 생성
            command.add("-movflags");
            command.add("faststart+empty_moov");     // MP4 최적화
            command.add("-map_metadata");
            command.add("-1");                       // 모든 메타데이터 제거
            
            // 덮어쓰기 및 출력
            command.add("-y");
            command.add(tempFile.getAbsolutePath());
            
            System.out.println("메타데이터 수정 명령어: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 출력 로그 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg: " + line);
            }
            
            int exitCode = process.waitFor();
            System.out.println("메타데이터 수정 작업 완료. 종료 코드: " + exitCode);
            
            // 파일 교체
            if (exitCode == 0 && tempFile.exists() && tempFile.length() > 0) {
                // 원본 파일 백업
                File backupFile = new File(originalPath + ".bak");
                if (videoFile.renameTo(backupFile)) {
                    // 새 파일을 원본 이름으로 변경
                    if (tempFile.renameTo(videoFile)) {
                        System.out.println("메타데이터 수정 완료: " + videoFile.getName());
                        // 백업 파일 삭제
                        backupFile.delete();
                    } else {
                        System.err.println("임시 파일 이름 변경 실패, 백업에서 복원합니다.");
                        backupFile.renameTo(videoFile);
                    }
                } else {
                    System.err.println("원본 파일 백업 실패, 임시 파일을 정리합니다.");
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }
            } else {
                System.err.println("메타데이터 수정 실패. 임시 파일 삭제");
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("메타데이터 수정 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 