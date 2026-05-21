# Testing on this server (headless emulator + remote scrcpy)

The server has no display, but it does have KVM + a headless Android 14 emulator. You drive it from your **local machine** by tunneling adb over SSH and running scrcpy locally — the mirror window shows the emulator running on the server.

## What's already set up here

- JDK 17 at `/usr/lib/jvm/java-17-openjdk-amd64`
- Gradle 8.9 wrapper in the project
- Android SDK at `/home/amd/tools/android-sdk` (platform-tools, build-tools 34, platform 34, emulator, Google APIs system image x86_64)
- AVD `rt_test` (Pixel 6, Android 14)
- scrcpy 1.25 (system package, used here only for sanity checks)
- App APK built at [app/build/outputs/apk/debug/app-debug.apk](app/build/outputs/apk/debug/app-debug.apk) and installed on the emulator
- You are in the `kvm` group (effective on next login; current session uses `sg kvm` shim)

## Boot the emulator (server side)

```bash
export ANDROID_HOME=/home/amd/tools/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

# In a fresh shell (after re-login so kvm group is active):
nohup emulator -avd rt_test -no-window -no-audio -no-boot-anim \
    -gpu swiftshader_indirect -no-snapshot -port 5554 \
    > /home/amd/tools/emulator-logs/emu.log 2>&1 &

# Wait for boot
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb devices   # should show emulator-5554 device
```

If you haven't re-logged in yet, wrap the `emulator …` call in `sg kvm -c '…'`.

## Mirror it from your local machine (scrcpy)

This part runs on **your laptop / workstation**, not the server.

### One-time, local-machine prep

- Install scrcpy 2.x (Linux: `apt install scrcpy`; macOS: `brew install scrcpy`; Windows: download from <https://github.com/Genymobile/scrcpy/releases>).
- Install `adb` (Linux: `apt install adb`; macOS: `brew install android-platform-tools`; Windows: SDK platform-tools zip).

### Each session

```bash
# 1. Free local adb so the tunnel can bind port 5037.
adb kill-server

# 2. Open the SSH tunnel. Leave this terminal open.
ssh -NL 5037:localhost:5037 amd@10.216.183.241
#                                 ^^^^^^^^^^^^^^
#                                 server IP — replace with hostname if you have DNS

# 3. In a SECOND local terminal:
adb devices       # should show 'emulator-5554  device' — proves the tunnel works
scrcpy            # opens a window mirroring the emulator
```

`adb` on your laptop is now talking to the adb daemon **on the server**, which is talking to the emulator. scrcpy pushes its server jar to the emulator through that path and streams H.264 video back over the same tunnel.

Expect 10–30 fps depending on your SSH latency. Mouse, keyboard, and (on scrcpy 2.x) drag-and-drop file install all work.

## What to test (matches the plan's verification steps)

1. **Empty list renders** — app launches to "No restaurants yet. Tap Add restaurant to add your first." Search with no matches reads "No restaurants match your search."
2. **Add restaurant** — tap the FAB, fill in name + date + rating + companions + price + address, place a map pin, attach a photo, save. The list shows the new entry.
3. **Detail** — tap the entry. All fields render, map is embedded read-only.
4. **Edit** — change the rating, save, confirm list reflects the change.
5. **Search / sort / filter** — add a second restaurant, search by name, sort by rating, filter by min-rating.
6. **Delete** — confirm delete dialog, row disappears from list.
7. **Process death** — `adb shell am force-stop com.dangodiary`, relaunch — data still there.

## Caveats

- **Map tiles** on the detail screen will render gray without a Maps SDK API key in [local.properties](local.properties). Pin coordinates still save/load correctly. Add `MAPS_API_KEY=…` and rebuild to fix. Full GCP walkthrough in [CLOUD-SETUP.md](CLOUD-SETUP.md).
- **Address autocomplete** needs **both** Places APIs enabled on the same key (the new one for programmatic calls, the legacy one because the fullscreen autocomplete widget still hits it under the hood), plus the emulator's debug SHA-1 added to the key's Android-app restriction. Without that the autocomplete activity closes immediately on tap — check Logcat for `tag:AddressAutocomplete` to confirm. Full setup in [CLOUD-SETUP.md](CLOUD-SETUP.md).
- **Camera capture** has no real camera on the emulator — it uses a synthetic test image. The gallery picker / `Pick photo from gallery` flow is the realistic path on the emulator.
- **scrcpy 1.x clipboard warning** in the server logs is harmless — affects clipboard sync only, not display or input. scrcpy 2.x on your local side avoids it.

## Useful adb commands (run on server)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk   # reinstall after rebuild
adb shell am start -n com.dangodiary/.MainActivity   # launch
adb shell am force-stop com.dangodiary               # kill
adb exec-out screencap -p > shot.png                        # one-off screenshot
adb logcat -s 'com.dangodiary:V' '*:E'               # app + errors
adb emu kill                                                # stop the emulator cleanly
```

## Stop the emulator

```bash
adb emu kill
# or
pkill -f qemu-system-x86_64-headless
```
