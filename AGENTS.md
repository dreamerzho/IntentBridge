# IntentBridge Development Guide

Guidelines and commands for developing the IntentBridge Android application.

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
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot"
```

### Build Tasks

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean assembleDebug   # Clean and rebuild
./gradlew test                  # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
./gradlew test --tests "com.intentbridge.data.local.CardDaoTest"        # Single test class
./gradlew test --tests "com.intentbridge.data.local.CardDaoTest.getCardById"  # Single test method
./gradlew test --info           # Verbose test output
./gradlew lint                  # Check code style
./gradlew lintDebug            # Lint for debug variant
```

### Gradle Options

```bash
./gradlew assembleDebug --daemon    # Faster builds
./gradlew assembleDebug --parallel  # Parallel execution
./gradlew assembleDebug -x test -x lint  # Skip tests
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
| Packages | lowercase | `com.intentbridge.data.model` |
| Classes/Objects | PascalCase | `CardRepository`, `MainViewModel` |
| Functions | camelCase, verb | `onCardClick()`, `loadCards()` |
| Variables | camelCase | `cardList`, `isLoading` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Enums | PascalCase | `CardCategory.URGENT` |
| Composables | PascalCase | `MainScreen()`, `CardButton()` |
| State Classes | PascalCase | `MainScreenState`, `ConfigScreenState` |
| ViewModels | PascalCase | `MainViewModel`, `ConfigViewModel` |

### File Organization

```
app/src/main/java/com/intentbridge/
├── data/model/          # Data classes, entities, enums
├── data/local/          # Room DAOs, Database, Converters
├── data/repository/     # Repository implementations
├── di/                  # Hilt modules
├── service/             # Business services (TTS, etc.)
├── ui/components/       # Reusable Compose components
├── ui/screens/         # Screen composables + ViewModels
├── ui/theme/           # Colors, Typography, Theme
├── IntentBridgeApp.kt  # Application class
└── MainActivity.kt      # Entry point
```

### Imports
**Order**: Kotlin stdlib → Android framework → Jetpack Compose → Third-party → Project internal

```kotlin
import kotlin.math.*
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.intentbridge.data.model.Card
```

Avoid wildcard imports except for `kotlinx.coroutines.*` or `androidx.compose.ui.*`

### Formatting
- **Indentation**: 4 spaces
- **Line length**: Maximum 120 characters
- **Trailing commas**: Always use for readability

### Type System
- **Explicit return types** for public functions
- **Prefer sealed classes** over enums when you need associated data

```kotlin
sealed class UiState {
    data object Loading : UiState()
    data class Success(val cards: List<Card>) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Compose Guidelines
1. **One composable per file** when complex
2. **State hoisting**: Lift state up for testability
3. **Use `remember`** for derived state
4. **Flow collection**: Use `collectAsState()` or `collectAsStateWithLifecycle()`
5. **Use Material 3**: Prefer M3 over M2

### Error Handling
1. **Never use empty catch blocks**: Always handle or log exceptions
2. **Prefer Result type** for functions that can fail

```kotlin
fun parseColor(colorString: String): Color? {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        null
    }
}
```

### Hilt DI

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val ttsService: TTSService,
) : ViewModel()
```

### Room Database

```kotlin
@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY category DESC, displayOrder ASC")
    fun getAllCards(): Flow<List<Card>>
}
```

### Testing

```kotlin
@Test
fun `getCardById returns card when exists`() = runTest {
    val card = repository.getCardById(1)
    assertNotNull(card)
}
```

## Common Issues

### Java Version Error
```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot"
```

### Missing SDK Platform
```bash
sdkmanager "platforms;android-34"
```
