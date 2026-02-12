# Colored Bracket Guides — IntelliJ Plugin 프로젝트 계획서

## 1. 개요

### 1.1 프로젝트명
**colored-bracket-guides**

### 1.2 목적
IntelliJ Platform 기반 IDE에서 VS Code의 "Bracket Pair Guides" 기능을 구현한다. Bracket pair에 연결되는 수직 indent guide line과 수평 guide line에 nesting depth 기반 색상을 적용하여 코드 구조의 가독성을 높인다.

### 1.3 핵심 차별점
| 기존 플러그인 | 제공 기능 | 미제공 기능 |
|---|---|---|
| Rainbow Brackets | bracket 자체 색상화, rainbow indent guide (유료) | horizontal guide 없음 |
| highlight-bracket-pair | 커서 위치 bracket pair 하이라이트 | guide line 색상화 없음, horizontal guide 없음 |
| brackets | bracket 색상화 (3단계) | guide line 없음 |

**colored-bracket-guides의 차별점:**
- Bracket pair 기반 **수직 indent guide line 색상화** (무료)
- **Horizontal guide line** 렌더링 (IntelliJ 생태계에서 유일)
- Active scope 강조 (현재 커서 위치의 scope guide만 밝게 표시)
- 경량 설계 — guide line에 집중, bracket 자체 색상화는 후순위

### 1.4 라이선스
**GPL v3.0**

- 참고 코드인 `highlight-bracket-pair` (AprilViolet)의 원본 `HighlightBracketPair` (qeesung)은 Apache 2.0 라이선스
- Apache 2.0 → GPL v3.0 방향의 라이선스 전환은 합법 (GPL v3는 Apache 2.0과 호환)
- JetBrains Marketplace에서 GPL v3 플러그인 배포에 제약 없음 (JetBrains 공식 확인)

---

## 2. 기능 요구사항

### 2.1 Phase 1 — MVP (수직 guide + 수평 guide)

#### 2.1.1 수직 Bracket Guide Line
- Bracket pair의 opening bracket에서 closing bracket까지 수직선을 렌더링
- Nesting depth에 따라 서로 다른 색상 적용 (기본 6색 순환)
- 현재 커서가 위치한 scope의 guide line을 **active** 상태로 강조 (더 밝거나 두꺼운 선)
- 비활성 scope의 guide line은 dimmed 처리

#### 2.1.2 Horizontal Bracket Guide Line
- Opening bracket과 closing bracket 위치에서 수평선을 렌더링
- 수직 guide line과 동일한 depth 색상 적용
- Active scope에서만 수평선 표시 (VS Code의 `bracketPairHorizontal: active` 동작과 동일)

#### 2.1.3 설정
- Guide line 활성화/비활성화 토글
- 수직/수평 guide 개별 토글
- Active scope만 표시 vs 전체 표시 모드 선택
- 색상 커스터마이징 (Settings > Editor > Color Scheme > Colored Bracket Guides)

#### 2.1.4 설정 영속화 및 Import/Export
- 모든 설정값은 IDE 재시작 후에도 유지 (IntelliJ `PersistentStateComponent` 활용)
- 설정값을 JSON 파일로 **Export** 하는 기능
- JSON 파일에서 설정값을 **Import** 하는 기능
- 팀 내 설정 공유 또는 백업 용도
- IDE의 `Settings Repository` / `Settings Sync`와 자동 호환 (XML 기반 state 저장)
- **기본 프리셋 제공:** Default Dark, Default Light (사용자가 덮어쓰기 가능)
- 설정 변경 시 에디터에 **실시간 반영** (IDE 재시작 불필요)

**설정 항목 전체 목록:**

