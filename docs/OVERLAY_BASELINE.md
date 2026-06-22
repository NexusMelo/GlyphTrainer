# GlyphON / GlyphTrainer — Overlay Functional Baseline

Baseline documented from commit `a88466a` (`Standardize square and rectangular control styles`, 2026-06-21).

This document records the current expected behavior before further development. It is not a product roadmap. If implementation and this document diverge, confirm whether the behavior change was intentional before updating the baseline.

## Current project state

GlyphTrainer is an Android application whose active launcher flow opens a system overlay after obtaining `SYSTEM_ALERT_WINDOW` permission. The active experience is the PLAY overlay; there is no visible startup menu.

The overlay supports drawing sequences of 3, 4, or 5 glyphs, displaying the captured previews, showing a GO cue, and replaying the sequence. It can be minimized into a movable floating group and restored.

The implementation is functional but sensitive to UI and lifecycle changes. Overlay creation, capture, replay, positioning, visual state, and preference persistence are closely coupled. Validation should be performed on a real device after any relevant change.

## Main flows

### Launch and permission

1. Launch the app from the launcher.
2. If overlay permission is missing, Android opens the permission settings screen.
3. After permission is granted and the user returns, the app starts the overlay and closes its launcher activity.
4. If permission already exists, the overlay starts directly.
5. Every new service creation starts in the expanded state, even when it was previously closed while minimized.

### Glyph capture and presentation

1. Select the glyph count with the numbered control. The cycle is `5 -> 3 -> 4 -> 5`.
2. Start capture with PLAY.
3. Draw one valid single-pointer gesture for each required glyph inside the capture area.
4. Each accepted gesture fills the next preview slot.
5. When the selected number of glyphs is complete, capture stops.
6. With glyph display enabled, GO appears and the captured sequence is replayed one glyph at a time.
7. RESET clears the current sequence and immediately starts a new capture.

### Minimize and restore

1. MINIMIZE hides the drawing area and main controls.
2. Minimizing cancels capture/replay and clears the current glyph sequence.
3. The minimized group exposes restore, MANUAL/AUTO, close, and configuration controls.
4. The group can be dragged; its position is persisted.
5. Restore returns to the expanded overlay.
6. In AUTO, restoring also starts capture. In MANUAL, restoring does not start capture.

### Configuration while minimized

- MANUAL/AUTO controls whether capture starts automatically after restore.
- The configuration flyout exposes theme/skin, opacity, and glyph display controls.
- Theme/skin cycles through `STANDARD -> GREEN -> BLUE -> STANDARD`.
- Opacity cycles through `100% -> 80% -> 60% -> 100%`.
- SHOW controls whether GO and sequence replay are presented after capture.

### Close

- The expanded close control stops the overlay service.
- The minimized close control also stops the overlay service.
- Closing does not revoke the Android overlay permission.
- Captured glyph paths and minimized/expanded state are not persisted.

## Active functionality

- Direct launcher-to-overlay flow.
- Android overlay permission request and validation.
- Expanded PLAY overlay.
- Glyph-count modes 3, 4, and 5.
- Single-pointer glyph capture with minimum gesture filtering.
- Per-slot preview of captured glyphs.
- GO cue and sequential replay.
- PLAY and RESET controls.
- Horizontal and vertical glyph scale adjustments.
- Minimized floating controls and drag positioning.
- MANUAL/AUTO restore behavior.
- STANDARD, GREEN, and BLUE themes/skins.
- Overlay opacity at 100%, 80%, and 60%.
- SHOW ON/OFF behavior.
- Persistence of glyph count, horizontal/vertical scale, MANUAL/AUTO, floating position, theme/skin, opacity, and SHOW state.
- Defensive handling of overlay permission loss and `WindowManager` failures.

## Hidden or disabled functionality

- The guided tutorial implementation is preserved but disabled by `TUTORIAL_ENABLED = false`.
- PROGRAM mode is preserved but is not part of the normal launcher flow.
- `PasswordActivity`, `ProgramActivity`, and `GameActivity` remain declared/preserved but are not exposed by the current launcher flow.
- The pure glyph-recognition model and its unit tests exist but are not connected to the active drawing/capture flow.
- Premium-theme preference and usage counters exist as dormant policy/telemetry state; no premium restriction is enforced.

Do not treat preserved code as active functionality without confirming a deliberate product decision.

## Maintenance rules

- Keep functional changes separate from visual artwork changes.
- Do not change capture, preview, GO timing, replay timing, glyph geometry, and H/V scaling in the same change.
- Derive capture, replay, preview, and normalized paths from consistent bounds.
- Preserve the glyph-count cycle unless a task explicitly changes it.
- Preserve expanded and minimized controls as separate visual/state concerns.
- Preserve the current MANUAL/AUTO restore distinction.
- Preserve preference compatibility unless a migration or reset is intentional.
- Keep overlay operations guarded against permission loss and `WindowManager` failures.
- Do not convert the service to a foreground service without a product decision.
- Treat PROGRAM and tutorial code as preserved-but-hidden.
- Keep PNG/vector density and fixed-pixel overlay sizing behavior in mind when changing assets.
- Validate relevant UI changes on a real device.

## Regression risk areas

