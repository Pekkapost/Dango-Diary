# Restaurant Tracker

<h4 align="center">A local-only Android app for tracking the restaurants you have visited.</h4>

## Layout

```
Restaurant-App/
├── app/                            # Single Gradle module
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/restauranttracker/
│       │   ├── RestaurantApp.kt    # Application — owns DB + PhotoStorage singletons
│       │   ├── MainActivity.kt     # Single activity, hosts the Compose nav graph
│       │   ├── data/               # Room entity, DAO, Database, photo-paths JSON
│       │   ├── ui/                 # theme/, nav/, list/, detail/, edit/, common/
│       │   └── util/               # PhotoStorage, Formatting
│       └── res/                    # strings, themes, drawables
├── DESIGN.md                       # Project conventions (Kotlin adaptation)
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
| [Maps Compose](https://github.com/googlemaps/android-maps-compose) | 4.4.1 | Embedded map + pin picker |

Entry point is [MainActivity.kt](app/src/main/java/com/restauranttracker/MainActivity.kt). All library code lives under `app/src/main/java/com/restauranttracker/`.

## Setup (Windows)

The fastest path on Windows is **Android Studio** — it bundles the JDK, Android SDK, AVD manager, and emulator in one installer. Total disk: ~10 GB.

### **1. Install Android Studio**

1. Download from <https://developer.android.com/studio>.
2. Run the installer with defaults. When the **Setup Wizard** offers a "Standard" install, accept it — this downloads the Android SDK, platform-tools, and an emulator system image (~5 GB after install).
3. When prompted to accept SDK licenses, accept them all.

After install, note where Android Studio put the SDK. Open **Settings → Languages & Frameworks → Android SDK** and copy the **Android SDK Location** value. It's usually `C:\Users\<you>\AppData\Local\Android\Sdk`.

### **2. Get the code**

```powershell
git clone https://github.com/Pekkapost/Restaurant-App.git
cd Restaurant-App
```

If you don't have git, download the repo as a ZIP and unzip it. Then `cd` into the folder in PowerShell.

### **3. Create `local.properties`**

In the project root, create a file named **`local.properties`** with:

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=
```

- `sdk.dir` — the SDK path you copied in step 1. **Use double backslashes** (`\\`) or forward slashes (`/`); a single `\` will be read as a Java escape.
- `MAPS_API_KEY` — leave empty for now. Map tiles render gray and address autocomplete is disabled without one, but the rest of the app (including saved entries) works fine. The full Google Cloud walkthrough — creating the key, enabling Maps SDK plus both Places APIs (new + legacy, the autocomplete widget needs both), and setting cost guards so you can't be charged — is in **[CLOUD-SETUP.md](CLOUD-SETUP.md)**.

### **4. Open the project in Android Studio**

`File → Open → ` select the **`Restaurant-App`** folder (not a subfolder). Android Studio runs a Gradle sync that downloads ~1 GB of dependencies the first time — takes 2–5 minutes. Wait until the bottom status bar reads "Gradle sync finished".

If the sync fails complaining about an SDK component, click the offered "Install missing platform/build-tools" link.

### **5. Create an emulator**

`Tools → Device Manager → Create Device`. Pick **Pixel 6**, click Next, pick the **API 34 (Android 14)** system image (download it if there's an arrow icon next to it), click Next, then Finish.

In Device Manager, click the ▶ play icon next to the new device. A phone-shaped window opens and boots Android — this takes 30–60 seconds the first time.

### **6. Run the app**

With the emulator running, click the green **▶ Run 'app'** button in the toolbar (or press `Shift+F10`). Android Studio builds the debug APK, installs it on the emulator, and launches it. The emulator shows the empty-list screen with a "+ Add restaurant" button.

### **7. Use it**

Tap **+ Add restaurant**, fill in the form, tap **Save**. The entry appears in the list. Tap the entry to see the detail view; the edit and delete actions are in the top app bar.

Things to know on the emulator:
- **Camera** uses a synthetic test image, not a real photo. Use **Choose from gallery** for realistic photo testing — drag a JPG from your desktop onto the emulator window to add it to the gallery first.
- **Address autocomplete** needs the Places API enabled on your key plus the emulator's debug SHA-1 added to the key restriction (see [CLOUD-SETUP.md](CLOUD-SETUP.md) step 9). Without that the autocomplete activity closes immediately on tap.
- **Detail map preview** centers on the saved pin. If you haven't picked an address yet, the map section is hidden — there's no separate map picker in the edit flow.
- **Hot reload** — when you change Kotlin code, just press ▶ again; Android Studio rebuilds and reinstalls in a few seconds.

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
   adb shell am start -n com.restauranttracker/.MainActivity
   ```

## Logs

In Android Studio, the **Logcat** panel at the bottom shows live device output. Filter for `com.restauranttracker` to isolate the app.

From the command line:

```powershell
adb logcat -s RestaurantApp PhotoStorage
```

## Architecture

| Module | Description |
|---|---|
| [app/build.gradle.kts](app/build.gradle.kts) | Module config — pulls `MAPS_API_KEY` from `local.properties` into the manifest. |
| [RestaurantApp.kt](app/src/main/java/com/restauranttracker/RestaurantApp.kt) | Application class. Lazy-initializes the Room DB and PhotoStorage. |
| [MainActivity.kt](app/src/main/java/com/restauranttracker/MainActivity.kt) | Single activity. Sets the Compose content and theme. |
| [data/](app/src/main/java/com/restauranttracker/data/) | `Restaurant` entity, `RestaurantDao` (Flow + suspend), `RestaurantDatabase`, JSON helpers for photo paths. |
| [ui/list/](app/src/main/java/com/restauranttracker/ui/list/) | List screen, search/filter/sort ViewModel. |
| [ui/detail/](app/src/main/java/com/restauranttracker/ui/detail/) | Detail screen with embedded read-only map + delete flow. |
| [ui/edit/](app/src/main/java/com/restauranttracker/ui/edit/) | New/edit form, map picker, photo capture/import. |
| [ui/common/](app/src/main/java/com/restauranttracker/ui/common/) | Reusable composables (`RatingStars`, `DatePickerField`, `PhotoGrid`). |
| [util/PhotoStorage.kt](app/src/main/java/com/restauranttracker/util/PhotoStorage.kt) | Copies captured/imported photos into `filesDir/photos/` and returns stable paths. |
| [util/Formatting.kt](app/src/main/java/com/restauranttracker/util/Formatting.kt) | Date and currency formatters. |

## Features

| Surface | Description |
|---|---|
| **List screen** | All restaurants you've recorded, with text search, sort (recency / rating / name / price), and filter chips (min rating, has-photo). |
| **Add/edit form** | Name, date visited, 1–5 star rating, who you went with, dish price + currency, notes, address text, optional map pin, photos from camera or gallery. |
| **Detail screen** | All fields, embedded read-only map if a pin is set, "Open in Maps" intent, edit, delete. |

All data is stored locally in a Room database at the app's private data directory. Photos are copied into the app's `filesDir/photos/`. Nothing leaves the device.

## Limitations

- Requires a Google Maps SDK key to render map tiles. Without one, the map area is blank but pin coordinates still save.
- No cloud sync, no export/import. Backups are the user's responsibility (Android's auto-backup will capture the DB and photos).
- No reverse geocoding — the address text field is whatever you type.
- Map picker centers on (0, 0) when no initial pin is set; tap "Use my location" (granting location permission) to recenter.
- Currency is per-row and free-text; no validation beyond "is this a known ISO 4217 code". Unknown codes fall back to the device default at format time.

## Authors

- [@Pekkapost](https://github.com/Pekkapost) — Author
