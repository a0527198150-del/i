<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/925e4192-4373-43f3-9337-c9d6f1c7c4d8

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Run the app on an emulator or physical device

> **Note about building the release APK locally:** the release build is signed
> with `debug.keystore` (same key as the GitHub Actions builds). If that file is
> missing locally, `assembleRelease` fails with "Keystore file not found". For
> local testing use `assembleDebug` instead - but keep in mind a debug APK is
> signed with a *different* key, so it can only be installed over another debug
> APK, never over the release APK from GitHub.

## Updates (install new versions without uninstalling)

Every APK produced by GitHub Actions is signed with the **same stable key**
(the `debug.keystore` restored from the `DEBUG_KEYSTORE_BASE64` secret), and
`versionCode` is automatically derived from the number of commits, so it always
increases between releases. This means:

- **Installing a new version over the old one works directly** - download the
  new APK from the latest successful run (artifact `app-release-apk`) and open
  it; Android recognizes it as an update and keeps all data.
- **Never uninstall before updating** unless the device says the signature
  doesn't match (this can only happen if the currently installed APK was signed
  with a different key, e.g. a debug build or a build from before the signing
  key was set up - in that case a one-time uninstall is required).
- The signing key is a **secret** (`DEBUG_KEYSTORE_BASE64` in the repository
  settings → Secrets and variables → Actions). Keep it safe - losing it means
  future APKs can no longer update over existing installs.
