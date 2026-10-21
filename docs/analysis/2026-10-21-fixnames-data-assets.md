# Names, item models and the unreachable textures — 2026-10-21

The data-layer gaps of the port-fidelity audit of 2026-10-21
(`docs/analysis/2026-10-21-audit-a10-data.md`, §6 assets and §7 language files) are closed.
Oracle: TC4 4.2.3.5 (`/home/Acak/work/thaumcraft-4.2.3.5-oracle/`), the only source of truth.
The original 1.7.10 art and the 1.12 reference language files stay read-only.

## 1. Result

| What | Before | After |
|---|---|---|
| Language keys per file (en_us / ru_ru) | 2328 | 2423 |
| Registered ids without a name that 1.21.1 resolves | 51 items, 4 blocks, 33 entities, 7 container titles | 0 |
| Registered items without an item model | 28 | 0 |
| Original textures that no native model or code path reaches | 63 | 63 (see §3 — every one of them is now explained) |

* **95 names per language file** (51 items, 4 blocks, 33 entities, 7 container titles), 190 values.
* **28 item models** added (17 spawn eggs, 10 cultist armour pieces, the flux crystal).
* **11 original textures** became reachable (10 cultist armour icons, the cultist boots layer).
* The asset test now walks the item models and the registry, not only the blockstates.

## 2. The names

### 2.1 The keys 1.21.1 resolves

| Kind | Key | Count |
|---|---|---|
| item | `item.thaumcraft.<id>` | 51 |
| block | `block.thaumcraft.<id>` | 4 |
| entity | `entity.thaumcraft.<id>` | 33 |
| container | `container.<id>` | 7 |

The 1.7.10 spellings (`item.thaumcraft.<id>.name`, `tile.thaumcraft.<id>.name`,
`entity.thaumcraft.<camelCase>.name`) are dead keys in 1.21.1: the game never resolves them, so a
name that only exists in that spelling is not a name. The audit counted 27 unnamed items because
its coverage check accepted the dead `.name` spelling; the real number was 51.

### 2.2 The names and where they come from

English: the 1.7.10 `en_US.lang` and the 1.12 reference `en_us.lang`.
Russian: the 1.12 reference `ru_ru.lang`; for the four entities that only exist in the later
Thaumcraft 6 build the `ru_ru.lang` of `Thaumcraft-1.12.2-6.1.BETA26.jar`.

