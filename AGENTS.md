# IntentBridge Development Guide

This document provides guidelines and commands for developing the IntentBridge Android application.

## Project Overview

- **Platform**: Android (minSdk 24, targetSdk 34)
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (BOM 2024.02.00)
- **Architecture**: MVVM with Hilt DI, Room Database, Coroutines/Flow
- **Build System**: Gradle 8.11.1 with Kotlin DSL

## Build Commands

### Prerequisites

- Java 17 (required for Gradle 8.x)
- Android SDK with API 34 platform

```bash
# Set Java 17 if not default
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot"
```

### Build Tasks

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Clean and rebuild
./gradlew clean assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.intentbridge.data.local.CardDaoTest"

# Run a single test method
./gradlew test --tests "com.intentbridge.data.local.CardDaoTest.getCardById"

# Run tests with verbose output
./gradlew test --info

# Check code style (lint)
./gradlew lint

# Check lint for debug variant only
./gradlew lintDebug

# View detailed lint report
open app/build/reports/lint-results.html
```

### Gradle Options

```bash
# Use daemon for faster builds (recommended for development)
./gradlew assembleDebug --daemon

# Parallel execution
./gradlew assembleDebug --parallel

# Skip tests during build
./gradlew assembleDebug -x test -x lint

# Build with configuration cache
./gradlew assembleDebug --configuration-cache
```

## Code Style Guidelines

### General Principles

1. **Use Kotlin DSL** for Gradle files (`.kts` extension)
2. **Prefer immutability**: Use `val` over `var` whenever possible
3. **Avoid nullable types** unless necessary; use safe calls (`?.`) and Elvis operator (`?:`)
4. **Never suppress type errors**: Never use `as any`, `@ts-ignore`, `@ts-expect-error`
5. **Never commit secrets**: Never commit API keys, credentials, or sensitive files

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Packages | lowercase, single words | `com.intentbridge.data.model` |
| Classes/Objects | PascalCase | `CardRepository`, `MainViewModel` |
| Functions | camelCase, verb prefix | `onCardClick()`, `loadCards()` |
| Variables/Properties | camelCase, noun | `cardList`, `isLoading` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| Enums | PascalCase | `CardCategory.URGENT` |
| Composables | PascalCase, noun/verb | `MainScreen()`, `CardButton()` |
| State Classes | PascalCase, ends with State/UiState | `MainScreenState`, `ConfigScreenState` |
| ViewModels | PascalCase, ends with ViewModel | `MainViewModel`, `ConfigViewModel` |

### File Organization

```
app/src/main/java/com/intentbridge/
├── data/
│   ├── model/          # Data classes, entities, enums
│   ├── local/          # Room DAOs, Database, Converters
│   └── repository/     # Repository implementations
├── di/                 # Hilt modules
├── service/            # Business services (TTS, etc.)
├── ui/
│   ├── components/     # Reusable Compose components
│   ├── screens/        # Screen composables + ViewModels
│   └── theme/          # Colors, Typography, Theme
├── IntentBridgeApp.kt  # Application class
└── MainActivity.kt     # Entry point
```

### Imports

**Order imports by category** (IDE will auto-organize):
1. Kotlin standard library
2. Android framework
3. Jetpack Compose
4. Third-party libraries (Hilt, Room, Coil, etc.)
5. Project internal imports

```kotlin
// Correct order
import kotlin.math.*
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.intentbridge.data.model.Card
import com.intentbridge.data.repository.CardRepository
```

**Avoid wildcard imports** except for:
- `kotlinx.coroutines.*` (when using multiple coroutine utilities)
- `androidx.compose.ui.*` (when using many basic UI elements)

### Formatting

- **Indentation**: 4 spaces (Kotlin default)
- **Line length**: Maximum 120 characters
- **Blank lines**: One blank line between top-level declarations
- **Trailing commas**: Always use trailing commas for readability

```kotlin
// Good
@Composable
fun CardButton(
    card: Card,
    cardColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ...
}

// Avoid
@Composable
fun CardButton(card: Card, cardColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // ...
}
```

### Type System

- **Explicit return types**: Always specify return type for public functions
- **Type inference**: Use inference for local variables when type is obvious
- **Prefer sealed classes** over enums when you need associated data

```kotlin
// Good - explicit return type
fun getCardById(id: Long): Card?

// Good - inference for local variables
val urgentCards = mutableListOf<Card>()

// Good - sealed class for state
sealed class UiState {
    data object Loading : UiState()
    data class Success(val cards: List<Card>) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Compose Guidelines

1. **One composable per file** when complex; small helpers can be in same file
2. **State hoisting**: Lift state up to enable testability and reusability
3. **Remember for expensive computations**: Use `remember` for derived state
4. **Flow collection in Compose**: Use `collectAsState()` or `collectAsStateWithLifecycle()`
5. **Use Material 3**: Prefer M3 components over M2

```kotlin
// Good - state hoisting
@Composable
fun CardButton(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // ...
}

// Good - collect Flow in Compose
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    // ...
}
```

### Error Handling

1. **Never use empty catch blocks**: Always handle or log exceptions
2. **Prefer Result type** for functions that can fail
3. **Use try-catch sparingly**: Let exceptions propagate for critical failures

```kotlin
// Good - explicit error handling
fun parseColor(colorString: String): Color? {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        null // Return null for invalid colors
    }
}

// Avoid - empty catch
try {
    parseColor(input)
} catch (e: Exception) {
    // Do nothing - WRONG!
}
```

### Hilt Dependency Injection

1. **Use constructor injection** for ViewModels and Services
2. **@Inject constructor** with private dependencies
3. **@HiltViewModel** annotation for ViewModels
4. **@AndroidEntryPoint** for Activities

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val ttsService: TTSService,
) : ViewModel() {
    // ...
}
```

### Room Database

1. **One DAO per entity** or logical grouping
2. **Use Flow** for reactive queries
3. **Type converters** for custom types (enums, dates, etc.)

```kotlin
@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY category DESC, displayOrder ASC")
    fun getAllCards(): Flow<List<Card>>
}
```

### Testing Guidelines

1. **Unit test ViewModels** with `kotlinx-coroutines-test`
2. **Test repository layer** with in-memory database
3. **Use `runTest` block** for coroutine testing

```kotlin
@Test
fun `getCardById returns card when exists`() = runTest {
    val card = repository.getCardById(1)
    assertNotNull(card)
}
```

### Resource Files

- **Strings**: Always use `strings.xml` for user-facing text (supports localization)
- **Colors**: Define in `colors.xml` or in Compose theme
- **Dimensions**: Use dp for layouts, sp for text

```xml
<!-- strings.xml -->
<string name="app_name">意图桥</string>
<string name="urgent_zone">紧急区</string>
```

## Common Issues

### Build Fails with Java Version Error
```bash
# Ensure Java 17 is set
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot"
```

### KSP/Compose Compilation Errors
- Ensure `kotlinCompilerExtensionVersion` matches Kotlin version
- Use compatible versions: Kotlin 1.9.22 → Compose Compiler 1.5.8

### Missing SDK Platform
```bash
# Install via Android Studio SDK Manager or command line
sdkmanager "platforms;android-34"
```
