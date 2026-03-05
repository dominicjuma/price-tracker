# PriceTracker — Real-Time Stock Price Tracker

A real-time stock price tracker built with Jetpack Compose, WebSocket integration, and MVI-lite architecture. Displays live price updates for 25 stock symbols with a feed screen and symbol detail screen.

## Demo
### Light Mode

https://github.com/user-attachments/assets/9768d448-de1c-4560-8329-74781a6a4ab1 

### Dark Mode

https://github.com/user-attachments/assets/b47df03d-18d2-4ee7-b4c8-71423903cec9

## Requirements Checklist

### Core Features

- [x] **25 stock symbols** in a scrollable LazyColumn
- [x] **WebSocket echo integration** — `wss://ws.postman-echo.com/raw`, batched JSON, updates only on echo receipt
- [x] **Feed screen** — symbol, price, direction indicator (Material Icons), sorted by price descending
- [x] **Top bar** — left: connection indicator (green/red dot + Live/Offline), right: Feed label + Switch
- [x] **Detail screen** — symbol title, company name, large price with indicator, description, deep link URI
- [x] **Row tap** navigates to detail screen

### Technical Expectations

- [x] **100% Jetpack Compose** — no Views, no AndroidView
- [x] **MVVM with UDF** — single immutable `UiState` via `StateFlow`, events flow up via ViewModel functions
- [x] **Navigation Compose** with NavHost — type-safe `@Serializable` routes (Navigation 2.8+)
- [x] **Kotlin Flow** — `callbackFlow` for WebSocket, `StateFlow` for state, `SharedFlow` for errors
- [x] **Immutable UI state** — `@Immutable` data classes, `MutableStateFlow.update {}` for atomic mutations
- [x] **ViewModel + StateFlow** — `stateIn(WhileSubscribed(5_000))`, `collectAsStateWithLifecycle()`
- [x] **SavedStateHandle** — Detail ViewModel reads symbol from nav argument
- [x] **Shared WebSocket** — singleton repository, both ViewModels observe same StateFlow

### Bonus

- [x] **Price flash animation** — `Animatable<Color>` + `drawBehind` (draw phase only)
- [x] **62 tests** — 41 local JVM + 21 instrumented, all fakes, no mocking framework
- [x] **Light/dark themes** — Material 3 dynamic color, follows system
- [x] **Deep link** — `stocks://symbol/{symbol}` via type-safe `navDeepLink<Detail>`
- [x] **Structured error handling** — `DataError` → `UiError` mapping with localized strings
- [x] **Connectivity-aware reconnection** — waits for internet before retrying

## Architecture

```mermaid
graph TD
    subgraph UI Layer
        FS[FeedScreen] --> FVM[FeedViewModel]
        DS[DetailScreen] --> DVM[DetailViewModel]
    end
    subgraph Domain Layer
        FVM --> UC[ObserveStocksUseCase]
        DVM --> UC
    end
    subgraph Data Layer
        UC --> SPR[StockPriceRepository]
        UC --> SMR[StockMetadataRepository]
        SPR --> WS[WebSocketDataSource]
    end
    subgraph Common
        CO
        CO[ConnectivityObserver]
        CLK[Clock]
    end
```

### Data Flow

```
WebSocket echo → Repository (atomic update) → UseCase (combine + direction) →
ViewModel (sort/filter + map + error) → UiState → Compose
```

### Error Flow

```
Data layer exception → DataError (sealed) → ViewModel.toUiError() →
UiError (enum + @StringRes) → Snackbar
```

## Tech Choices & Rationale

