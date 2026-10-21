# Recipes and arcane crafting - the verified coverage table of the original TC4 recipe catalogue

Agent: `recipes` (wave of 2026-10-23). Worktree: `/home/cyberfox-kali/wt/recipes`, branch
`agent/recipes`.

Oracle: `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/config/ConfigRecipes.java`
(the pinned 1.7.10 reference of TC4 4.2.3.5). **The line numbers of this document are the line
numbers of THAT file.** The private oracle of the earlier waves numbers the same registrations
slightly differently (its `ConfigRecipes` is a few lines longer), so the line numbers quoted in
`common/src/test/java/thaumcraft/assets/NormalRecipeParityTest.java` and in the older ledger
notes are NOT the numbers of this document; the quoted source lines are.

## 1. What the original catalogue is

`ConfigRecipes.init()` (line 38) calls five registration methods. Between them they register
**308 distinct original recipe keys** (279 `ConfigResearch.recipes.put` call sites, five of which
are inside a loop and expand to 34 keys):

| original method | oracle lines | keys | kind |
|---|---|---|---|
| `initializeCompoundRecipes` | 53-75 | 9 | wand constructs (a plain `List`, not a recipe) |
| `initializeAlchemyRecipes` | 77-166 | 52 | crucible |
| `initializeArcaneRecipes` | 168-289 | 139 | arcane workbench |
| `initializeInfusionEnchantmentRecipes` | 291-316 | 24 | infusion (enchantment) |
| `initializeInfusionRecipes` | 318-391 | 63 | infusion (stack) |
| `initializeNormalRecipes` | 393-483 | 21 | ordinary crafting table |

The count of 308 is machine-derived: the key set is the union of the 279 `put` call sites with the
five loop-generated families expanded (`BalancedShard_0..5`, `Banner_0..15`, `PrimalArrow_0..5`,
`Clusters0..6`, `JarLabel0..47`).

The research book of the original names only 272 of the 308 keys. The other 36 are the 16 banners
and the 6 primal arrows (the original shows them as an ARRAY of recipes, `ConfigResearch:194-199`
and `:216-220`), the 6 balanced shards, the 7 crystal clusters, `RunicGirdleKinetic_2` and the 48
`JarLabel<aspect>` recipes.

## 2. The verified coverage table

`port status` is one of:

* **native** - a native 1.21.1 recipe file carries the original key and the port's
  `ResearchRecipeCoverage` names it;
* **native (gap closed)** - a native 1.21.1 recipe file carried the original key but
  `ResearchRecipeCoverage` did not name it; this wave mapped it;
* **native (duplicate)** - the original registration can never fire (see section 4), the port
  carries the identical recipe twice;
* **construct** - the original stores the key as a plain `List`, never as an `IRecipe`; it is a
  wand construct blueprint, not a recipe;
* **deferred** - the original key has no native recipe, with the reason in the last column.