| 카테고리 | 항목 | 타입 | 기본값 |
|---|---|---|---|
| 일반 | 플러그인 활성화 | boolean | true |
| 일반 | 수직 guide 활성화 | boolean | true |
| 일반 | 수평 guide 활성화 | boolean | true |
| 일반 | 표시 모드 | enum | ACTIVE_ONLY |
| 일반 | 선 두께 (px) | int | 1 |
| 일반 | Active 선 두께 (px) | int | 2 |
| 일반 | Active 선 스타일 | enum | SOLID |
| 일반 | 비활성 선 투명도 (0.0~1.0) | float | 0.3 |
| 색상 | Depth 1 색상 | Color (hex) | #FFD700 |
| 색상 | Depth 2 색상 | Color (hex) | #DA70D6 |
| 색상 | Depth 3 색상 | Color (hex) | #179FFF |
| 색상 | Depth 4 색상 | Color (hex) | #00CC7A |
| 색상 | Depth 5 색상 | Color (hex) | #FF6B6B |
| 색상 | Depth 6 색상 | Color (hex) | #CC8833 |
| 색상 | 색상 순환 | boolean | true |
| 고급 | CaretListener debounce (ms) | int | 50 |
| 고급 | 최대 nesting depth 표시 | int | 30 |

### 2.2 Phase 2 — 확장 기능 (후속)
- Bracket 자체 rainbow 색상화
- Gutter 영역 bracket depth 표시
- 언어별 bracket 정의 커스터마이징
- 색상 프리셋 (VS Code 호환 색상 등)

---

## 3. 기술 설계

### 3.1 아키텍처

