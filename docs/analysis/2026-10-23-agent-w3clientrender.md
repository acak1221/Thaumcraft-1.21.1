# Wave 3 agent `w3clientrender` — the 25 remaining client rendering units

Worktree `/home/cyberfox-kali/wt/w3clientrender`, branch `agent/w3clientrender`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859).
Second opinion only: `/home/cyberfox-kali/OpenTC4` (it differs from the 4.2.3.5 jar, see
§"The 1.7.10 mirror flag" below).

Unit list: `/home/cyberfox-kali/units/w3clientrender.txt` (25 units, all of them still
`partial` in `docs/porting/scope-ledger.json`).

Status legend: `partial` -> `parity_verified` only when the native renderer/model
really reproduces the original's visible result.

## 0. The two findings the client reported

### 0.1 "the guardian's `HoodEye` is hidden in the port (`ModelEldritchGuardian.java:15`)" — NOT A DEFECT

The client read `isRender` (oracle `:122-127`) and concluded that the original draws the
hood eye in the normal pass. It does not.

* oracle `thaumcraft/client/renderers/models/entities/ModelEldritchGuardian.java:428-465`
  is the model's render method (`func_78088_a`). It renders **38** parts:
  `BeltR, Mbelt, MbeltL, MbeltR, BeltL, Chestplate, Hood1, Backplate, ShoulderplateTopR,
  ShoulderplateR1, ShoulderplateR2, ShoulderplateR3, ShoulderR, ArmL1, ArmR1, ShoulderL,
  ShoulderplateLtop, ShoulderplateL1, ShoulderplateL2, ShoulderplateL3, LegpanelR4,
  LegpanelR5, LegpanelR6, BackpanelR1, BackpanelR2, BackpanelR3, BackpanelL3, LegpanelL4,
  LegpanelL5, LegpanelL6, BackpanelL1, BackpanelL2, Cloak1, SidepanelR1, SidepanelL1,
  SidepanelL2, SidepanelR2, LegpanelC1`.
  **`HoodEye` is not in that list.** (`grep -c` over the oracle file: 38 `func_78785_a`
  calls in the list plus the one inside the warden block below.)
* The only place the original ever draws `HoodEye` is oracle `:467-479`:

  ```java
  if (entity instanceof EntityEldritchWarden) {
      GL11.glPushMatrix();
      GL11.glEnable(GL_BLEND);
      GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE);      // additive
      GL11.glScaled(1.01, 1.01, 1.01);
      int j = (int)(195.0f + MathHelper.func_76126_a(entity.field_70173_aa / 3.0f) * 15.0f + 15.0f);
      OpenGlHelper.func_77475_a(OpenGlHelper.field_77476_b, k / 1.0f, l / 1.0f);
      this.HoodEye.func_78785_a(f5);
      GL11.glDisable(GL_BLEND);
      GL11.glPopMatrix();
  }
  ```

  i.e. the eye is drawn **only for the warden**, in a separate additive pass at 1.01 scale.
* The native port does exactly that:
  * `common/src/main/java/thaumcraft/client/renderers/models/ModelEldritchGuardian.java:15`
    hides `HoodEye` for the *guardian* (the ordinary `MobRenderer` pass) — which is what
    the original's 38 part list does.
  * `common/src/main/java/thaumcraft/client/renderers/entity/RenderEldritchWarden.java:73-86`
    re-enables it, scales by `1.01` and draws it with `LightTexture.FULL_BRIGHT`.
* Textures: the port's `textures/models/eldritch_guardian.png` and `eldritch_warden.png`
  are byte identical to the 4.2.3.5 jar (`md5 1167d8b0d90d4930c58a2c27dad3b3cd` and
  `36abfde13ed567f544c2eeca5b0ff0c8`). The hood eye box samples the model UV region
  `(0,0)-(16,16)` of the 128x64 model space = pixels `(0,0)-(32,32)` of the 256x128 sheet.
  That region is **empty in the guardian sheet** and holds the dark red eye
  (`rgba(128,0,0,255)` around pixels x=18..31, y=21..25) in the **warden** sheet.

