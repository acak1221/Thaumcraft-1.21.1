# Agent wave 4 `w4golems` — the last eleven `partial` units of the golemmancy (2026-10-23)

Branch: `agent/w4golems` (worktree `/home/cyberfox-kali/wt/w4golems`).
Domain: `golems_and_tasks` — the 11 units of `/home/cyberfox-kali/units/w4golems.txt`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the **official Mojang 1.7.10 `client.jar`**, downloaded for this wave
  (`https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar`),
  for the vanilla 1.7.10 `assets/minecraft/textures/particle/particles.png`;
* the 1.21.1 port `common/src/main/java/thaumcraft/**`.

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*. The
changed sources are **type-checked** with `javac` against the access-widened 1.21.1 jar of the
leader's Gradle cache (see §6).

---

## 1. The bobber sprite (the unit `RenderGolemBobber.java`)

### 1.1 What the original does

Oracle `thaumcraft/client/renderers/entity/RenderGolemBobber.java:31,40-45`:

```java
private static final ResourceLocation tex = new ResourceLocation("textures/particle/particles.png");
...
int b0 = 1;
int b1 = 2;
float f2 = (float)(b0 * 8 + 0) / 128.0f;   // 0.0625
float f3 = (float)(b0 * 8 + 8) / 128.0f;   // 0.125
float f4 = (float)(b1 * 8 + 0) / 128.0f;   // 0.125
float f5 = (float)(b1 * 8 + 8) / 128.0f;   // 0.1875
```

The quad reads the cell **(1,2)** of the **vanilla 1.7.10** particle sheet: the columns 8..15 and
the rows 16..23 of the 128x128 `particles.png`.

### 1.2 The asset is now obtainable

The wave-3 report could not fetch the sheet because `launcherresources.mojang.com` does not resolve
here. It does not have to: the **1.7.10 `client.jar` still carries the assets**. From the version
manifest (`https://launchermeta.mojang.com/mc/game/version_manifest_v2.json` →
`https://piston-meta.mojang.com/v1/packages/ed5d8789ed29872ea2ef1c348302b0c55e3f3468/1.7.10.json`)
the client jar is
`https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar`
(5 256 245 bytes), and it contains `assets/minecraft/textures/particle/particles.png` (3 613 bytes,
128x128, RGBA). The 1.7.10 asset index only lists the language files and the icons, so the jar is
the only place the sheet exists.

The cell was lifted out of that file and is shipped as
`common/src/main/resources/assets/thaumcraft/textures/entity/golem_bobber.png` (8x8, RGBA, 110 bytes).

### 1.3 The change

* **new** `common/src/main/java/thaumcraft/client/renderers/entity/GolemBobberSprite.java` — the
  sheet constants of the original (128, 8, the cell 1/2 and the four sheet fractions) and the
  mapping of a sheet fraction into the cell. It holds no Minecraft type, so the numbers can be
  asserted without a client.
* `RenderGolemBobber.java:19-36` — the texture is the port's own 8x8 asset instead of the vanilla
  fishing hook; `:48-53` — the four corners are the original's four corners of the cell, mapped
  through `GolemBobberSprite.cellU/cellV`.

The alpha of the cell is only `0` or `255`, so the original's `GL11.glEnable(GL_BLEND)` and the
native `RenderType.entityCutout` draw the same pixels.

---

## 2. The fluid set of the Decanting golem

...

## 3. The marked essentia transports

...

## 4. The Use core and the three switches of its menu

...

## 5. The ledger

...

## 6. The `javac` proof

...