| Choice | Why | Alternative Considered |
|--------|-----|----------------------|
| **OkHttp WebSocket** | Standard Android HTTP client. `callbackFlow` bridges callbacks cleanly. | Ktor (extra engine), Scarlet (unmaintained) |
| **MVVM with UDF** | Single `UiState` via `StateFlow`, events flow up via ViewModel functions. Follows Google's architecture guide. | Pure MVI with sealed Intents (over-engineering for 2 screens) |
| **Domain UseCase** | Combines two repos + computes direction. Reused by both ViewModels. | No domain layer (duplicates logic) |
| **Kotlinx Serialization** | No reflection, Kotlin-native, compile-time `@Serializable`. | Gson (reflection), Moshi (viable) |
| **Type-safe Navigation 2.8+** | `@Serializable` routes = compile-time checking. | String routes (error-prone) |
| **Fakes over Mocks** | Lightweight, explicit, no MockK dependency. | MockK (adds dep, harder to reuse) |
| **Turbine** | `awaitItem()` is deterministic, tests emission ordering. | Manual collect + `.value` (race-prone) |
| **Clock injection** | No `System.currentTimeMillis()` in production. Deterministic tests. | Hardcoded timestamps in tests |
| **`@IoDispatcher` injection** | JSON serialization on IO, not Default. Testable. | Hardcoded `Dispatchers.IO` |
| **`Network` prefix on wire models** | Instantly signals boundary. | No prefix (ambiguous in `data/model/`) |
| **`common/` package** | Cross-cutting infra (Clock, Dispatcher, Connectivity) isn't data-layer. | Everything in `data/` (unclear ownership) |

## Key Tradeoffs

### Server-Authoritative Pricing
Prices update ONLY on echo receipt, never on send. The UI waits for server confirmation before reflecting changes — consistent with real-world trading systems where the server is the source of truth.

### Feed Toggle Without Disconnection
Toggle pauses the ticker but keeps the WebSocket open. Reconnection is expensive (backoff + handshake), so pausing sends is significantly cheaper. Tradeoff: idle WebSocket stays open while paused.

### Flash Animation in Draw Phase Only
`Animatable<Color>` read inside `drawBehind` skips composition and layout entirely. With 25 rows flashing every 2s, this avoids 25 unnecessary recompositions per tick. Tradeoff: requires `Color.VectorConverter` setup.

### SharedFlow for Errors, StateFlow for State
Errors are one-shot events (show once, dismiss), not persistent state. `SharedFlow(extraBufferCapacity=1)` ensures emit never suspends. If nobody is collecting, the error is dropped — acceptable for transient UI messages like Snackbars.

### Connectivity-Aware Retry
Before retrying, checks `ConnectivityObserver.isOnline`. If offline, suspends via `first { it }` until connectivity returns, then retries with reset backoff. Avoids wasting retry attempts while the device has no internet.

### No Multi-Module
Single module. For a 2-screen app with one developer, multi-module adds Gradle configuration overhead without meaningful benefit. In production with multiple teams: `:common`, `:data`, `:domain`, `:feature-feed`, `:feature-detail` using the contract module pattern.

### No Room / No Offline
The spec does not require offline data persistence. Prices are transient by nature — they change every 2 seconds. Adding Room would introduce entities, DAOs, and migrations for a feature nobody requested.

## Testing Strategy

**62 tests** — all fakes, no mocking framework.

```
src/test/ (local JVM)
├── StockPriceRepositoryTest    13 tests
├── ObserveStocksUseCaseTest     8 tests
├── FeedViewModelTest           10 tests
└── DetailViewModelTest         10 tests

src/androidTest/ (instrumented)
├── FeedScreenTest              10 tests
└── DetailScreenTest            11 tests
```

### Interfaces & Test Doubles

| Production | Test Double |
|---|---|
| `StockPriceWebSocketDataSource` | `FakeStockPriceDataSource` |
| `StockPriceRepositoryImpl` | `FakeStockPriceRepository` |
| `AndroidConnectivityObserver` | `FakeConnectivityObserver` |
| `SystemClock` | `FakeClock` |
| `Dispatchers.Main` | `MainDispatcherRule` |
| `Dispatchers.IO` | `UnconfinedTestDispatcher` |

### Notable Test Patterns
- **Echo-only verification**: proves prices don't update on send
- **StateFlow conflation awareness**: split tests for identical `UiState` values
- **`backgroundScope`** for infinite `connectWithRetry()` loop
- **Stateless composable testing**: `FeedContent`/`DetailContent` directly — no Hilt

## Project Structure