=> The port matches the original. The client's finding is a false positive: it read
`isRender` but not the render list. **No change made to the guardian model.**

The one remaining difference in this area is the blend state of the warden's eye pass
(additive in the original, plain translucent in the port) — see §0.3.

### 0.2 The 1.21.1 `mirror` vs the 1.7.10 mirror — the port is right, the premise is wrong

The client wrote: "the 1.21.1 `mirror` negates only the **X** of a face normal, while the
1.7.10 winding reversal negates the **whole** normal".

**The premise is incomplete: the 1.7.10 mirror is not only a winding reversal, it also
mirrors the geometry in X.** The two effects together leave exactly the X component
negated, which is what 1.21.1 does.

Evidence (vanilla 1.7.10/1.8 `ModelBox`, the class `ModelRenderer.Cube` was derived from):

```java
// ModelBox(ModelRenderer renderer, int u, int v, float x, float y, float z, int w, int h, int d, float shrink)
if (renderer.field_78809_i) {          // field_78809_i == ModelRenderer.mirror
    float var14 = var11;              // var11 == x + width
    var11 = x;                        // swap min-x and max-x  -> the box is mirrored in X
    x = var14;
}
...  // the six quads are built with the (possibly swapped) x
if (renderer.field_78809_i) {
    for (int i = 0; i < quads.length; ++i) quads[i].flip();   // reverse every quad's winding
}
```

* The 1.7.10 `ModelRenderer` has the public flag `field_78809_i` (default `false`);
  `field_78806_j` is `isRender` (default `true`) and `field_78807_k` is the hide flag
  (default `false`) — the render method tests `!field_78807_k && field_78806_j`.
  `func_78787_b(int,int)` is `setTextureSize(int,int)` and sets the **part's own**
  texture size (`field_78801_a`/`field_78799_b`), which is what the UV division uses.
* 1.21.1 `ModelPart$Cube` (byte code, `minecraft-merged-1.21.1-loom...jar`,
  `net/minecraft/client/model/geom/ModelPart$Cube`): with `mirror == true` it swaps
  `minX`/`maxX` exactly like 1.7.10 (`fstore_3`/`fstore 16` swap), and
  `ModelPart$Polygon` reverses the vertex order (`vertices[j] <-> vertices[len-1-j]`)
  exactly like 1.7.10's `TexturedQuad.flip()`.
* The **normal**: 1.7.10 derives the entity model normal from the geometry, 1.21.1 uses
  the fixed per-face `Direction.step()` and then `normal.mul(-1, 1, 1)` for a mirrored
  polygon. These agree:
  * a geometric mirror in X maps a normal `n` to `-M n` with `M = diag(-1,1,1)`
    (a mirror reverses the handedness), and the extra winding flip negates it once more,
    so the net normal is `M n = (-nx, ny, nz)` — the **X component only**;
  * 1.21.1 computes exactly `(-nx, ny, nz)`.
  * So "a winding reversal negates the whole normal" is true only for the winding
    reversal *alone*; 1.7.10 never applies it alone.

=> **The port's `CubeListBuilder.create().texOffs(...).mirror()` is the exact 1.21.1
equivalent of the 1.7.10 `ModelRenderer.mirror = true`.** No compensation is needed and
nothing is not representable. The difference the client saw is a difference between
"winding reversal alone" and "1.7.10 mirror", not a difference between the original
and the port.

### 0.3 One real difference next to the warden's eye: the blend state

* original oracle `ModelEldritchGuardian.java:467-479`: `glEnable(GL_BLEND)` +
  `glBlendFunc(GL_SRC_ALPHA, GL_ONE)` (additive) for the eye pass, then `glDisable`.
* port `RenderEldritchWarden.java:80-86`: the eye is drawn into the same
  `RenderType.entityTranslucent(SKIN)` buffer, i.e. `SRC_ALPHA, ONE_MINUS_SRC_ALPHA`.

The original adds the eye colour to the hood, the port replaces it. Fixed in this wave
with an additive render type (see the commit list).

## 1. The 25 units

(filled in below, one subsection per unit)
