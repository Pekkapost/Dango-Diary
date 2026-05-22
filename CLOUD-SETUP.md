# Google Cloud Setup

Walkthrough for setting up the Google Cloud project, APIs, and key that this app needs for its map and restaurant-name autocomplete features. Assumes you're starting fresh with no existing project.

After this you'll have:

- A Cloud project with billing enabled
- Maps SDK for Android + Places API (New) enabled
- An API key that only your signed app can use
- Hard daily quota caps that make overage requests fail instead of charge
- A $1 budget email alert as an early-warning safety net

The combination makes a surprise bill essentially impossible at personal-use scale — the free $200/month Maps Platform credit covers ~70k autocomplete searches before a cent hits your card, and the quota caps stop runaway requests long before that.

---

## 1. Sign in and create a Cloud account

- Go to <https://console.cloud.google.com>
- Sign in with the Google account you want to own this. **Use a personal Google account, not a school/work one** — those can have admin restrictions that block billing or API setup.
- First-time only: accept the welcome terms.

## 2. Create a new project

- Top bar, project selector (left of the search box) → **New Project**
- Project name: `dango-diary` (or whatever)
- Organization/Location: leave at "No organization" for a personal account
- Click **Create**
- Wait ~10 seconds for it to provision, then make sure the top-bar selector switches to it (sometimes you have to click in and pick it manually)

## 3. Set up a billing account and link it

- Left sidebar → **Billing**
- **Manage billing accounts** → **Create account**
- Account name: `personal` (or whatever); country/currency: yours
- Add a payment method (credit/debit card) → Confirm
- Google offers a **free trial credit** (currently $300 over 90 days for new accounts) — accept it; this stacks with the standing $200/mo Maps Platform credit
- Back to **Billing** → **Link a billing account** → pick the one you just made → **Set account**

## 4. Enable the two APIs you need

- Left sidebar → **APIs & Services → Library**
- Search `Maps SDK for Android` → click it → **Enable**
- Back to Library → search `Places API (New)` (the one labelled **New**) → **Enable**

Each takes ~30 seconds to provision.

> **Just the new Places API?** Yes — the app uses the programmatic `findAutocompletePredictions` / `fetchPlace` calls from the Places SDK, which go to the new API. The legacy Places API isn't needed.

## 5. Create the API key

- **APIs & Services → Credentials** → top bar **+ Create Credentials → API key**
- A modal pops up with your new key — **copy it now**; you'll paste it into the app in the next step
- Click **Edit API key** (or close the modal and click into the key from the list) — keep this tab open, you'll come back in step 9 to restrict it

## 6. Paste the key into the app

- Open `local.properties` in the project root
- Add (or replace) this line:
  ```
  MAPS_API_KEY=PASTE_YOUR_KEY_HERE
  ```
- Save. Don't commit this file — `local.properties` is already gitignored.

## 7. Cap the daily quotas — the actual cost guard

This step is the real safety net. Even if your key leaks or you typo something into an infinite loop, you cannot be charged for usage that the API refuses to serve.

Repeat the same shape for each API:

- **APIs & Services → Enabled APIs & Services** → click the API name
- Top tabs → **Quotas & System Limits**
- Find the most restrictive **per day** quota row
- Check its box → pencil/edit icon at top → set the cap → **Save**

Caps to set:

| API | Quota | Cap |
|---|---|---|
| Places API (New) | Autocomplete requests per day (or similar per-day row) | `100` |
| Maps SDK for Android | Map loads per day | `1000` |

Quotas can take a few minutes to apply.

## 8. Set a $1 budget alert

- Left sidebar → **Billing → Budgets & alerts → Create budget**
- Name: `dango-diary-watch`
- Scope: leave at default (your project)
- Amount → Target amount: `1` USD
- Next → Actions: check **Email alerts to billing admins and users**
- Thresholds: defaults (50%, 90%, 100%) are fine; add a fourth at 10% if you want even earlier warning
- **Finish**

This won't *stop* spending, but it tells you within a day if something is wrong. Step 7 stops the bleeding; step 8 tells you to look.

## 9. Restrict the API key

Back to the Credentials tab from step 5 (or **APIs & Services → Credentials** → click your key name).

