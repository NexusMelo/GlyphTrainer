# OverlayService Technical Map

This document maps the current responsibilities and coupling inside `OverlayService.kt`. It describes the existing implementation; it is not an authorization to refactor or change behavior.

Scales used below:

- Regression risk: `0` means negligible risk; `10` means very high probability or impact of regression.
- Extraction difficulty: `0` means isolated/pure code; `10` means deeply coupled state and lifecycle behavior.

## Conceptual modules

| Module | Responsibility | Main dependencies | Main functions/state | Regression risk | Extraction difficulty |
|---|---|---|---|---:|---:|
| Service lifecycle and permissions | Start and stop the service safely, validate overlay access, observe permission revocation, and coordinate teardown. | `Service`, `Settings`, `AppOpsManager`, `WindowManager`, all initialized views and pending callbacks. | `onCreate`, `onStartCommand`, `onDestroy`, `canUseOverlay`, `isOverlayReady`, permission listener registration | 9 | 6 |
| Persistent state | Restore configuration at startup and save individual user choices. | `SharedPreferences`, preference keys/defaults, layout version, `AppThemeConfig`. | `restorePreferences`, all `save*` functions | 7 | 3 |
| Overlay UI creation | Create the draw layer and every expanded, floating, configuration, and hidden tutorial view. | `WindowManager`, `DrawView`, `TextView`, resources, themes, `LayoutParams`. | `createDrawLayer`, `createButtons`, `createFloatingControls`, `create*Control`, button factories | 8 | 8 |
| Layout and positioning | Position expanded controls, constrain the floating group, and update every related `LayoutParams`. | Screen dimensions, system-bar sizes, `drawArea`, floating state, configuration state. | `positionOverlayControls`, `applyFloatingGroupPosition`, `floating*X/Y`, height calculations | 8 | 7 |
| Events and callbacks | Connect clicks, dragging, `DrawView` callbacks, and delayed work to state transitions. | All mutable service state, `Handler`, `MotionEvent`, `DrawView.OverlayListener`. | click/touch listeners, `handleFloatingDrag`, `onAreaUpdated`, `onCaptureFinished`, runnables | 10 | 9 |
| Modes 3/4/5 | Cycle and persist the glyph limit, clear the old sequence, and update expanded/floating visuals. | `glyphLimit`, `DrawView`, persistence, current theme and drawable resources. | mode-button action, `updateModeButton`, `updateFloatingButton`, glyph-limit resource selectors | 7 | 5 |
| Capture orchestration | Enable touch input, delay the capture start, stop input, and receive completion. Gesture/path mechanics remain in `DrawView`. | `DrawView`, `drawParams` flags, permission/readiness checks, PLAY mode, `Handler`. | `enableCapture`, `disableCapture`, `startCaptureRunnable`, `capturing`, `onCaptureFinished` | 9 | 8 |
| Preview orchestration | Keep captured slots visible and request completed-sequence presentation. Rendering and path storage remain in `DrawView`. | `DrawView`, glyph limit, capture state, SHOW setting. | calls to `startCapture`, `resetGlyphs`, `showCompletedSequence`; completion callback | 8 | 8 |
| GO presentation | Display and clear GO between capture completion and replay. | `DrawView`, SHOW setting, sequence/replay cancellation, delayed callbacks. | `onCaptureFinished`, `showGlyphSequenceRunnable`, `cancelReplay` | 8 | 6 |
| Replay | Show captured glyphs sequentially, track replay progress, cancel safely, and minimize at completion. | `Handler`, `DrawView`, glyph limit, SHOW, permission/readiness checks, capture and minimization. | `startReplay`, `replayStepRunnable`, `cancelSequencePresentation`, `cancelReplay` | 10 | 9 |
| Minimize and restore | Transition between expanded and floating UI, cancel active work, clear the sequence, and optionally start AUTO capture on restore. | Almost every view, capture, replay, tutorial, configuration, positioning and AUTO state. | `minimizeOverlay`, `restoreOverlay`, `setMainControlsVisibility`, `updateConfigControlsVisibility` | 9 | 8 |
| Theme/SKIN and presentation options | Change theme, opacity and SHOW; apply assets, colors and state to visible controls. | `AppThemeConfig`, `DrawView`, UI controls, tutorial, resources and persistence. | `applyCurrentTheme`, theme/style helpers, `applyOverlayOpacity`, `updateShowButton` | 6 | 4 |
| Hidden tutorial | Preserve the disabled tutorial UI, navigation and target positioning. | `TutorialHudUi`, theme, control coordinates and overlay state. | `createTutorialControls`, `showTutorial`, `hideTutorial`, tutorial positioning helpers | 4 while disabled | 6 |

## Important coupling

### Creation order

The service currently performs this sequence:

1. Check overlay permission.
2. Obtain `WindowManager` and restore preferences.
3. Create the `DrawView` layer.
4. Create main controls.
5. Create floating controls.
6. Create configuration, theme, opacity and SHOW controls.
7. Optionally create the tutorial, currently disabled.
8. Apply initial capture and visual state.
9. Register the permission listener.