- Overlay permission grant, denial, revocation, and service shutdown.
- `WindowManager` add/update/remove ordering and partial creation failure.
- Capture state around delayed callbacks, RESET, minimize, close, and permission loss.
- GO/replay callback cancellation when changing mode or overlay state.
- Synchronization between capture bounds, normalized paths, previews, and replay targets.
- Transition between expanded and minimized visibility states.
- AUTO capture starting only on restore, not unexpectedly elsewhere.
- Floating-group drag bounds and saved position across screen/system-bar variations.
- Fixed-pixel sizing across device densities and screen dimensions.
- Theme, opacity, and SHOW state being applied consistently to all visible controls.
- Preference restoration after reopening, including invalid or older saved values.
- Hidden PROGRAM/tutorial behavior becoming visible accidentally.

## Manual smoke test checklist

Run on a real device. Start with the app installed and note whether overlay permission is already granted. Record device model, Android version, orientation, build/commit, and result for any failure.

### A. Launch and overlay permission

- [ ] Revoke or clear the app's overlay permission before the first launch.
- [ ] Launch the app and confirm Android opens the overlay permission settings.
- [ ] Grant permission and return to the app.
- [ ] Confirm the launcher activity closes and the expanded overlay appears without a persistent startup screen.
- [ ] Close the overlay, launch the app again with permission already granted, and confirm the expanded overlay appears directly.
- [ ] Confirm the overlay contains the capture area and close, PLAY, glyph-count, RESET, and MINIMIZE controls.

### B. Three-glyph mode

- [ ] Cycle the numbered control until `3` is selected.
- [ ] Confirm exactly three preview slots are presented for the active sequence.
- [ ] Press PLAY and draw three distinct valid glyphs inside the capture area.
- [ ] Confirm one preview slot is filled after each glyph.
- [ ] Confirm capture stops after the third glyph.
- [ ] With SHOW ON, confirm GO appears after completion.
- [ ] Confirm all three glyphs replay in capture order and no fourth glyph is shown.

### C. Four-glyph mode

- [ ] Cycle the numbered control from `3` to `4`.
- [ ] Confirm the previous sequence is cleared and four slots are presented.
- [ ] Press PLAY and capture four distinct valid glyphs.
- [ ] Confirm preview order matches capture order.
- [ ] Confirm capture stops after the fourth glyph.
- [ ] With SHOW ON, confirm GO appears and exactly four glyphs replay in order.

### D. Five-glyph mode

- [ ] Cycle the numbered control from `4` to `5`.
- [ ] Confirm the previous sequence is cleared and five slots are presented.
- [ ] Press PLAY and capture five distinct valid glyphs.
- [ ] Confirm preview order matches capture order.
- [ ] Confirm capture stops after the fifth glyph.
- [ ] With SHOW ON, confirm GO appears and exactly five glyphs replay in order.
- [ ] Cycle once more and confirm the mode returns from `5` to `3`.

### E. Capture, preview, GO, and replay controls

- [ ] Press PLAY and confirm drawing is accepted only after capture starts.
- [ ] Draw a very short/accidental gesture and confirm it does not consume a slot.
- [ ] Draw a valid gesture and confirm its preview appears in the next slot.
- [ ] Press RESET during a partial sequence and confirm all slots clear and capture restarts.
- [ ] Complete a sequence with SHOW ON and confirm GO appears before replay.
- [ ] Confirm replay shows one glyph at a time in the original order.
- [ ] Change glyph-count mode during GO/replay and confirm the old presentation is cancelled and cleared.

### F. Minimize, restore, and close

- [ ] Start a partial capture, then press MINIMIZE.
- [ ] Confirm the drawing area and main controls disappear.
- [ ] Confirm the partial sequence/capture is cleared.
- [ ] Confirm restore, MANUAL/AUTO, close, and configuration controls appear in the minimized group.
- [ ] Drag the minimized group and confirm all controls move together and remain usable.
- [ ] In MANUAL, restore and confirm the expanded overlay returns without automatically starting capture.
- [ ] Minimize again, switch to AUTO, restore, and confirm capture starts automatically.
- [ ] Close from the expanded overlay and confirm all overlay views disappear.
- [ ] Reopen, minimize, close from the minimized group, and confirm all overlay views disappear.

### G. MANUAL/AUTO, theme/skin, opacity, and SHOW

- [ ] While minimized, switch from MANUAL to AUTO and confirm the label/artwork changes.
- [ ] Switch back to MANUAL and confirm the state changes back.
- [ ] Open the configuration flyout and confirm theme/skin, opacity, and SHOW controls appear.
- [ ] Cycle theme/skin through STANDARD, GREEN, BLUE, and back to STANDARD.
- [ ] Confirm borders, glyph rendering, and visible controls follow each selected theme consistently.
- [ ] Cycle opacity through 100%, 80%, 60%, and back to 100%; confirm the overlay visibly follows each value.
- [ ] Set SHOW OFF, restore, capture a complete sequence, and confirm GO/replay are not presented.
- [ ] Set SHOW ON, capture another complete sequence, and confirm GO/replay return.

### H. Persistence after close/reopen

- [ ] Select a non-default glyph count, AUTO, a non-default theme/skin, non-default opacity, and SHOW OFF.
- [ ] Minimize, drag the floating group to a clearly different position, and close the overlay.
- [ ] Launch the app again.
- [ ] Confirm the overlay starts expanded rather than minimized.
- [ ] Confirm the selected glyph count, theme/skin, opacity, and SHOW state were restored.
- [ ] Minimize and confirm AUTO and the floating-group position were restored.
- [ ] Restore and confirm AUTO starts capture automatically.
- [ ] Confirm no glyph sequence from the previous session was restored.

## Smoke test result

- Device / Android:
- Build / commit:
- Date:
- Tester:
- Result: PASS / FAIL
- Failed checklist items and observations:

