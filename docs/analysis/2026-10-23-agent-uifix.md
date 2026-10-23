# UI drawing parity — agent/uifix

## Authority and reference discrepancy

The supplied `ref_thaumonomicon.png` was inspected directly, together with the enlarged original GUI and HUD sheets. It shows carved rune-covered wood, a bright magenta map, bilateral tabs, Search, and a blue bottom-right arrow. **The supplied screenshot is not a literal rendering of the supplied 4.2.3.5 oracle.** Search and that arrow do not occur in `GuiResearchBrowser` or the unchanged `gui_research.png`; they cannot be added faithfully from these inputs. The oracle switches to right-hand tabs only at category ten (:468-470), whereas the native port has six categories. Inventing extra categories, duplicating tabs, painting replacement buttons or changing texture brightness would fake parity.

`hud.png` is also not the original Thaumometer reticle. `ClientTickEventsFML:319-324` dispatches it for a casting wand or sanity checker, not a Thaumometer. Its `renderHoverHUD:558-615` is the hover harness, and uses `particles.png` (:583-590), not HUD. The actual Thaumometer uses `scanner.png`, `scanscreen.png` and in-world aspect/name readout (`ItemThaumometerRenderer:148-258`). No invented gold crosshair or wand bars were added to it.

## Per-defect changes

| Defect | Oracle authority | Change | Visual evidence |
|---|---|---|---|
| Flat rectangle instead of carved browser | `GuiResearchBrowser:75-76,272-275,460-513` | Restore 256×230 frame, 224×196 interior at +16,+17, exterior 24px category tabs, frame blit last. Remove replacement title, drag footer, Done button and green completion squares. Right-tab mirror geometry retained for category ten onward. | Final 01: four carved edges and runes, correct proportions and exterior left tabs. Only five categories unlocked, hence no right tabs. |
| Dark-corner / raw-pixel background pan | `GuiResearchBrowser:100-106,139-142,258-291` | Restore map `(lastColumn*24-82,lastRow*24-70)`, full-category extrema, normalized vx×288/vy×316 and scale 2. Preserve pan across category switches. Retain original implicit **256 UV denominator even for the 512px sheet**, rather than silently correcting the legacy blit. No texture changes. | Final 01: bright magenta/pink nebula, matching the reference's structure rather than the old dark pane. |
| Giant staircase connectors (discovered visually) | `GuiResearchBrowser:732-779` | Native `DEBUG_LINE_STRIP` replaces filled rectangles, retaining original vertices/gradient, physical GL line width 3, and line smoothing. | Final 01: thin curved connections instead of giant black/green/blue bands. |
| Oversized book, muddy text, invented rules/navigation | `GuiResearchRecipe:83-84,216-257,272-289,1232-1236,1287-1298`; original `TCFontRenderer.renderSplitString` bytecode offsets 168-196 | Fixed 256×181 book at scale 1.3, centered. Unscaled text width 139 and side offset 152. Title `0xFF303030`; unformatted body `0xFF505050` per requested correction; existing recipe headings already `0xFF505050`. Original textured 96×4 rules at UV (24,184). Original animated navigation art replaces vanilla button row; no page counter. | Final 02-05: compact centered two-page book, neutral-grey text, original black decorative rules and red arrows, no vanilla button row. Next/Previous/Back worked in the actual English script. |
| Unused HUD sheet | `ClientTickEventsFML:397-485` | Fabric HUD callback invokes new casting-wand renderer: half-size gold dial, rotated aspect bars, focus-cost marker and delayed increase/decrease glyphs. Reads native component-backed vis only. | `fabric/run/screenshots/2026-10-23_20.34.34.png`: gold dial and six rotated glass tubes visibly present at top left; yellow/green/red/blue/white/dark fills show different supplied vis amounts. |

### Initial camera choice and rejected run