| Key | English | Russian |
|---|---|---|
| `item.thaumcraft.cultist_cleric_spawn_egg` | Crimson Cleric Spawn Egg | Яйцо призыва багрового мага |
| `item.thaumcraft.cultist_knight_spawn_egg` | Crimson Knight Spawn Egg | Яйцо призыва багрового рыцаря |
| `item.thaumcraft.cultist_leader_spawn_egg` | Praetor Spawn Egg | Яйцо призыва претора |
| `item.thaumcraft.eldritch_crab_spawn_egg` | Eldritch Crab Spawn Egg | Яйцо призыва древнего краба |
| `item.thaumcraft.eldritch_golem_spawn_egg` | Eldritch Construct Spawn Egg | Яйцо призыва древнего голема |
| `item.thaumcraft.eldritch_warden_spawn_egg` | Eldritch Warden Spawn Egg | Яйцо призыва древнего хранителя |
| `item.thaumcraft.inhabited_zombie_spawn_egg` | Shambling Husk Spawn Egg | Яйцо призыва волочащейся оболочки |
| `item.thaumcraft.taint_crawler_spawn_egg` | Tainted Crawler Spawn Egg | Заражённый ползун: яйцо призыва |
| `item.thaumcraft.taint_creeper_spawn_egg` | Tainted Creeper Spawn Egg | Заражённый крипер: яйцо призыва |
| `item.thaumcraft.taintacle_giant_spawn_egg` | Giant Taintacle Spawn Egg | Гигантское заражённое щупальце: яйцо призыва |
| `item.thaumcraft.tainted_chicken_spawn_egg` | Tainted Chicken Spawn Egg | Заражённая курица: яйцо призыва |
| `item.thaumcraft.tainted_cow_spawn_egg` | Tainted Cow Spawn Egg | Заражённая корова: яйцо призыва |
| `item.thaumcraft.tainted_pig_spawn_egg` | Tainted Pig Spawn Egg | Заражённая свинья: яйцо призыва |
| `item.thaumcraft.tainted_sheep_spawn_egg` | Tainted Sheep Spawn Egg | Заражённая овца: яйцо призыва |
| `item.thaumcraft.tainted_villager_spawn_egg` | Tainted Villager Spawn Egg | Заражённый житель: яйцо призыва |
| `item.thaumcraft.thaum_slime_spawn_egg` | Thaumic Slime Spawn Egg | Яйцо призыва таум-слизня |
| `item.thaumcraft.watcher_spawn_egg` | Watcher Spawn Egg | Яйцо призыва наблюдателя |
| `item.thaumcraft.discount_ring_aer` | Apprentices Ring of Air | Кольцо подмастерья - Воздух |
| `item.thaumcraft.discount_ring_terra` | Apprentices Ring of Earth | Кольцо подмастерья - Земля |
| `item.thaumcraft.discount_ring_ignis` | Apprentices Ring of Fire | Кольцо подмастерья - Огонь |
| `item.thaumcraft.discount_ring_aqua` | Apprentices Ring of Water | Кольцо подмастерья - Вода |
| `item.thaumcraft.discount_ring_ordo` | Apprentices Ring of Order, Regularity, Purity | Кольцо подмастерья - Порядок, Организованность |
| `item.thaumcraft.discount_ring_perditio` | Apprentices Ring of Entropy, Chaos, Destruction | Кольцо подмастерья - Хаос, Разрушение |
| `item.thaumcraft.flux_crystal` | Flux Crystal | Кристалл Выбросов |
| `item.thaumcraft.thaumonomicon_cheat` | Creative Thaumonomicon | Таумономикон для творческого режима |
| `item.thaumcraft.unknown_research_note` | Research Notes | Записки исследований |
| `item.thaumcraft.unknown_research_note_24` | Research Notes | Записки исследований |
| `item.thaumcraft.crimson_sword` | Crimson Blade | Багровый меч |
| `item.thaumcraft.cultist_boots` | Cultist Boots | Ботинки культиста |
| `item.thaumcraft.cultist_leader` | Crimson Praetor Chestplate | Кираса Претора культистов |
| `item.thaumcraft.cultist_leader_helm` | Crimson Praetor Helm | Шлем Претора культистов |
| `item.thaumcraft.cultist_leader_legs` | Crimson Praetor Greaves | Поножи Претора культистов |
| `item.thaumcraft.cultist_plate` | Crimson Cult Chestplate | Кираса культиста |
| `item.thaumcraft.cultist_plate_helm` | Crimson Cult Helm | Шлем культиста |
| `item.thaumcraft.cultist_plate_legs` | Crimson Cult Greaves | Поножи культиста |
| `item.thaumcraft.cultist_robe` | Crimson Cult Robe | Мантия культиста |
| `item.thaumcraft.cultist_robe_hood` | Crimson Cult Hood | Капюшон культиста |
| `item.thaumcraft.cultist_robe_legs` | Crimson Cult Leggings | Шаровары культиста |
| `item.thaumcraft.eldritch_placer` | Eldritch Obelisk Placer | Установщик древних обелисков |
| `item.thaumcraft.eldritch_tablet` | Runed Tablet | Рунная скрижаль |
| `item.thaumcraft.nugget_beef` | Beef Nugget | Кусочек говядины |
| `item.thaumcraft.nugget_chicken` | Chicken Nugget | Кусочек курятины |
| `item.thaumcraft.nugget_fish` | Fish Nugget | Кусочек рыбки |
| `item.thaumcraft.nugget_pork` | Pork Nugget | Кусочек свинины |
| `item.thaumcraft.primal_arrow_air` | Air Arrow | Грозовая стрела |
| `item.thaumcraft.primal_arrow_earth` | Earth Arrow | Каменная стрела |
| `item.thaumcraft.primal_arrow_entropy` | Entropy Arrow | Разрушающая стрела |
| `item.thaumcraft.primal_arrow_fire` | Fire Arrow | Огненная стрела |
| `item.thaumcraft.primal_arrow_order` | Order Arrow | Стрела порядка |
| `item.thaumcraft.primal_arrow_water` | Water Arrow | Ледяная стрела |
| `item.thaumcraft.triple_meat_treat` | Triple Meat Treat | Мясное ассорти |
| `block.thaumcraft.arcane_light` | Glimmer of Light | Мерцающий эфир |
| `block.thaumcraft.hole` | Empty Space | Пустое место |
| `block.thaumcraft.shock_trap` | Static Field | Статическое поле |
| `block.thaumcraft.warded` | Warded Stone Block | Усиленный камень |
| `entity.thaumcraft.aspect_orb` | Aspect Orb | Сфера аспекта |
| `entity.thaumcraft.bottle_taint` | Bottled Taint | Порча в бутылке |
| `entity.thaumcraft.cultist_cleric` | Crimson Cleric | Багровый маг |
| `entity.thaumcraft.cultist_knight` | Crimson Knight | Багровый рыцарь |
| `entity.thaumcraft.cultist_leader` | Praetor | Претор |
| `entity.thaumcraft.cultist_portal` | Crimson Portal | Багровый портал |
| `entity.thaumcraft.cultist_portal_greater` | Greater Crimson Portal | Большой багровый портал |
| `entity.thaumcraft.eldritch_crab` | Eldritch Crab | Древний краб |
| `entity.thaumcraft.eldritch_golem` | Eldritch Construct | Древний голем |
| `entity.thaumcraft.eldritch_warden` | Eldritch Warden | Древний хранитель |
| `entity.thaumcraft.explosive_orb` | Explosive Fireball | Огненный шар |
| `entity.thaumcraft.follow_item` | Following Item | Сопровождающий предмет |
| `entity.thaumcraft.golem_orb` | Energy Orb | Энергетический шар |
| `entity.thaumcraft.inhabited_zombie` | Shambling Husk | Волочащаяся оболочка |
| `entity.thaumcraft.inventory_item` | Inventory Item | Предмет инвентаря |
| `entity.thaumcraft.item_grate` | Item Grate | Железная решетка |
| `entity.thaumcraft.pech_blast` | Pech Blast | Гномий заряд |
| `entity.thaumcraft.permanent_item` | Permanent Item | Постоянный предмет |
| `entity.thaumcraft.primal_arrow` | Primal Arrow | Сингулярная стрела |
| `entity.thaumcraft.shock_orb` | Shocking Orb | Шоковый шар |
| `entity.thaumcraft.special_item` | Magic Item | Магический предмет |
| `entity.thaumcraft.taint_crawler` | Tainted Crawler | Заражённый ползун |
| `entity.thaumcraft.taint_creeper` | Tainted Creeper | Заражённый крипер |
| `entity.thaumcraft.taint_seed` | Taint Seed | Семя заражения |
| `entity.thaumcraft.taint_seed_prime` | Giant Taint Seed | Гигантское семя заражения |
| `entity.thaumcraft.taintacle_giant` | Giant Taintacle | Гигантское заражённое щупальце |
| `entity.thaumcraft.tainted_chicken` | Tainted Chicken | Заражённая курица |
| `entity.thaumcraft.tainted_cow` | Tainted Cow | Заражённая корова |
| `entity.thaumcraft.tainted_pig` | Tainted Pig | Заражённая свинья |
| `entity.thaumcraft.tainted_sheep` | Tainted Sheep | Заражённая овца |
| `entity.thaumcraft.tainted_villager` | Tainted Villager | Заражённый житель |
| `entity.thaumcraft.thaum_slime` | Thaumic Slime | Таум-слизень |
| `entity.thaumcraft.watcher` | Watcher | Наблюдатель |
| `container.decontable` | Deconstruction Table | Стол расщепления |
| `container.pedestal` | Arcane Pedestal | Магический пьедестал |
| `container.wandpedestal` | Wand Recharge Pedestal | Пьедестал перезарядки |
| `container.focuspouch` | Focus Pouch | Сумка для набалдашников |
| `container.thaumcraft.magic_box` | Magic Box | Магический ящик |
| `container.thaumcraft` | Thaumcraft | Таумкрафт |
| `container.fake` | Fake Inventory | Фальшивый инвентарь |

