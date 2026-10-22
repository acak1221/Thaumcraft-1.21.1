# Agent `focuslabel` — the focus radial's centre label (frame 43)

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/focuslabel`, branch `agent/focuslabel`.
Task: the one open visual defect of the passing `full` client smoke — the centre
label of the held-F focus radial is two overlapping text runs — plus one false
row of the scope ledger.

---

## 0. Result in one paragraph

**The centre label of the radial is drawn twice, with two different strings, in
one frame.** The client smoke renders `GuiFocusSelect` **twice into the same
framebuffer**: once by the game with the real OS cursor (which points at nothing
in the radial) and once by the harness's scripted hover render
(`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/BookClientSmoke.java:272-276`).
The first pass draws the hover placeholder *"Point at a focus"*, the second pass
draws the focused focus *"Wand Focus: Fire — slot 9"*; both are centred on the
same x and drawn on the same y, so the two runs lie on top of each other.
`GuiFocusSelect.centered()` is **not** the fault: it draws one string, once, and
centres it with the original's own expression. The fault is that the centre
line's *content* depends on the mouse hover, so two render passes of the same
screen leave two different strings in the same place.

The fix: the centre line may only carry the **focused focus**. When nothing is
focused, the radial draws **no** centre line at all (the original draws no
centre text either), so a second render pass can never leave a second string
behind the name.

---

## 1. What the frame actually contains

The frame of the passing run
(`client-smoke-3814fcfa-fc5d-4502-bb0b-10512ccb695d-43-native-held-F-focus-selection-radial-en.png`,
1280x720, GUI scale 2) was measured pixel by pixel. The text band of the
centre line is physical y 436..462:

```
glyph column groups (physical px):
[(510, 519), (522, 531), (534, 543), (546, 555), (560, 611), (614, 633),
 (638, 683), (686, 707), (710, 721), (724, 727), (730, 739), (742, 747),
 (758, 767)]
```

* The first four groups are `W`, `a`, `n`, `d`: each ten physical pixels wide,
  twelve physical pixels apart — the vanilla font's own advance (6 logical px at
  GUI scale 2). **They are clean.**
* From x = 560 on, the columns merge into one continuous blob: two glyphs now
  occupy the same columns.

Two runs, each centred on the screen centre x = 640:

| run | string | physical span | source |
|-----|--------|---------------|--------|
| A | `Wand Focus: Fire — slot 9` | 510 … 767 | the focused focus (`tc.port.focus.entry`) |
| B | `Point at a focus` | ~557 … ~723 | the hover placeholder (`tc.port.focus.choose`) |

Run B starts 47 physical pixels (23.5 logical px) after run A, which is exactly
why `Wand` survives: run A's first four glyphs are still the only ink there.
From run B's first glyph on, the two runs share their columns and lie on top of
each other. This is the "overlap from the fifth character on" of the frame
verdict, and it also explains the "the same string renders cleanly in the
tooltip": the tooltip's first line is the item name `Wand Focus: Fire` in the
same font, drawn once, at another place.

Everything else in the frame is drawn twice at the same place with the *same*
content (the title, `Equipped: None`, the help line, the ring, the item), so it
is pixel-identical and looks clean. Only the centre line is hover-dependent.

## 2. Why there are two render passes

`BookClientSmoke` registers a `ScreenEvents.afterRender` hook
(`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/BookClientSmoke.java:237-283`).
For `GuiFocusSelect` it re-renders the screen with a scripted hover point into
the framebuffer that the game has already drawn this frame:

```java
} else if (rendered instanceof GuiFocusSelect && !clothingOnly && ...) {
    var point = FocusClientSequence.hoverPoint(rendered);
    graphics.flush();
    rendered.render(graphics, point[0], point[1], delta);
}
```

So the captured frame contains

1. the game's own render of the screen with the **real** OS cursor — the
   cursor is not over the ring, so `choiceAt()` returns -1 and the old code
   drew the placeholder `tc.port.focus.choose` = *"Point at a focus"*; and
2. the harness's render with the **scripted** hover point over the Fire Focus
   icon — `choiceAt()` returns the focus, so the old code drew
   `tc.port.focus.entry` = *"Wand Focus: Fire — slot 9"* and the item tooltip.

The tooltip in the frame is the one of pass 2 (it is clamped to the left edge
because the "Frugal and Potency are implemented; …" line is wider than the
screen).

## 3. What the original does (the specification)

The original 4.2.3.5 radial draws **no centre text at all**:

* oracle `thaumcraft/client/lib/REHWandHandler.java:185` — `renderFocusRadialHUD`
  is the whole radial;
* oracle `:210-230` — the two ring quads (`textures/misc/radial.png`,
  `radial2.png`) are drawn centred on the screen centre;
* oracle `:231-249` — the equipped focus item is drawn at the centre, the
  hovered focus item is picked up for the tooltip;
* oracle `:287-289` — **the only text of the radial** is the tooltip, drawn
  **once**, at the mouse:
  `UtilsFX.drawCustomTooltip(mc.field_71462_r, ri, mc.field_71466_p, tt.func_82840_a(...), -4, 20, 11);`

There is no centred label in the original to copy, so the port's centre line is
a port-only addition and the original's *rule* for a centred line is the one the
port must follow — the original centres a string with

* oracle `thaumcraft/client/gui/GuiResearchRecipe.java:452-453`:
  `int offset = this.fr.getStringWidth(text) / 2; this.fr.drawString(text, x + start + 16 - offset, y + 33 + count * 50, 0x505050);`

i.e. **one draw of one string at `centre - font.width(text) / 2`**. Vanilla
1.21.1 does the same inside `GuiGraphics#drawCenteredString`
(`x - font.width(text) / 2`, verified in the client jar's bytecode), so the
port's `centered()` was already correct.

**The original therefore draws every string of the radial exactly once.** The
port's centre line broke that rule by making its *content* depend on the mouse
hover while two render passes of the screen share one framebuffer.

## 4. The defect in the port

`common/src/main/java/thaumcraft/client/gui/GuiFocusSelect.java` (before the
fix):

```java
Component selected = selectedIndex >= 0 ? entryMessage(choices.get(selectedIndex))
        : Component.translatable(choices.isEmpty() ? "tc.port.focus.empty" : "tc.port.focus.choose");
...
centered(graphics, selected, infoY, 0xFFE8D8B4);
```

`centered()` (`:222-224` before the fix) is innocent:

```java
private void centered(GuiGraphics graphics, Component text, int y, int color) {
    graphics.drawCenteredString(font, font.plainSubstrByWidth(text.getString(), Math.max(1, width - 16)), centerX, y, color);
}
```

`GuiGraphics#drawCenteredString(Font, String, int, int, int)` is
`drawString(font, text, x - font.width(text) / 2, y, color)` — one string, one
draw, the original's expression. The fault is the **ternary above it**: the
centre line's content is a function of the hover, so the two render passes of
the frame write two different strings to the same rectangle.

## 5. The fix

*(filled in below — see the commit list at the end)*

## 6. The test

*(filled in below)*

## 7. The ledger row

*(filled in below)*
