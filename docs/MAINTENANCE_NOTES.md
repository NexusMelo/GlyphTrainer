# GlyphTrainer Maintenance Notes

This project is currently optimized for a small, touch-sensitive overlay workflow.
Keep changes narrow and validate each UI adjustment on a real device.

## Stable Behavior Rules

- Do not change capture, replay, preview, GO timing, H/V scaling, or glyph geometry together with UI artwork changes.
- Keep the capture area, replay target, and saved-path normalization derived from the same bounds.
- Keep the PLAY overlay controls and minimized controls as separate visual concerns.
- Preserve the `3 -> 4 -> 5 -> 3` glyph limit cycle unless a task explicitly changes it.
- Treat `PROGRAM` and Tutorial code as preserved-but-hidden features.

## Overlay Rules

- `WindowManager.addView`, `updateViewLayout`, and `removeView` failures should be logged with the `GlyphTrainerOverlay` tag.
- If an existing WindowManager failure path calls `stopSelf()`, keep that behavior unless a task explicitly changes lifecycle handling.
- Do not make the service foreground without a product decision, because it changes user-visible behavior.

## Asset Rules

- Button artwork is PNG-based by design for the current UI phase.
- Direct controls use a transparent background with a rounded rectangular outline whose color follows the active skin.
- Menu controls open or collapse option groups and use a black background; only menu controls may use a black background.
- When replacing a button asset, keep the existing `WindowManager.LayoutParams` unless the task explicitly asks for size or position changes.
- Consider density behavior before moving PNGs between `drawable` and `drawable-nodpi`; it can change visual scale on real devices.

## Validation

For safe technical changes, run:

```bash
./gradlew assembleDebug
```

Avoid emulator, instrumented tests, and clean builds unless a task explicitly asks for them.
