package com.srt;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * 설정 대화상자 클래스
 */
public class SettingsDialog extends Dialog<Void> {
    private final Recorder recorder;
    private TextField cachePathField;
    private CheckBox webServerEnabledCheckbox;
    private TextField webServerPortField;
    private TextField naverIdField;
    private PasswordField naverPwField;
    private TextField nidSesField;
    private TextField nidAutField;
    private Spinner<Integer> maxThreadsSpinner;
    private Spinner<Integer> speedLimitSpinner;
    private ComboBox<String> recorderProgramComboBox;
    
    public SettingsDialog(Recorder recorder) {
        this.recorder = recorder;
        
        setTitle("환경설정");
        setHeaderText("SRT 스트리머 녹화 도구 설정");
        
        // 다이얼로그 내용 생성
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 일반 설정 탭
        Tab generalTab = new Tab("일반");
        generalTab.setContent(createGeneralSettingsPanel());
        
        // 웹 서버 설정 탭
        Tab webServerTab = new Tab("웹 서버");
        webServerTab.setContent(createWebServerSettingsPanel());
        
        // 기타 설정 탭
        Tab miscTab = new Tab("기타");
        miscTab.setContent(createMiscSettingsPanel());
        
        tabPane.getTabs().addAll(generalTab, webServerTab, miscTab);
        
        getDialogPane().setContent(tabPane);
        
        // 버튼 설정
        ButtonType saveButtonType = new ButtonType("저장", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // 저장 버튼 클릭 이벤트 처리
        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                saveSettings();
            }
            return null;
        });
        
        // 초기 값 설정
        initializeFields();
    }
    
    /**
     * 일반 설정 패널 생성
     */
    private VBox createGeneralSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // 캐시 경로 설정
        Label cachePathLabel = new Label("캐시 경로:");
        cachePathField = new TextField();
        Button browseCacheButton = new Button("찾아보기...");
        
        browseCacheButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("캐시 경로 선택");
            
            if (cachePathField.getText() != null && !cachePathField.getText().isEmpty()) {
                File initialDir = new File(cachePathField.getText());
                if (initialDir.exists()) {
                    directoryChooser.setInitialDirectory(initialDir);
                }
            }
            
            File selectedDir = directoryChooser.showDialog(getDialogPane().getScene().getWindow());
            if (selectedDir != null) {
                cachePathField.setText(selectedDir.getAbsolutePath());
            }
        });
        
        HBox cachePathBox = new HBox(5, cachePathField, browseCacheButton);
        cachePathBox.setHgrow(cachePathField, javafx.scene.layout.Priority.ALWAYS);
        
        // 녹화 프로그램 선택
        Label recorderProgramLabel = new Label("녹화 프로그램:");
        recorderProgramComboBox = new ComboBox<>();
        recorderProgramComboBox.getItems().addAll("streamlink", "yt-dlp", "ffmpeg");
        recorderProgramComboBox.setPromptText("녹화 프로그램 선택");
        
        // 최대 스레드 설정
        Label maxThreadsLabel = new Label("최대 다운로드 스레드:");
        maxThreadsSpinner = new Spinner<>(1, 24, 4);
        maxThreadsSpinner.setEditable(true);
        maxThreadsSpinner.setPrefWidth(80);
        
        // 속도 제한 설정
        Label speedLimitLabel = new Label("속도 제한 (KB/s, 0=무제한):");
        speedLimitSpinner = new Spinner<>(0, 100000, 0);
        speedLimitSpinner.setEditable(true);
        speedLimitSpinner.setPrefWidth(100);
        
        // 그리드에 컨트롤 배치
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        
        gridPane.add(cachePathLabel, 0, 0);
        gridPane.add(cachePathBox, 1, 0);
        
        gridPane.add(recorderProgramLabel, 0, 1);
        gridPane.add(recorderProgramComboBox, 1, 1);
        
        gridPane.add(maxThreadsLabel, 0, 2);
        gridPane.add(maxThreadsSpinner, 1, 2);
        
        gridPane.add(speedLimitLabel, 0, 3);
        gridPane.add(speedLimitSpinner, 1, 3);
        
        panel.getChildren().add(gridPane);
        
        return panel;
    }
    
    /**
     * 웹 서버 설정 패널 생성
     */
    private VBox createWebServerSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // 웹 서버 활성화 여부
        webServerEnabledCheckbox = new CheckBox("웹 서버 활성화");
        
        // 웹 서버 포트
        Label portLabel = new Label("웹 서버 포트:");
        webServerPortField = new TextField();
        webServerPortField.setPromptText("8080");
        webServerPortField.setPrefWidth(100);
        
        // 웹 서버 활성화 체크박스에 따른 포트 필드 활성화/비활성화
        webServerEnabledCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            webServerPortField.setDisable(!newVal);
        });
        
        // 그리드에 컨트롤 배치
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        
        gridPane.add(webServerEnabledCheckbox, 0, 0, 2, 1);
        gridPane.add(portLabel, 0, 1);
        gridPane.add(webServerPortField, 1, 1);
        
        panel.getChildren().add(gridPane);
        
        // 구분선
        Separator separator = new Separator();
        panel.getChildren().add(separator);
        
        // 네이버 인증 정보
        Label naverLabel = new Label("네이버 인증 정보");
        naverLabel.setStyle("-fx-font-weight: bold");
        
        Label naverIdLabel = new Label("네이버 ID:");
        naverIdField = new TextField();
        
        Label naverPwLabel = new Label("네이버 비밀번호:");
        naverPwField = new PasswordField();
        
        Label nidSesLabel = new Label("NID_SES:");
        nidSesField = new TextField();
        
        Label nidAutLabel = new Label("NID_AUT:");
        nidAutField = new TextField();
        
        GridPane naverGrid = new GridPane();
        naverGrid.setHgap(10);
        naverGrid.setVgap(10);
        
        naverGrid.add(naverLabel, 0, 0, 2, 1);
        naverGrid.add(naverIdLabel, 0, 1);
        naverGrid.add(naverIdField, 1, 1);
        naverGrid.add(naverPwLabel, 0, 2);
        naverGrid.add(naverPwField, 1, 2);
        naverGrid.add(nidSesLabel, 0, 3);
        naverGrid.add(nidSesField, 1, 3);
        naverGrid.add(nidAutLabel, 0, 4);
        naverGrid.add(nidAutField, 1, 4);
        
        panel.getChildren().add(naverGrid);
        
        return panel;
    }
    
    /**
     * 기타 설정 패널 생성
     */
    private VBox createMiscSettingsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        
        // 프로그램 정보
        Label aboutLabel = new Label("SRT 스트리머 녹화 도구");
        aboutLabel.setStyle("-fx-font-weight: bold");
        
        Label versionLabel = new Label("버전: 1.0.0");
        Label authorLabel = new Label("제작자: SRT Team");
        
        panel.getChildren().addAll(aboutLabel, versionLabel, authorLabel);
        
        return panel;
    }
    
    /**
     * 설정 필드 초기화
     */
    private void initializeFields() {
        Platform.runLater(() -> {
            // 캐시 경로
            cachePathField.setText(recorder.getCachePath());
            
            // 녹화 프로그램
            recorderProgramComboBox.setValue(recorder.getRecorderProgram());
            
            // 웹 서버 설정
            webServerEnabledCheckbox.setSelected(recorder.isWebServerEnabled());
            webServerPortField.setText(String.valueOf(recorder.getWebServerPort()));
            webServerPortField.setDisable(!recorder.isWebServerEnabled());
            
            // 네이버 인증 정보
            naverIdField.setText(recorder.getNaverId());
            naverPwField.setText(recorder.getNaverPw());
            nidSesField.setText(recorder.getNidSes());
            nidAutField.setText(recorder.getNidAut());
            
            // 기타 설정
            maxThreadsSpinner.getValueFactory().setValue(recorder.getMaxThreads());
            speedLimitSpinner.getValueFactory().setValue(recorder.getSpeedLimit());
        });
    }
    
    /**
     * 설정 저장
     */
    private void saveSettings() {
        try {
            // 캐시 경로
            recorder.setCachePath(cachePathField.getText());
            
            // 녹화 프로그램
            recorder.setRecorderProgram(recorderProgramComboBox.getValue());
            
            // 웹 서버 설정
            recorder.setWebServerEnabled(webServerEnabledCheckbox.isSelected());
            
            try {
                int port = Integer.parseInt(webServerPortField.getText());
                if (port > 0 && port < 65536) {
                    recorder.setWebServerPort(port);
                }
            } catch (NumberFormatException e) {
                // 포트 번호가 올바르지 않을 경우 기본값 사용
            }
            
            // 네이버 인증 정보
            recorder.setNaverId(naverIdField.getText());
            recorder.setNaverPw(naverPwField.getText());
            recorder.setNidSes(nidSesField.getText());
            recorder.setNidAut(nidAutField.getText());
            
            // 기타 설정
            recorder.setMaxThreads(maxThreadsSpinner.getValue());
            recorder.setSpeedLimit(speedLimitSpinner.getValue());
            
            // 설정 저장
            recorder.saveSettings();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("오류");
            alert.setHeaderText("설정 저장 중 오류가 발생했습니다.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
} 