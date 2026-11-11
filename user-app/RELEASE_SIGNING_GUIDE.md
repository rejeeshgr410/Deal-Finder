# Android Release Signing & Build Guide

Good news: `app/build.gradle.kts` **already has a working release signing config**.
It reads the keystore location and passwords from environment variables, so you do
**not** need to edit the build file. You only need to (1) create an upload keystore
and (2) set three env vars, then build.

The relevant config (already in your `app/build.gradle.kts`):
```kotlin
signingConfigs {
  create("release") {
    val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
    storeFile = file(keystorePath)
    storePassword = System.getenv("STORE_PASSWORD")
    keyAlias = "upload"
    keyPassword = System.getenv("KEY_PASSWORD")
  }
}
```
Note the **key alias must be `upload`** — use that exact alias when creating the keystore.

---

## Step 1 — Create the upload keystore (ONE TIME)

Run this in the `user-app` folder. It creates `my-upload-key.jks`.

**Windows (PowerShell or CMD):**
```bat
keytool -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

`keytool` ships with the JDK that Android Studio installs. If "keytool is not
recognized", use the full path, e.g.:
```
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

It will prompt for:
- A **keystore password** (remember it → this is STORE_PASSWORD)
- Your name / org / location (can be anything reasonable)
- A **key password** (you can press Enter to reuse the keystore password → KEY_PASSWORD)

### ⚠️ CRITICAL — back this file up
`my-upload-key.jks` signs every update to your app. **If you lose it, you can never
update your app again** (you'd have to publish a brand-new listing). Copy it somewhere
safe (password manager / encrypted backup). **Do NOT commit it to git** — it's already
covered by `.gitignore` patterns, but double-check.

---

## Step 2 — Set the three environment variables

The build reads these at build time. Set them in the same terminal you'll build from.

**Windows (CMD):**
```bat
set KEYSTORE_PATH=C:\Deal Finder\user-app\my-upload-key.jks
set STORE_PASSWORD=your_keystore_password
set KEY_PASSWORD=your_key_password
```

**Windows (PowerShell):**
```powershell
$env:KEYSTORE_PATH="C:\Deal Finder\user-app\my-upload-key.jks"
$env:STORE_PASSWORD="your_keystore_password"
$env:KEY_PASSWORD="your_key_password"
```

(If KEYSTORE_PATH is left unset, the build falls back to `my-upload-key.jks` in the
project root, which is where Step 1 created it — so that env var is optional if you
keep the default location.)

---

## Step 3 — Build a signed release AAB

Google Play wants an **Android App Bundle (.aab)**, not an APK.

**In Android Studio (easiest):**
1. Open the `user-app` project. Let it sync / generate the Gradle wrapper.
2. Remember the README's note: if the project won't build because of the debug
   keystore, you can remove `signingConfig = signingConfigs.getByName("debugConfig")`
   from the `debug { }` block — this only affects debug builds, not your release.
3. Menu: **Build → Generate Signed Bundle / APK → Android App Bundle**.
4. Choose your `my-upload-key.jks`, alias `upload`, enter the passwords.
5. Build variant: **release**. The `.aab` lands in `app/release/`.

**Or via command line (once the Gradle wrapper exists):**
```bash
cd "C:\Deal Finder\user-app"
./gradlew bundleRelease     # Windows: gradlew.bat bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## Step 4 — Get your SHA-1 (only needed later for Google Sign-In)

Email/Password login works without this. If/when you implement the Google Sign-In
button in the app, get the **upload key** SHA-1 and add it in Firebase Console →
Project Settings → your Android app → Add fingerprint:
```bat
keytool -list -v -keystore my-upload-key.jks -alias upload
```
Also add Play's **App Signing** SHA-1 later (Play Console → App integrity), because
Google re-signs your app on their side.

---

## Quick reference
| Item | Value |
|---|---|
| applicationId | `com.aistudio.dealfinderuae.jlkqpz` |
| key alias (required) | `upload` |
| versionCode / versionName | `1` / `1.0` (bump versionCode for each update) |
| min / target SDK | 24 / 36 |
| output for Play | `.aab` (Android App Bundle) |
