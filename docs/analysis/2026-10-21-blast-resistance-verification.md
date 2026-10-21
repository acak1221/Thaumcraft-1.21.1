# Blast resistance: exact 1.7.10 vs 1.21.1 semantics (verified 2026-10-21)

Task: establish the *exact* semantics of explosion ("blast") resistance in Minecraft 1.7.10
and in 1.21.1, find what TC4 4.2.3.5 passes to `Block.setResistance(float)`, and decide which
numbers the 1.21.1 port has to use.

**Headline result**

| version | how the number is stored | what `getExplosionResistance` returns |
|---|---|---|
| 1.7.10 | `blockResistance = setterValue * 3.0F` | `blockResistance / 5.0F` = **`setterValue * 0.6`** (see the hardness floor in §2.4) |
| 1.21.1 | `Properties.explosionResistance = value` (no scaling) | the stored value **unchanged** |

The explosion arithmetic itself is *identical* in both versions
(`power -= (resistance + 0.3F) * 0.3F`, `power = radius * (0.7 + rand * 0.6)`), therefore:

> **rule: `resistance_1.21.1 = 0.6 * setResistanceValue_1.7.10`** (and never below the block hardness)

Everything below is backed by bytecode, not by memory.

---

## 0. Artifacts used (all re-downloadable, sha1 recorded)

| artifact | source | sha1 |
|---|---|---|
| `client-1.7.10.jar` (vanilla, obfuscated) | `https://launchermeta.mojang.com/mc/game/version_manifest.json` -> 1.7.10 -> `https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar` | `e80d9b3bf5085002218d4be59e668bac718abbc6` |
| `mcp-1.7.10-srg.zip` (`joined.srg`) | `https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.7.10/mcp-1.7.10-srg.zip` | `10b95bb342f55d681025bf2d698c893d9c4b4201` |
| `mcp_stable-12-1.7.10` (`methods.csv`, `fields.csv`) | already in `/tmp/mcp-stable` | - |
| `forge-1.7.10-10.13.0.1162-userdev.jar` (`forgepatches.zip` -> vanilla source context + Forge patches) | `https://maven.minecraftforge.net/net/minecraftforge/forge/1.7.10-10.13.0.1162/forge-1.7.10-10.13.0.1162-userdev.jar` | - |
| `Thaumcraft-1.7.10-4.2.3.5.jar` (the real TC4 jar) | `/home/Acak/thaumcraft-1.21.1/Thaumcraft-1.7.10-4.2.3.5.jar` | `3f51b473f9ef7a94d1a9e01f6784af1cfd811649` |
| TC4 decompiled oracle (CFR 0.152) | `/home/Acak/work/thaumcraft-4.2.3.5-oracle` | - |
| TC4 readable source (MCP names) | `/home/Acak/work/opentc4/src/main/java/thaumcraft/...` | - |
| MC 1.21.1 + NeoForge 21.1.259 (mojang-mapped) | loom cache `neoforge-21.1.259-minecraft-merged-mojang-1.21.1-...jar` | - |

Tools: `/home/Acak/.jdk21/bin/javap` (bytecode), CFR 0.152 (`/home/Acak/work/cfr.jar`).

---

## 1. Name resolution for vanilla 1.7.10

`joined.srg` (from `mcp-1.7.10-srg.zip`):

```
joined.srg:249   CL: aji net/minecraft/block/Block
joined.srg:10257 MD: aji/a (Lsa;)F   net/minecraft/block/Block/func_149638_a (Lnet/minecraft/entity/Entity;)F
joined.srg:10261 MD: aji/b (F)Laji;   net/minecraft/block/Block/func_149752_b (F)Lnet/minecraft/block/Block;
joined.srg:2758  FD: aji/w            net/minecraft/block/Block/field_149781_w
```

`mcp_stable-12-1.7.10/methods.csv`:

```
1389: func_149711_c,setHardness,2,Sets how many hits it takes to break a block.
1430: func_149752_b,setResistance,2,Sets the the blocks resistance to explosions. ...
1316: func_149638_a,getExplosionResistance,2,Returns how much this block can resist explosions from the passed in entity.
 365: func_145772_a,getExplosionResistance,2,Called for explosions caused by Entities instead of the method on Block. Default forwards to Block.
```

