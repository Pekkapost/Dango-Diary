# Design Document — Conventions & Style

The driving idea: **make it easy for a future reader to walk through the codebase top-to-bottom and never be surprised.** Surprise costs more than verbosity.

---

## 1. Project layout

```
Dango-Diary/
├── README.md
├── DESIGN.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml   # Single source of truth for dependency versions
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/dangodiary/
        │   ├── DangoDiaryApp.kt       # Application — owns DB, PhotoStorage, AppSettings
        │   ├── MainActivity.kt        # Single activity, hosts the Compose nav graph
        │   ├── data/                  # Room entity, DAO, Database, JSON converters (Photos, Dishes)
        │   ├── ui/
        │   │   ├── theme/             # Material3 theme + ThemeOption palette presets
        │   │   ├── nav/               # NavHost + route definitions
        │   │   ├── list/, detail/, edit/, settings/   # One subfolder per screen
        │   │   └── common/            # Reusable composables (RatingStars, PhotoGrid, ...)
        │   └── util/                  # PhotoStorage, AppSettings (DataStore wrapper), Formatting
        └── res/                       # strings, themes, drawables, mipmaps
```

Why: A single Gradle module keeps build times minimal until splitting is justified. Screens are grouped as `Screen.kt + ViewModel.kt` siblings so the file-tree alone explains the structure.

---

## 2. Naming

| Identifier kind | Convention | Example |
|---|---|---|
| Class / object / interface / file | `PascalCase` | `EntryDao`, `EntryDao.kt` |
| Function / property / parameter | `camelCase` | `loadFrom`, `dishPriceCents` |
| Const val (top-level / companion) | `UPPER_SNAKE_CASE` | `PHOTOS_DIR`, `TAG` |
| Composable function | `PascalCase` | `EntryListScreen` |
| Test / preview function | `PascalCase` (Compose) or `camelCase` (JUnit) | |

- One top-level class per file; filename matches the class.
- Loop / lambda variables get a descriptive name (`entry`, `path`) unless the lambda body is one line and the scope is trivial — then `it` is fine.
- Never single-letter variables except `i` in tight loops; even there, `idx` is preferable.

---

## 3. Imports

The IDE re-sorts imports, so don't fight it. Conceptually they fall into stdlib → AndroidX/Compose → third-party → local; blank-line groups are optional but encouraged in long files.

---

## 4. Comments and docstrings

Default: **write none.** Identifiers should explain themselves.

Add a comment only when **the why is non-obvious**:

- A hidden constraint, invariant, or ordering requirement.
- A workaround for a framework / library bug (with a link or one-line reason).
- A surprising design choice a maintainer would otherwise "fix" by accident.

Do not document:

- What the code does (the code already says it).
- The current task / commit context.
- Specific callers (rot as the codebase changes).

KDoc (`/** … */`) goes on non-trivial public functions/classes. Single-line summary in the imperative mood; document exceptions and any non-obvious return semantics. Don't restate the type signature.

---

## 5. State & dependency injection

- App-wide singletons (Room DB, `PhotoStorage`, `AppSettings`) live as `by lazy` properties on the `Application` subclass [`DangoDiaryApp`](app/src/main/java/com/dangodiary/DangoDiaryApp.kt). No DI framework. Re-evaluate if the graph grows beyond a handful of nodes.
- ViewModels receive their dependencies via a `ViewModelProvider.Factory` defined in a `companion object` of the ViewModel itself. The screen Composable obtains the VM via `viewModel(factory = …)`.
- UI state is `StateFlow<…>` exposed by the ViewModel and collected in Composables via `collectAsStateWithLifecycle()`.
- No `LiveData`, no `GlobalScope`, no `runBlocking` on the main thread.
- Lifecycle-tied resource cleanup (e.g. deleting unsaved photo imports) belongs in `ViewModel.onCleared()`, not in a screen-level `DisposableEffect`. The Compose dispose fires when the screen leaves composition — including when navigating *forward* to another screen, with the back stack entry still alive — so it would tear down state the user expects back when they return.

---

## 6. Persistence (Room)

- Room transactions are atomic. Use `suspend` DAO functions; never block.
- Schema migrations live as numbered `Migration` constants on the `Database` companion. Pass them to `Room.databaseBuilder(...).addMigrations(...)`. **Never** `fallbackToDestructiveMigration()` — losing user data is not an upgrade path.
- Backfill optional fields with sensible defaults in the entity declaration so older rows continue to load.

---

## 7. Error handling

- Validate at boundaries: the edit form is the boundary. Validation lives in the ViewModel's `save()`, which sets per-field error strings rather than throwing.
- Internal helpers and the DAO trust their callers and do not defensively guard.
- Best-effort cleanup (e.g. deleting orphan photo files) wraps `runCatching` so a missing-file race never crashes the app — the placeholder rendering handles the visual fallback.

---

## 8. Logging

```kotlin
private const val TAG = "PhotoStorage"
Log.i(TAG, "imported photo at $path")
```

- One `TAG` per file, `private const val`.
- Use `Log.i` for noteworthy state changes, `Log.w` for recoverable issues, `Log.e(tag, msg, throwable)` for handled errors with context.

---

## 9. Strings

- User-visible strings live in [strings.xml](app/src/main/res/values/strings.xml) and are referenced via `stringResource(R.string.…)`.
- No hardcoded user-visible text in Kotlin sources.

---

## 10. Anti-patterns to avoid

- Hardcoded strings in Composables (use `stringResource`).
- `runBlocking` anywhere outside of a one-off CLI / test entry point.
- `GlobalScope.launch` — always use `viewModelScope` or a scope you own.
- Premature interfaces / repositories that wrap a single DAO with no extra behavior.
- Adding a Hilt module for the second injectable thing — re-evaluate at the fifth.
- Feature flags or backwards-compatibility shims for code we control end-to-end.
- Catching `Throwable` to silence a real bug.

---

## 11. When to break these rules

When a specific reader experience benefits, when a framework forces an idiom, or when strict adherence would obscure intent. The goal is to be **predictable** — predictability beats personal preference.