The control factories assign `WindowManager.LayoutParams` to fields according to which field has not yet been initialized. Creation order is therefore part of current behavior.

### Expanded and minimized state

Expanded and minimized controls are separate views with separate layout parameters and visual assets. Minimizing is not a visibility-only operation: it also cancels sequence presentation, disables capture, clears glyphs, resets configuration state and repositions the floating group.

### `DrawView` contract

`OverlayService` orchestrates the workflow, while `DrawView` owns gesture handling, normalized paths, captured slots, previews, GO rendering and replay rendering. Their contract is formed by:

- `DrawView.OverlayListener`;
- `onAreaUpdated`;
- `onCaptureFinished`;
- capture enable/disable calls;
- preview, GO and replay commands;
- the shared glyph limit, scale and theme values.

## PLAY to minimization pipeline

```text
PLAY click
  -> enableCapture()
     -> cancel previous GO/replay
     -> make DrawView touchable
     -> schedule startCaptureRunnable (140 ms)
        -> DrawView.startCapture()
           -> gestures accepted and previews filled by DrawView
           -> required glyph count reached
              -> onCaptureFinished()
                 -> disableCapture()
                 -> mark PLAY inactive
                 -> cancel previous presentation
                 -> if SHOW ON: DrawView.showGoMessage()
                 -> schedule showGlyphSequenceRunnable
                    -> DrawView.showCompletedSequence()
                    -> startReplay()
                       -> schedule replayStepRunnable
                          -> show one glyph
                          -> wait for glyph duration
                          -> clear glyph
                          -> repeat until glyphLimit
                          -> minimizeOverlay()
```

### Pipeline cancellation points

The active pipeline is cancelled by:

- starting a new capture;
- RESET;
- changing the 3/4/5 mode;
- minimizing;
- service destruction;
- permission loss;
- failed overlay readiness;
- explicit sequence/replay cancellation.

A delayed callback that survives one of these transitions can act on stale state or detached views.

## Areas that must not be changed in isolation

- Capture, GO and replay timing constants together with their three runnables.
- `drawParams` touchability/focus flags together with `enableCapture` and `disableCapture`.
- The chain `onCaptureFinished -> GO -> showGlyphSequenceRunnable -> startReplay -> replayStepRunnable`.
- Callback removal in `cancelSequencePresentation`, `cancelReplay` and `onDestroy`.
- The ordering inside `minimizeOverlay` and `restoreOverlay`.
- `showGlyphs` behavior across completion, GO, replay and automatic minimization.
- The `DrawView` listener contract and the bounds passed through `onAreaUpdated`.
- Capture bounds, normalized paths, preview targets and replay targets.
- Button creation order while `LayoutParams` fields are assigned by initialization order.
- Floating-group geometry, gravity direction, drag deltas and system-bar bounds.
- Preference keys, defaults, coercion rules and floating-layout versioning.
- `WindowManager` add/update/remove wrappers and their shutdown behavior.
- Permission-listener registration, revocation handling and teardown.
- PLAY/PROGRAM checks while preserved PROGRAM functionality remains hidden.

## Safer candidates for future extraction

### 1. Pure resource selection

Initial candidates:

- `glyphLimitIcon`;
- `glyphLimitFloatingButton`;
- `overlayOpacityButton`.

These functions mainly map explicit state to resources and have little lifecycle coupling.

### 2. Preference storage

Move preference keys, defaults, reads and writes behind one component while preserving:

- existing key names;
- default values;
- coercion rules;
- layout-version reset behavior;
- compatibility with existing installations.

### 3. Visual style helpers

Initial candidates:

- square/rectangular outline styling;
- vector-icon application;
- reference-background application;
- simple control factories.

The extraction must not change pixel sizes, drawable density behavior or control creation order.

### 4. Floating geometry calculations

The coordinate and height calculations can become pure functions if all inputs are explicit: screen dimensions, insets, group position, configuration state and control dimensions. Applying the calculated values to `WindowManager` should remain separate initially.

### 5. Hidden tutorial block

The tutorial is relatively delimited and currently disabled. It can be isolated later, provided it stays disabled and its extraction does not change active view creation order.

## Recommended future extraction order

1. **Resource selectors** — establish a very low-risk extraction pattern.
2. **Preference storage** — remove persistence detail without touching runtime transitions.
3. **Visual style helpers and simple factories** — reduce UI-construction noise while preserving dimensions and order.
4. **Pure floating geometry** — separate calculations from `WindowManager` mutations.
5. **Hidden tutorial** — isolate dormant functionality from the active overlay path.
6. **Explicit overlay state model** — describe expanded/minimized, capturing, replaying and configuration state without yet moving behavior.
7. **Minimization coordinator** — only after state transitions are characterized.
8. **Capture/GO/replay coordinator** — last, because it contains the highest timing and regression risk.

Capture, GO, replay, minimization, permission lifecycle and event callbacks should not be the first extraction targets.