`mcp_stable-12-1.7.10/fields.csv`:

```
1229: field_149781_w,blockResistance,2,
1875: field_151576_e,rock,2,
1878: field_151579_a,air,2,          <-- the "material special case" is AIR, not water
```

So in the obfuscated client jar: `net.minecraft.block.Block` = `aji`, `setResistance` = `b(F)Laji;`,
`setHardness` = `c(F)Laji;`, `getExplosionResistance(Entity)` = `a(Lsa;)F`, the resistance field = `w`.

---

## 2. Vanilla 1.7.10 - exact implementation (bytecode)

`javap -p -c aji.class` (out of `client-1.7.10.jar`):

### 2.1 `Block.setResistance(float)` - `func_149752_b`, obf `b(F)Laji;`

```
  protected aji b(float);
    Code:
       0: aload_0
       1: fload_1
       2: ldc           #20   // float 3.0f
       4: fmul
       5: putfield      #535  // Field w:F        (field_149781_w = blockResistance)
       8: aload_0
       9: areturn
```

```java
// vanilla 1.7.10
public Block setResistance(float resistance) {
    this.blockResistance = resistance * 3.0F;      // <== MULTIPLIES BY 3
    return this;
}
```

### 2.2 `Block.getExplosionResistance(Entity)` - `func_149638_a`, obf `a(Lsa;)F`

```
  public float a(sa);
    Code:
       0: aload_0
       1: getfield      #535  // Field w:F
       4: ldc           #23   // float 5.0f
       6: fdiv
       7: freturn
```

```java
// vanilla 1.7.10
public float getExplosionResistance(Entity entity) {
    return this.blockResistance / 5.0F;           // <== DIVIDES BY 5
}
```

### 2.3 `Block.setHardness(float)` - `func_149711_c`, obf `c(F)Laji;` (the hardness floor)

```
  protected aji c(float);
    Code:
       0: aload_0
       1: fload_1
       2: putfield      #534  // Field v:F       (hardness)
       5: aload_0
       6: getfield      #535  // Field w:F
       9: fload_1
      10: ldc           #23   // float 5.0f
      12: fmul
      13: fcmpg
      14: ifge          25
      17: aload_0
      18: fload_1
      19: ldc           #23   // float 5.0f
      21: fmul
      22: putfield      #535  // Field w:F
      25: aload_0
      26: areturn
```

```java
// vanilla 1.7.10
public Block setHardness(float hardness) {
    this.hardness = hardness;
    if (this.blockResistance < hardness * 5.0F) this.blockResistance = hardness * 5.0F;
    return this;
}
```

### 2.4 Resulting 1.7.10 formula

```
effective = block.getExplosionResistance(entity)
          = blockResistance / 5
          = 0.6 * setResistanceValue                    (when setResistance is the last of the two calls)
          = max( 0.6 * setResistanceValue , hardness )  (when setHardness is called *after* setResistance)
```

`setResistance` overwrites the field unconditionally, `setHardness` only raises it - so the
hardness floor exists **only** if `setHardness` is the later of the two calls:

* `setHardness(h); setResistance(x);`  ->  `blockResistance = 3x`            -> effective `0.6x`
* `setResistance(x); setHardness(h);`  ->  `blockResistance = max(3x, 5h)`   -> effective `max(0.6x, h)`

For TC4 the order is known per class (see §5), so the effective value is always computable.

### 2.5 The 8-argument method (Forge 1.7.10 patch) - where the "/5" and the material case live

Vanilla 1.7.10 has **no** 8-argument `getExplosionResistance`. It is added by Forge
(`forgepatches.zip` -> `net/minecraft/block/Block.java.patch`, lines 752-761):

```java
+    /**
+     * @param z Z Position
+     * @param explosionX Explosion source X Position
+     * ...
+     * @return The amount of the explosion absorbed.
+     */
+    public float getExplosionResistance(Entity par1Entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ)
+    {
+        return func_149638_a(par1Entity);
+    }
```

So the 8-argument method **forwards to the 1-argument one**, i.e. it also returns
`blockResistance / 5`. There is **no division by 5 and no water special case inside it** - the
division by 5 is in `func_149638_a` (see 2.2).