```
┌─────────────────────────────────────┐
│          IntelliJ Platform          │
│                                     │
│  ┌─────────────┐  ┌──────────────┐  │
│  │  PSI Tree   │  │   Lexer /    │  │
│  │  (증분 파싱) │  │ BraceMatcher │  │
│  └──────┬──────┘  └──────┬───────┘  │
│         │                │          │
│  ┌──────▼────────────────▼───────┐  │
│  │   BracketPairAnalyzer         │  │
│  │   - bracket pair 탐색         │  │
│  │   - nesting depth 계산        │  │
│  │   - 결과 캐싱                 │  │
│  └──────────────┬────────────────┘  │
│                 │                    │
│  ┌──────────────▼────────────────┐  │
│  │   GuideLineRenderer           │  │
│  │   (TextEditorHighlightingPass)│  │
│  │   - 수직 guide line 렌더링    │  │
│  │   - 수평 guide line 렌더링    │  │
│  │   - active scope 강조         │  │
│  └──────────────┬────────────────┘  │
│                 │                    │
│  ┌──────────────▼────────────────┐  │
│  │   Settings / Color Scheme     │  │
│  │   - 색상 설정                 │  │
│  │   - on/off 토글               │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### 3.2 핵심 컴포넌트

#### 3.2.1 BracketPairAnalyzer
- **역할:** 문서 내 bracket pair를 탐색하고 nesting depth를 계산
- **구현 방식:** IntelliJ의 `HighlighterIterator` + `BraceMatcher`를 활용하여 토큰 스트림에서 bracket을 식별
- **캐싱:** line 단위로 depth 정보를 캐싱하고, `DocumentListener`로 변경된 범위만 무효화
- **참고:** `highlight-bracket-pair`의 bracket 매칭 로직을 참고 (Apache 2.0 → GPL v3 호환)

#### 3.2.2 GuideLineRenderer
- **역할:** 에디터에 수직/수평 guide line을 그리는 렌더링 담당
- **구현 방식:** `TextEditorHighlightingPassFactory` + `TextEditorHighlightingPass` 등록
- **수직선:** `EditorLinePainter` 또는 커스텀 `CustomHighlighterRenderer`로 indent 위치에 색상 선 렌더링
- **수평선:** bracket 라인에서 indent guide 시작점까지 수평 선 렌더링
- **viewport 최적화:** `Editor.getScrollingModel().getVisibleArea()`로 보이는 영역만 처리

#### 3.2.3 ActiveScopeTracker
- **역할:** 커서 위치에 해당하는 현재 active bracket scope를 추적
- **구현 방식:** `CaretListener`로 커서 이동 감지, PSI tree에서 현재 위치의 bracket pair 조회
- **최적화:** 50ms debounce 적용, 빠른 커서 이동 시 불필요한 재계산 방지

#### 3.2.4 SettingsConfigurable + 설정 영속화
- **역할:** 플러그인 설정 UI 제공 + 설정값 저장/불러오기
- **위치:** `Settings > Editor > Color Scheme > Colored Bracket Guides` (색상), `Settings > Other Settings > Colored Bracket Guides` (일반/고급)
- **영속화 방식:** `PersistentStateComponent<PluginState>` 구현
    - IntelliJ가 자동으로 `APP_CONFIG/ColoredBracketGuides.xml`에 직렬화
    - IDE의 Settings Sync / Settings Repository와 자동 호환
- **실시간 반영:** 설정 변경 시 `MessageBus`를 통해 `SettingsChangedListener` 이벤트 발행 → 열려 있는 모든 에디터가 즉시 repaint
- **Import/Export:**
    - Export: 현재 설정을 JSON 파일로 저장 (`FileChooser` 다이얼로그)
    - Import: JSON 파일을 선택하여 설정 덮어쓰기
    - JSON 스키마는 `PluginState` data class와 1:1 매핑
- **Color Scheme 통합:** `TextAttributesKey`로 depth별 색상을 등록하여 IDE의 Color Scheme Editor에서도 색상 변경 가능

**PluginState 구조:**
```kotlin
@State(
    name = "ColoredBracketGuides",
    storages = [Storage("ColoredBracketGuides.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {

    data class State(
        // 일반
        var enabled: Boolean = true,
        var verticalGuideEnabled: Boolean = true,
        var horizontalGuideEnabled: Boolean = true,
        var displayMode: DisplayMode = DisplayMode.ACTIVE_ONLY,
        var lineWidth: Int = 1,
        var activeLineWidth: Int = 2,
        var activeLineStyle: LineStyle = LineStyle.SOLID,
        var inactiveOpacity: Float = 0.3f,

        // 색상 (hex string)
        var depthColors: MutableList<String> = mutableListOf(
            "#FFD700", "#DA70D6", "#179FFF",
            "#00CC7A", "#FF6B6B", "#CC8833"
        ),
        var cycleColors: Boolean = true,

        // 고급
        var caretDebounceMs: Int = 50,
        var maxNestingDepth: Int = 30
    )

    enum class DisplayMode { ALL, ACTIVE_ONLY }
    enum class LineStyle { SOLID, DASHED, DOTTED }

    private var state = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): PluginSettings =
            ApplicationManager.getApplication()
                .getService(PluginSettings::class.java)
    }
}
```

### 3.3 사용할 IntelliJ Platform API

| API | 용도 |
|---|---|
| `TextEditorHighlightingPassFactory` | guide line 렌더링 pass 등록 |
| `TextEditorHighlightingPass` | viewport 변경 시 증분 렌더링 실행 |
| `HighlighterIterator` | Lexer 토큰 스트림 순회 (bracket 식별) |
| `BraceMatcher` | 언어별 bracket 정의 조회 |
| `CaretListener` | 커서 위치 변경 감지 |
| `DocumentListener` | 문서 변경 감지 (캐시 무효화) |
| `VisibleAreaListener` | viewport 변경 감지 |
| `EditorColorsScheme` | 색상 설정 연동 |
| `TextAttributesKey` | 색상 키 정의 (Color Scheme 통합) |
| `PersistentStateComponent` | 설정값 XML 직렬화/역직렬화 (IDE 재시작 후 유지) |
| `MessageBus` / `Topic` | 설정 변경 이벤트를 열린 에디터에 전파 |
| `FileChooserFactory` | Import/Export 시 파일 선택 다이얼로그 |

### 3.4 성능 전략

IntelliJ의 PSI 인프라가 증분 파싱을 보장하므로, VS Code 블로그에서 설명한 (2,3)-tree 등의 커스텀 자료구조는 불필요하다.

| 전략 | 설명 |
|---|---|
| Viewport 제한 | 보이는 영역의 bracket pair만 처리 |
| 증분 업데이트 | `TextEditorHighlightingPass`가 변경된 영역만 재실행 |
| Depth 캐싱 | line별 nesting depth를 캐싱, document 변경 시 dirty 범위만 재계산 |
| CaretListener debounce | 50ms debounce로 빠른 커서 이동 시 과도한 재계산 방지 |
| Lazy 초기화 | IDE 시작 시 즉시 활성화하지 않고 에디터 열림 시점에 초기화 |

---

## 4. 프로젝트 구조

```
colored-bracket-guides/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── LICENSE                          # GPL v3.0
├── README.md
├── CHANGELOG.md
├── src/main/
│   ├── kotlin/com/github/{user}/coloredbracketguides/
│   │   ├── analyzer/
│   │   │   ├── BracketPairAnalyzer.kt
│   │   │   └── BracketPairCache.kt
│   │   ├── renderer/
│   │   │   ├── GuideLineHighlightingPassFactory.kt
│   │   │   ├── GuideLineHighlightingPass.kt
│   │   │   ├── VerticalGuideRenderer.kt
│   │   │   └── HorizontalGuideRenderer.kt
│   │   ├── scope/
│   │   │   └── ActiveScopeTracker.kt
│   │   ├── settings/
│   │   │   ├── PluginSettings.kt        # PersistentStateComponent (영속화)
│   │   │   ├── PluginConfigurable.kt    # 설정 UI 패널
│   │   │   ├── ColorSchemeExtension.kt  # TextAttributesKey 등록
│   │   │   └── SettingsImportExport.kt  # JSON import/export 로직
│   │   └── util/
│   │       └── ColorUtil.kt
│   └── resources/
│       └── META-INF/
│           └── plugin.xml
└── src/test/
    └── kotlin/...