```
com.multibankgroup.pricetracker/
├── app/
│   ├── PriceTrackerApp.kt                @HiltAndroidApp
│   ├── MainActivity.kt                   Single activity, edge-to-edge
│   └── navigation/
│       ├── Screen.kt                     @Serializable routes
│       └── PriceTrackerNavGraph.kt       Type-safe NavHost + deep link
├── common/
│   ├── connectivity/
│   │   ├── ConnectivityObserver.kt        Interface
│   │   └── AndroidConnectivityObserver.kt
│   ├── di/
│   │   ├── Qualifiers.kt                 @ApplicationScope, @IoDispatcher
│   │   └── CommonModule.kt
│   └── util/
│       ├── Clock.kt                       Interface
│       └── SystemClock.kt
├── data/
│   ├── di/DataModule.kt
│   ├── model/
│   │   ├── DataError.kt                   Sealed error hierarchy
│   │   ├── NetworkStockPriceMessage.kt    Wire format (@Serializable)
│   │   ├── StockData.kt
│   │   └── StockInfo.kt
│   ├── repository/
│   │   ├── StockPriceRepository.kt        Interface
│   │   ├── StockPriceRepositoryImpl.kt
│   │   ├── StockMetadataRepository.kt     Interface
│   │   └── StockMetadataRepositoryImpl.kt
│   └── websocket/
│       ├── StockPriceDataSource.kt        Interface
│       ├── StockPriceWebSocketDataSource.kt
│       ├── WebSocketEvent.kt
│       └── WebSocketFactory.kt
├── domain/
│   ├── model/
│   │   ├── PriceDirection.kt
│   │   └── Stock.kt
│   └── ObserveStocksUseCase.kt
└── feature/
    ├── detail/
    │   ├── DetailScreen.kt
    │   ├── DetailUiState.kt
    │   └── DetailViewModel.kt
    ├── feed/
    │   ├── FeedScreen.kt
    │   ├── FeedUiState.kt
    │   └── FeedViewModel.kt
    └── shared_ui/
        ├── components/
        │   ├── ConnectionIndicator.kt
        │   ├── PriceChangeIndicator.kt
        │   └── StockRow.kt
        ├── model/UiError.kt
        └── theme/
            ├── Color.kt
            ├── StockColors.kt
            ├── Theme.kt
            └── Type.kt
```

## Production Considerations

If this were a production app, the following would be added:

- **Multi-module architecture** — `:common`, `:data`, `:domain`, `:feature-feed`, `:feature-detail` with contract module pattern (API + impl modules) for build time optimization and team parallel development
- **Room persistence** — cache last-known prices for offline display and faster cold starts
- **Pagination** — cursor-based pagination if the symbol list grows beyond a single screen load
- **ProGuard/R8 rules** — keep rules for `@Serializable` classes and Kotlin reflection
- **CI/CD** — GitHub Actions for lint, unit tests, instrumented tests on merge, staged rollout via Play Console
- **Screenshot tests** — Compose Preview Screenshot Testing for visual regression detection
- **Accessibility** — `contentDescription` on all interactive elements, TalkBack testing, minimum 48dp touch targets
- **Analytics & crash reporting** — Firebase Crashlytics for production errors, analytics for feed engagement and detail screen visits
- **Feature flags** — remote config to toggle WebSocket URL, ticker interval, and new features without app update
- **Certificate pinning** — OkHttp `CertificatePinner` to prevent MITM attacks on the WebSocket connection
- **Force upgrade** — version check on app launch to sunset old clients with breaking API changes
- **Localization** — extract all strings (already done), add translations, handle RTL layouts
- **Adaptive layouts** — tablet/foldable support with list-detail two-pane layout
- **Performance monitoring** — baseline profiles for startup, Macrobenchmark for scroll performance
- **Offline-first with sync** — Room as single source of truth, network writes to DB, UI reads from DB only. Optimistic UI updates with server reconciliation on sync
- **Data integrity** — conflict resolution strategy (server wins with timestamp comparison), idempotency keys on outbound requests to prevent duplicate operations on retry
- **Sync queue** — pending actions (e.g., watchlist changes) batched and sent when connectivity returns, with exponential backoff and deduplication

## Build & Run

```bash
git clone https://github.com/dominicjuma/price-tracker.git
cd price-tracker

./gradlew assembleDebug

# Unit tests (41 tests)
./gradlew test

# Instrumented tests (21 tests, requires emulator)
./gradlew connectedAndroidTest

# Deep link
adb shell am start -a android.intent.action.VIEW -d "stocks://symbol/AAPL"
```

## Dependencies

See `gradle/libs.versions.toml`. Key:

- Compose BOM 2026.02.01
- Navigation Compose 2.9.7
- Hilt 2.59.2 (KSP)
- OkHttp 5.3.2
- Kotlinx Serialization 1.10.0
- Turbine 1.2.1 (test)
- Kotlin 2.3.10

Min SDK 24 · Target SDK 36