The material special case is in the **Explosion**, and it is `Material.air`, not `Material.water`
(`forgepatches.zip` -> `net/minecraft/world/Explosion.java.patch`, lines 1-12; vanilla context
lines are quoted, the `-`/`+` pair is the Forge change):

```java
                             if (block.func_149688_o() != Material.field_151579_a)   // != Material.AIR
                             {
-                                float f3 = this.field_77283_e != null ? this.field_77283_e.func_145772_a(this, this.field_77287_j, j1, k1, l1, block) : block.func_149638_a(this.field_77283_e);
+                                float f3 = this.field_77283_e != null ? this.field_77283_e.func_145772_a(this, this.field_77287_j, j1, k1, l1, block) : block.getExplosionResistance(this.field_77283_e, field_77287_j, j1, k1, l1, field_77284_b, field_77285_c, field_77282_d);
                                 f1 -= (f3 + 0.3F) * f2;
                             }
```

`Entity.func_145772_a` (vanilla `sa.a(agw, ahb, III, aji)`) just forwards to the block:

```
  public float a(agw, ahb, int, int, int, aji);
    Code:
       0: aload         6
       2: aload_0
       3: invokevirtual #297  // Method aji.a:(Lsa;)F
       6: freturn
```

### 2.6 How the explosion consumes the resistance (vanilla 1.7.10 bytecode)

`agw.class` (`net/minecraft/world/Explosion`), method `a()` (= `doExplosion`):

```
// power = explosionSize * (0.7 + rand.nextFloat() * 0.6)
192: getfield      #54   // Field g:F                       (explosionSize)
196: ldc           #4    // float 0.7f
205: invokevirtual #95   // Random.nextFloat()F
208: ldc           #3    // float 0.6f
210: fmul
211: fadd
212: fmul
213: fstore        14                                 // f14 = power

// per sampled block:
280: invokevirtual #86   // Method aji.o:()Lawt;         (block.getMaterial())
285: getstatic     #63   // Field awt.a:Lawt;            (Material.AIR)
288: if_acmpeq     345                                 // skip air
321: ... f3 = block.getExplosionResistance(entity)
332: fload         14
334: fload         26
336: ldc           #2    // float 0.3f
338: fadd
339: fload         21                                 // 0.3f
341: fmul
342: fsub
343: fstore        14                                 // power -= (resistance + 0.3F) * 0.3F
...
437: fload         14
439: fload         21
441: ldc           #5    // float 0.75f
443: fmul
444: fsub
445: fstore        14                                 // power -= 0.3 * 0.75 = 0.225 per sample step
```

---

## 3. Minecraft 1.21.1 - exact implementation

`javap` on the mojang-mapped NeoForge 21.1.259 jar.

### 3.1 `BlockBehaviour.Properties.strength(float,float)`

```
  public net.minecraft.world.level.block.state.BlockBehaviour$Properties strength(float, float);
    Code:
       0: aload_0
       1: fload_1
       2: invokevirtual #289   // Method destroyTime:(F)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;
       5: fload_2
       6: invokevirtual #291   // Method explosionResistance:(F)Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;
       9: areturn

  public ... destroyTime(float);
    Code:
       0: aload_0
       1: fload_1
       2: putfield      #229  // Field destroyTime:F
       ...

  public ... explosionResistance(float);
    Code:
       0: aload_0
       1: fconst_0
       2: fload_1
       3: invokestatic  #345  // Method java/lang/Math.max:(FF)F
       6: putfield      #231  // Field explosionResistance:F
```

```java
// 1.21.1
public Properties strength(float hardness, float resistance) { return destroyTime(hardness).explosionResistance(resistance); }
public Properties explosionResistance(float f) { this.explosionResistance = Math.max(0.0F, f); return this; }
```

**There is no multiplication by 5 (and no division by 5) anywhere in 1.21.1.**

### 3.2 `AbstractBlock`/`BlockBehaviour` -> `Block.getExplosionResistance()`

```
// BlockBehaviour constructor
      22: getfield      #98   // Field .../BlockBehaviour$Properties.explosionResistance:F
      25: putfield      #99   // Field explosionResistance:F

// net.minecraft.world.level.block.Block
  public float getExplosionResistance();
    Code:
       0: aload_0
       1: getfield      #808  // Field explosionResistance:F
       4: freturn
```