### 2.3 The cases that need an explanation

* **`entity.thaumcraft.cultist_leader`** — the original name is a format string
  (`entity.thaumcraft.cultistleader.name` = `Praetor %s the %s`, `Претор %s %s`). A key that 1.21.1
  resolves for every cultist leader would print the raw placeholders, so the flat key carries the
  name without them. The format string itself stays, the cultist entities still use it
  (`EntityCultistLeader.getName()`).
* **The four cultist entities** (`cultist_cleric`, `cultist_knight`, `cultist_leader`,
  `cultist_portal`) — the port overrode `getName()` with the 1.7.10 spelling
  (`entity.thaumcraft.cultistcleric.name`), which works, so the audit counted them as named. The
  flat 1.21.1 keys are added anyway: `EntityType.getDescriptionId()` is what the game uses for the
  entity list, the commands and the advancement criteria. The old keys stay, the
  `CultistRulesTest` asserts them.
* **`entity.thaumcraft.watcher` and `entity.thaumcraft.inventory_item`** — two native-only
  entities: the TC4 `EntityWatcher` was never registered and the original has no name for it, and
  `EntityInventoryItem` is the port's own drop entity. They are named after their class and their
  role; no original name exists.
* **`entity.thaumcraft.taint_crawler`, `taint_seed`, `taint_seed_prime`,
  `cultist_portal_greater`** — these four exist only in the later Thaumcraft 6 build; the names and
  the Russian names come from the language file of `Thaumcraft-1.12.2-6.1.BETA26.jar`.
