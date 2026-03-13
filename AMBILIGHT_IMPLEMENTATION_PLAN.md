# Ambilight for Meta Quest — Multi-Agent Implementation Plan

## Objective
Implement a **soft, low-latency ambient glow** around the streamed frame (similar to TV ambilight), tuned for Meta Quest usage.

This document is a **task board + execution guide** for multiple coding agents. Follow steps in order unless explicitly marked parallelizable.

---

## High-level constraints
- Keep latency impact minimal (target: no perceptible input/video lag increase).
- Keep GPU overhead modest (Quest thermals matter).
- Default visual style: **subtle** (not neon, not distracting).
- Integrate with current render paths safely:
  - `MODE_AI_3D` / `MODE_AI_3D_MOVIE` (GL pipeline): primary target.
  - `MODE_2D` (`SurfaceView` path): either unsupported in phase 1 or explicitly gated.

---

## Team roles (suggested)
- **Agent A (Render Pipeline):** Shader + GL pass + runtime uniforms.
- **Agent B (Settings/Config):** Preferences UI + config read/write + defaults.
- **Agent C (Integration/QA):** Wiring in `Game`/`StreamContainer`, tests, perf checks, docs.

---

## Phase 0 — Baseline and branch hygiene (must do first)

### Step 0.1: Verify baseline builds
1. Run app module compile check.
2. Run existing unit tests relevant to renderer/preferences.
3. Record baseline FPS/latency using existing perf overlay.

**Deliverable:** baseline numbers captured in PR description.

### Step 0.2: Create feature branch strategy
- Branch naming suggestion:
  - `feature/ambilight-core`
  - `feature/ambilight-settings`
  - `feature/ambilight-integration`

**Deliverable:** clean branch structure for parallel work.

---

## Phase 1 — Core renderer feature (Agent A)

## Scope
Add an ambient-light pass in GL renderer that samples stream colors near edges and renders a soft halo around content.

### Step 1.1: Add shader constants
Status: ✅ Done on branch `work`.

Files:
- `app/src/main/java/com/limelight/utils/ShaderUtils.java`

Tasks:
1. Add one fragment shader string for ambilight (prefer external OES sampling to avoid extra copy).
2. Keep parameters controlled by uniforms:
   - `u_ambilightEnabled`
   - `u_intensity`
   - `u_spread`
   - `u_saturationBoost`
   - `u_edgeWidth`
3. Keep precision medium unless artifacts appear.

**Acceptance criteria:** shader compiles; no GLES link errors.

### Step 1.2: Extend `Stereo3DRenderer` state
Status: ✅ Done on branch `work`.

Files:
- `app/src/main/java/com/limelight/utils/Stereo3DRenderer.java`

Tasks:
1. Add fields for ambilight program handles and uniform locations.
2. Add runtime config fields and safe defaults.
3. Initialize and delete program with existing lifecycle.

**Acceptance criteria:** no resource leak, clean teardown.

### Step 1.3: Implement draw order
Status: ✅ Done on branch `work`.

1. In `onDrawFrame`, render sequence:
   - clear frame
   - draw ambilight background pass
   - draw existing stream content (3D path)
2. Ensure glow sits **behind** content and does not wash center image.

**Acceptance criteria:** visible soft glow around frame edges in GL modes.

### Step 1.4: Optional temporal smoothing
Status: ✅ Done on branch `work`.

1. Add lightweight temporal smoothing on sampled edge colors.
2. Clamp transitions to avoid flicker on fast scene cuts.

**Acceptance criteria:** smoother color transitions, no strobing.

### Step 1.5: Performance guardrails
Status: ✅ Done on branch `work`.

1. Implement quality tiering:
   - low (fewer samples)
   - medium (default)
   - high (optional)
2. Auto-fallback to low quality if frame time spikes.

**Acceptance criteria:** stable FPS on Quest target profiles.

---

✅ **Stage 1 complete** (Steps 1.1–1.5).