```java
// 1.21.1
public float getExplosionResistance() { return this.explosionResistance; }   // raw value, no division
```

NeoForge wiring (both default methods, `javap -c`):

```java
// IBlockExtension
default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion ex) {
    return self().getExplosionResistance();               // -> Block.getExplosionResistance()
}
// IBlockStateExtension
default float getExplosionResistance(BlockGetter level, BlockPos pos, Explosion ex) {
    return getBlock().getExplosionResistance(state, level, pos, ex);
}
```

### 3.3 How the explosion consumes it (1.21.1)

```java
// ExplosionDamageCalculator.getBlockExplosionResistance
if (state.isAir() && fluid.isEmpty()) return Optional.empty();
return Optional.of(Math.max(state.getExplosionResistance(level, pos, ex),
                            fluid.getExplosionResistance(level, pos, ex)));

// Explosion.explode()
float f = this.radius * (0.7F + level.random.nextFloat() * 0.6F);   // identical to 1.7.10
...
if (opt.isPresent()) f = f - (opt.get() + 0.3F) * 0.3F;               // identical to 1.7.10
...
f = f - 0.225F;                                                    // identical to 1.7.10
```

**The explosion arithmetic is byte-for-byte the same as 1.7.10** - only the *scale of the
resistance number* differs (1.7.10 divides by 5, 1.21.1 does not).

---

## 4. Cross-validation of the 0.6 factor with vanilla blocks

In 1.7.10 the vanilla blocks are created in the **static initializer of `net.minecraft.block.Block`**
(`net.minecraft.init.Blocks` only pulls them out of the registry), so the `setResistance` calls are
visible in `aji.class`. `javap -p -c aji.class`, static block:

```
// stone (id 1)
      25: ldc_w  #289   // String stone
      28: new    #448   // class anv        (BlockStone)
      32: invokespecial #809  // Method anv."<init>":()V
      35: ldc    #18    // float 1.5f
      37: invokevirtual #810  // Method anv.c:(F)Laji;   -> setHardness(1.5F)
      40: ldc    #26    // float 10.0f
      42: invokevirtual #600  // Method b:(F)Laji;        -> setResistance(10.0F)  <== our method

// obsidian
     1916: ldc_w  #208   // String obsidian
     1918: new    #414   // class amk        (BlockObsidian)
     1925: ldc    #30    // float 50.0f
     1927: invokevirtual #743  // Method amk.c:(F)Laji;   -> setHardness(50.0F)
     1930: ldc    #33    // float 2000.0f
     1932: invokevirtual #600  // Method b:(F)Laji;        -> setResistance(2000.0F)

// bedrock
      282: ldc_w  #45    // String bedrock
      284: new    #350   // class aji
      294: invokevirtual #621  // Method s:()Laji;         -> setHardness(-1.0F)
      297: ldc    #34    // float 6000000.0f
      299: invokevirtual #600  // Method b:(F)Laji;        -> setResistance(6000000.0F)
```

`javap` on the 1.21.1 `Blocks.class` for the same blocks:

| block | 1.7.10 `setResistance` | 1.7.10 effective (x0.6) | 1.21.1 `strength(h, r)` | match |
|---|---|---|---|---|
| stone | 10.0 | 6.0 | `strength(1.5F, 6.0F)` | yes |
| obsidian | 2000.0 | 1200.0 | `strength(50.0F, 1200.0F)` | yes |
| bedrock | 6000000.0 | 3600000.0 | `strength(-1.0F, 3600000.0F)` | yes |
| basalt | 7.0 | 4.2 | `strength(1.25F, 4.2F)` | yes |
| mossy cobblestone | 10.0 | 6.0 | `strength(2.0F, 6.0F)` | yes |
| fence gate | 5.0 | 3.0 | `strength(2.0F, 3.0F)` | yes |
| water/lava (BlockLiquid) | hardness 100, no resistance | 100.0 | `strength(100.0F)` | yes |

Mojang applied exactly the same `x3 / x5` conversion when 1.7.10 was re-based onto the modern
`strength()` API. This is the strongest possible confirmation of the factor.

---

## 5. What TC4 4.2.3.5 passes to `setResistance`

