# Agent wave 4 `w4wands` — the wand / node / vis family (2026-10-23)

Branch: `agent/w4wands` (worktree `/home/cyberfox-kali/wt/w4wands`).
Domain: `wands_nodes_and_vis` — the 9 units of `/home/cyberfox-kali/units/w4wands.txt`.

Sources used:

* the **oracle** `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte-exact, the authority);
* the **official Mojang 1.7.10 `client.jar`** (downloaded for this wave) for the two vanilla classes
  the oracle does not contain — `net.minecraft.client.renderer.entity.RenderPlayer` (notch `bop`)
  and `net.minecraft.client.renderer.entity.RenderLivingBase` (notch `boh`), decompiled with CFR
  0.152 — and the **MCP stable 12-1.7.10 mapping** (`methods.csv`, `fields.csv`) to resolve
  `field_71011_bu` = `getItemInUse`, `field_71057_bx` = `getItemInUseDuration`,
  `field_76379_h` = `fall` and `field_150383_bp` = `cauldron`;
* the 1.21.1 port `common/src/main/java/thaumcraft/**`;
* the original assets of `Thaumcraft-1.7.10-4.2.3.5.jar` (the two robe sheets are byte-identical
  to the port's copies).

**No Gradle was run in this worktree.** Every test named below is *authored but not executed*. The
changed sources were **type-checked** with `javac` against the access-widened 1.21.1 jar of the
leader's Gradle cache: the whole `common/src/main/java` tree and the whole `common/src/test/java`
tree compile with **zero errors** (`/tmp/opencode/check_w4wands.sh`).

---

## 0. The summary

| unit | before | after |
|---|---|---|
| `thaumcraft/api/nodes/INode.java` | partial | **parity_verified** |
| `thaumcraft/common/tiles/TileNode.java` | partial | **parity_verified** |
| `thaumcraft/common/world/aura/AuraChunk.java` | partial | **parity_verified** (disposition `exclude`) |
| `thaumcraft/common/world/aura/AuraHandler.java` | partial | **parity_verified** (disposition `exclude`) |
| `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/tile/ItemNodeRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/fx/beams/FXBeamWand.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/item/ItemWandRenderer.java` | partial | **parity_verified** |
| `thaumcraft/client/lib/REHWandHandler.java` | partial | **parity_verified** |

(work in progress — this table is filled in as the units are closed)
