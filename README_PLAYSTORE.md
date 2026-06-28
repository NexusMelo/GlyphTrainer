# GlyphON — Google Play Store Readiness

## Application name

GlyphON

## Package name

`pt.vicktor.glyphon`

## Version

- `versionCode`: `1`
- `versionName`: `1.0`

## SDK configuration

- `compileSdk`: `36`, minor API level `1`
- `targetSdk`: `36`
- `minSdk`: `26`

## Permissions used

The current Android manifest declares:

- `android.permission.SYSTEM_ALERT_WINDOW`

No internet, location, camera, microphone, contacts, storage, notification, accessibility, or foreground service permissions are currently declared.

## Permission rationale

### SYSTEM_ALERT_WINDOW

GlyphON requires Android's “Display over other apps” permission to present its floating training overlay. The overlay contains the capture area, preview area, replay display, and direct controls such as play, reset, minimize, close, glyph count, and configuration controls.

This permission is central to the app's current functionality. It is not used to collect personal data, read private content from other apps, or transmit information externally.

## Main features

- Floating overlay for glyph training.
- User-controlled capture area.
- Glyph sequence capture and replay.
- Upper preview area.
- Manual and auto capture modes.
- Configurable glyph count.
- Overlay opacity settings.
- Theme/skin selection.
- Show/hide glyph display toggle.
- Minimized overlay menu.
- Local preference persistence for app settings.

## Data handling summary

- GlyphON does not collect personal information.
- GlyphON does not currently send data to external servers.
- GlyphON does not currently include analytics, ads, crash reporting, or cloud services.
- App settings are stored locally on the device using Android shared preferences.
- Touch input used for glyph capture/replay is processed locally for the training feature.

## Public privacy policy

The privacy policy file is provided at:

`privacy-policy.html`

After GitHub Pages is enabled for this repository using the included workflow, the expected public URL is:

`https://nexusmelo.github.io/GlyphTrainer/privacy-policy.html`

## Current project state

The project is prepared for Google Play documentation requirements by adding a local privacy policy and a GitHub Pages deployment workflow. No Kotlin source code, Android resources, Gradle configuration, or application behavior were changed as part of this documentation update.

Static validation only was requested. The app was not compiled or executed.