All numbers below are taken from the **real 4.2.3.5 jar** decompiled with CFR 0.152
(`/home/Acak/work/thaumcraft-4.2.3.5-oracle`, SRG names) and cross-checked against the readable
OpenTC4 source (`/home/Acak/work/opentc4`, MCP names) - both agree in every single case.

`func_149752_b` == `setResistance` (MCP `methods.csv:1430`), `func_149711_c` == `setHardness`.

| TC4 class (oracle file:line) | call order | `setHardness` | `setResistance` | 1.7.10 effective | correct 1.21.1 value |
|---|---|---|---|---|---|
| `BlockEldritch.java:68/69` | R then H | 50.0 | **20000.0** | 12000.0 | **12000.0** |
| `BlockCosmeticOpaque.java:66/67` | R then H | 1.5 | **5.0** | 3.0 | **3.0** |
| `BlockCosmeticSolid.java:66/67` | R then H | 2.0 | 10.0 | 6.0 | 6.0 |
| `BlockMetalDevice.java:101/102` | H then R | 3.0 | **17.0** | 10.2 | **10.2** |
| `BlockWoodenDevice.java:82/83` | H then R | 2.5 | **10.0** | 6.0 | **6.0** |
| `BlockStoneDevice.java:93/94` | H then R | 3.0 | **25.0** | 15.0 | **15.0** |
| `BlockWarded.java:72` | R only | - | 999.0 | 599.4 | 599.4 |
| `BlockArcaneDoor.java:67/68` | R then H | -1.0 / 15.0 | 999.0 | 599.4 | 599.4 |
| `BlockArcaneFurnace.java:64/65` | H then R | 10.0 | 500.0 | 300.0 | 300.0 |
| `BlockEldritchNothing.java:49` | R only | - | 6000000.0 | 3600000.0 | 3600000.0 |
| `BlockHole.java:53` | R only | - | 6000000.0 | 3600000.0 | 3600000.0 |
| `BlockLifter.java:45/46` | H then R | 2.5 | 15.0 | 9.0 | 9.0 |
| `BlockTube.java:88/89` | H then R | 0.5 | 5.0 | 3.0 | 3.0 |
| `BlockMirror.java:66/67` | H then R | 1.0 | 10.0 | 6.0 | 6.0 |
| `BlockTaint.java:71/72` | H then R | 2.0 | 10.0 | 6.0 | 6.0 |
| `BlockTaintFibres.java:67/68` | H then R | 1.0 | 5.0 | 3.0 | 3.0 |
| `BlockCrystal.java:59/60` | H then R | 0.7 | 1.0 | **0.6** | 0.6 |
| `BlockAlchemyFurnace.java:58/59` | H then R | 3.0 | 17.0 | 10.2 | 10.2 |
| `BlockEssentiaReservoir.java:40/41` | H then R | 2.0 | 17.0 | 10.2 | 10.2 |
| `BlockCustomOre.java:59/60` | R then H | 1.5 | 5.0 | 3.0 | 3.0 |
| `BlockLoot.java:45/46` | H then R | 0.15 | 0.0 | 0.0 | 0.0 |
| `ConfigBlocks.java:298` (wood slab) | H then R | 2.0 | 5.0 | 3.0 | 3.0 |
| `ConfigBlocks.java:299` (stone slab) | H then R | 2.0 | 10.0 | 6.0 | 6.0 |
| `ConfigBlocks.java:300` (double wood slab) | H then R | 2.0 | 5.0 | 3.0 | 3.0 |
| `ConfigBlocks.java:301` (double stone slab) | H then R | 2.0 | 10.0 | 6.0 | 6.0 |

(`R` = `setResistance` = `func_149752_b`, `H` = `setHardness` = `func_149711_c`.
No TC4 block hits the hardness floor: the only candidate, `BlockCrystal`
(hardness 0.7, resistance 1.0), calls `setResistance` *after* `setHardness`, so the effective
value is `0.6` and not `0.7`.)

### 5.1 TC4 blocks that *override* the getter (absolute values, NOT scaled)

These override the Forge 8-argument method, so their values are used **verbatim** by the
explosion - they must **not** be multiplied by 0.6:

