# Dango Diary

<h4 align="center">A local-only Android app for tracking the restaurants you have visited.</h4>

## Layout

```
Dango-Diary/
├── app/                            # Single Gradle module
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/dangodiary/
│       │   ├── DangoDiaryApp.kt    # Application — owns DB + PhotoStorage singletons
│       │   ├── MainActivity.kt     # Single activity, hosts the Compose nav graph
│       │   ├── data/               # Room entity, DAO, Database, photo-paths JSON
│       │   ├── ui/                 # theme/, nav/, list/, detail/, edit/, common/
│       │   └── util/               # PhotoStorage, Formatting
│       └── res/                    # strings, themes, drawables
├── DESIGN.md                       # Project conventions and style
├── CLOUD-SETUP.md                  # Step-by-step Google Cloud setup (key, APIs, cost guards)
├── TESTING.md                      # Headless emulator + remote scrcpy workflow
└── gradle/libs.versions.toml       # Single source of dependency versions
```

## Tech Stack

| Dependency | Version | Purpose |
|---|---|---|
| [Kotlin](https://kotlinlang.org/) | 2.0.21 | Language |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | BOM 2024.10.01 | UI |
| [Room](https://developer.android.com/training/data-storage/room) | 2.6.1 | On-device database |
| [Navigation-Compose](https://developer.android.com/jetpack/compose/navigation) | 2.8.3 | Screen routing |
| [Coil](https://coil-kt.github.io/coil/) | 2.7.0 | Image loading from local files |
| [Maps Compose](https://github.com/googlemaps/android-maps-compose) | 4.4.1 | Embedded read-only map on the detail screen |
| [Places SDK](https://developers.google.com/maps/documentation/places/android-sdk) | 4.1.0 | Address autocomplete in the edit form |

Entry point is [MainActivity.kt](app/src/main/java/com/dangodiary/MainActivity.kt). All library code lives under `app/src/main/java/com/dangodiary/`.

## Setup (Windows)

The fastest path on Windows is **Android Studio** — it bundles the JDK, Android SDK, AVD manager, and emulator in one installer. Total disk: ~10 GB.

### **1. Install Android Studio**

1. Download from <https://developer.android.com/studio>.
2. Run the installer with defaults. When the **Setup Wizard** offers a "Standard" install, accept it — this downloads the Android SDK, platform-tools, and an emulator system image (~5 GB after install).
3. When prompted to accept SDK licenses, accept them all.

After install, note where Android Studio put the SDK. Open **Settings → Languages & Frameworks → Android SDK** and copy the **Android SDK Location** value. It's usually `C:\Users\<you>\AppData\Local\Android\Sdk`.

### **2. Get the code**

```powershell
git clone https://github.com/Pekkapost/Dango-Diary.git
cd Dango-Diary
```

If you don't have git, download the repo as a ZIP and unzip it. Then `cd` into the folder in PowerShell.

### **3. Create `local.properties`**

In the project root, create a file named **`local.properties`** with:

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=
```

- `sdk.dir` — the SDK path you copied in step 1. **Use double backslashes** (`\\`) or forward slashes (`/`); a single `\` will be read as a Java escape.
- `MAPS_API_KEY` — leave empty for now. Map tiles render gray and the Name field's restaurant autocomplete is disabled without one, but the rest of the app (including saved entries and manual address entry) works fine. The full Google Cloud walkthrough — creating the key, enabling Maps SDK + Places API (New), and setting cost guards so you can't be charged — is in **[CLOUD-SETUP.md](CLOUD-SETUP.md)**.

### **4. Open the project in Android Studio**

`File → Open → ` select the **`Dango-Diary`** folder (not a subfolder). Android Studio runs a Gradle sync that downloads ~1 GB of dependencies the first time — takes 2–5 minutes. Wait until the bottom status bar reads "Gradle sync finished".

If the sync fails complaining about an SDK component, click the offered "Install missing platform/build-tools" link.

### **5. Create an emulator**

`Tools → Device Manager → Create Device`. Pick **Pixel 6**, click Next, pick the **API 34 (Android 14)** system image (download it if there's an arrow icon next to it), click Next, then Finish.

In Device Manager, click the ▶ play icon next to the new device. A phone-shaped window opens and boots Android — this takes 30–60 seconds the first time.

### **6. Run the app**

With the emulator running, click the green **▶ Run 'app'** button in the toolbar (or press `Shift+F10`). Android Studio builds the debug APK, installs it on the emulator, and launches it. The emulator shows the empty-list screen with a "+ Add restaurant" button.

### **7. Use it**

Tap **+ Add restaurant**, fill in the form, tap **Save**. The entry appears in the list. Tap the entry to see the detail view; the edit and delete actions are in the top app bar.

Things to know on the emulator:

| Feature | Note |
|---|---|
| **Camera** | Uses a synthetic test image, not a real photo. For realistic photo testing use **Choose from gallery** — drag a JPG from your desktop onto the emulator window to add it to the gallery first. |
| **Name autocomplete** | Needs the Places API (New) enabled on your key plus the emulator's debug SHA-1 added to the key restriction (see [CLOUD-SETUP.md](CLOUD-SETUP.md) step 9). Without that, typing in the Name field shows no suggestions — manual entry still works. |
| **Detail map preview** | Centers on the saved pin. If you haven't picked an address yet, the map section is hidden — there's no separate map picker in the edit flow. |
| **Hot reload** | When you change Kotlin code, just press ▶ again; Android Studio rebuilds and reinstalls in a few seconds. |

## Setup (command line, without Android Studio)

If you'd rather not install Android Studio, you need JDK 17 + the Android command-line tools. This is more work; **prefer Android Studio above** unless you have a reason.

1. Install **JDK 17** (Temurin: <https://adoptium.net/temurin/releases/?version=17>). Verify with `java -version` in a new PowerShell window.
2. Install **Android command-line tools**: download from <https://developer.android.com/studio#command-line-tools-only>, unzip to `C:\Android\cmdline-tools\latest\`.
3. Set environment variables in **System Properties → Environment Variables**:
   - `ANDROID_HOME` = `C:\Android`
   - Append `%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator` to `Path`.
4. In a new PowerShell, install SDK components:
   ```powershell
   sdkmanager --licenses     # accept all (press 'y' a few times)
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "emulator" "system-images;android-34;google_apis;x86_64"
   ```
5. Create + boot an emulator:
   ```powershell
   echo no | avdmanager create avd -n rt_test -k "system-images;android-34;google_apis;x86_64" -d pixel_6
   emulator -avd rt_test
   ```
6. From the project folder, with the emulator running:
   ```powershell
   .\gradlew.bat :app:installDebug
   adb shell am start -n com.dangodiary/.MainActivity
   ```

## Logs

In Android Studio, the **Logcat** panel at the bottom shows live device output. Filter for `com.dangodiary` to isolate the app.

From the command line:

```powershell
adb logcat -s DangoDiaryApp PhotoStorage
```

## Architecture

| Module | Description |
|---|---|
| [app/build.gradle.kts](app/build.gradle.kts) | Module config — pulls `MAPS_API_KEY` from `local.properties` into the manifest and into `BuildConfig` (for Places SDK init). |
| [DangoDiaryApp.kt](app/src/main/java/com/dangodiary/DangoDiaryApp.kt) | Application class. Lazy-initializes the Room DB, PhotoStorage, and AppSettings (DataStore), and initializes the Places SDK. |
| [MainActivity.kt](app/src/main/java/com/dangodiary/MainActivity.kt) | Single activity. Sets the Compose content and theme. |
| [data/](app/src/main/java/com/dangodiary/data/) | `Entry` entity, `EntryDao` (Flow + suspend), `DiaryDatabase`, the cuisine catalog, and JSON helpers for photos (path + optional caption) and dish lists. |
| [ui/list/](app/src/main/java/com/dangodiary/ui/list/) | List screen, search/filter/sort ViewModel. |
| [ui/detail/](app/src/main/java/com/dangodiary/ui/detail/) | Detail screen with section layout (location → meal → dined with → notes → photos) + delete flow. |
| [ui/edit/](app/src/main/java/com/dangodiary/ui/edit/) | New/edit form, restaurant-name autocomplete, photo capture/import. Form fields grouped into Restaurant / Meal / Company & notes / Photos sections. |
| [ui/settings/](app/src/main/java/com/dangodiary/ui/settings/) | App-wide settings screen — theme preset + default currency. Reachable from the list screen's top-bar gear. |
| [ui/theme/](app/src/main/java/com/dangodiary/ui/theme/) | `DangoDiaryTheme` (reactive — re-themes on settings change) and the `ThemeOption` palette presets. |
| [ui/common/](app/src/main/java/com/dangodiary/ui/common/) | Reusable composables (`RatingStars`, `DatePickerField`, `CuisinePickerField`, `RestaurantNameField`, `PhotoGrid`). |
| [util/PhotoStorage.kt](app/src/main/java/com/dangodiary/util/PhotoStorage.kt) | Copies captured/imported photos into `filesDir/photos/` and returns stable paths. |
| [util/AppSettings.kt](app/src/main/java/com/dangodiary/util/AppSettings.kt) | Preferences DataStore wrapper for app-wide user settings. |
| [util/Formatting.kt](app/src/main/java/com/dangodiary/util/Formatting.kt) | Date, currency, and city-extraction formatters. |

## Features

| Surface | Summary |
|---|---|
| **List Screen** | Search · sort (recency / rating / name / nearest) · filter bottom sheet (rating slider, cuisine, has-photo). Rows show photo, name, cuisine·city, rating, date, total. |
| **Add/Edit Form** | Four sections: **Restaurant Information** · **Dishes** · **Company & Notes** · **Photos**. |
| **Detail Screen** | Header + sections (Location, Dishes, Companions, Notes, Photos). Sections appear only when populated. |
| **Settings** | Three sections: **Theme** · **Display** · **Defaults**. |

### List Screen

| Element | Behaviour |
|---|---|
| **Search** | Text query against name, notes, companions, address. |
| **Sort menu** | Recently visited · Highest rated · Name (A–Z) · Nearest first. Nearest prompts for a coarse-location permission on first use; entries without coordinates sink to the bottom. |
| **Filter sheet** | Minimum-rating slider, cuisine filter (specific cuisine or a whole supertype like Cafés), has-photo switch. |
| **Row** | Photo · name · cuisine + city · half-star rating · date · total price. Total is hidden when **Settings → Hide total price** is on. |

### Add/Edit Form

| Section | Fields |
|---|---|
| **Restaurant Information** | Restaurant Name (Google Places autocomplete — picking a suggestion auto-fills address + map pin); cuisine + date in a paired row; half-star rating (1–5 in 0.5 increments); address (editable, auto-filled or typed manually). |
| **Dishes** | One or more dishes per entry. Each row: Dish name + Dish price. "Add another dish" appends rows. Drag a row's handle to reorder. Currency defaults to the app-wide setting. |
| **Company & Notes** | Companions + free-form notes. |
| **Photos** | Capture from camera or pick from gallery. Each photo has an optional caption. Drag a row's handle to reorder. |

### Detail Screen

| Section | Content |
|---|---|
| **Header** | Name (bold, in app bar) · half-star rating · cuisine · date · total price subtitle. Total price hidden when the setting is on. |
| **Location** | Address text; expand icon reveals an embedded read-only map; map icon opens the location in external Maps. |
| **Dishes** | One row per saved dish — dish name on the left, price on the right. |
| **Companions** | Free text. |
| **Notes** | Free text. |
| **Photos** | 3-column grid. Captions render centred under each thumbnail when present. |

### Settings

| Section | Knob |
|---|---|
| **Theme** | Pick a preset (Purple (app default) / Brown / Blue / Red / Pink `#FDA2F5`). Each row shows a colour swatch; change applies live. |
| **Display** | Hide total price — hides per-entry totals on the list and the detail subtitle. Per-dish prices stay visible in the Dishes section. |
| **Defaults** | Default currency code for new entries. Existing entries keep their saved currency. |

All data is stored locally in a Room database at the app's private data directory. Photos are copied into the app's `filesDir/photos/`. Nothing leaves the device except Places autocomplete queries while you're typing a restaurant name.

## Limitations

| Area | Notes |
|---|---|
| **Google Cloud key** | Required for map tiles + Places API (New) restaurant-name autocomplete. Without it the Name field still works for manual entry but won't suggest matches, and the detail-screen map is blank. See [CLOUD-SETUP.md](CLOUD-SETUP.md). |
| **Backups** | No cloud sync, no export/import. Backups are the user's responsibility — Android's auto-backup will capture the DB and photos. |
| **Autocomplete bias** | Places autocomplete is biased by the device's IP with no explicit location restriction. Entering a restaurant from another city by name alone may need a city qualifier, or pick a similar-named place and edit the address manually. |
| **Currency** | Per-row and free-text; no validation beyond "is this a known ISO 4217 code". Unknown codes fall back to the device default at format time. |
| **Cuisine catalog** | Hardcoded in [Cuisine.kt](app/src/main/java/com/dangodiary/data/Cuisine.kt); no UI to add custom cuisines. |

## Authors

| Name | Role |
|---|---|
| [@Pekkapost](https://github.com/Pekkapost) | Author |
