# SRT - 스트리머 녹화 도구

SRT는 트위치, 유튜브, 아프리카TV 등 다양한 플랫폼의 스트리머 방송을 감시하고 자동으로 녹화하는 도구입니다.

## 기능

- 다양한 스트리밍 플랫폼 지원 (트위치, 유튜브, 아프리카TV 등)
- 스트리머 온라인 상태 자동 감지
- 자동 녹화 기능
- 웹 인터페이스를 통한 원격 제어
- 시스템 트레이에 최소화 가능

## 실행 방법

### 콘솔 없이 실행 (권장)

**처음 실행하는 경우**:
1. `build_fatjar.bat`을 실행하여 모든 종속성이 포함된 JAR 파일을 생성합니다.
2. 생성이 완료되면 `start_without_console.vbs` 파일을 더블클릭하여 실행합니다.

**또는 설정 캐시 오류를 우회하는 방법**:
1. `run_hidden.vbs` 파일을 더블클릭하여 실행합니다.

**바로가기 생성**:
1. `create_shortcut.bat`을 실행하여 바탕화면에 바로가기를 생성하고, 해당 바로가기를 사용합니다.

### 콘솔과 함께 실행

**설정 캐시 비활성화 방법(권장)**:
1. `run_no_config_cache.bat` 파일을 실행합니다. 이 방법은 설정 캐시 오류를 방지합니다.

**기존 방법**:
1. `run_jar.bat` 또는 `run_with_javafx.bat` 파일을 더블클릭하여 실행합니다.
2. 문제가 발생하는 경우 콘솔 창에 오류 메시지가 표시됩니다.

## 사전 요구사항

### 필수 설치 항목

- Java 17 이상
- 녹화를 위한 streamlink (선택사항이지만 권장)

### streamlink 설치 방법

1. `install_streamlink.bat` 파일을 실행하여 자동으로 설치합니다.
2. 또는 https://streamlink.github.io/install.html 에서 수동으로 설치할 수 있습니다.

## 자주 발생하는 문제 해결

### 빌드 오류: "Configuration cache problems found in this build"

1. `run_no_config_cache.bat` 파일을 사용하여 설정 캐시 없이 실행하세요.
2. 또는 `build_fatjar.bat`을 실행한 후 `start_without_console.vbs`로 실행하세요.

### 스트리머가 항상 오프라인으로 표시됩니다

1. streamlink가 설치되어 있는지 확인하세요.
2. URL이 올바른지 확인하세요. 예: https://twitch.tv/채널명

### 프로그램 실행 시 콘솔 창이 나타납니다

1. `start_without_console.vbs` 또는 `run_hidden.vbs` 파일을 사용하여 실행하세요.
2. 또는 바탕화면 바로가기를 사용하세요.

### 다른 문제가 발생하는 경우

아래 디렉토리에 생성된 로그 파일을 확인하세요:
- 캐시 폴더/logs/

## 개발자 정보

SRT는 JavaFX 및 streamlink를 활용하여 개발되었습니다. 