| # | original key | oracle line | kind | port status | native recipe id | notes |
|---|---|---|---|---|---|---|
| 1 | `Thaumonomicon` | 55 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 2 | `ArcTable` | 57 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 3 | `ResTable` | 58 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 4 | `Crucible` | 59 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 5 | `InfernalFurnace` | 61 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 6 | `InfusionAltar` | 65 | wand construct | construct | `thaumcraft:arcane/infusion_matrix` | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 7 | `NodeJar` | 67 | wand construct | construct | - | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 8 | `Thaumatorium` | 69 | wand construct | construct | `thaumcraft:arcane/alchemical_construct` | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 9 | `AdvAlchemyFurnace` | 72 | wand construct | construct | `thaumcraft:arcane/advanced_alchemical_construct` | the original stores this key as a plain List, not as a recipe (ConfigRecipes:53-75); it is a wand construct blueprint |
| 10 | `BalancedShard_0` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_air` |  |
| 11 | `BalancedShard_1` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_fire` |  |
| 12 | `BalancedShard_2` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_water` |  |
| 13 | `BalancedShard_3` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_earth` |  |
| 14 | `BalancedShard_4` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_order` |  |
| 15 | `BalancedShard_5` | 89 | crucible | native | `thaumcraft:crucible/balanced_shard_entropy` |  |
| 16 | `Alumentum` | 92 | crucible | native | `thaumcraft:crucible/alumentum` |  |
| 17 | `Nitor` | 93 | crucible | native | `thaumcraft:crucible/nitor` |  |
| 18 | `Thaumium` | 94 | crucible | native | `thaumcraft:crucible/thaumium` |  |
| 19 | `VoidMetal` | 95 | crucible | native | `thaumcraft:crucible/void_ingot` |  |
| 20 | `VoidSeed` | 96 | crucible | native | `thaumcraft:crucible/void_seed` |  |
| 21 | `Tallow` | 97 | crucible | native | `thaumcraft:crucible/tallow` |  |
| 22 | `AltGunpowder` | 98 | crucible | native | `thaumcraft:crucible/alt_gunpowder` |  |
| 23 | `AltSlime` | 99 | crucible | native | `thaumcraft:crucible/alt_slime` |  |
| 24 | `AltClay` | 100 | crucible | native | `thaumcraft:crucible/alt_clay` |  |
| 25 | `AltGlowstone` | 101 | crucible | native | `thaumcraft:crucible/alt_glowstone` |  |
| 26 | `AltInk` | 102 | crucible | native | `thaumcraft:crucible/alt_ink` |  |
| 27 | `AltWeb` | 103 | crucible | native | `thaumcraft:crucible/alt_web` |  |
| 28 | `AltMossyCobble` | 104 | crucible | native | `thaumcraft:crucible/alt_mossy_cobble` |  |
| 29 | `AltIce` | 105 | crucible | native | `thaumcraft:crucible/alt_ice` |  |
| 30 | `AltCrackedBrick` | 106 | crucible | native | `thaumcraft:crucible/alt_cracked_brick` |  |
| 31 | `AltBonemeal` | 107 | crucible | native | `thaumcraft:crucible/alt_bonemeal` |  |
| 32 | `PureIron` | 108 | crucible | native | `thaumcraft:crucible/pure_iron` |  |
| 33 | `PureGold` | 109 | crucible | native | `thaumcraft:crucible/pure_gold` |  |
| 34 | `PureCopper` | 111 | crucible | native | `thaumcraft:crucible/pure_copper` |  |
| 35 | `PureTin` | 115 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundTinIngot, and 1.21.1 has no tin at all |
| 36 | `PureSilver` | 119 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundSilverIngot, and 1.21.1 has no silver at all |
| 37 | `PureLead` | 123 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundLeadIngot, and 1.21.1 has no lead at all |
| 38 | `TransIron` | 126 | crucible | native | `thaumcraft:crucible/trans_iron` |  |
| 39 | `TransGold` | 127 | crucible | native | `thaumcraft:crucible/trans_gold` |  |
| 40 | `TransCopper` | 129 | crucible | native | `thaumcraft:crucible/trans_copper` |  |
| 41 | `TransTin` | 133 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundTinIngot, and 1.21.1 has no tin at all |
| 42 | `TransSilver` | 137 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundSilverIngot, and 1.21.1 has no silver at all |
| 43 | `TransLead` | 141 | crucible | deferred | - | third-party ore: the original registers this crucible recipe only when Config.foundLeadIngot, and 1.21.1 has no lead at all |
| 44 | `EtherealBloom` | 144 | crucible | native | `thaumcraft:crucible/ethereal_bloom` |  |
| 45 | `LiquidDeath` | 145 | crucible | native | `thaumcraft:crucible/bucket_of_death` |  |
| 46 | `BottleTaint` | 148 | crucible | native | `thaumcraft:crucible/bottle_taint` |  |
| 47 | `GolemStraw` | 149 | crucible | native | `thaumcraft:crucible/golem_straw` |  |
| 48 | `GolemWood` | 150 | crucible | native | `thaumcraft:crucible/golem_wood` |  |
| 49 | `GolemTallow` | 151 | crucible | native | `thaumcraft:crucible/golem_tallow` |  |
| 50 | `GolemClay` | 152 | crucible | native | `thaumcraft:crucible/golem_clay` |  |
| 51 | `GolemFlesh` | 153 | crucible | native | `thaumcraft:crucible/golem_flesh` |  |
| 52 | `GolemStone` | 154 | crucible | native | `thaumcraft:crucible/golem_stone` |  |
| 53 | `GolemIron` | 155 | crucible | native | `thaumcraft:crucible/golem_iron` |  |
| 54 | `GolemThaumium` | 156 | crucible | native | `thaumcraft:crucible/golem_thaumium` |  |
| 55 | `CoreGather` | 157 | crucible | native | `thaumcraft:crucible/golem_core_gather` |  |
| 56 | `CoreFill` | 158 | crucible | native | `thaumcraft:crucible/golem_core_fill` |  |
| 57 | `CoreEmpty` | 159 | crucible | native | `thaumcraft:crucible/golem_core_empty` |  |
| 58 | `CoreHarvest` | 160 | crucible | native | `thaumcraft:crucible/golem_core_harvest` |  |
| 59 | `CoreGuard` | 161 | crucible | native | `thaumcraft:crucible/golem_core_guard` |  |
| 60 | `CoreButcher` | 162 | crucible | native | `thaumcraft:crucible/golem_core_butcher` |  |
| 61 | `CoreLiquid` | 163 | crucible | native | `thaumcraft:crucible/golem_core_liquid` |  |
| 62 | `BathSalts` | 164 | crucible | native | `thaumcraft:crucible/bath_salts` |  |
| 63 | `SaneSoap` | 165 | crucible | native | `thaumcraft:crucible/sanity_soap` |  |
| 64 | `Banner_0` | 173 | arcane | native | `thaumcraft:arcane/banner_white` |  |
| 65 | `Banner_1` | 173 | arcane | native | `thaumcraft:arcane/banner_orange` |  |
| 66 | `Banner_10` | 173 | arcane | native | `thaumcraft:arcane/banner_purple` |  |
| 67 | `Banner_11` | 173 | arcane | native | `thaumcraft:arcane/banner_blue` |  |
| 68 | `Banner_12` | 173 | arcane | native | `thaumcraft:arcane/banner_brown` |  |
| 69 | `Banner_13` | 173 | arcane | native | `thaumcraft:arcane/banner_green` |  |
| 70 | `Banner_14` | 173 | arcane | native | `thaumcraft:arcane/banner_red` |  |
| 71 | `Banner_15` | 173 | arcane | native | `thaumcraft:arcane/banner_black` |  |
| 72 | `Banner_2` | 173 | arcane | native | `thaumcraft:arcane/banner_magenta` |  |
| 73 | `Banner_3` | 173 | arcane | native | `thaumcraft:arcane/banner_light_blue` |  |
| 74 | `Banner_4` | 173 | arcane | native | `thaumcraft:arcane/banner_yellow` |  |
| 75 | `Banner_5` | 173 | arcane | native | `thaumcraft:arcane/banner_lime` |  |
| 76 | `Banner_6` | 173 | arcane | native | `thaumcraft:arcane/banner_pink` |  |
| 77 | `Banner_7` | 173 | arcane | native | `thaumcraft:arcane/banner_gray` |  |
| 78 | `Banner_8` | 173 | arcane | native | `thaumcraft:arcane/banner_light_gray` |  |
| 79 | `Banner_9` | 173 | arcane | native | `thaumcraft:arcane/banner_cyan` |  |
| 80 | `PrimalCharm` | 176 | arcane | native | `thaumcraft:arcane/primal_charm` |  |
| 81 | `ArcaneDoor` | 177 | arcane | native | `thaumcraft:arcane/arcane_door` |  |
| 82 | `WardedGlass` | 178 | arcane | native | `thaumcraft:arcane/warded_glass` |  |
| 83 | `IronKey` | 179 | arcane | native | `thaumcraft:arcane/iron_key` |  |
| 84 | `FluxScrubber` | 180 | arcane | native | `thaumcraft:arcane/flux_scrubber` |  |
| 85 | `GoldKey` | 182 | arcane | native | `thaumcraft:arcane/gold_key` |  |
| 86 | `ArcanePressurePlate` | 183 | arcane | native | `thaumcraft:arcane/arcane_pressure_plate` |  |
| 87 | `NodeStabilizer` | 186 | arcane | native | `thaumcraft:arcane/node_stabilizer` |  |
| 88 | `NodeTransducer` | 187 | arcane | native | `thaumcraft:arcane/node_transducer` |  |
| 89 | `NodeRelay` | 188 | arcane | native | `thaumcraft:arcane/vis_relay` |  |
| 90 | `NodeChargeRelay` | 189 | arcane | native | `thaumcraft:arcane/vis_charge_relay` |  |
| 91 | `FocalManipulator` | 190 | arcane | native | `thaumcraft:arcane/focal_manipulator` |  |
| 92 | `GolemFetter` | 191 | arcane | native | `thaumcraft:arcane/golem_fetter` |  |
| 93 | `ArcaneStone1` | 192 | arcane | native | `thaumcraft:arcane/arcane_stone` |  |
| 94 | `ArcaneStone2` | 193 | shaped | native | `thaumcraft:arcane_stone_bricks` |  |
| 95 | `ArcaneStone3` | 194 | shaped | native | `thaumcraft:arcane_stone_brick_stairs` |  |
| 96 | `ArcaneStone4` | 195 | shaped | native | `thaumcraft:arcane_stone_brick_slab` |  |
| 97 | `PaveTravel` | 196 | arcane | native | `thaumcraft:arcane/paving_stone_travel` |  |
| 98 | `ArcaneLamp` | 197 | arcane | native | `thaumcraft:arcane/arcane_lamp` |  |
| 99 | `ArcaneSpa` | 198 | arcane | native | `thaumcraft:arcane/arcane_spa` |  |
| 100 | `PaveWard` | 199 | arcane | native | `thaumcraft:arcane/paving_stone_warding` |  |
| 101 | `Levitator` | 200 | arcane | native | `thaumcraft:arcane/levitator` |  |
| 102 | `ArcaneEar` | 201 | arcane | native | `thaumcraft:arcane/arcane_ear` |  |
| 103 | `MirrorGlass` | 202 | arcane | native | `thaumcraft:arcane/mirrored_glass` |  |
| 104 | `BoneBow` | 203 | arcane | native | `thaumcraft:arcane/bone_bow` |  |
| 105 | `PrimalArrow_0` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_air` |  |
| 106 | `PrimalArrow_1` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_fire` |  |
| 107 | `PrimalArrow_2` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_water` |  |
| 108 | `PrimalArrow_3` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_earth` |  |
| 109 | `PrimalArrow_4` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_order` |  |
| 110 | `PrimalArrow_5` | 210 | arcane | native | `thaumcraft:arcane/primal_arrow_entropy` |  |
| 111 | `InfusionMatrix` | 213 | arcane | native | `thaumcraft:arcane/infusion_matrix` |  |
| 112 | `ArcanePedestal` | 214 | arcane | native | `thaumcraft:arcane/arcane_pedestal` |  |
| 113 | `WardedJar` | 215 | arcane | native | `thaumcraft:arcane/warded_jar` |  |
| 114 | `JarVoid` | 216 | arcane | native | `thaumcraft:arcane/jar_void` |  |
| 115 | `WandCapGold` | 221 | arcane | native | `thaumcraft:arcane/wand_cap_gold` |  |
| 116 | `WandCapCopper` | 223 | arcane | native | `thaumcraft:arcane/wand_cap_copper` |  |
| 117 | `WandCapSilverInert` | 227 | arcane | deferred | - | third-party ore: the original registers this arcane recipe only when Config.foundSilverIngot, and 1.21.1 has no silver wand cap item |
| 118 | `WandCapThaumiumInert` | 230 | arcane | native | `thaumcraft:arcane/wand_cap_thaumium_inert` |  |
| 119 | `WandCapVoidInert` | 231 | arcane | native | `thaumcraft:arcane/wand_cap_void_inert` |  |
| 120 | `WandRodGreatwood` | 232 | arcane | native | `thaumcraft:arcane/wand_rod_greatwood` |  |
| 121 | `WandRodGreatwoodStaff` | 233 | arcane | native | `thaumcraft:arcane/staff_rod_greatwood` |  |
| 122 | `WandRodObsidianStaff` | 234 | arcane | native | `thaumcraft:arcane/staff_rod_obsidian` |  |
| 123 | `WandRodSilverwoodStaff` | 235 | arcane | native | `thaumcraft:arcane/staff_rod_silverwood` |  |
| 124 | `WandRodIceStaff` | 236 | arcane | native | `thaumcraft:arcane/staff_rod_ice` |  |
| 125 | `WandRodQuartzStaff` | 237 | arcane | native | `thaumcraft:arcane/staff_rod_quartz` |  |
| 126 | `WandRodReedStaff` | 238 | arcane | native | `thaumcraft:arcane/staff_rod_reed` |  |
| 127 | `WandRodBlazeStaff` | 239 | arcane | native | `thaumcraft:arcane/staff_rod_blaze` |  |
| 128 | `WandRodBoneStaff` | 240 | arcane | native | `thaumcraft:arcane/staff_rod_bone` |  |
| 129 | `FocusFire` | 241 | arcane | native | `thaumcraft:arcane/focus_fire` |  |
| 130 | `FocusFrost` | 242 | arcane | native | `thaumcraft:arcane/focus_frost` |  |
| 131 | `FocusShock` | 243 | arcane | native | `thaumcraft:arcane/focus_shock` |  |
| 132 | `FocusTrade` | 244 | arcane | native | `thaumcraft:arcane/focus_trade` |  |
| 133 | `FocusExcavation` | 245 | arcane | native | `thaumcraft:arcane/focus_excavation` |  |
| 134 | `FocusPrimal` | 246 | arcane | native | `thaumcraft:arcane/focus_primal` |  |
| 135 | `FocusPouch` | 247 | arcane | native | `thaumcraft:arcane/focus_pouch` |  |
| 136 | `Deconstructor` | 248 | arcane | native | `thaumcraft:arcane/deconstruction_table` |  |
| 137 | `ArcaneBoreBase` | 249 | arcane | native | `thaumcraft:arcane/arcane_bore_base` |  |
| 138 | `EnchantedFabric` | 250 | arcane | native | `thaumcraft:arcane/enchanted_fabric` |  |
| 139 | `RobeChest` | 251 | arcane | native | `thaumcraft:arcane/robe_chestplate` |  |
| 140 | `RobeLegs` | 252 | arcane | native | `thaumcraft:arcane/robe_leggings` |  |
| 141 | `RobeBoots` | 253 | arcane | native | `thaumcraft:arcane/robe_boots` |  |
| 142 | `Goggles` | 255 | arcane | native | `thaumcraft:arcane/goggles` |  |
| 143 | `HungryChest` | 256 | arcane | native | `thaumcraft:arcane/hungry_chest` |  |
| 144 | `GolemBell` | 257 | arcane | native | `thaumcraft:arcane/golem_bell` |  |
| 145 | `CoreBlank` | 258 | arcane | native | `thaumcraft:arcane/golem_core_blank` |  |
| 146 | `UpgradeAir` | 259 | arcane | native | `thaumcraft:arcane/golem_upgrade_air` |  |
| 147 | `UpgradeEarth` | 260 | arcane | native | `thaumcraft:arcane/golem_upgrade_earth` |  |
| 148 | `UpgradeFire` | 261 | arcane | native | `thaumcraft:arcane/golem_upgrade_fire` |  |
| 149 | `UpgradeWater` | 262 | arcane | native | `thaumcraft:arcane/golem_upgrade_water` |  |
| 150 | `UpgradeOrder` | 263 | arcane | native | `thaumcraft:arcane/golem_upgrade_order` |  |
| 151 | `UpgradeEntropy` | 264 | arcane | native | `thaumcraft:arcane/golem_upgrade_entropy` |  |
| 152 | `TinyHat` | 265 | arcane | native | `thaumcraft:arcane/golem_decoration_tophat` |  |
| 153 | `TinyFez` | 266 | arcane | native | `thaumcraft:arcane/golem_decoration_fez` |  |
| 154 | `TinyBowtie` | 267 | arcane | native | `thaumcraft:arcane/golem_decoration_bowtie` |  |
| 155 | `TinyGlasses` | 268 | arcane | native | `thaumcraft:arcane/golem_decoration_glasses` |  |
| 156 | `TinyDart` | 269 | arcane | native | `thaumcraft:arcane/golem_decoration_dart` |  |
| 157 | `TinyVisor` | 270 | arcane | native | `thaumcraft:arcane/golem_decoration_visor` |  |
| 158 | `TinyArmor` | 271 | arcane | native | `thaumcraft:arcane/golem_decoration_armor` |  |
| 159 | `TinyHammer` | 272 | arcane | native | `thaumcraft:arcane/golem_decoration_mace` |  |
| 160 | `Filter` | 273 | arcane | native | `thaumcraft:arcane/vis_filter` |  |
| 161 | `AlchemyFurnace` | 274 | arcane | native | `thaumcraft:arcane/alchemy_furnace` |  |
| 162 | `Alembic` | 275 | arcane | native | `thaumcraft:arcane/alembic` |  |
| 163 | `Bellows` | 276 | arcane | native | `thaumcraft:arcane/bellows` |  |
| 164 | `Tube` | 277 | arcane | native | `thaumcraft:arcane/essentia_tube` |  |
| 165 | `Resonator` | 278 | arcane | native | `thaumcraft:arcane/resonator` |  |
| 166 | `TubeValve` | 279 | arcane | native | `thaumcraft:arcane/essentia_valve` |  |
| 167 | `TubeFilter` | 280 | arcane | native | `thaumcraft:arcane/essentia_filter` |  |
| 168 | `TubeRestrict` | 281 | arcane | native | `thaumcraft:arcane/essentia_restrict` |  |
| 169 | `TubeOneway` | 282 | arcane | native | `thaumcraft:arcane/essentia_oneway` |  |
| 170 | `TubeBuffer` | 283 | arcane | native | `thaumcraft:arcane/essentia_buffer` |  |
| 171 | `AlchemicalConstruct` | 284 | arcane | native | `thaumcraft:arcane/alchemical_construct` |  |
| 172 | `AdvAlchemyConstruct` | 285 | arcane | native | `thaumcraft:arcane/advanced_alchemical_construct` |  |
| 173 | `Centrifuge` | 286 | arcane | native | `thaumcraft:arcane/centrifuge` |  |
| 174 | `EssentiaCrystalizer` | 287 | arcane | native | `thaumcraft:arcane/essentia_crystallizer` |  |
| 175 | `MnemonicMatrix` | 288 | arcane | native | `thaumcraft:arcane/mnemonic_matrix` |  |
| 176 | `InfEnchRepair` | 292 | infusion-ench | native | `thaumcraft:infusion_enchantment/repair` |  |
| 177 | `InfEnchHaste` | 293 | infusion-ench | native | `thaumcraft:infusion_enchantment/haste` |  |
| 178 | `InfEnch0` | 294 | infusion-ench | native | `thaumcraft:infusion_enchantment/protection` |  |
| 179 | `InfEnch1` | 295 | infusion-ench | native | `thaumcraft:infusion_enchantment/fire_protection` |  |
| 180 | `InfEnch2` | 296 | infusion-ench | native | `thaumcraft:infusion_enchantment/blast_protection` |  |
| 181 | `InfEnch3` | 297 | infusion-ench | native | `thaumcraft:infusion_enchantment/projectile_protection` |  |
| 182 | `InfEnch4` | 298 | infusion-ench | native | `thaumcraft:infusion_enchantment/feather_falling` |  |
| 183 | `InfEnch5` | 299 | infusion-ench | native | `thaumcraft:infusion_enchantment/respiration` |  |
| 184 | `InfEnch6` | 300 | infusion-ench | native | `thaumcraft:infusion_enchantment/aqua_affinity` |  |
| 185 | `InfEnch7` | 301 | infusion-ench | native | `thaumcraft:infusion_enchantment/thorns` |  |
| 186 | `InfEnch8` | 302 | infusion-ench | native | `thaumcraft:infusion_enchantment/sharpness` |  |
| 187 | `InfEnch9` | 303 | infusion-ench | native | `thaumcraft:infusion_enchantment/smite` |  |
| 188 | `InfEnch10` | 304 | infusion-ench | native | `thaumcraft:infusion_enchantment/bane_of_arthropods` |  |
| 189 | `InfEnch11` | 305 | infusion-ench | native | `thaumcraft:infusion_enchantment/knockback` |  |
| 190 | `InfEnch12` | 306 | infusion-ench | native | `thaumcraft:infusion_enchantment/fire_aspect` |  |
| 191 | `InfEnch13` | 307 | infusion-ench | native | `thaumcraft:infusion_enchantment/looting` |  |
| 192 | `InfEnch14` | 308 | infusion-ench | native | `thaumcraft:infusion_enchantment/efficiency` |  |
| 193 | `InfEnch15` | 309 | infusion-ench | native | `thaumcraft:infusion_enchantment/silk_touch` |  |
| 194 | `InfEnch16` | 310 | infusion-ench | native | `thaumcraft:infusion_enchantment/unbreaking` |  |
| 195 | `InfEnch17` | 311 | infusion-ench | native | `thaumcraft:infusion_enchantment/fortune` |  |
| 196 | `InfEnch18` | 312 | infusion-ench | native | `thaumcraft:infusion_enchantment/power` |  |
| 197 | `InfEnch19` | 313 | infusion-ench | native | `thaumcraft:infusion_enchantment/punch` |  |
| 198 | `InfEnch20` | 314 | infusion-ench | native | `thaumcraft:infusion_enchantment/flame` |  |
| 199 | `InfEnch21` | 315 | infusion-ench | native | `thaumcraft:infusion_enchantment/infinity` |  |
| 200 | `WandCapSilver` | 320 | infusion | deferred | - | third-party ore: the original registers this infusion recipe only when Config.foundSilverIngot, and 1.21.1 has no silver wand cap item |
| 201 | `WandCapThaumium` | 323 | infusion | native | `thaumcraft:infusion/wand_cap_thaumium` |  |
| 202 | `WandCapVoid` | 324 | infusion | native | `thaumcraft:infusion/wand_cap_void` |  |
| 203 | `WandRodObsidian` | 325 | infusion | native | `thaumcraft:infusion/wand_rod_obsidian` |  |
| 204 | `WandRodIce` | 326 | infusion | native | `thaumcraft:infusion/wand_rod_ice` |  |
| 205 | `WandRodQuartz` | 327 | infusion | native | `thaumcraft:infusion/wand_rod_quartz` |  |
| 206 | `WandRodReed` | 328 | infusion | native | `thaumcraft:infusion/wand_rod_reed` |  |
| 207 | `WandRodBlaze` | 329 | infusion | native | `thaumcraft:infusion/wand_rod_blaze` |  |
| 208 | `WandRodBone` | 330 | infusion | native | `thaumcraft:infusion/wand_rod_bone` |  |
| 209 | `WandRodSilverwood` | 331 | infusion | native | `thaumcraft:infusion/wand_rod_silverwood` |  |
| 210 | `WandRodPrimalStaff` | 332 | infusion | native | `thaumcraft:infusion/staff_rod_primal` |  |
| 211 | `FocusHellbat` | 333 | infusion | native | `thaumcraft:infusion/focus_hellbat` |  |
| 212 | `FocusPortableHole` | 334 | infusion | native | `thaumcraft:infusion/focus_portable_hole` |  |
| 213 | `FocusWarding` | 335 | infusion | native | `thaumcraft:infusion/focus_warding` |  |
| 214 | `WandPed` | 336 | infusion | native | `thaumcraft:infusion/wand_pedestal` |  |
| 215 | `WandPedFocus` | 337 | infusion | native | `thaumcraft:infusion/wand_pedestal_focus` |  |
| 216 | `NodeStabilizerAdv` | 338 | infusion | native | `thaumcraft:infusion/advanced_node_stabilizer` |  |
| 217 | `JarBrain` | 339 | infusion | native | `thaumcraft:infusion/jar_brain` |  |
| 218 | `AdvancedGolem` | 340 | infusion | native | `thaumcraft:infusion/golem_advanced` |  |
| 219 | `HoverHarness` | 341 | infusion | native | `thaumcraft:infusion/hover_harness` |  |
| 220 | `HoverGirdle` | 342 | infusion | native | `thaumcraft:infusion/hover_girdle` |  |
| 221 | `VisAmulet` | 343 | infusion | native | `thaumcraft:infusion/vis_amulet` |  |
| 222 | `RunicAmulet` | 344 | infusion | native | `thaumcraft:infusion/runic_amulet` |  |
| 223 | `RunicAmuletEmergency` | 345 | infusion | native | `thaumcraft:infusion/runic_amulet_emergency` |  |
| 224 | `RunicRing` | 346 | infusion | native | `thaumcraft:infusion/runic_ring` |  |
| 225 | `RunicRingCharged` | 347 | infusion | native | `thaumcraft:infusion/runic_ring_charged` |  |
| 226 | `RunicRingHealing` | 348 | infusion | native | `thaumcraft:infusion/runic_ring_healing` |  |
| 227 | `RunicGirdle` | 349 | infusion | native | `thaumcraft:infusion/runic_girdle` |  |
| 228 | `RunicGirdleKinetic` | 350 | infusion | native | `thaumcraft:infusion/runic_girdle_kinetic` |  |
| 229 | `RunicGirdleKinetic_2` | 351 | infusion | native (duplicate) | `thaumcraft:infusion/runic_girdle_kinetic_2` | the original registration is dead (a potion metadata no item of 1.7.10 can carry); the port carries the identical recipe twice |
| 230 | `Mirror` | 355 | infusion | native | `thaumcraft:infusion/mirror` |  |
| 231 | `MirrorHand` | 356 | infusion | native | `thaumcraft:infusion/hand_mirror` |  |
| 232 | `MirrorEssentia` | 357 | infusion | deferred | - | the original result is blockMirror metadata 6, the essentia mirror; the native mirror block keeps it as the essentia block state and has no separate essentia mirror item, and a 1.21.1 recipe result cannot carry a block state |
| 233 | `ElementalAxe` | 360 | infusion | native | `thaumcraft:infusion/elemental_axe` |  |
| 234 | `ElementalPick` | 361 | infusion | native | `thaumcraft:infusion/elemental_pickaxe` |  |
| 235 | `ElementalSword` | 362 | infusion | native | `thaumcraft:infusion/elemental_sword` |  |
| 236 | `ElementalShovel` | 363 | infusion | native | `thaumcraft:infusion/elemental_shovel` |  |
| 237 | `ElementalHoe` | 364 | infusion | native | `thaumcraft:infusion/elemental_hoe` |  |
| 238 | `BootsTraveller` | 365 | infusion | native | `thaumcraft:infusion/boots_traveller` |  |
| 239 | `CoreAlchemy` | 366 | infusion | native | `thaumcraft:infusion/golem_core_alchemy` |  |
| 240 | `CoreSorting` | 367 | infusion | native | `thaumcraft:infusion/golem_core_sorting` |  |
| 241 | `CoreLumber` | 368 | infusion | native | `thaumcraft:infusion/golem_core_lumber` |  |
| 242 | `CoreFishing` | 369 | infusion | native | `thaumcraft:infusion/golem_core_fishing` |  |
| 243 | `CoreUse` | 370 | infusion | native | `thaumcraft:infusion/golem_core_use` |  |
| 244 | `ArcaneBore` | 371 | infusion | native | `thaumcraft:infusion/arcane_bore` |  |
| 245 | `TravelTrunk` | 372 | infusion | native | `thaumcraft:infusion/traveling_trunk` |  |
| 246 | `LampGrowth` | 373 | infusion | native | `thaumcraft:infusion/growth_lamp` |  |
| 247 | `LampFertility` | 374 | infusion | native | `thaumcraft:infusion/fertility_lamp` |  |
| 248 | `ThaumiumFortressHelm` | 375 | infusion | native | `thaumcraft:infusion/fortress_helm` |  |
| 249 | `ThaumiumFortressChest` | 376 | infusion | native | `thaumcraft:infusion/fortress_chest` |  |
| 250 | `ThaumiumFortressLegs` | 377 | infusion | native | `thaumcraft:infusion/fortress_legs` |  |
| 251 | `VoidRobeHelm` | 378 | infusion | native | `thaumcraft:infusion/void_robe_helm` |  |
| 252 | `VoidRobeChest` | 379 | infusion | native | `thaumcraft:infusion/void_robe_chest` |  |
| 253 | `VoidRobeLegs` | 380 | infusion | native | `thaumcraft:infusion/void_robe_legs` |  |
| 254 | `HelmGoggles` | 382 | infusion | native | `thaumcraft:infusion/fortress_goggles` |  |
| 255 | `MaskGrinningDevil` | 383 | infusion | native | `thaumcraft:infusion/fortress_mask_devil` |  |
| 256 | `MaskAngryGhost` | 384 | infusion | native | `thaumcraft:infusion/fortress_mask_ghost` |  |
| 257 | `MaskSippingFiend` | 385 | infusion | native | `thaumcraft:infusion/fortress_mask_fiend` |  |
| 258 | `SanityCheck` | 386 | infusion | native | `thaumcraft:infusion/sanity_checker` |  |
| 259 | `EssentiaReservoir` | 387 | infusion | native | `thaumcraft:infusion/essentia_reservoir` |  |
| 260 | `SinStone` | 388 | infusion | native | `thaumcraft:infusion/compass_stone` |  |
| 261 | `PrimalCrusher` | 389 | infusion | native | `thaumcraft:infusion/primal_crusher` |  |
| 262 | `EldritchEye` | 390 | infusion | native | `thaumcraft:infusion/eldritch_eye` |  |
| 263 | `MundaneAmulet` | 402 | shaped | native | `thaumcraft:mundane_amulet` |  |
| 264 | `MundaneRing` | 403 | shaped | native | `thaumcraft:mundane_ring` |  |
| 265 | `MundaneBelt` | 404 | shaped | native | `thaumcraft:mundane_belt` |  |
| 266 | `JarLabel` | 411 | shapeless | native | `thaumcraft:jar_label` |  |
| 267 | `JarLabelNull` | 425 | shapeless | native | `thaumcraft:jar_label_clear` |  |
| 268 | `WandBasic` | 426 | shaped | native | `thaumcraft:wand_wood_iron` |  |
| 269 | `WandCapIron` | 427 | shaped | native | `thaumcraft:wand_cap_iron` |  |
| 270 | `KnowFrag` | 428 | shaped | native | `thaumcraft:unknown_research_note` |  |
| 271 | `PlankGreatwood` | 429 | shaped | native | `thaumcraft:greatwood_planks` |  |
| 272 | `PlankSilverwood` | 430 | shaped | native | `thaumcraft:silverwood_planks` |  |
| 273 | `BlockFlesh` | 435 | shaped | native | `thaumcraft:flesh_block` |  |
| 274 | `BlockThaumium` | 436 | shaped | native | `thaumcraft:thaumium_block` |  |
| 275 | `BlockTallow` | 438 | shaped | native | `thaumcraft:tallow_block` |  |
| 276 | `Clusters0` | 442 | shapeless | native | `thaumcraft:clusters/air` |  |
| 277 | `Clusters1` | 442 | shapeless | native | `thaumcraft:clusters/fire` |  |
| 278 | `Clusters2` | 442 | shapeless | native | `thaumcraft:clusters/water` |  |
| 279 | `Clusters3` | 442 | shapeless | native | `thaumcraft:clusters/earth` |  |
| 280 | `Clusters4` | 442 | shapeless | native | `thaumcraft:clusters/order` |  |
| 281 | `Clusters5` | 442 | shapeless | native | `thaumcraft:clusters/entropy` |  |
| 282 | `Clusters6` | 445 | shapeless | native | `thaumcraft:clusters/mixed` |  |
| 283 | `Grate` | 451 | shaped | native | `thaumcraft:item_grate` |  |
| 284 | `Phial` | 452 | shaped | native | `thaumcraft:phial_empty` |  |
| 285 | `Table` | 453 | shaped | native | `thaumcraft:table` |  |
| 286 | `Scribe1` | 454 | shapeless | native | `thaumcraft:scribing_tools_phial` |  |
| 287 | `Scribe2` | 455 | shapeless | native | `thaumcraft:scribing_tools_bottle` |  |
| 288 | `Scribe3` | 456 | shapeless | native | `thaumcraft:scribing_tools_refill` |  |
| 289 | `Thaumometer` | 457 | shaped | native | `thaumcraft:thaumometer` |  |
| 290 | `ThaumiumHelm` | 458 | shaped | native | `thaumcraft:thaumium_helmet` |  |
| 291 | `ThaumiumChest` | 459 | shaped | native | `thaumcraft:thaumium_chestplate` |  |
| 292 | `ThaumiumLegs` | 460 | shaped | native | `thaumcraft:thaumium_leggings` |  |
| 293 | `ThaumiumBoots` | 461 | shaped | native | `thaumcraft:thaumium_boots` |  |
| 294 | `ThaumiumShovel` | 462 | shaped | native | `thaumcraft:thaumium_shovel` |  |
| 295 | `ThaumiumPick` | 463 | shaped | native | `thaumcraft:thaumium_pickaxe` |  |
| 296 | `ThaumiumAxe` | 464 | shaped | native | `thaumcraft:thaumium_axe` |  |
| 297 | `ThaumiumHoe` | 465 | shaped | native | `thaumcraft:thaumium_hoe` |  |
| 298 | `ThaumiumSword` | 466 | shaped | native | `thaumcraft:thaumium_sword` |  |
| 299 | `VoidHelm` | 467 | shaped | native | `thaumcraft:void_helmet` |  |
| 300 | `VoidChest` | 468 | shaped | native | `thaumcraft:void_chestplate` |  |
| 301 | `VoidLegs` | 469 | shaped | native | `thaumcraft:void_leggings` |  |
| 302 | `VoidBoots` | 470 | shaped | native | `thaumcraft:void_boots` |  |
| 303 | `VoidShovel` | 471 | shaped | native | `thaumcraft:void_shovel` |  |
| 304 | `VoidPick` | 472 | shaped | native | `thaumcraft:void_pickaxe` |  |
| 305 | `VoidAxe` | 473 | shaped | native | `thaumcraft:void_axe` |  |
| 306 | `VoidHoe` | 474 | shaped | native | `thaumcraft:void_hoe` |  |
| 307 | `VoidSword` | 475 | shaped | native | `thaumcraft:void_sword` |  |
| 308 | `TallowCandle` | 476 | shaped | native | `thaumcraft:tallow_candle` |  |

