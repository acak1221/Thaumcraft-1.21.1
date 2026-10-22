# Wave 5 — `w5world`: the world generation, the chunk infrastructure and the cosmetic stairs (2026-10-23)

Agent: `w5world`. Worktree: `/home/cyberfox-kali/wt/w5world`, branch `agent/w5world`.
Oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859 fingerprints).

**No Gradle was run.** Every test below is **authored but NOT executed**; the leader runs the
build, the JUnit suite and the client.

---

## 0. The four units of this lane

| unit | family | the open difference before this wave |
|---|---|---|
| `thaumcraft/common/blocks/BlockCosmeticStairs.java` | base_content_and_small_interactions | the position hash of the texture of the eldritch stairs |
| `thaumcraft/common/lib/events/EventHandlerWorld.java` | shared_infrastructure | the chunk save/load marker |
| `thaumcraft/common/lib/events/ServerTickEventsFML.java` | shared_infrastructure | the chunk regeneration queue |
| `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` | natural_resources_and_worldgen | the scattered feature node, the `auraGen` exclusivity |

---

## 1. The eldritch stairs: the position hash of the texture

### 1.1 What the original does

`oracle thaumcraft/common/blocks/BlockCosmeticStairs.java:36-42`:

```java
public IIcon func_149673_e(IBlockAccess ba, int x, int y, int z, int side) {
    if (this.refBlock == ConfigBlocks.blockCosmeticSolid && this.refMeta == 11) {
        return this.refBlock.func_149673_e(ba, x, y, z, side + 100);
    }
    return this.refBlock.func_149691_a(side, this.refMeta);
}
```

The eldritch stairs are `new BlockCosmeticStairs(blockCosmeticSolid, 11)`
(`oracle thaumcraft/common/config/ConfigBlocks.java:297`), so the stair asks the cosmetic solid
for its icon with the side index **plus a hundred**. The cosmetic solid answers with the
position hash (`oracle thaumcraft/common/blocks/BlockCosmeticSolid.java:157-162`):

```java
if (md == 11 || md == 13 || side >= 100) {
    String l = x + "" + y + "" + z;
    Random r1 = new Random(Math.abs(l.hashCode() * 100) + 1);
    int i = r1.nextInt(12345 + side) % 4;
    return this.icon[17 + i];
}
```

The four icons 17 to 20 are the ancient stone textures `es_1`, `es_2`, `es_3` and `es_4`
(`oracle BlockCosmeticSolid:92-95`).

The 1.7.10 renderer asks the block for the icon **of every rendered face**: the stair renderer
of the official 1.7.10 client jar (`net/minecraft/client/renderer/RenderBlocks`, the obf class
`blm`, the method `a(BlockStairs,int,int,int)`, the six calls
`this.a(aji2, this.a, n, n2, n3, 0..5)`) draws the six faces with the side indices 0 to 5.
⇒ **the original eldritch stair can show up to four different ancient stone textures at once**,
one per face.

### 1.2 The claim of the previous wave was wrong

The note of the ledger row said: *"the same raw draw is taken modulo six different bounds, so
all six faces of a stair agree in all but one of about twelve thousand positions"*.

That is **false**. Measured on the JVM with the formula of the oracle (200 000 positions,
`/tmp/opencode/w5/HashModel.java`):

```
stairs faces agree: 0.023
solid faces agree: 0.0285
```

The six faces of a stair carry the **same** texture index in only **2.3 %** of the positions.
The raw draw `next(31)` is indeed the same for the six bounds, but the six bounds differ, so
the six residues differ, and the residues differ modulo four as well.

### 1.3 What the port does now

The 1.21.1 stair model is baked per block state and the vanilla stair model has **one** texture
for the four vertical faces, so the port cannot paint six different textures of one stair
without six state properties (4^6 = 4096 models per shape). The port therefore

* carries the index of the texture in the block state (`BlockCosmeticStairs.TEXTURE`, 0..3),
* computes the index with the **original formula** (`BlockCosmeticStairs.textureIndex`),
* uses the index of the **top face** (the 1.7.10 side index one) for the whole stair,
* and has one model per index and stair shape (twelve models) and 160 blockstate variants.

Files: `common/src/main/java/thaumcraft/common/blocks/BlockCosmeticStairs.java`,
`common/src/main/resources/assets/thaumcraft/models/block/eldritch_stairs{,_inner,_outer}_{0..3}.json`,
`common/src/main/resources/assets/thaumcraft/blockstates/eldritch_stairs.json`,
`common/src/main/resources/assets/thaumcraft/models/item/eldritch_stairs.json`.

`python3 scripts/porting/convert_assets.py --verify` → `missing_references: 0`.

### 1.4 The remaining difference (a 1.21.1 model limit)

The original draws an index **per face**; the port draws one index per stair and paints every
face with it. The two stairs of the same kind still look different (the purpose of the
original), but a single stair of the original is a patchwork of up to four ancient stone
textures and the stair of the port shows one of them. A 1.21.1 stair model has a single
texture slot for the four vertical faces, so the per face granularity needs a custom client
renderer, not a block state.

---