## Phase 2 — Preferences and config plumbing (Agent B)

## Scope
Expose ambilight settings in preferences and load into runtime config.

### Step 2.1: Add preference keys/UI
Files:
- `app/src/main/res/xml/preferences.xml`
- `app/src/main/res/values/strings.xml`

Tasks:
1. Add toggle:
   - `checkbox_enable_ambilight`
2. Add sliders:
   - `ambilight_intensity`
   - `ambilight_spread`
   - `ambilight_smoothing`
3. Optional expert setting:
   - `ambilight_quality`

**Acceptance criteria:** settings visible, persisted, and localizable.

### Step 2.2: Extend configuration model
Files:
- `app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java`

Tasks:
1. Add constants for keys.
2. Add fields on config object.
3. Read values with safe defaults in `readPreferences()`.
4. Keep backward compatibility for existing users.

**Acceptance criteria:** no NPEs, defaults applied when keys absent.

---

## Phase 3 — App integration and mode handling (Agent C)

## Scope
Wire config values into renderer and handle unsupported modes cleanly.

### Step 3.1: Wire into game startup
Files:
- `app/src/main/java/com/limelight/Game.java`
- `app/src/main/java/com/limelight/ui/StreamContainer.java`

Tasks:
1. Pass ambilight settings from `PreferenceConfiguration` to renderer initialization path.
2. Ensure values can be refreshed on stream restart.

**Acceptance criteria:** toggling settings takes effect on next stream session.

### Step 3.2: Mode compatibility policy
1. Phase 1 support: GL modes only.
2. If `MODE_2D` active and ambilight enabled, behavior options:
   - silently disable + log, or
   - show one-time info toast.
3. Add clear note in settings summary.

**Acceptance criteria:** predictable behavior with no crashes across modes.

---

## Phase 4 — Validation and quality gates

### Step 4.1: Functional checks
- Enable ambilight and verify glow appears.
- Verify disabled state matches previous rendering.
- Verify across bright/dark/high-contrast scenes.

### Step 4.2: Performance checks
- Compare pre/post frame pacing and FPS.
- Confirm no major thermal regression in 15–20 min run.
- Confirm no increase in decode latency stats outside noise.

### Step 4.3: Stability checks
- Repeated activity pause/resume.
- Stream start/stop loops.
- Rotate/display mode changes if applicable.

### Step 4.4: Regression checks
- Input handling unchanged.
- Overlay controls unchanged.
- 3D controls/parallax still functional.

---

## Phase 5 — Optional improvements (post-MVP)
- Add dynamic edge segmentation (top/left/right/bottom independent colors).
- Add vignette blending model selectable by user.
- Add `MODE_2D` GL compositing path for full ambilight support in 2D.
- Add device-specific presets for Quest 2 / Quest 3.

---

## Implementation checklist (copy into each PR)
- [ ] Added/updated shaders compile cleanly.
- [ ] Added ambilight config fields + defaults.
- [ ] Added settings labels and summaries.
- [ ] Added lifecycle-safe cleanup for all new GL resources.
- [ ] Verified behavior in supported/unsupported modes.
- [ ] Ran compile + relevant tests.
- [ ] Attached before/after screenshot or short capture (if available).
- [ ] Added perf impact note.

---

## Coordination protocol for agents
1. Each agent creates focused PR with narrow scope.
2. Agent A merges first (core hooks).
3. Agent B rebases onto A and merges settings.
4. Agent C performs integration, resolves conflicts, runs final QA.
5. Final squash merge with release notes.

**Conflict hotspots:**
- `Stereo3DRenderer.java`
- `PreferenceConfiguration.java`
- `preferences.xml`

---

## Definition of done (MVP)
- Ambilight works in GL-based stream modes on Quest.
- Default look is subtle and comfortable.
- Feature can be toggled on/off in settings.
- No crashes/regressions in normal streaming flow.
- Performance impact acceptable for sustained use.

