# Release Build Setup

One-time setup for building signed release APKs you can install on a personal Android phone and persist data across updates (same signing key = same Android app identity, so every reinstall keeps your existing entries, settings, theme, and photos).

The build is already wired for this — `app/build.gradle.kts` picks up a release signing config when a `keystore.properties` file exists in the repo root. You just need to generate the keystore, fill in the credentials file, register the keystore's fingerprint with Google Cloud, and back it up.

The full Google Cloud key + APIs setup lives in [CLOUD-SETUP.md](CLOUD-SETUP.md); that needs to be done before this. Both `local.properties` (containing `MAPS_API_KEY`) and `keystore.properties` (containing the release-signing credentials) must exist on the build machine.

## 1. Pull the latest

```bash
cd /path/to/Dango-Diary
git pull
```

Make sure the repo includes `keystore.properties.example` in the root — that's the template you'll copy in step 3.

## 2. Generate the release keystore

```bash
keytool -genkey -v -keystore release.keystore -alias dangodiary \
  -keyalg RSA -keysize 2048 -validity 36500
```

`-validity 36500` (≈100 years) means you won't have to deal with key expiry. Default 10000 (~27 years) is fine too; longer is just easier to forget about.

Prompts, in order:

1. **`Enter keystore password:`** Pick a strong one and **save it in a password manager *now*** before pressing Enter. Input is not echoed.
2. **`Re-enter new password:`** Same one.
3. **`What is your first and last name?`** Anything — goes into the cert as `CN=…`. Real name or `Pekkapost` is fine.
4. **`What is the name of your organizational unit?` ... country code:** Press Enter through these or fill in. None of it matters technically.
5. **`Is CN=…, OU=…, O=…, …, correct?`** Type `yes`.
6. **`Enter key password for <dangodiary> (RETURN if same as keystore password):`** Press Enter to reuse the keystore password (simpler).

You'll end up with `release.keystore` in the repo root (~2.5 KB). It's already gitignored.

```bash
ls -la release.keystore   # confirm it exists
```

## 3. Create `keystore.properties`

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` and replace both password placeholders with the password from step 2:

```properties
storeFile=release.keystore
storePassword=<your password>
keyAlias=dangodiary
keyPassword=<your password>
```

This file is gitignored.

## 4. Back up the keystore + password

**Do this now, before going further.** Two copies of each, in different places:

| Artifact | Where |
|---|---|
| `release.keystore` file (~2.5 KB) | Encrypted folder in cloud storage (Drive / Dropbox / iCloud), or attached to a password-manager entry that supports file attachments (1Password, Bitwarden Premium, ...) |
| The password itself | Password manager entry titled e.g. "Dango Diary release keystore" |

**If you lose either, every installed copy of the app becomes orphaned** — you'd have to uninstall (losing all data) and re-install with a new keystore.

## 5. Get the release SHA-1 fingerprint

```bash
keytool -list -v -keystore release.keystore -alias dangodiary
```

Enter your keystore password. In the output, find the line that starts with `SHA1:`:

```
SHA1: AB:CD:EF:01:23:45:...:9A:BC
```

Copy that hex string (value only, no `SHA1:` prefix).

## 6. Add the SHA-1 to your Google API key

In a browser:

1. <https://console.cloud.google.com/apis/credentials>
2. Verify the right project is selected at the top (the Dango Diary one).
3. Click the row for your Maps API key.
4. Under **Application restrictions → Android apps**, you should already see one entry for the debug keystore. Click **Add an item**.
5. **Package name:** `com.dangodiary`
6. **SHA-1 certificate fingerprint:** paste the hex string from step 5.
7. Click **Done** on the row, then **Save** at the bottom of the page.

Google Cloud propagates the change in seconds — sometimes ~1 minute.

## 7. Build the release APK

```bash
./gradlew assembleRelease
```

Output:

```
app/build/outputs/apk/release/app-release.apk
```

If you see `SigningConfig with name 'release' not found`, your `keystore.properties` file is missing or has a typo — re-check step 3.

## 8. Install on your phone

### Option A — USB (easiest)

If Developer options aren't enabled yet on the phone:

- Settings → About phone → tap **Build number** 7 times.
- System → Developer options → toggle **USB debugging** on.
- Plug the phone in. On the phone, accept **"Allow USB debugging from this computer?"** (check "Always allow").

Then:

```bash
adb devices   # should list the phone

adb install -r app/build/outputs/apk/release/app-release.apk
```

`Success` means installed.

### Option B — No USB

Put the APK in a cloud-storage folder accessible from the phone (Drive, Dropbox, etc.), tap it on the phone to install. First time, Android will ask you to allow "install unknown apps" for the app that delivered the file.

## 9. Smoke test on the phone

| Check | What it confirms |
|---|---|
| **Type a restaurant name** in the Name field → Places suggestions drop down after ~300 ms | Step 6's SHA-1 + API key chain works on this device. If nothing after 60 s, re-check step 6 for typos. |
| **Save the entry** → it appears in the list | Insert path works. |
| **Force-stop the app** (Settings → Apps → Dango Diary → Force stop), reopen | Room DB persists across process restarts. |
| **Theme picker** in Settings → switch between presets | DataStore reads + reactive theme work. |
| **Detail screen → ⋮ → Search on Yelp** | Opens Yelp app (deep link) if installed, browser otherwise. |

## Day-to-day, after code changes

```bash
git pull                            # if changes came from elsewhere
# edit code...
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Data persists across reinstalls because the signing key is the same. Icon doesn't change, settings stay, entries stay.

## Common gotchas

| Symptom | Cause + fix |
|---|---|
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` on `adb install -r` | The installed APK is signed with a different key than the one you're installing. Either you regenerated the keystore (don't!) or you're installing a debug APK over a release one. Fix: pick one signing config and stay there. If you must switch, `adb uninstall com.dangodiary` — but this wipes data. |
| Maps + Places stop working after a Google Cloud edit | Usually a typo in the SHA-1 or the wrong project selected. Cross-check Cloud Console → Credentials → your key → Application restrictions. |
| You move to a new build machine | Copy `release.keystore` and `keystore.properties` from your backup to the repo root on the new machine. `assembleRelease` will use them. |
| Forgot the keystore password | No recovery. The keystore is unrecoverable. You'll need to generate a new one (step 2), add its SHA-1 to the API key (step 6), uninstall the old app on each device, install the new one (loses all data). Don't forget the password. |