```

---

## 5. 지원 범위

### 5.1 지원 IDE
IntelliJ Platform 기반 모든 IDE (IntelliJ IDEA, WebStorm, PyCharm, CLion, GoLand, Rider 등)

### 5.2 지원 언어 (Phase 1)
`BraceMatcher`가 등록된 모든 언어를 자동 지원한다. IntelliJ의 언어 플러그인이 bracket 정의를 제공하므로 별도의 언어별 구현은 불필요하다.

주요 대상: Java, Kotlin, JavaScript, TypeScript, Python, Go, Rust, C/C++, HTML, JSON, XML 등

### 5.3 호환 버전
- IntelliJ Platform: 2024.1+ (최신 API 안정성 확보)
- JDK: 17+
- Gradle: 8.x + IntelliJ Platform Gradle Plugin 2.x

---

## 6. 마일스톤

| 단계 | 내용 | 산출물 |
|---|---|---|
| M1 | 프로젝트 셋업, 빌드 환경 구성, plugin.xml 작성 | 빌드 가능한 빈 플러그인 |
| M2 | BracketPairAnalyzer 구현 — bracket 탐색 + depth 계산 | 단위 테스트 통과 |
| M3 | 수직 guide line 렌더링 구현 | 에디터에서 depth별 색상 수직선 확인 |
| M4 | Active scope 추적 + 강조 | 커서 위치에 따른 active guide 강조 동작 |
| M5 | 수평 guide line 렌더링 구현 | 수평선 렌더링 확인 |
| M6 | 설정 UI + Color Scheme 통합 | 설정에서 색상/토글 변경 가능 |
| M7 | 테스트, 성능 프로파일링, 엣지 케이스 처리 | 안정화 완료 |
| M8 | JetBrains Marketplace 배포 | v1.0.0 릴리즈 |

---

## 7. 참고 자료

| 자료 | URL |
|---|---|
| IntelliJ Platform SDK 문서 | https://plugins.jetbrains.com/docs/intellij/ |
| IntelliJ Plugin Template | https://github.com/JetBrains/intellij-platform-plugin-template |
| highlight-bracket-pair (참고 구현) | https://github.com/AprilViolet/highlight-bracket-pair |
| HighlightBracketPair 원본 (참고 구현) | https://github.com/qeesung/HighlightBracketPair |
| Rainbow Brackets (아키텍처 참고) | https://github.com/izhangzhihao/intellij-rainbow-brackets |
| VS Code Bracket Pair Colorization 블로그 | https://code.visualstudio.com/blogs/2021/09/29/bracket-pair-colorization |
| VS Code Colored Bracket Pair Guides | https://neutrondev.com/vs-code-colored-bracket-pair-guides/ |
| Extension Point 목록 | https://plugins.jetbrains.com/docs/intellij/extension-point-list.html |