* **`item.thaumcraft.taint_crawler` and `entity.thaumcraft.taint_spider`** — both are named
  "Tainted Crawler", because that is the original name of the TC4 taint spider and of the TC6 taint
  crawler. The port splits one original mob into two entities, so the two share a display name.
  Renaming one of them is a decision for the taint subsystem, not for the data layer.
* **`item.thaumcraft.unknown_research_note` / `_24`** — the original notes are the research notes
  (`item.researchnotes.name`); the two flat ids are the metadata 42 and 24 variants, and their
  `getDescriptionId()` is overridden to the note item, so the flat keys only complete the catalogue.
* **`block.thaumcraft.shock_trap`** — the original metadata value of the block the shock orb
  places (`blockAiry` metadata 10, `tile.blockAiry.10.name` = `Static Field`). The blockstate of the
  airy hole covers the same metadata value and carries the same name; the two native blocks are a
  port split, not an original pair.
* **`block.thaumcraft.arcane_light`** — the original metadata value 3 of the same block
  (`tile.blockAiry.3.name` = `Glimmer of Light`).

## 3. The assets

### 3.1 What the port now reaches that it did not reach before

| Texture | Original | Now |
|---|---|---|
| `textures/items/cultistrobehelm.png` … 10 cultist armour icons | 1.7.10 `textures/items/cultist*.png` | the icon of the ten cultist armour pieces, through the new item models |
| `textures/models/cultistboots.png` | 1.7.10 armour layer of the cultist boots | `textures/models/armor/cultist_boots_layer_1.png`, the 1.21.1 layer name of the `cultist_boots` armour material |
| `textures/models/robes_1.png`, `robes_2.png`, `robes_1_overlay.png`, `robes_2_overlay.png`, `thaumium_1.png`, `thaumium_2.png`, `void_1.png`, `void_2.png`, `goggles.png`, `bootstraveler.png` | 1.7.10 armour layer sheets | already present byte-identical under the 1.21.1 names `textures/models/armor/<name>_layer_<1|2>.png`; the 1.21.1 `ArmorMaterial.Layer` resolves `<name>` to exactly that path, so the audit's path check simply did not see the rename |

### 3.2 The 28 item models that were missing

The registry test found 28 registered items without an item model; they showed the missing-model
cube in the inventory and in the creative tab.

* 17 spawn eggs → `{"parent":"minecraft:item/template_spawn_egg"}`, the same parent the ten spawn
  eggs that already had a model use.
* 10 cultist armour pieces → `minecraft:item/generated` with the original 16×16 icon
  (`thaumcraft:items/cultistrobehelm`, `cultistrobechest`, `cultistrobelegs`, `cultistplatehelm`,
  `cultistplatechest`, `cultistplatelegs`, `cultistplateleaderhelm`, `cultistplateleaderchest`,
  `cultistplateleaderlegs`, `cultistboots`). These icons existed in the port but no model named
  them.
* `flux_crystal` → `minecraft:item/generated` with `thaumcraft:blocks/crystal`. The item is a
  Thaumcraft 6 concept the port introduced (the taint crawler and the taint seeds drop it); no
  original item icon exists for it, so it borrows the crystal texture. A dedicated icon would have
  to be drawn.