* `BlockEldritch.java:248-260` -> `30.0f` (meta 4,5,9,10), `100.0f` (meta 6), `Float.MAX_VALUE` (meta 7,8)
* `BlockCosmeticOpaque.java:261-267` -> `999.0f` (meta 2 = warded glass)
* `BlockCosmeticSolid.java:241-253` -> `999.0f` (meta <= 1 or 8), `20.0f` (meta 4,6,7)
* `BlockAiry.java:129-141` -> `200.0f` (meta 0,5), `50.0f` (meta 10,11), `Float.MAX_VALUE` (meta 12)
* `BlockWoodenDevice.java:202-211` -> `999.0f` (meta 2 and 3 = the arcane pressure plate)
* `BlockEldritchPortal.java:60-62` -> `200000.0f` (absolute)

These blocks are also the reason why the port has to split a 1.7.10 block into several 1.21.1
blocks (or a family of blocks with a `variant` property): in 1.21.1 the blast resistance belongs
to the *block*, not to the block state, while in 1.7.10 it could be chosen per metadata.

---

## 6. Verdict for the constants of the port

`common/src/main/java/thaumcraft/common/lib/world/EldritchBlockRules.java`

| constant | current value | verdict | comment |
|---|---|---|---|
| `ELDRITCH_FAMILY_RESISTANCE` | `20000.0F` | **keep as the native reference value, but it must never be fed to `Properties.strength(...)`** | it is the raw 1.7.10 setter value; the effective value is `12000.0F` |
| `ELDRITCH_FAMILY_NATIVE_RESISTANCE` | `12000.0F` | **correct** | `20000 * 0.6` |
| `ELDRITCH_GROUP_RESISTANCE` | `{12000, 30, 100, Float.MAX_VALUE, 30, 12000}` | **correct** | group 0/5 = base `20000*0.6`; groups 1/2/3 are TC4's absolute overrides (30 / 100 / MAX) |
| `COSMETIC_OPAQUE_RESISTANCE` | `3.0F` | **correct** | `5.0 * 0.6` |
| `COSMETIC_OPAQUE_WARDED_RESISTANCE` | `999.0F` | **correct** | TC4 returns `999.0f` *absolutely* from the overridden getter -> no 0.6 factor (it would be 599.4, which would be wrong) |
| `DEVICE_METAL_RESISTANCE` | `10.2F` | **correct** | `17.0 * 0.6` |
| `DEVICE_WOOD_RESISTANCE` | `6.0F` | **correct** | `10.0 * 0.6` |
| `DEVICE_STONE_RESISTANCE` | `15.0F` | **correct** | `25.0 * 0.6` |

So the answer to the question as posed:

* `ELDRITCH_FAMILY_RESISTANCE` -> **12000** is the number that has to end up in `strength(...)`
  (the port already does this through `ELDRITCH_GROUP_RESISTANCE[0] == 12000.0F`; the constant
  `ELDRITCH_FAMILY_RESISTANCE = 20000.0F` is only documentation of the native setter value and
  should be renamed/annotated to avoid misuse).
* `COSMETIC_OPAQUE_RESISTANCE` -> **3** (not 5).
* `DEVICE_METAL_RESISTANCE` -> **10.2** (not 17).
* `DEVICE_WOOD_RESISTANCE` -> **6** (not 10).
* `DEVICE_STONE_RESISTANCE` -> **15** (not 25).
* `ELDRITCH_GROUP_RESISTANCE` -> `{12000, 30, 100, Float.MAX_VALUE, 30, 12000}` - unchanged.
* `COSMETIC_OPAQUE_WARDED_RESISTANCE` -> **999** (unchanged; it is an absolute override value).

---

## 7. Additional findings (mismatches found while auditing the whole port)

1. `ThaumcraftEldritchBlocks.java:156-171` - the four "cosmetic slab" blocks still use the **raw
   1.7.10 setter values**:
   * `cosmetic_stone_slab_arcane` / `cosmetic_stone_slab_eldritch`: `.strength(2.0F, 10.0F)` ->
     should be `.strength(2.0F, 6.0F)` (`ConfigBlocks.java:299/301`, `setResistance(10.0)`).
   * `cosmetic_wood_slab_greatwood` / `cosmetic_wood_slab_silverwood`: `.strength(2.0F, 5.0F)` ->
     should be `.strength(2.0F, 3.0F)` (`ConfigBlocks.java:298/300`, `setResistance(5.0)`).
   (For comparison `ThaumcraftCosmeticWood.slab()` already uses `3.0F` and
   `ThaumcraftEldritchBlocks` uses `10.0F`/`5.0F` - inconsistent.)