Counts: {'construct': 9, 'native': 289, 'deferred': 9, 'native (duplicate)': 1} total 308 (the 14 keys of section 6 are counted as native now, they were counted as
"native (gap closed)" before this wave mapped them).

### 2.1 The totals

| measure | value |
|---|---|
| original recipe keys | 308 |
| covered by a native 1.21.1 recipe | 290 (289 named as native + 1 duplicate) |
| original wand constructs (not recipes in the original either) | 9 |
| deferred | 9 |
| original keys the research book names | 272 |
| native recipe files packaged under `data/thaumcraft/recipe/` | 344 |

The brief of this wave quoted "272 covered and 16 deferred". Both numbers are stale: the port
resolved 279 keys and deferred 9 before this wave, and the wave mapped 14 more coverage entries
and fixed 10 real content defects. The verified state is the table above.

## 3. How the coverage was verified

Every original key was parsed out of the oracle with a Java-expression parser (top level comma
splitting, `new ItemStack(...)` / `new Object[]{...}` / `Arrays.asList(...)` structure) and compared
against the native recipe JSON of the port. The following fields were compared field by field for
all 290 covered keys:

| field | oracle source | native source |
|---|---|---|
| arcane vis cost | the `AspectList` of the `addArcaneCraftingRecipe` call | the `vis` map |
| crucible aspect cost | the `AspectList` of the `addCrucibleRecipe` call | the `aspects` map |
| infusion aspect cost | the `AspectList` of the `addInfusionCraftingRecipe` call | the `aspects` map |
| infusion instability | the `int` argument of `addInfusionCraftingRecipe` | `instability` |
| result count | the `count` of the result `ItemStack` | `result.count` |
| pattern | the `String...` rows of the call | `pattern` |
| ingredient symbols | the `'X', ingredient` pairs of the call | the `key` map |
| ingredient count | the length of the ingredient array | the length of `ingredients` / `components` |
| ingredient identity | the item of the call | the item or the tag of the native ingredient |