### 3.3 The 63 textures that stay unreachable, and why

Nothing of the original art was deleted: all 820 original textures are still in the tree, byte for
byte. The list below is the ones that no native model and no code path names any more.

**The original particle sheets of the 1.7.10 effect engine (30).** `models/hemis1`…`hemis15` (15)
and `models/ripple1`…`ripple15` (15) are the sheets of the original `FXHemispheres` and
`FXRipples` effects. The port replaced that effect engine with its own particle system
(`RunicHitParticle`, `WarpSonicParticle` draw `hemis.obj` as native vertices), so the sheets have
no counterpart. `models/hemis0` and `models/ripple0` are the base sheets of the same two effects
and are still referenced.

**The original armour layer sheets (8).** `models/robes_1`, `robes_2`, `robes_1_overlay`,
`robes_2_overlay`, `thaumium_1`, `thaumium_2`, `void_1`, `void_2` are the 1.7.10 armour layer
files of the robes, the thaumium armour and the void armour. The port renders those pieces from the
original single sheets (`cultist_robe_armor.png`, `void_robe_armor.png`, `fortress_armor.png`) with
its own native models, and it ships byte-identical copies of the layer sheets under the 1.21.1
names (see §3.1). The original paths are not referenced any more, the pixels are used.

**The armour pieces whose 1.7.10 layer was a model sheet (3).**
`models/cultistboots` is now used (§3.1). `models/zombie_plate_armor` is the cultist plate as worn
by an inhabited zombie (`ItemCultistPlateArmor.getArmorTexture`); the port renders every cultist
plate with `cultist_plate_armor.png` and does not reproduce the zombie variant — a behaviour gap
of the cultist subsystem, not a missing texture. `models/void_robe_armor_overlay` is the dyeable
outer layer of the void robe; the port tints the void robe sheet itself
(`ThaumcraftGearModelsClient.renderRobe`), so the overlay has no counterpart.

**The two 1.12 items that never existed in TC4 (2).** `models/runic_goggles` and
`models/runicbootstraveler` belong to the Thaumcraft 6 runic accessories; the TC4 catalogue has no
such items.

**The wand caps that the port does not register (2).** `models/wand_cap_copper` and
`models/wand_cap_silver` are the sheets of the copper and the silver wand caps. The copper cap is
a known catalogue gap (audit D2), the silver cap cannot exist in 1.21.1; neither item is
registered, so neither sheet can be reached.

**The features the port does not render (8).**

| Texture | The original use | Why it stays unreachable |
|---|---|---|
| `models/scanner`, `models/scanscreen` | the thaumometer's scan model and its animated scan screen (`ItemThaumometerRenderer`) | the port has no thaumometer renderer at all; the item icon exists (`textures/items/thaumometer.png`, the same pixels as `models/scanner.png`) |
| `models/golem_decoration` | the sheet of the fourteen worn golem pieces (`RenderGolemBase`) | the port has `ModelGolemAccessories`, but nothing references it: the worn golem pieces are not rendered |
| `models/jarbrine` | the brine volume of the essentia jar (`TileJarRenderer`) | the port draws the stored liquid with the `animatedglow` atlas sprite; `ModelJar` is not used |
| `models/moneychanger` | the skin of the banker villager (`ClientProxy`) | the port has no villager professions |
| `models/thaumaturge_eyes` | — | no use in the original either |
| `models/eldritch_taintacle` | — | no use in the original either |
| `models/spec_h` | the Halloween wand sheet (`ModelWand` binds it on the 31st of October) | the port has no Halloween flag; the wand always uses `models/wand.png` |
| `gui/hud` | the background of the casting wand HUD and of the sanity HUD (`ClientTickEventsFML`) | the port has no HUD overlay for the wand |

**The GUI and effect sheets of features the port does not draw (5).**

