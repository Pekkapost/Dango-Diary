# Google Cloud Setup

Walkthrough for setting up the Google Cloud project, APIs, and key that this app needs for its maps and address-autocomplete features. Assumes you're starting fresh with no existing project.

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
- Project name: `restaurant-app` (or whatever)
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
- Back to Library → search `Places API (New)` (the one labelled **New**, not the older "Places API") → **Enable**
- Each takes ~30 seconds to provision.

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

- **APIs & Services → Enabled APIs & Services → Places API (New)**
- Top tabs → **Quotas & System Limits**
- Find the **per day** quota row (for Places New this may show as "Autocomplete requests per day" or similar — pick the most restrictive day-scoped one)
- Check its box → pencil/edit icon at top → set to `100` → **Save**
- Back → **Enabled APIs → Maps SDK for Android → Quotas & System Limits**
- Find **Map loads per day** → edit → set to `1000` → **Save**

Quotas can take a few minutes to apply.

## 8. Set a $1 budget alert

- Left sidebar → **Billing → Budgets & alerts → Create budget**
- Name: `restaurant-app-watch`
- Scope: leave at default (your project)
- Amount → Target amount: `1` USD
- Next → Actions: check **Email alerts to billing admins and users**
- Thresholds: defaults (50%, 90%, 100%) are fine; add a fourth at 10% if you want even earlier warning
- **Finish**

This won't *stop* spending, but it tells you within a day if something is wrong. Step 7 stops the bleeding; step 8 tells you to look.

## 9. Restrict the API key

Back to the Credentials tab from step 5 (or **APIs & Services → Credentials** → click your key name).

- **Application restrictions** → **Android apps** → **Add an item**:
  - Package name: `com.restauranttracker`
  - SHA-1: get it via one of the three methods below
- **API restrictions** → **Restrict key** → in the dropdown check **only**:
  - Maps SDK for Android
  - Places API (New)
- **Save** at the bottom

When you eventually sign a release build, come back here and add the release keystore's SHA-1 as a second entry.

### Getting your debug SHA-1 fingerprint

Pick whichever is least painful:

#### Option A — Android Studio (easiest, no terminal)

If Android Studio is open with this project:

1. Right side of the IDE → click the **Gradle** tab (elephant icon)
2. Drill down: `Restaurant-App` → `:app` → `Tasks` → `android` → double-click **signingReport**
3. The Run/Build panel at the bottom prints:
   ```
   Variant: debug
   Config: debug
   ...
   SHA1: 12:34:56:78:9A:BC:...:EF
   ```
4. Copy the SHA1 value (the colon-separated hex string).

#### Option B — Windows Command Prompt

`keytool` lives inside the JDK that Android Studio bundles. Open Command Prompt (Start menu → type `cmd`) and paste:

```
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr SHA1
```

If that errors with "file not found," try replacing `jbr` with `jre` in the path. If both fail, use Option A.

#### Option C — Windows PowerShell

```
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | Select-String SHA1
```

#### Option D — macOS / Linux

```
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

If `~/.android/debug.keystore` doesn't exist yet, build the app once in Android Studio first — it generates the debug keystore on first build.

## 10. Rebuild and confirm

- From the project root:
  ```
  ./gradlew clean :app:assembleDebug
  ```
  (or run it from Android Studio.) This picks up the new key and the `BuildConfig` wiring.
- Install and run the app → tap an Address field on a new restaurant → Google's autocomplete UI should launch.
- If it doesn't launch, run `adb logcat | grep AddressAutocomplete` and check for an error. The message names what's missing:
  - **"API has not been enabled"** → step 4 didn't take, or the wrong Places API is enabled
  - **"key not authorized" / "API_KEY_HTTP_REFERRER_BLOCKED"** → step 9's SHA-1 or package name doesn't match (most common cause: typo in SHA-1, or you're testing a release build with only the debug SHA-1 registered)
  - **"This API key is not authorized to use this service"** → step 9's API restrictions list doesn't include Places API (New)

---

## What happens if something goes wrong later

- **You get a budget alert email** → check the Billing dashboard's "Reports" section to see which API is racking up requests, then investigate the app or revoke the key (Credentials → click key → Delete) and create a new one
- **Autocomplete suddenly stops working** → most likely the daily quota cap was hit. Either bump it (step 7) or wait until the next day for the reset
- **You want to disable everything immediately** → Billing → Account management → **Close billing account**, or unlink it from the project. All Maps Platform calls will fail until you re-link.
