# Privacy Display

A free Android app by **SONI** that darkens your screen with an adjustable, translucent overlay so
that what is on your display is harder to read at a glance.

* Package: `com.soni.privacydisplay`
* Minimum Android: 8.0 (API 26) - Target SDK: 35
* Kotlin, Jetpack Compose (Material 3), DataStore, no internet, no ads, no analytics.

## What the app does

* Master ON/OFF switch, a big Start/Stop button and a 0-90 % intensity slider that updates live.
* Black overlay, or a subtle dark-blue tint (Settings).
* Live preview card, ACTIVE/INACTIVE status, overlay permission status.
* Remembers your intensity and color. Optionally restores the filter when you open the app.
* Foreground-service notification with a **Stop** button.
* Optional system-brightness dimming (off by default). Your original brightness is saved once and restored when you switch it off or stop the filter.

## What it cannot do (please read)

This is a **software overlay**. It is **not** Samsung's hardware Privacy Display and it does **not**
block people from seeing your screen from the side. It only makes the screen darker.

* Android may not let overlays cover some system screens (for example the permission dialogs,
  some security screens and parts of the lock screen).
* On Android 12 and newer, Android can stop touches from passing through very dark overlays.
  If touches stop working at high intensity, lower the slider (try 80 % or less).
* Some devices, "game modes" or battery managers may stop the service. Open the app again to restore it.
* The app never screenshots, records or reads your screen and never sends data anywhere.

## Upload the project to GitHub from a phone

Your file manager cannot upload folders to GitHub, so the easiest way is Termux:

```sh
pkg install -y git unzip
cd ~
unzip PrivacyDisplay-Android-Project.zip      # adjust the path if the zip is in Downloads
cd PrivacyDisplay
git init -b main
git add .
git commit -m "Privacy Display"
git remote add origin https://github.com/YOUR_USERNAME/PrivacyDisplay.git
git push -u origin main
```

1. In the GitHub app/website create an **empty** repository named `PrivacyDisplay` (no README).
2. Run the commands above. When Git asks for a password, paste a **personal access token**
   (GitHub > Settings > Developer settings > Personal access tokens, scope: `repo` and `workflow`).

## Build the APK with GitHub Actions

1. Open your repository on github.com (in the phone browser use "Desktop site" if needed).
2. Tap the **Actions** tab. The build starts automatically after every push to `main`.
3. To start it by hand: Actions > **Android Build** > **Run workflow** > **Run workflow**.
4. Tap the running/finished job. A green check means success; a red X means the build failed (open the failed step to read the error).

## Download the APK

1. Open the finished workflow run.
2. Scroll to the bottom to the **Artifacts** section.
3. Tap **PrivacyDisplay-debug-apk** to download a ZIP, then extract it (the Files app can do this). Inside is `app-debug.apk`.

## Install the APK

1. Tap `app-debug.apk`.
2. If Android asks, allow "Install unknown apps" for your file manager or browser.
3. Tap **Install**.

## Grant the overlay permission and use the filter

1. Open **Privacy Display** and tap the big switch or **Start filter**.
2. Read the explanation, tap **Continue**, then switch on **Allow display over other apps** for Privacy Display.
3. Press Back to return to the app. On Android 13+ allow notifications when asked (needed to see the Stop button).
4. Tap **Start filter** again. Drag the slider to change the darkness.
5. Turn it off with the switch, the **Stop filter** button, or the **Stop** button in the notification.

## Troubleshooting

* **Switch does nothing / asks for permission again** - the overlay permission is not granted yet. Tap **Grant**.
* **No notification** - allow notifications for the app in system settings. The filter still works without it.
* **Touches stop working** - lower the intensity (see limitations above).
* **Filter disappeared** - the system stopped the service. Open the app and tap Start (or enable "Restore filter when app opens").
* **Brightness stays low** - open the app and turn **Also dim system brightness** off; your original value is restored. If you removed the "Modify system settings" permission, grant it again so it can be restored.
* **Build failed on GitHub** - open the failed step in the Actions run and read the red error text; see `BUILDING.md`.

## License

MIT - see `LICENSE`.