| Texture | The original use | Why it stays unreachable |
|---|---|---|
| `gui/hex2` | the hex highlight of the research table (`GuiResearchTable.drawHexHighlight`) | the port's research table draws the hexes with `gui/hex1.png` and has no hover highlight |
| `misc/script` | the glowing rune on a wand focus (`ModelWand.drawRune`) | the port's wand model has no rune |
| `misc/architect_arrows` | the area arrows of the architect wand (`REHWandHandler`) | the port has no architect wand overlay |
| `misc/home`, `misc/mark` | the research markers in the world (`RenderEventHandler`) | the port has no world marker rendering |
| `misc/beam3`, `misc/beamh` | the third beam of the wand focus and the arc of the infusion matrix (`FXBeamWand`, `FXArc`) | the port's beam system uses `misc/beam1` and `misc/beam2`; the infusion arc is `InfusionArcParticle` with `p_large`/`p_small` |

**The research category icons (5).** `misc/r_thaumaturgy`, `misc/r_artifice`, `misc/r_crucible`,
`misc/r_golemancy`, `misc/r_eldritch` — the icons of the five research categories. Another agent
owns the research category icons; this wave does not touch them.

**The one renamed file (1).** `blocks/es_i_1 - copy` — a duplicate of `blocks/es_i_1.png` whose file
name has a space, which 1.21.1 cannot read. The port ships it as `blocks/es_i_1_-_copy.png`; no
model uses either copy.

**The two focus upgrade families (21) that are reachable.** The 21 `foci/*.png` icons are not named
by a literal, they are built at runtime from the focus upgrade id
(`FocusUpgradeType`), so they are reachable; the audit listed them as unreachable for the same
reason it missed the armour layer rename.

### 3.4 A finding that is not a texture problem

The cultist armour of the *worn* pieces is drawn by the vanilla armour layer, and the armour
materials of the cultist robe, plate, leader and boots name layer textures that do not exist
(`textures/models/armor/cultist_robe_layer_1.png` and so on). The port has its own renderers for
exactly those pieces (`ThaumcraftGearModelsClient.renderRobe`, `renderKnightArmor`,
`renderLeaderArmor`) and `renderRobe` even selects the cultist sheet — but the loader registers
only the void robe and the fortress armour (`fabric/.../ThaumcraftFabricClient`), so the cultist
robe, plate and leader are drawn by the vanilla layer with a missing texture. The cultist boots
are the fourth case and are fixed here (`cultist_boots_layer_1.png`). The three other pieces need
the loader registration, which belongs to the client/gear wave.

## 4. The asset test

`common/src/test/java/thaumcraft/assets/RegisteredAssetCoverageTest.java` (new) and
`RegistryInventory.java` (new) read the registry of the port by reflection, exactly like the
existing `EldritchAndDeviceAssetsTest`:

* `RegistryInventory` loads every class of `thaumcraft.common.config` and reads the id of every
  `RegistrySupplier` field, including the `Map<String, RegistrySupplier<…>>` fields of the
  families that register in a loop (the resources, the shards, the crystals, the golem pieces, the
  coloured candles, the arrows, the nuggets). The game is bootstrapped first, because the
  registration classes touch the vanilla registries while they fill their entries.
* `RegisteredAssetCoverageTest` fails when a registered item has no item model or no name, when a
  registered block has no blockstate, a missing model, no loot table or no name, when a registered
  entity has no name, and when a container title that the compiled code uses is missing from one of
  the two language files. The container titles are read from the constant pool of the compiled
  classes, so a container added later is covered without touching the test.
* `BlockResourceCoverageTest` now walks the item models as well as the blockstates: a model of a
  registered item has to carry a name in both languages. The `NO_BLOCK_NAME` exception list is
  gone — every block has a name now.

## 5. Verification

```
JAVA_HOME=/home/Acak/.jdk21 GRADLE_USER_HOME=/home/Acak/gradle-homes/fixnames \
  ./gradlew --no-daemon --console=plain :common:compileJava :common:test
# BUILD SUCCESSFUL — 224 test classes, 1550 tests, 0 failures, 0 errors, 0 skipped

python3 -B scripts/porting/convert_assets.py --verify
# {"atlas_mipmap_level_guard": 4, "atlas_sprites_available": 516,
#  "checked_atlas_sprite_refs": 3288, "checked_local_model_refs": 1523,
#  "checked_model_texture_refs": 3288, "checked_sound_event_ids": 66,
#  "checked_sound_refs": 111, "missing_references": 0}
```

The client render of the new cultist boots layer and of the new item models was not inspected in a
running game; the resource wiring is verified by the asset test and by the asset verification
script.
