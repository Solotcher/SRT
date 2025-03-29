/*
 * SRT Streamer Recorder Tool
 */
package com.srt;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.scene.Node;

import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import com.srt.Recorder.StreamerInfo;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * SRT 메인 애플리케이션 클래스
 * 스트리머 녹화 도구의 진입점입니다.
 */
public class App extends Application {
    private Stage primaryStage;
    private FXTrayIcon trayIcon;
    private Recorder recorder;
    private WebServer webServer;
    private TableView<Recorder.StreamerInfo> streamerTable;
    private ObservableList<Recorder.StreamerInfo> streamerList;
    private Label statusLabel;
    
    // 애플리케이션 시작 전 코드페이지 설정
    static {
        try {
            // 인코딩 설정
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            
            // Windows에서 콘솔 코드 페이지 변경 (chcp 65001 - UTF-8)
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("cmd /c chcp 65001");
            }
            
            // 로깅 초기화
            Logger logger = Logger.getLogger("SRT");
            logger.setLevel(Level.INFO);
            
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new SimpleFormatter() {
                private static final String format = "[%1$tF %1$tT] [%2$s] %3$s %n";
                
                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format(format,
                            new Date(lr.getMillis()),
                            lr.getLevel().getLocalizedName(),
                            lr.getMessage()
                    );
                }
            });
            logger.addHandler(handler);
            
            // 애플리케이션 시작 로깅
            logger.info("애플리케이션을 시작합니다. 인코딩: " + System.getProperty("file.encoding"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        stage.setTitle("SRT - 스트리머 녹화 도구");
        
        System.out.println("SRT 애플리케이션 시작");
        
        // Recorder 인스턴스 생성 및 초기화
        recorder = new Recorder();
        recorder.initialize();
        
        // WebServer 인스턴스 생성 및 초기화
        webServer = new WebServer(recorder, recorder.getWebServerPort());
        webServer.initialize();
        
        // 웹서버 자동 시작 설정이 켜져 있으면 웹서버 시작
        if (recorder.isWebServerEnabled()) {
            webServer.start();
        }
        
        // Recorder 콜백 설정
        recorder.setCallback((name, status) -> {
            Platform.runLater(() -> {
                System.out.println("스트리머 상태 변경: " + name + " -> " + status);
                updateStreamerTableStatus(name, status);
                updateStatusBar();
            });
        });
        
        // 스트리머 목록 초기화
        streamerList = FXCollections.observableArrayList(recorder.getStreamers());
        System.out.println("초기 스트리머 수: " + streamerList.size());
        
        // 메인 레이아웃 설정
        BorderPane root = new BorderPane();
        
        // 상단 메뉴 바 생성
        MenuBar menuBar = createMenuBar();
        
        // 도구 모음 생성
        ToolBar toolBar = createToolBar();
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);
        
        // 스트리머 목록 테이블 생성
        streamerTable = createStreamerTable();
        root.setCenter(streamerTable);
        
        // 하단 상태 바 생성
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        // 씬 생성 및 스테이지에 설정
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        
        // 시스템 트레이 아이콘 설정
        setupTrayIcon(stage);
        
        // 윈도우 닫기 이벤트 설정 (트레이로 최소화)
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
        
        // 테이블 자동 갱신 타이머 설정 (5초마다)
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), event -> {
                System.out.println("테이블 UI 자동 갱신");
                Platform.runLater(() -> {
                    streamerTable.refresh();
                    updateStatusBar();
                });
            })
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
        
        // 시작 시 모든 스트리머 상태 수동 갱신 요청
        System.out.println("초기 스트리머 상태 갱신 요청");
        recorder.forceUpdateAllStreamers();
        
        stage.show();
        
        // 초기 상태 바 업데이트
        updateStatusBar();
    }
    
    /**
     * 메뉴바 생성 메서드
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // 파일 메뉴
        Menu fileMenu = new Menu("파일");
        MenuItem exitItem = new MenuItem("종료");
        exitItem.setOnAction(e -> {
            recorder.stopAllRecordings();
            boolean saved = recorder.saveSettings();
            if (!saved) {
                System.err.println("종료 전 설정 저장 중 오류가 발생했습니다.");
            }
            webServer.stop();
            Platform.exit();
            System.exit(0);
        });
        fileMenu.getItems().add(exitItem);
        
        // 스트리머 메뉴
        Menu streamerMenu = new Menu("스트리머");
        MenuItem addItem = new MenuItem("스트리머 추가");
        addItem.setOnAction(e -> showAddStreamerDialog());
        
        MenuItem refreshStatusItem = new MenuItem("상태 새로고침");
        refreshStatusItem.setOnAction(e -> {
            recorder.forceUpdateAllStreamers();
            streamerTable.refresh();
            updateStatusBar();
        });
        
        MenuItem startAllItem = new MenuItem("전체 녹화 시작");
        startAllItem.setOnAction(e -> recorder.startAllRecordings());
        
        MenuItem stopAllItem = new MenuItem("전체 녹화 중단");
        stopAllItem.setOnAction(e -> recorder.stopAllRecordings());
        
        streamerMenu.getItems().addAll(addItem, refreshStatusItem, new SeparatorMenuItem(), startAllItem, stopAllItem);
        
        // 설정 메뉴
        Menu settingsMenu = new Menu("설정");
        MenuItem preferencesItem = new MenuItem("환경설정");
        preferencesItem.setOnAction(e -> showSettingsDialog());
        
        CheckMenuItem autoRecordItem = new CheckMenuItem("자동 녹화");
        autoRecordItem.setSelected(recorder.isAutoRecordEnabled());
        autoRecordItem.setOnAction(e -> {
            recorder.setAutoRecordEnabled(autoRecordItem.isSelected());
            recorder.saveSettings();
        });
        
        // 웹 서버 메뉴
        Menu webServerMenu = new Menu("웹 서버");
        CheckMenuItem webServerEnabledItem = new CheckMenuItem("웹 서버 활성화");
        webServerEnabledItem.setSelected(recorder.isWebServerEnabled());
        webServerEnabledItem.setOnAction(e -> {
            recorder.setWebServerEnabled(webServerEnabledItem.isSelected());
            
            if (webServerEnabledItem.isSelected()) {
                if (!webServer.isRunning()) {
                    webServer.start();
                }
            } else {
                if (webServer.isRunning()) {
                    webServer.stop();
                }
            }
            
            recorder.saveSettings();
            updateStatusBar();
        });
        
        MenuItem openWebPageItem = new MenuItem("웹 페이지 열기");
        openWebPageItem.setOnAction(e -> {
            if (webServer.isRunning()) {
                openWebPage("http://localhost:" + webServer.getPort());
            } else {
                showAlert("웹 서버 비활성화", "웹 서버가 실행 중이지 않습니다.");
            }
        });
        
        webServerMenu.getItems().addAll(webServerEnabledItem, openWebPageItem);
        
        settingsMenu.getItems().addAll(preferencesItem, autoRecordItem, new SeparatorMenuItem(), webServerMenu);
        
        menuBar.getMenus().addAll(fileMenu, streamerMenu, settingsMenu);
        return menuBar;
    }
    
    /**
     * 웹 페이지 열기
     */
    private void openWebPage(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            showAlert("웹 페이지 열기 실패", "웹 페이지를 열지 못했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 도구 모음 생성 메서드
     */
    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        
        Button addButton = new Button("스트리머 추가");
        addButton.setOnAction(e -> showAddStreamerDialog());
        
        Button startAllButton = new Button("전체 녹화 시작");
        startAllButton.setOnAction(e -> recorder.startAllRecordings());
        
        Button stopAllButton = new Button("전체 녹화 중단");
        stopAllButton.setOnAction(e -> recorder.stopAllRecordings());
        
        Button settingsButton = new Button("환경설정");
        settingsButton.setOnAction(e -> showSettingsDialog());
        
        CheckBox autoRecordCheckBox = new CheckBox("자동 녹화");
        autoRecordCheckBox.setSelected(recorder.isAutoRecordEnabled());
        autoRecordCheckBox.setOnAction(e -> {
            recorder.setAutoRecordEnabled(autoRecordCheckBox.isSelected());
            recorder.saveSettings();
        });
        
        Button openWebButton = new Button("웹 페이지");
        openWebButton.setOnAction(e -> {
            if (webServer.isRunning()) {
                openWebPage("http://localhost:" + webServer.getPort());
            } else {
                showAlert("웹 서버 비활성화", "웹 서버가 실행 중이지 않습니다.");
            }
        });
        
        // 새로고침 버튼 추가
        Button refreshButton = new Button("상태 새로고침");
        refreshButton.setOnAction(e -> {
            // 상태 수동 업데이트
            recorder.forceUpdateAllStreamers();
            // UI 새로고침
            streamerTable.refresh();
            updateStatusBar();
        });
        
        toolBar.getItems().addAll(
            addButton, 
            new Separator(), 
            startAllButton, 
            stopAllButton, 
            new Separator(), 
            settingsButton,
            new Separator(),
            autoRecordCheckBox,
            new Separator(),
            openWebButton,
            new Separator(),
            refreshButton
        );
        
        return toolBar;
    }
    
    /**
     * 스트리머 목록 테이블 생성 메서드
     */
    private TableView<Recorder.StreamerInfo> createStreamerTable() {
        TableView<Recorder.StreamerInfo> table = new TableView<>();
        
        // 컬럼 생성
        TableColumn<Recorder.StreamerInfo, String> nameColumn = new TableColumn<>("스트리머명");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<Recorder.StreamerInfo, String> titleColumn = new TableColumn<>("방송제목");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(300);
        
        TableColumn<Recorder.StreamerInfo, String> statusColumn = new TableColumn<>("녹화상태");
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String displayStatus = Recorder.getStatusDisplayText(status);
            return new SimpleStringProperty(displayStatus);
        });
        statusColumn.setCellFactory(column -> {
            return new TableCell<Recorder.StreamerInfo, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        
                        if ("온라인".equals(item)) {
                            setStyle("-fx-text-fill: green;");
                        } else if ("녹화중".equals(item)) {
                            setStyle("-fx-text-fill: red;");
                        } else if ("오류".equals(item)) {
                            setStyle("-fx-text-fill: orange;");
                        } else if ("확인중".equals(item)) {
                            setStyle("-fx-text-fill: blue;");
                        } else {
                            setStyle("-fx-text-fill: gray;");
                        }
                    }
                }
            };
        });
        statusColumn.setPrefWidth(100);
        
        TableColumn<Recorder.StreamerInfo, String> durationColumn = new TableColumn<>("녹화시간");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationColumn.setPrefWidth(100);
        
        // 작업 버튼 컬럼
        TableColumn<Recorder.StreamerInfo, Void> actionColumn = new TableColumn<>("작업");
        actionColumn.setPrefWidth(150);
        
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button startButton = new Button("시작");
            private final Button stopButton = new Button("중지");
            private final Button removeButton = new Button("삭제");
            
            {
                startButton.setOnAction(event -> {
                    Recorder.StreamerInfo info = getTableView().getItems().get(getIndex());
                    recorder.startRecording(info.getName());
                });
                
                stopButton.setOnAction(event -> {
                    Recorder.StreamerInfo info = getTableView().getItems().get(getIndex());
                    recorder.stopRecording(info.getName());
                });
                
                removeButton.setOnAction(event -> {
                    Recorder.StreamerInfo info = getTableView().getItems().get(getIndex());
                    removeStreamer(info.getName());
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, startButton, stopButton, removeButton);
                    setGraphic(buttons);
                    
                    Recorder.StreamerInfo info = getTableView().getItems().get(getIndex());
                    
                    // 버튼 활성화/비활성화 설정
                    if (info.isRecording()) {
                        startButton.setDisable(true);
                        stopButton.setDisable(false);
                    } else {
                        startButton.setDisable("offline".equals(info.getStatus()));
                        stopButton.setDisable(true);
                    }
                }
            }
        });
        
        table.getColumns().addAll(nameColumn, titleColumn, statusColumn, durationColumn, actionColumn);
        table.setItems(streamerList);
        
        return table;
    }
    
    /**
     * 상태 바 생성 메서드
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        
        statusLabel = new Label("준비됨");
        Label streamersLabel = new Label("스트리머: 0명");
        Label recordingLabel = new Label("녹화중: 0명");
        Label webServerLabel = new Label("웹 서버: 비활성화");
        
        statusBar.getChildren().addAll(
            statusLabel, 
            new Separator(javafx.geometry.Orientation.VERTICAL), 
            streamersLabel, 
            new Separator(javafx.geometry.Orientation.VERTICAL), 
            recordingLabel,
            new Separator(javafx.geometry.Orientation.VERTICAL),
            webServerLabel
        );
        
        return statusBar;
    }
    
    /**
     * 상태 바 업데이트 메서드
     */
    private void updateStatusBar() {
        if (statusLabel == null) return;
        
        List<Recorder.StreamerInfo> streamers = recorder.getStreamers();
        int totalStreamers = streamers.size();
        int recordingStreamers = 0;
        int onlineStreamers = 0;
        
        for (Recorder.StreamerInfo info : streamers) {
            if (info.isRecording()) {
                recordingStreamers++;
            }
            if ("online".equals(info.getStatus())) {
                onlineStreamers++;
            }
        }
        
        HBox statusBar = (HBox) statusLabel.getParent();
        if (statusBar != null) {
            // 상태 레이블 텍스트 업데이트
            statusLabel.setText("준비됨");
            
            // 스트리머 수 업데이트
            Label streamersLabel = (Label) statusBar.getChildren().get(2);
            streamersLabel.setText("스트리머: " + totalStreamers + "명");
            
            // 녹화중 수 업데이트
            Label recordingLabel = (Label) statusBar.getChildren().get(4);
            recordingLabel.setText("녹화중: " + recordingStreamers + "명");
            
            // 웹 서버 상태 업데이트
            Label webServerLabel = (Label) statusBar.getChildren().get(6);
            if (webServer.isRunning()) {
                webServerLabel.setText("웹 서버: 활성화 (포트 " + webServer.getPort() + ")");
            } else {
                webServerLabel.setText("웹 서버: 비활성화");
            }
        }
    }
    
    /**
     * 시스템 트레이 아이콘 설정 메서드
     */
    private void setupTrayIcon(Stage stage) {
        try {
            // 아이콘 파일이 없는 경우 기본 아이콘 사용
            trayIcon = new FXTrayIcon(stage);
            
            // 트레이 메뉴 아이템 생성
            MenuItem showItem = new MenuItem("보이기");
            showItem.setOnAction(e -> stage.show());
            
            MenuItem settingsItem = new MenuItem("환경설정");
            settingsItem.setOnAction(e -> {
                stage.show();
                showSettingsDialog();
            });
            
            // 웹 서버 메뉴 아이템
            CheckMenuItem webServerItem = new CheckMenuItem("웹 서버 활성화");
            webServerItem.setSelected(recorder.isWebServerEnabled());
            webServerItem.setOnAction(e -> {
                recorder.setWebServerEnabled(webServerItem.isSelected());
                
                if (webServerItem.isSelected()) {
                    if (!webServer.isRunning()) {
                        webServer.start();
                    }
                } else {
                    if (webServer.isRunning()) {
                        webServer.stop();
                    }
                }
                
                recorder.saveSettings();
                updateStatusBar();
            });
            
            MenuItem openWebItem = new MenuItem("웹 페이지 열기");
            openWebItem.setOnAction(e -> {
                if (webServer.isRunning()) {
                    openWebPage("http://localhost:" + webServer.getPort());
                } else {
                    showAlert("웹 서버 비활성화", "웹 서버가 실행 중이지 않습니다.");
                }
            });
            
            MenuItem exitItem = new MenuItem("종료");
            exitItem.setOnAction(e -> {
                recorder.stopAllRecordings();
                boolean saved = recorder.saveSettings();
                if (!saved) {
                    System.err.println("종료 전 설정 저장 중 오류가 발생했습니다.");
                }
                webServer.stop();
                Platform.exit();
                System.exit(0);
            });
            
            // 트레이 메뉴에 아이템 추가
            trayIcon.addMenuItem(showItem);
            trayIcon.addMenuItem(settingsItem);
            trayIcon.addSeparator();
            trayIcon.addMenuItem(webServerItem);
            trayIcon.addMenuItem(openWebItem);
            trayIcon.addSeparator();
            trayIcon.addMenuItem(exitItem);
            
            trayIcon.show();
        } catch (Exception e) {
            System.err.println("시스템 트레이 초기화 오류: " + e.getMessage());
        }
    }
    
    /**
     * 스트리머 추가 대화상자 표시 메서드
     */
    private void showAddStreamerDialog() {
        Dialog<Recorder.StreamerInfo> dialog = new Dialog<>();
        dialog.setTitle("스트리머 추가");
        dialog.setHeaderText("새 스트리머 정보를 입력하세요");
        
        // 버튼 설정
        ButtonType addButtonType = new ButtonType("추가", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // 다이얼로그 내용 생성
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("스트리머 이름");
        
        TextField urlField = new TextField();
        urlField.setPromptText("방송 URL");
        
        // 녹화 품질 선택 콤보박스
        ComboBox<String> qualityComboBox = new ComboBox<>();
        qualityComboBox.getItems().addAll("best", "1080p", "720p", "480p", "360p", "worst");
        qualityComboBox.setValue("best");
        qualityComboBox.setEditable(true);
        
        // 저장 경로 설정
        TextField pathField = new TextField();
        pathField.setPromptText("저장 위치 (비워두면 기본 경로 사용)");
        pathField.setText(recorder.getCachePath());
        
        Button browseButton = new Button("찾아보기");
        browseButton.setOnAction(e -> {
            javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
            directoryChooser.setTitle("녹화 파일 저장 위치 선택");
            File initialDirectory = new File(pathField.getText());
            if (initialDirectory.exists()) {
                directoryChooser.setInitialDirectory(initialDirectory);
            }
            File selectedDirectory = directoryChooser.showDialog(dialog.getDialogPane().getScene().getWindow());
            if (selectedDirectory != null) {
                pathField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        
        HBox pathBox = new HBox(5, pathField, browseButton);
        pathBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(pathField, javafx.scene.layout.Priority.ALWAYS);
        
        grid.add(new Label("스트리머 이름:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("방송 URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("녹화 품질:"), 0, 2);
        grid.add(qualityComboBox, 1, 2);
        grid.add(new Label("저장 위치:"), 0, 3);
        grid.add(pathBox, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // 기본 포커스 설정
        Platform.runLater(nameField::requestFocus);
        
        // 추가 버튼 활성화 조건 설정
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // 이름과 URL 필드가 비어있지 않을 때만 추가 버튼 활성화
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty() || urlField.getText().trim().isEmpty());
        });
        
        urlField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty() || nameField.getText().trim().isEmpty());
        });
        
        // 결과 컨버터 설정
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String streamerName = nameField.getText().trim();
                String streamerUrl = urlField.getText().trim();
                String quality = qualityComboBox.getValue();
                String outputPath = pathField.getText().trim();
                
                Recorder.StreamerInfo info = new Recorder.StreamerInfo();
                info.setName(streamerName);
                info.setUrl(streamerUrl);
                info.setQuality(quality);
                info.setOutputPath(outputPath);
                return info;
            }
            return null;
        });
        
        // 대화상자 표시 및 결과 처리
        Optional<Recorder.StreamerInfo> result = dialog.showAndWait();
        
        result.ifPresent(info -> {
            boolean success = recorder.addStreamer(
                info.getName(), 
                info.getUrl(), 
                info.getQuality(), 
                info.getOutputPath()
            );
            
            if (success) {
                // 스트리머 목록 업데이트
                updateStreamerList();
                
                // UI 새로고침
                streamerTable.refresh();
            } else {
                showAlert(Alert.AlertType.ERROR, "추가 실패", "스트리머 추가에 실패했습니다.", "이미 존재하는 이름이거나 올바르지 않은 URL입니다.");
            }
        });
    }
    
    /**
     * 스트리머 제거 메서드
     */
    private void removeStreamer(String name) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("스트리머 삭제");
        alert.setHeaderText(name + " 스트리머를 삭제하시겠습니까?");
        alert.setContentText("이 작업은 되돌릴 수 없습니다.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (recorder.removeStreamer(name)) {
                // 테이블에서 제거
                streamerList.removeIf(info -> info.getName().equals(name));
                recorder.saveSettings();
                updateStatusBar();
            } else {
                showAlert("스트리머 삭제 실패", "스트리머 삭제에 실패했습니다.");
            }
        }
    }
    
    /**
     * 설정 대화상자 표시 메서드
     */
    private void showSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(recorder);
        settingsDialog.initOwner(primaryStage);
        settingsDialog.initModality(Modality.APPLICATION_MODAL);
        settingsDialog.showAndWait();
        
        // 설정 변경 후 웹 서버 상태 업데이트
        if (recorder.isWebServerEnabled()) {
            if (!webServer.isRunning()) {
                webServer.setPort(recorder.getWebServerPort());
                webServer.start();
            } else if (webServer.getPort() != recorder.getWebServerPort()) {
                webServer.stop();
                webServer.setPort(recorder.getWebServerPort());
                webServer.start();
            }
        } else {
            if (webServer.isRunning()) {
                webServer.stop();
            }
        }
        
        updateStatusBar();
    }
    
    /**
     * 스트리머 목록 업데이트 메서드
     */
    private void updateStreamerList() {
        // 이전 스트리머 목록 백업
        List<String> selectedStreamers = new ArrayList<>();
        if (streamerTable.getSelectionModel().getSelectedItem() != null) {
            selectedStreamers.add(streamerTable.getSelectionModel().getSelectedItem().getName());
        }
        
        // 목록 갱신
        List<Recorder.StreamerInfo> newStreamers = recorder.getStreamers();
        
        Platform.runLater(() -> {
            streamerList.clear();
            streamerList.addAll(newStreamers);
            
            // UI 갱신
            streamerTable.refresh();
            
            // 선택된 항목 복원
            if (!selectedStreamers.isEmpty()) {
                for (int i = 0; i < streamerList.size(); i++) {
                    if (selectedStreamers.contains(streamerList.get(i).getName())) {
                        streamerTable.getSelectionModel().select(i);
                        streamerTable.scrollTo(i);
                        break;
                    }
                }
            }
            
            updateStatusBar();
        });
        
        // 설정 저장
        recorder.saveSettings();
    }
    
    /**
     * 경고 대화상자 표시 메서드
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * 경고 대화상자 표시 메서드 (유형 지정)
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * 스트리머 테이블 상태 업데이트 메서드
     */
    private void updateStreamerTableStatus(String name, String status) {
        Platform.runLater(() -> {
            for (Recorder.StreamerInfo info : streamerList) {
                if (info.getName().equals(name)) {
                    info.setStatus(status);
                    streamerTable.refresh();
                    
                    // 상태바도 함께 업데이트
                    updateStatusBar();
                    break;
                }
            }
        });
    }
    
    @Override
    public void stop() {
        System.out.println("애플리케이션 종료 중...");
        
        // 먼저 설정을 저장
        System.out.println("설정 저장 중...");
        boolean saved = recorder.saveSettings();
        if (saved) {
            System.out.println("설정이 성공적으로 저장되었습니다.");
        } else {
            System.err.println("설정 저장 중 오류가 발생했습니다.");
        }
        
        // 모든 녹화 중지
        System.out.println("모든 녹화 중지 중...");
        recorder.stopAllRecordings();
        
        // 레코더 및 웹서버 종료
        System.out.println("레코더 종료 중...");
        recorder.shutdown();
        
        System.out.println("웹서버 종료 중...");
        webServer.stop();
        
        System.out.println("애플리케이션 종료 완료");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
