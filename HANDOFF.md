# Handoff: set up the TCL TV as a clean, ad-free kids TV

Hi, local Claude Code session. You are running on a PC on the same LAN as the
TV, which the cloud session that wrote this could not reach. Your job is to run
the setup in this repo against the TV and check that it works.

## The TV

| Field | Value |
|---|---|
| Model / device name | TCL Android TV, "BeyondTV" |
| Firmware | V8-R851T02-LF1V620.018337 |
| IP (DHCP, Wi-Fi "Alaa") | **10.73.1.89** (adb: `10.73.1.89:5555`) |
| MAC | d4:ab:cd:74:b0:9b |

The goal is a clean home screen with big, kid-friendly tiles and **no ads**.

## What is already built (PR #1, branch `kids-home-launcher`)

- `app/` is **Kids Home**, a small Android TV launcher in plain Java with no
  dependencies. Package `com.mytv.kidshome`, home activity `.HomeActivity`.
  - It shows only parent-approved apps as big tiles. Back does nothing.
  - The Parents button needs a PIN (default `1234`). Behind it you can pick
    the apps, change the PIN and open TV settings.
  - On first run it shows YouTube Kids, Netflix, Disney+ and PBS KIDS, if
    they are installed.
- `scripts/setup.ps1` (Windows) and `scripts/setup.sh` (macOS/Linux) do four
  steps:
  1. Install `KidsHome.apk`.
  2. Run `pm disable-user` on each entry in `scripts/debloat-list.txt`.
     Packages the TV doesn't have are skipped.
  3. Set private DNS to `family.adguard-dns.com`.
  4. Try `cmd package set-home-activity`.

  Flags: `-Replace` / `--replace` also disables the stock launcher, and
  `-Scan` / `--scan` lists TCL and promo packages.
- `scripts/restore.ps1` / `restore.sh` undo everything. The stock launcher
  that `-Replace` disabled is recorded in `scripts/.stock-launcher`.
- CI (`.github/workflows/build.yml`) builds the APK. It passed on PR #1
  (run 39636178691, artifact **KidsHome**). A Release (and the
  `releases/latest/download/KidsHome.apk` URL the script uses) only exists
  after the PR is merged to `main`.

## Steps for you

1. `git clone https://github.com/ayahya1983/My-tv && cd My-tv && git checkout kids-home-launcher`
2. Make sure `adb` exists. On Windows, run `winget install Google.PlatformTools`
   and restart the shell.
3. Get the APK into `scripts/KidsHome.apk`. Pick one:
   - Download the **KidsHome** artifact from the Actions run above (it's a
     zip) and unzip it.
   - Build it locally with `./gradlew assembleDebug`, if the Android SDK is
     installed, then copy `app/build/outputs/apk/debug/app-debug.apk`.
   - Merge PR #1 (ask the user first) and let the script download it.
4. Ask the user to confirm that **USB debugging** (and **Network debugging**,
   if listed) is on. The setting is under Settings → Device Preferences →
   Developer options. Developer options are unlocked by pressing OK 7 times on
   About → Build / Product Information.
5. Run `adb connect 10.73.1.89:5555`. The user must accept **"Allow
   debugging"** on the TV and tick Always allow.
6. **Scan first** with `scripts/setup.ps1 -Scan` (or `./setup.sh --scan`).
   - The TCL names in `debloat-list.txt` are best guesses. Review the scan
     against that list and show the user what will be disabled.
   - Never disable input-source, tuner, settings, keyboard, system UI, Google
     Play services or casting packages.
7. Run `scripts/setup.ps1` (or `./setup.sh`).
8. Check the result:
   - Run `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`.
     It should resolve to `com.mytv.kidshome/.HomeActivity`.
   - If it doesn't, explain the risk to the user and get their OK before
     running with `-Replace`. On some TCL firmware the stock launcher also
     provides the input-source menu.
   - Ask the user to press Home on the remote and confirm Kids Home appears.
   - Ask them to open an approved app and confirm it launches.
   - Ask them to open Parents with PIN 1234 and **change the PIN**.
9. If you change scripts or the debloat list, commit to `kids-home-launcher`
   and push. PR #1 is open as a draft.

## Things not verified yet

- The app hasn't run on a real TV yet; it was only compiled in CI.
  Specifically unverified:
  - Tile focus and scaling with the D-pad.
  - That the PIN dialog's on-screen keyboard works with the remote.
  - That banners load for every app.
- Whether this TCL firmware honours `set-home-activity`. Some Android TV
  builds ignore it, which is what `-Replace` is for.
- Whether the TV's Android version supports Private DNS (Android 9+). Check
  with `adb shell getprop ro.build.version.release`.
- Easy undo at any time: `scripts/restore.ps1` / `restore.sh`.