The original static startup center is (-5,-6), `GuiResearchBrowser:804-806`. I tried it faithfully first: `.tmp/uifix-first/*01-browser-en.png` shows the root book entry outside the viewport, and the smoke script correctly failed its click. The final native first-open view instead uses remembered center **(0,0)** with the original constructor offsets (-82,-70). This selects a legitimate brighter portion of the sheet and keeps the basics entries visible. Subsequent opens/queued research restore the real remembered center. This is an explicit initial-camera choice, not a texture alteration or changed research coordinate.

### Body colour authority caveat

The supplied `GuiResearchRecipe:329` is a construction-recipe heading, not body prose. Actual prose at :1236 passes colour **0** to `TCFontRenderer.drawSplitString`; the shipped class bytecode confirms this is stored as `textColor`. This change uses the user's expressly requested neutral `0x505050` for unformatted body text, rather than claiming that :329 proves that prose colour. Embedded formatting colours (including magenta links and reset-to-black) are preserved. The title and recipe-heading colours are direct oracle matches.

## Exact HUD regions restored

All UVs below use the original 256×256 denominator and the original half-scale:

- `(0,0,64,64)`: gold wand dial (:424).
- `(104,0,8,loc)`, `loc = int(30*amount/max)`, drawn at `(-4,35-loc)`: tinted vis fill (:441-445).
- `(72,0,16,42)` at `(-8,-3)`: aspect tube (:449).
- `(136,0,8,8)` at `(-4,-8)`: focus-cost marker (:454).
- `(128,0,8,8)` / `(120,0,8,8)`: decreasing/increasing markers (:460,465), with original one-second sampled old values.

Not ported here: sanity-checker bar regions, runic-armour pieces, hover-harness fuel, bottom-dial configuration (native configuration has no equivalent), focus/trade icon/count presentation, and sneaking modified focus-cost numeric labels. These are not claimed complete. Sneaking current-vis labels are drawn at the oracle's coordinates.

## Verification and final frame judgements

Reused input: the supplied stopped `fabric/run/visual-seed` was copied read-only from the main repository into this worktree, then locally copied to `fabric/run/saves/New World` for the default harness. These are supplied fixtures, **not** survival acquisition/progression evidence.

Final book/browser capture prefix:

`/home/cyberfox-kali/wt/uifix/.tmp/uifix-final-book/client-smoke-ff9d8e2a-d417-44b3-afee-0d080a2d22c1-`

Every frame below was opened directly, not judged from a passing harness:

| Suffix | Judgement |
|---|---|
| `01-browser-en.png` | Compared directly against `ref_thaumonomicon.png`: carved wood on all four edges, runes, proportional interior, bright magenta stars, original cell badges, thin connectors. Five unlocked left tabs; no fictitious right categories. No GTNH Search/blue arrow by design, not defects. The world/held book behind the screen differs from the cropped reference. |
| `02-thaumonomicon-en.png` | Centered compact parchment spread; grey prose clearly neutral, not brown. Original two title rules. Red Back arrow, no vanilla buttons or counter. |
| `03-research-en.png` | Same fixed spread, readable neutral text and original separator. Red next arrow available. Embedded black/magenta formatting remains. |
| `04-research-illustrated-en.png` | Recipe and scanner illustration fit inside opposite pages. Original navigation sprites visible; title not incorrectly repeated. |
| `05-research-previous-en.png` | Previous actually moved back two pages. Prose and formatting fit within the parchment; red arrows remain correctly placed. |

The draw quad measures **332.8×235.3 logical pixels**, or **665.6×470.6 framebuffer pixels at GUI scale 2** (1280×720 captures). Visible opaque book art spans about x=319..960 and y=127..585 because the original sheet includes transparent margins; this is not a 480×337 stretched book.

Default full smoke is **NOT green**: final run compiled successfully and captured 01-07, successfully exercising English Next/Previous/Back, then stopped at `RUSSIAN_NEXT`: it assumes a third page for the shorter Russian Thaumonomicon text, whereas the fixed original-width layout has only a two-page spread. No fake extra page or harness change was made to satisfy that assumption.