The wand cap and wand rod craft costs of the oracle are dynamic
(`WandCap.caps.get("gold").getCraftCost()`); they were resolved from the original
`ConfigItems:287-309` (cap: iron 1, gold 3, thaumium 6, void 9, copper 2, silver 4; rod: greatwood
3, obsidian 6, blaze 6, ice 6, quartz 6, bone 6, reed 6, silverwood 9; staff rod: greatwood 8,
obsidian 14, blaze 14, ice 14, quartz 14, bone 14, reed 14, silverwood 24, primal 32).

**811 ingredient slots were compared. The aspect costs, the instabilities, the result counts, the
patterns and the ingredient counts are equal in every one of the 290 covered recipes.** The ten
ingredient identities that differed are the defects of section 5.

## 4. The differences that are NOT defects

1. `Alembic` - the oracle declares a fifth ingredient symbol `'L'` (magical leaves, line 275) that
   its own pattern never uses. The native `thaumcraft:arcane/alembic` drops the unused symbol
   because the native `ArcaneShapedRecipe` codec rejects a key that the pattern does not use
   (`ArcaneShapedRecipe.java:56-63`: "Arcane key must contain exactly the non-space pattern
   symbols"). Dropping an unused symbol cannot change what the recipe accepts.
2. `JarLabelNull` - the oracle recipe is a shapeless recipe with one ingredient (a jar label, line
   425). The native recipe is a Java `CustomRecipe`
   (`ThaumcraftLabels.ClearRecipe`), so its JSON carries no `ingredients` array. The native recipe
   accepts exactly one jar label and returns a blank one, which is the original behaviour: the
   oracle registers it with `shapelessOreDictRecipe`, and Forge 1.7.10's `ShapelessOreRecipe`
   compares the item and the damage only, not the NBT - which is precisely why TC4 wrote its own
   `ShapelessNBTOreRecipe` (`thaumcraft/common/lib/crafting/ShapelessNBTOreRecipe.java:74-88`
   overrides `matches` to add `ItemStack.areItemStackTagsEqual`).
3. `BlockFlesh`, `Thaumometer`, `TallowCandle` - the oracle pattern uses a different symbol letter
   (`K` instead of `R`, `1` instead of `S`) and, for the tallow candle, a blank border
   (`" S "`, `" T "`, `" T "`). 1.21.1 trims the blank border of a shaped pattern:
   `ShapedRecipePattern.shrink` removes the leading and the trailing spaces of every row and the
   empty rows (verified in the 1.21.1 class file
   `net/minecraft/world/item/crafting/ShapedRecipePattern.class`, method `shrink`, which computes
   `firstNonSpace`/`lastNonSpace` over every row). The native `tallow_candle` therefore carries
   the trimmed `["S","T","T"]`, which accepts exactly the same vertical stack of one string above
   two tallow. The pattern symbol letters carry no meaning.
4. `RunicGirdleKinetic_2` - the oracle registers the second kinetic runic girdle with the potion
   metadata 24620 (`0x602C`, line 351). That metadata carries the brewed flag `0x2000` and the
   splash flag `0x4000` at the same time, and 1.7.10 brewing clears `0x2000` when it sets `0x4000`
   (`PotionHelper.gunpowderEffect` is `"+14&13-13"`), while the loot bag only ever issues
   `i | 0x2000` or `i | 0x4000`. No item of 1.7.10 can carry that metadata, so the original
   registration is dead in the original itself. 1.21.1 potions have no metadata, so the port
   carries the same recipe twice.

## 5. The defects this wave found and fixed

All ten are a wrong INGREDIENT: the native recipe asked the player for a different item than the
original. The original matches the components of an infusion and the ingredients of an arcane
recipe as ITEMS (`thaumcraft/api/crafting/InfusionEnchantmentRecipe.matches` compares the
ItemStacks, and the native `InfusionEnchantmentRecipe.matches:120-139` does the same through
`InfusionMatching`), so a substitute item changes what the player has to bring.

| # | original key | oracle line | original ingredient | native ingredient before the fix |
|---|---|---|---|---|
| 1 | `InfEnch3` (projectile protection) | 297 | `Items.arrow` | `minecraft:feather` |
| 2 | `InfEnch4` (feather falling) | 298 | `Items.feather` | `minecraft:string` |
| 3 | `InfEnch5` (respiration) | 299 | `Items.reeds` | `minecraft:cod` |
| 4 | `InfEnch6` (aqua affinity) | 300 | `Items.reeds` | `minecraft:cod` |
| 5 | `InfEnch7` (thorns) | 301 | `Blocks.deadbush` | `minecraft:cactus` |
| 6 | `InfEnch9` (smite) | 303 | `Items.glowstone_dust` | `minecraft:rotten_flesh` |
| 7 | `InfEnch15` (silk touch) | 309 | `Blocks.web` | `minecraft:glass` |
| 8 | `InfEnch21` (infinity) | 315 | `Items.arrow` | `minecraft:feather` |
| 9 | `ArcaneSpa` | 198 | `blockCosmeticSolid` metadata 6 (arcane stone block) | `thaumcraft:arcane_stone_bricks` (metadata 7) |
| 10 | `HoverHarness` | 341 | `blockWoodenDevice` metadata 6 (greatwood planks), twice | `thaumcraft:bellows` (metadata 0), twice |

Every one of the original items still exists in 1.21.1 under the same id (`minecraft:arrow`,
`minecraft:feather`, `minecraft:sugar_cane`, `minecraft:dead_bush`, `minecraft:glowstone_dust`,
`minecraft:cobweb`, `thaumcraft:arcane_stone`, `thaumcraft:greatwood_planks`), so nothing forced
the substitution. The metadata of the two block families is proved by the original language file
(`assets/thaumcraft/lang/en_US.lang`): `tile.blockWoodenDevice.6.name=Greatwood Planks` and
`tile.blockWoodenDevice.0.name=Arcane Bellows`, and
`tile.blockCosmeticSolid.6.name=Arcane Stone Block` and
`tile.blockCosmeticSolid.7.name=Arcane Stone Bricks`. The port was already inconsistent with
itself: the traveling trunk of the same file uses the greatwood planks for the same metadata
(line 372), and the flux scrubber uses the arcane stone bricks for the same metadata (line 180).

The game test that pinned the two bellows of the hover harness
(`common/src/gameTest/java/thaumcraft/common/testing/TravelGearRuntimeChecks.java:112`) now pins
the two greatwood planks.

## 6. The real coverage gaps this wave closed

Fourteen original keys had a native recipe file but were not named by
`ResearchRecipeCoverage`, so the catalogue was not closed over the original and the research book
could not resolve them:

| original key | oracle line | native recipe |
|---|---|---|
| `BalancedShard_0..5` | 89 | `thaumcraft:crucible/balanced_shard_{air,fire,water,earth,order,entropy}` |
| `Clusters0..5` | 442 | `thaumcraft:clusters/{air,fire,water,earth,order,entropy}` |
| `Clusters6` | 445 | `thaumcraft:clusters/mixed` |
| `RunicGirdleKinetic_2` | 351 | `thaumcraft:infusion/runic_girdle_kinetic_2` |

The coverage map now names all 308 original keys, the outside-book set names the 36 keys the
research corpus never references, and the catalogue guard of
`common/src/test/java/thaumcraft/research/ResearchRecipeCoverageTest.java` pins the whole set.

## 7. The deferrals, re-checked

The nine deferrals are the eight foreign ore recipes and the essentia mirror. The deferral
reasons were wrong or incomplete and are now exact:

* **The eight foreign ore recipes** (`PureTin`, `PureSilver`, `PureLead`, `TransTin`,
  `TransSilver`, `TransLead`, `WandCapSilver`, `WandCapSilverInert`). The original registers them
  only when another mod supplies the metal (`Config.foundTinIngot`, `foundSilverIngot`,
  `foundLeadIngot`), and 1.21.1 has no tin, no silver and no lead at all, so the original never
  registers them either. The CATALYST side of the port is already ready: the tag
  `thaumcraft:original_ore/ore_tin` points at the optional convention tag `c:ores/tin` and
  `thaumcraft:original_ore/nugget_tin` at `c:nuggets/tin`, so a third party tin mod would be picked
  up. What is missing is the RESULT item: the original result of `PureTin` is the tin cluster
  (`itemNugget` metadata 18) and of `TransTin` three tin nuggets (`itemNugget` metadata 2), and the
  port has no tin, silver or lead nugget or cluster item. The port already carries the copper
  equivalents, because copper is vanilla in 1.21.1.
* **The essentia mirror** (`MirrorEssentia`). The deferral used to say that "a 1.21.1 recipe result
  cannot carry a block state". That is FALSE: the `minecraft:block_state` component of an item
  stack holds the block state properties (`BlockItemStateProperties.CODEC` is a map of string to
  string) and `BlockItem.updateBlockStateFromTag` applies them to the placed state, so a recipe
  result CAN carry the `essentia` property of the mirror. The real blocker is the port's own
  mirror block: the essentia mirror is the same block with the `essentia` property true, its block
  entity is chosen by that property (`BlockMirror.createBlockEntity`), and `Level.setBlock` keeps
  the block entity of a block whose type did not change - a mirror placed with the component would
  keep the tile of the ordinary mirror and would never tick. The missing part is a separate
  essentia mirror ITEM, not a recipe format. A new item and the block entity it needs is content of
  the base content domain, so the deferral stays, with the exact reason.

## 8. The deviations the port keeps on purpose

These are not defects of this wave; they are the port's own documented positions. They are listed
here so that the leader can decide about them.

| original key | oracle | native | why the port does it that way |
|---|---|---|---|
| `GolemClay` | catalyst `Blocks.brick_block` (bricks) | `minecraft:clay` | the original asks for bricks for the clay golem; the port asks for clay |
| `EldritchEye` | component `itemResource` metadata 17 (void seed) | `thaumcraft:void_ingot` | the original asks for the void seed |
| `LampFertility` | component `Items.carrot` | `minecraft:golden_carrot` | the original asks for a carrot |
| `AltInk` | result and catalyst `Items.dye` metadata 0 | `minecraft:ink_sac` | the original asks for the dye item; the port keeps the ink of the research page |
| `MaskGrinningDevil` | component `Items.dye` metadata 0 | `minecraft:ink_sac` | the port keeps the ink of the mask |
| `TinyVisor`, `TinyArmor` | component `Items.iron_helmet` / `Items.iron_chestplate` (any wear) | `minecraft:leather_helmet` / `minecraft:leather_chestplate` | the port accepts leather armour of any wear and no iron one; the game test `GolemDecorationsRuntimeChecks` states that position |

The native item ids of three recipes differ from the original names, but the display names are
the original ones: `thaumcraft:key_iron` and `thaumcraft:key_gold` are the iron and the gold
arcane key (the original `itemKey` metadata 0 and 1), `thaumcraft:golem_core_essentia` is the
"Golem Animation Core: Alchemy" (the original `itemGolemCore` metadata 6) and
`thaumcraft:unknown_research_note` is the research note (the original `itemResearchNotes`).

The four potions of the runic accessories are not deviations: the original names them by their 1.7
metadata (8226, 8233, 8257, 16428) and the port names them by their 1.21.1 effect with a
component predicate (`InfusionPotionRequirement`), which is the exact same potion.

## 9. The tests

Both are **authored but NOT EXECUTED** - this agent does not run Gradle (the leader owns every
build; concurrent Loom daemons corrupt the shared cache).

* `common/src/test/java/thaumcraft/research/ResearchRecipeCoverageTest.java` - the corpus guard,
  EXTENDED: it now pins the whole catalogue of 308 original keys
  (`theWholeOriginalCatalogueIsClassified`), the 293 native recipe ids
  (`everyMappedOriginalKeyResolvesToAPackagedNativeRecipe`), the 36 outside-book keys and the
  fourteen keys this wave mapped (`theOriginalRecipesOutsideTheResearchBookAreMappedToTheirNativeRecipes`).
* `common/src/test/java/thaumcraft/assets/RecipeIngredientParityTest.java` - NEW, the content
  guard of this wave: it pins the original ingredient of the ten restored recipes, the two arcane
  stones of the arcane spa and the flux scrubber, the greatwood planks of the hover harness and
  the traveling trunk, and the aspect cost of the six balanced shards.

The existing game test of the hover harness was updated with the recipe.

## 10. The ledger

`docs/porting/scope-ledger.json`, the nine units of the `recipes_and_arcane_crafting` family
only:

| unit | before | after |
|---|---|---|
| `thaumcraft/common/config/ConfigRecipes.java` | replace / partial | replace / **parity_verified** |
| `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java` | replace / partial | replace / **parity_verified** |
| `thaumcraft/common/container/SlotCraftingArcaneWorkbench.java` | adapt / partial | adapt / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesArcaneSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesSmeltingSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesSpecialSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/client/integration/jei/JeiRecipeData.java` | exclude / missing | exclude / missing (the port has no JEI) |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` | exclude / missing | exclude / missing (the port has no JEI) |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` | exclude / missing | exclude / missing (the port has no JEI) |

The shape of the file is unchanged, `counts` is untouched and the review fingerprints of the nine
rows still match their sources, so `stale_review_fingerprints` stays 0. The check itself
(`python3 scripts/porting/update_scope_ledger.py --check`) could NOT be run in this worktree: the
script needs the private oracle root `work/thaumcraft-4.2.3.5-oracle`, which only the leader has.
The equivalent computation was run over the file instead: the stored `review_fingerprints_current`
of every one of the 1044 rows still equals the value the script computes, and no row is stale.

## 11. What was NOT done

* The nine deferrals stay deferred, with the exact blocker of section 7. Closing the six foreign
  ore crucible recipes needs six new items (the tin, silver and lead nugget and cluster items),
  which is item and asset content of the base content domain; closing the essentia mirror needs a
  separate mirror item and its block entity.
* No Gradle build, no test run, no client or server session: this agent does not run Gradle. Every
  test above is authored but unexecuted.
* The `docs/MIGRATION_STATUS.md` and `docs/HANDOFF.md` were not touched; they are shared documents
  of the whole port.
