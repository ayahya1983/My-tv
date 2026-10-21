# My TV – Kids Home

A clean, ad-free, kid-friendly home screen for the TCL Android TV
(`BeyondTV`, firmware `V8-R851T02`, IP `10.73.1.89`), plus a one-command setup
that you run from any computer on the same Wi-Fi.

What you get:

- **Kids Home launcher.** Big, colourful tiles that show only the apps a parent
  approved. There are no feeds, no sponsored rows and no ads. The Back button
  stays on the home screen.
- **Parents button, protected by a PIN** (default `1234`). It lets you tick
  which apps kids can see, change the PIN, and open TV settings.
- **Ad and promo apps switched off.** TCL promo services and Android TV's
  sponsored recommendation rows are disabled. They are disabled, not
  uninstalled, so the change can be undone.
- **Family DNS.** AdGuard Family DNS blocks ads and adult sites for every app
  on the TV.
- **Fully reversible.** The `restore` script puts the TV back the way it was.

On first run, Kids Home shows YouTube Kids, Netflix, Disney+ and PBS KIDS if
they are installed. You can change that list from the Parents button.

## 1. Turn on debugging on the TV (one time)

1. Go to **Settings → Device Preferences → About**.
2. Highlight **Build** (on some TCL menus it's **Product Information**) and
   press OK **7 times** until you see *"You are now a developer"*.
3. Go back to **Settings → Device Preferences → Developer options** and turn on
   **USB debugging**. If you also see **Network debugging** or
   **ADB over network**, turn that on too.

## 2. Run the setup from a computer on the same Wi-Fi

**Mac / Linux**

```bash
brew install android-platform-tools   # macOS (Linux: sudo apt install adb)
git clone https://github.com/ayahya1983/My-tv && cd My-tv/scripts
./setup.sh                            # uses 10.73.1.89 by default
```

**Windows (PowerShell)**

```powershell
winget install Google.PlatformTools
git clone https://github.com/ayahya1983/My-tv; cd My-tv\scripts
.\setup.ps1
```

When the TV asks **"Allow debugging?"**, tick *Always allow* and choose **OK**.

The script downloads the latest `KidsHome.apk` from this repo's Releases. If
the repo is private, download the APK by hand from **Releases** (or from the
**Actions → KidsHome** artifact) and save it as `scripts/KidsHome.apk` first.

### If the Home button still opens the TCL screen

```bash
./setup.sh --replace        # Windows: .\setup.ps1 -Replace
```

This also disables the stock TCL launcher, so Home always opens Kids Home.

### Want to switch off more TCL apps?

```bash
./setup.sh --scan           # lists TCL / promo packages on your TV
```

Add any you want gone to `scripts/debloat-list.txt` and run `./setup.sh` again.
Don't add anything that sounds like input sources, the tuner, settings or the
keyboard.

## Undo everything

```bash
./restore.sh                # Windows: .\restore.ps1
```

## Extra tips for kids

- Inside YouTube Kids, pick an age profile and turn **Search off**.
- In Netflix and Disney+, open the **Kids** profile and lock the adult
  profiles with a PIN.
- In the Google Play Store on the TV, open **Settings → Parental controls**,
  then set a rating limit and a PIN.

## Building the app

GitHub Actions builds `KidsHome.apk` on every push. Pushes to `main` publish it
as a release. To build it yourself with the Android SDK, run
`./gradlew assembleDebug`.