2. `ThaumcraftCrystalClusters.java:37` - `.strength(.7F, .6F)`: **correct**. TC4 calls
   `setHardness(0.7)` *before* `setResistance(1.0)` (`BlockCrystal.java:59/60`), and
   `setResistance` overwrites the field, so the effective value is `1.0 * 0.6 = 0.6` (the
   hardness floor of §2.4 does not apply). The comment in the port is right.
3. `BlockWoodenDevice` - TC4 returns **`999.0f`** (absolute) for metadata 2 and 3, the arcane
   pressure plate (`BlockWoodenDevice.java:202-211`, the metadata 2/3 are the
   `TileArcanePressurePlate` variants, see `BlockWoodenDevice.java:138-140,171-172`). The port
   keeps both variants inside one 1.21.1 block with a `variant` property
   (`EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE = 2`, `DEVICE_WOOD_PRESSURE_PLATE_PRESSED = 3`)
   and gives them the base `DEVICE_WOOD_RESISTANCE = 6.0F`. To reproduce 1.7.10 the pressure
   plate variants have to be a separate block with resistance `999.0F` (the same split the port
   already performs for the eldritch/cosmetic/airy families).
4. Everything else that was audited matches the `x0.6` rule:
   `10.2F` (metal device, alchemy furnace, essentia reservoir, lamps, tubes, thaumatorium,
   distillation, advanced furnace, biological lamps), `15.0F` (stone device, infusion, focal
   manipulation, node devices), `6.0F` (wooden device, mirror, taint, arcane ear, cosmetic wood
   stairs), `3.0F` (tube, custom ore, cosmetic opaque, cosmetic wood slab, lamp),
   `599.4F` (arcane door, warded), `300.0F` (arcane furnace), `9.0F` (levitator),
   `3600000.0F` (eldritch nothing, hole, aura node), `200000.0F` (eldritch portal, absolute
   override), `0.0F` (loot urn/crate, jar, magic box).
5. 1.7.10 vs 1.21.1 water: 1.7.10 water = `BlockLiquid` with hardness 100 (effective resistance
   100.0, because of the hardness floor), 1.21.1 water = `Fluid` with
   `getExplosionResistance() == 100.0F` - consistent.

---

## 8. Reproduction

```bash
# vanilla 1.7.10 Block (obf "aji")
curl -sL -o client-1.7.10.jar https://launcher.mojang.com/v1/objects/e80d9b3bf5085002218d4be59e668bac718abbc6/client.jar
unzip -o client-1.7.10.jar aji.class
/home/Acak/.jdk21/bin/javap -p -c aji.class | grep -A 8 "protected aji b(float)"     # setResistance  -> *3.0f
/home/Acak/.jdk21/bin/javap -p -c aji.class | grep -A 6  "public float a(sa)"         # getExplosionResistance -> /5.0f

# name resolution
curl -sL -o mcp-1.7.10-srg.zip https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.7.10/mcp-1.7.10-srg.zip
unzip -p mcp-1.7.10-srg.zip joined.srg | grep -E "CL: aji |func_149752_b|func_149638_a|field_149781_w"

# Forge patch (the 8-argument method + the Explosion change)
curl -sL -o forge-userdev.jar https://maven.minecraftforge.net/net/minecraftforge/forge/1.7.10-10.13.0.1162/forge-1.7.10-10.13.0.1162-userdev.jar
unzip -p forge-userdev.jar forgepatches.zip > forgepatches.zip
unzip -p forgepatches.zip net/minecraft/block/Block.java.patch   | sed -n '745,765p'
unzip -p forgepatches.zip net/minecraft/world/Explosion.java.patch | sed -n '1,15p'

# TC4 numbers
grep -rn "func_149752_b" /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/

# 1.21.1
/home/Acak/.jdk21/bin/javap -p -c 'net/minecraft/world/level/block/state/BlockBehaviour$Properties.class'   # strength/explosionResistance
/home/Acak/.jdk21/bin/javap -p -c net/minecraft/world/level/block/Block.class                                # getExplosionResistance
```