Final `ResearchBrowserBackgroundTest` (2) and `ResearchBrowserFramesTest` (3) tests **passed**. Final visuals-scope run **passed** (10 frames). Its relevant frame was opened directly:

`/home/cyberfox-kali/wt/uifix/fabric/run-clienttest/screenshots/client-smoke-cb2d6854-13e7-4090-bd5b-aea32703d5a0-207-research-graph-parchment-en.png`

**207 judgement:** same correctly proportioned carved/rune frame, bright magenta nebula, exterior tabs and thin connectors as 01. Same intentional category-count/GTNH differences; changing background world does not change the UI art.

The casting-wand HUD was inspected in a separate **normal client**, not an injected HUD preview. The initial quick-play argument with a spaced world name failed; I selected the owned `New World` seed from the actual world list instead. For this supplied creative-only fixture I enabled commands using the native Open to LAN menu, supplied the wand below, and closed the client normally after screenshots (the LAN listener is stopped). No progress/acquisition claim. The current-vis amounts were aer=2000, terra=1500, ignis=1000, aqua=500, ordo=2500, perditio=100 centivis.

`/home/cyberfox-kali/wt/uifix/fabric/run/screenshots/2026-10-23_20.34.34.png` was opened directly. Its top-left gold dial and coloured tubes match the corresponding original HUD-sheet regions; the final image is 1280×720 at the normal client's GUI scale 3. `.tmp/uifix-wand-hud-4x.png` is only a diagnostic 4× crop of that screenshot, not an edited asset. Focus-cost and transient +/- indicators are implemented but were **not visually exercised** by this no-focus static-vis fixture. The unrelated existing oversized held wand model and vanilla tutorial toast in this scene were not changed.

### Reproduce

From this worktree:

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/uifix/.gradle-uifix
export DISPLAY=:0 XAUTHORITY=/home/cyberfox-kali/.xauth-display0
rm -rf fabric/run-clienttest/screenshots
flock /tmp/opencode/gradle.lock -c 'cd /home/cyberfox-kali/wt/uifix && ./gradlew :fabric:runClientSmoke'
```

This produces 01-07 then the documented Russian Next assumption failure. To reproduce the passing graph-focused visual suite and tests (preserve book frames elsewhere before clearing screenshots):

```sh
flock /tmp/opencode/gradle.lock -c 'cd /home/cyberfox-kali/wt/uifix && ./gradlew :common:test --tests thaumcraft.client.gui.ResearchBrowserBackgroundTest --tests thaumcraft.client.gui.ResearchBrowserFramesTest :fabric:runClientSmoke -PclientSmokeScope=visuals'
```

For manual HUD inspection, launch `:fabric:runClient` under the same lock/environment, select the owned seed world, enable commands for the fixture, and hold:

```mcfunction
/item replace entity @s weapon.mainhand with thaumcraft:wand[thaumcraft:wand_data={rod:"wood",cap:"iron",vis:{aer:2000,terra:1500,ignis:1000,aqua:500,ordo:2500,perditio:100}}]
```

## Remaining differences / limitations

- No right tabs appear with fewer than ten categories; the right mirror code exists but has no natural >9-category fixture here.
- Reference modpack Search and blue arrow are intentionally excluded, **not** missing vanilla features.
- Native font metrics, inline-image pagination and resolved native recipe layouts are not a complete port of `TCFontRenderer`; no font/pagination parity claim is made.
- The native parent-screen Back action uses the original history-arrow art even when the original history stack would be empty, preserving this port's existing navigation contract.
- Initial camera starts at remembered (0,0), not the oracle's static (-5,-6), for the reason above.
- HUD omissions are enumerated above. Thaumometer rendering was already implemented and is **unchanged**.