- **Application restrictions** → **Android apps** → **Add an item**:
  - Package name: `com.dangodiary`
  - SHA-1: get it via one of the three methods below
- **API restrictions** → **Restrict key** → in the dropdown check **only**:
  - Maps SDK for Android
  - Places API (New)
- **Save** at the bottom

When you eventually sign a release build, come back here and add the release keystore's SHA-1 as a second entry.

### Getting your debug SHA-1 fingerprint

Pick whichever is least painful:

#### Option A — Run `signingReport` from a terminal (recommended)

Open a terminal inside the project. The fastest way from Android Studio: **Alt+F12** (or **View → Tool Windows → Terminal**) — it opens already positioned in your project directory. Then run the command for your shell:

- **Windows PowerShell** (the IDE terminal defaults to this; prompt starts with `PS C:\...>`):
  ```
  .\gradlew signingReport
  ```
- **Windows Command Prompt** (prompt starts with `C:\...>`):
  ```
  gradlew.bat signingReport
  ```
- **macOS / Linux**:
  ```
  ./gradlew signingReport
  ```

First run takes 30–60 seconds while Gradle warms up. Output includes a block like:

```
Variant: debug
Config: debug
...
SHA1: 12:34:56:78:9A:BC:...:EF
SHA-256: ...
```

Copy the SHA1 value (the colon-separated hex string).

#### Option B — Android Studio Gradle tool window

If you prefer clicking to typing:

1. **View → Tool Windows → Gradle** (the panel is hidden by default in recent Android Studio versions; it pins to the right edge)
2. Expand `Dango-Diary` → `:app` → `Tasks` → `android` → double-click **signingReport**
3. The Run/Build panel at the bottom prints the same SHA1 block as Option A

If you expand **Tasks** and see only `other` (no `android` group), Android Studio is suppressing the task list for performance. Fix: **File → Settings → Experimental → Gradle** → uncheck **"Do not build Gradle task list during Gradle sync"** → resync.

#### Option C — `keytool` direct (no project required)

Useful if you don't have the project open. `keytool` lives inside the JDK that Android Studio bundles.

- **Windows Command Prompt**:
  ```
  "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr SHA1
  ```
  If "file not found," try replacing `jbr` with `jre` in the path.

- **Windows PowerShell**:
  ```
  & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String SHA1
  ```

- **macOS / Linux**:
  ```
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
  ```

If `~/.android/debug.keystore` (or its Windows equivalent at `%USERPROFILE%\.android\debug.keystore`) doesn't exist yet, build the app once in Android Studio first — it generates the debug keystore on first build.

## 10. Rebuild and confirm

- From the project root:
  ```
  ./gradlew clean :app:assembleDebug
  ```
  (or run it from Android Studio.) This picks up the new key and the `BuildConfig` wiring.
- Install and run the app → on a new entry, type a restaurant name in the Name field → suggestions should drop down from the field after ~300 ms.
- If no suggestions appear, check Logcat for the failure status. Easiest path: in Android Studio, **View → Tool Windows → Logcat** (or **Alt+6**), then type `tag:RestaurantNameField` into the filter bar at the top. Type into the Name field and watch for a line like `W/RestaurantNameField: Autocomplete query failed for '<name>': <reason>`. From a terminal you can run the same thing with `adb logcat -s RestaurantNameField` (use `"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"` if `adb` isn't on your PATH on Windows). The status message names what's missing:
  - **"API has not been enabled"** → step 4 didn't take for Places API (New); re-enable it.
  - **"key not authorized" / "API_KEY_HTTP_REFERRER_BLOCKED"** → step 9's SHA-1 or package name doesn't match (most common cause: typo in SHA-1, or you're testing a release build with only the debug SHA-1 registered).
  - **"This API key is not authorized to use this service"** → step 9's API restrictions list is missing Places API (New).

---

## What happens if something goes wrong later

- **You get a budget alert email** → check the Billing dashboard's "Reports" section to see which API is racking up requests, then investigate the app or revoke the key (Credentials → click key → Delete) and create a new one
- **Autocomplete suddenly stops working** → most likely the daily quota cap was hit. Either bump it (step 7) or wait until the next day for the reset
- **You want to disable everything immediately** → Billing → Account management → **Close billing account**, or unlink it from the project. All Maps Platform calls will fail until you re-link.
