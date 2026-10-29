# Thaumcraft content, entities and the world

## Reaching Thaumcraft's items and blocks

Every item and block has a 1.21.1 id in the `thaumcraft` namespace.

| Need | Call |
|---|---|
| an item by id | `ItemsTC.require("thaumium_ingot")` (throws when unknown), `ItemsTC.get(...)` (null when unknown), `ItemsTC.stack("salis_mundus", 4)` |
| a block by id | `BlocksTC.get("arcane_stone")`, `BlocksTC.require(...)` |
| a 1.7.10 `ConfigItems`/`ConfigBlocks` field with metadata | `ItemApi.getItem("itemResource", 14)` (salis mundus), `ItemApi.getBlock(name, meta)`: the original names resolve to the flattened 1.21.1 item, block or state |

The whole table of original names is [LEGACY_IDS.md](../LEGACY_IDS.md). Call these from `onThaumcraftReady` or later:
Thaumcraft's items may not exist when your own `onInitialize` runs.

## Infusion stabilisers

A block that implements `IInfusionStabiliser` (`canStabaliseInfusion(level, pos)`) steadies an infusion when it stands
symmetrically around the altar, like candles and skulls do.

## Entities

| Interface | Effect |
|---|---|
| `IEldritchMob` | an eldritch mob: Thaumcraft's eldritch mobs treat it as an ally and do not attack it |
| `ITaintedMob` | a tainted creature: flux gas, taint bottles and taint conversion pass it by, and taintacles and taint creepers do not target it |
| `IThaumcraftBoss` | a boss is never promoted to a champion; `generateName()` names it after its champion modifier |

Entity tags (the aspects of an entity) are on the [aspects](aspects.md#entity-tags) page.

## Damage and effects

Damage types are data in 1.21.1, so the damage sources need a level or an entity:

| 1.7.10 | 1.21.1 |
|---|---|
| `DamageSourceThaumcraft.taint` | `DamageSourceThaumcraft.taint(level)` |
| `DamageSourceThaumcraft.dissolve` | `DamageSourceThaumcraft.dissolve(level)` |
| `causeTentacleDamage(entity)`, `causeSwarmDamage(entity)` | the same names |
| `DamageSourceIndirectThaumcraftEntity` | `DamageSourceIndirectThaumcraftEntity.create(direct, cause, type)` |

The original potions are registered mob effects: `PotionFluxTaint.instance()` / `holder()` / `KEY`, and the same for
`PotionVisExhaust`. Apply one with `new MobEffectInstance(PotionFluxTaint.holder(), duration, amplifier)`. The warp
effects have the same kind of handle in `WarpEffects` (next section).

## Stable keys

Thaumcraft's creatures, effects, armour materials and sounds are registered objects. A 1.7.10 add-on recognised them by
class (`instanceof EntityWisp`), by the numeric potion ids of `Config` or by a sound name in a string; in 1.21.1 the same
add-on would write `"thaumcraft:warp_ward"` and get no help from the compiler. The API names each of them once:

| Need | Keys | Resolve with |
|---|---|---|
| the seven warp effects (1.7.10 `Config.potionWarpWardID`, `potionSunScornedID`, `potionDeathGazeID`, `potionBlurredID`, `potionUnHungerID`, `potionInfVisExhaustID`, `potionThaumarhiaID`) | `WarpEffects.WARP_WARD`, `SUN_SCORNED`, `DEATH_GAZE`, `BLURRED_VISION`, `UNNATURAL_HUNGER`, `INFECTIOUS_VIS_EXHAUST`, `THAUMARHIA` (`WarpEffects.all()` lists them) | `WarpEffects.holder(key)` for `addEffect`, `hasEffect`, `removeEffect` and `new MobEffectInstance`; `WarpEffects.instance(key)` for the `MobEffect`. Only the ward is beneficial. The two flux effects keep `PotionFluxTaint` and `PotionVisExhaust` |
| a creature, a golem or the frost shard (1.7.10 `EntityWisp`, `EntityFireBat`, `EntityBrainyZombie`, `EntityGolemBase`, `EntityFrostShard`...) | `ThaumcraftEntities.WISP`, `FIREBAT`, `BRAINY_ZOMBIE`, `GOLEM`, `FROST_SHARD` and 32 more: the monsters, the cultists, the eldritch creatures, the tainted creatures and the traveling trunk | `ThaumcraftEntities.is(entity, key)` instead of `instanceof`; `type(key)` and `holder(key)` for a spawner or a predicate. `IEldritchMob`, `ITaintedMob` and `IThaumcraftBoss` still ask for a whole family |
| the thaumium, fortress, void and void robe armour materials (1.7.10 `ThaumcraftApi.armorMatThaumium` as the argument of `ItemArmor`) | `ThaumcraftMaterials.ARMORMAT_THAUMIUM_KEY`, `ARMORMAT_THAUMIUM_FORTRESS_KEY`, `ARMORMAT_VOID_KEY`, `ARMORMAT_VOID_ROBE_KEY` | `ThaumcraftMaterials.armorMatThaumiumHolder()`, `armorMatThaumiumFortressHolder()`, `armorMatVoidHolder()`, `armorMatVoidRobeHolder()` (and `ThaumcraftApi.armorMatThaumiumHolder()` for the first three): the `Holder` an `ArmorItem` takes. `holder.value()` is the object `armorMatThaumium()` returns |
| a sound (`"thaumcraft:wand"`, `zap`, `wind`, `tool`, `brain`, `egidle`, `key`...) | one `ResourceLocation` constant per sound event Thaumcraft registers: `ThaumcraftSoundIds.WAND`, `ZAP`, `WIND`, `TOOL`, `BRAIN`, `EGIDLE`, `KEY`... | `ThaumcraftSoundIds.event(id)` for `Level.playSound` |

An add-on that recognises a wisp, applies the ward and plays the wand sound, with no string in sight:

<!-- snippet:stable-keys -->
```java
// Thaumcraft's creatures, effects and sounds by key, never by string. A wisp killed with the amethyst core in the hand
// gives its killer a minute of warp ward (the effect that suspends the warp check) and plays the sound of the wand.
ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
    if (!ThaumcraftEntities.is(entity, ThaumcraftEntities.WISP) || !(source.getEntity() instanceof ServerPlayer killer)
            || !killer.getMainHandItem().is(AMETHYST_ROD)) return;
    killer.addEffect(new MobEffectInstance(WarpEffects.holder(WarpEffects.WARP_WARD), 1200, 0));
    killer.level().playSound(null, killer.blockPosition(), ThaumcraftSoundIds.event(ThaumcraftSoundIds.WAND),
            SoundSource.PLAYERS, 0.6F, 1.0F);
});
```
<!-- /snippet -->

- The keys are constants and exist when the class loads. The objects exist once Thaumcraft has registered them, which has
  happened by `onThaumcraftReady`: asking for an effect, entity type or material earlier (a static initialiser of
  yours) throws `IllegalStateException`, and `ThaumcraftSoundIds.event` throws `IllegalArgumentException` for an id that is
  not a registered sound. Resolve inside a handler or after the hook, as the snippet does. `ThaumcraftEntities.is` never throws.
- `armorMatSpecial()` and `armorMatVoidFortress()` are plain objects with the original numbers and are **not in the armour
  material registry at all**, so they have no key or holder: build an item from one with `Holder.direct(...)`, or register an
  `ArmorMaterial` of your own with your own worn textures. A material named `thaumcraft:special` *is* registered, but it is the
  goggles' adapter (the same defences, a gold repair ingredient and the goggles' worn layer): a different object from
  `armorMatSpecial()`, and not part of the API.
- Tested by the unit test `StableKeysApiTest` (every key is well formed and names an id the mod registers) and by the GameTest
  `stableKeysResolveToTheRegisteredObjects`, which resolves every key in a running world and checks the class, the category,
  the defence numbers and that the effect can be applied through the handle.

## World generation

| Call | Effect |
|---|---|
| `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, level)` | the original numeric dimension blacklist; 0 or 2 suppress Thaumcraft's ores and aura, every level suppresses its vegetation and structures |
| `ThaumcraftApi.addBiomeBlacklist(ResourceKey<Biome>, level)` | keeps Thaumcraft's vegetation and ores out of a biome (aura nodes still form) |
| the block tag `thaumcraft:portable_hole_blacklist` | blocks the portable hole focus will not open (the original `portableHoleBlackList`, now data) |

## Loot bags

`ThaumcraftApi.addLootBagItem(stack, weight, bagTypes...)` adds an entry to Thaumcraft's loot bags; bag types are 0
common, 1 uncommon, 2 rare (the example adds its shard to the first two; see [recipes](recipes.md#crafting-extras)).

## Felling trees

`ThaumcraftApiHelper.isWoodLog(level, pos)` and `breakFurthestBlock(level, pos, block, player)` are the two helpers the
Axe of the Stream used (`Utils.isWoodLog`, `BlockUtils.breakFurthestBlock`). The example add-on fells a tree when a log is
broken with its amethyst core in the hand:

<!-- snippet:felling -->
```java
// Breaking a log with the amethyst core in the hand fells the whole tree, the way the Axe of the Stream did. One call
// of breakFurthestBlock breaks ONE block (the far end of its walk) and wears no tool; the loop, its limit and the
// permission check are the caller's. The last call of the loop breaks the clicked log itself, so the break is cancelled.
PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
    if (!(level instanceof ServerLevel server) || player.isShiftKeyDown() || !player.getMainHandItem().is(AMETHYST_ROD)
            || !ThaumcraftApiHelper.isWoodLog(server, pos) || !server.mayInteract(player, pos)) return true;
    for (int logs = 0; logs < 64 && ThaumcraftApiHelper.breakFurthestBlock(server, pos, state.getBlock(), player); logs++) { }
    return false;
});
```
<!-- /snippet -->

- `isWoodLog` is the rule of the walk, not a tree detector: a block of the `#minecraft:logs` tag counts, so do leaf blocks,
  the plain oak log does not (see `docs/audit/KNOWN_DEVIATIONS.md`, section 9). An unloaded position is no log.
- `breakFurthestBlock` breaks **one** block per call: the end of a greedy walk, not the farthest log of the tree. The walk
  starts at `pos` and repeatedly steps to the first cell within two blocks, scanned x from -2 to +2, y from +2 down to -2,
  z from -2 to +2, that holds exactly `block`, is a log and is farther from `pos` than where it stands. It crosses gaps of
  a block and diagonals, can follow a neighbouring tree of the same wood, and never leaves 24 blocks sideways or 48 up and
  down. If no cell qualifies it breaks `pos` itself, **whatever block that is**: test `isWoodLog` first.
- The block drops its normal loot (rolled with the player's main-hand item; none for a creative or a null player) where it
  stood. The tool is **not** worn, no break event fires and nothing asks for permission, spawn protection or claims for the
  broken cell: that is the caller's job, as in the example.
- It answers true when it handed a breakable, non-air block to the harvester and false for a client level, an unloaded
  start, an air start or an unbreakable end, so `while (breakFurthestBlock(...))` ends when the start cell is air; put a
  limit on it anyway. Server thread only (`IllegalStateException` elsewhere): the walk keeps static state, as in the original.

## Client effects: particles

Particles are drawn on the client. A 1.7.10 add-on drew Thaumcraft's through `Thaumcraft.proxy`, whose server-side twin did
nothing; `thaumcraft.api.client.ThaumcraftClientFX` is that proxy, with a `Level` first:

| 1.7.10 `Thaumcraft.proxy` | `ThaumcraftClientFX` | What it draws |
|---|---|---|
| `sparkle(x, y, z, size, color, gravity)`, `sparkle(x, y, z, color)` | `sparkle(level, x, y, z, size, type, gravity)`, `sparkle(level, x, y, z, type)` | a glittering quad for 18 ticks, `type` 0 to 7 (violet, yellow, blue, green, red, black, white, cyan); drawn on a roll, four in six at "all" particles |
| `burst(world, x, y, z, scale)` | `burst(level, x, y, z, size)` | the white puff of the wisp |
| `wispFX`, `wispFX2`, `wispFX3` | `wisp(level, x, y, z, size, red, green, blue)`, `wisp(level, x, y, z, size, type, shrink, noClip, gravity)`, `wisp(level, x, y, z, tx, ty, tz, size, type, shrink, gravity)` | a mote of the wisp in any colour, in a primal colour (`type` 0 to 5), or flying towards a point |
| `blockRunes(world, x, y, z, r, g, b, duration, gravity)` | `blockRunes(level, x, y, z, r, g, b, duration, gravity)` | one rune glyph; **`x, y, z` is a block corner, the rune appears half a block from it on every axis** |
| `beamCont(world, player, tx, ty, tz, type, color, reverse, endmod, input, impact)` | `beam(level, player, to, rgb, type, reverse, endScale, impact)`, `beam(level, owner, from, to, rgb, type, reverse, endScale, impact)` | the continuous beam of the excavation focus, from the tip of the player's wand (`wandTip(player)`) or from a point you give; call it every tick, there is no `input` handle (and mind the phase of the call, below) |
| `smokeSpiral(world, x, y, z, radius, start, miny, color)` | `smokeSpiral(level, x, y, z, radius, start, minY, rgb)` | **one** puff of smoke that travels along a spiral; the ring of the Sword of the Zephyr is 5 calls and the awakening warden's 33, each with a random `start` angle, so loop |
| `essentiaTrailFx(world, x, y, z, tx, ty, tz, count, color, scale)` | `essentiaTrail(level, from, to, phase, rgb, scale)` | one mote of essentia between two blocks |
| `particleCount(base)` | `particleCount(level, base)` | 0 at "minimal", `base` at "decreased", `2 * base` at "all" |

<!-- snippet:client-fx -->
```java
// While the amethyst core is in the hand: a sparkle at the hand every tick (drawn with the chance and the particle setting
// of the original), a rune on the block under the feet twice a second, and a beam from the wand tip to the block looked
// at while use is held. Every call takes the Level and draws only on a client level; a beam must be called every tick,
// it ends a few ticks after the last call. Thaumcraft steps its beams (and counts the glint of a beam down) at the end of
// the client tick, so the calls are made at the START of the tick, before that step, as the originals' item ticks were.
ClientTickEvents.START_CLIENT_TICK.register(client -> {
    var player = client.player;
    if (player == null || !player.getMainHandItem().is(ExampleAddon.AMETHYST_ROD)) return;
    var level = player.level();
    Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.3, 0);
    ThaumcraftClientFX.sparkle(level, hand.x, hand.y, hand.z, 1.0F, 2, 0.0F);
    if (player.tickCount % 10 == 0) {
        // The position of blockRunes is a block corner: the rune appears half a block from it on every axis, so y + 0.7
        // puts it 1.2 above the bottom of the block under the feet, just over its top face.
        var feet = player.blockPosition().below();
        ThaumcraftClientFX.blockRunes(level, feet.getX(), feet.getY() + 0.7, feet.getZ(), 0.6F, 0.2F, 0.9F, 20, -0.02F);
    }
    if (client.options.keyUse.isDown() && player.pick(8, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
        ThaumcraftClientFX.beam(level, player, hit.getLocation(), 0x9933FF, 2, false, 1.0F, 5);
    }
});
```
<!-- /snippet -->

- **Client only, and safe to call from shared code.** A call with a server level, or with `null`, draws nothing and does not
  fail, as the 1.7.10 `CommonProxy` did not; the class names no client class in its signatures and loads on a dedicated
  server. A call with a client level must come from the client thread (a block's `animateTick`, a client tick, a render
  callback), otherwise it throws `IllegalStateException`: the particle engine is not thread safe.
- **Arguments are checked first, on every side.** A non-finite number, a coordinate outside the world, a size of zero or less
  (or over 16), a type outside its range (a beam texture of 4 or more, an end scale over 8, an impact over 5), a rune or spiral
  outside its limits throws `IllegalArgumentException`, so a mistake shows in a server-only test too. The ranges are in the
  Javadoc of each method. A colour component (`wisp`, `blockRunes`) may be any finite number and is clamped to 0 to 1: the
  original packed it into a byte unchecked, and a component of 2 spilled into the next channel.
- **The beam's glint counts down, so the phase of the call matters.** Thaumcraft steps its beams at the end of every client
  tick and the step lowers `impact` by one, as the particle of the original did. Called before that step (an item's or
  entity's client tick, a block's tick, a network task, `ClientTickEvents.START_CLIENT_TICK`, as the example does) the glint is
  drawn with `impact - 1`, which is how the original's foci looked; called after it (an `END_CLIENT_TICK` listener registered
  after Thaumcraft's, a render callback) it is drawn with the full value, one step larger. Pick one phase and keep to it.
- **A smoke spiral is one puff.** `smokeSpiral` (and its server twin) draws a single billboard that travels along the spiral,
  not the ring. The Sword of the Zephyr calls it 5 times and the awakening eldritch warden 33 times, each with a random
  `start` of 0 to 359, which is what makes the ring: do the same in a loop.
- **The particle setting** thins the sparkle and hides the beam, as in the original; the other effects draw what you ask for.
  Loop with `particleCount` where the original did.
- **The server twins** `ThaumcraftFX.sparkle`, `burst`, `blockRunes` and `smokeSpiral` ask every player within 32 blocks of
  the point to draw the effect (server thread; the client draws it through the very method above); `ThaumcraftFX.essentiaTrail`
  and `blockSparkle` already existed. Use them when the logic that wants the effect runs on the server.
- **Not provided:** the homing wisp (`wispFX4`), `beamPulseFX`, `bolt`, `arcLightning`, `crucibleBubble`, `blockWard`, the infusion
  particles and the returned beam object of `beamCont`. The beam of a `Player` starts at `wandTip(player)`, the point the original
  worked out and the excavation focus sends (`eye + (-cos(yaw) * 0.066, -0.06, -sin(yaw) * 0.04) + 0.3 * look`, yaw in radians),
  the same for every viewer: it does not follow the hand model of the first or third person view. The form with an entity takes
  the start from you.
- **Seen on a frame: no.** The argument rules, the geometry rules and the server senders are covered by `ClientFxApiTest`,
  `ClientFxPayloadTest`, `ClientFxParticleRulesTest` and the GameTest `clientFxServerTwinsCheckAndSendOnTheServerThread` (which
  reads the real bytes the sender writes for a player in range and shows a player out of range gets none). The particles
  are the ones Thaumcraft's own effects use; how they look from a call of yours was not inspected, and the client half of the
  server twins was not exercised by a connected client.

## Aspect tags in a screen

`thaumcraft.api.client.AspectRenderer.draw(graphics, aspect, x, y, amount, bonus)` draws the tag of an aspect the way
Thaumcraft's screens do (1.7.10 `UtilsFX.drawTag`). Client only: it names `GuiGraphics`, so call it from a screen, a HUD
callback or other client code. The example draws the aspects of the held item along the top of the screen:

<!-- snippet:aspect-tags -->
```java
// The aspects of the item in the main hand, as tags along the top left of the screen: sixteen pixels each, the amount
// in the bottom right corner of the tag (AspectRenderer.draw takes the position of the top left corner).
HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
    Minecraft client = Minecraft.getInstance();
    if (client.player == null || client.level == null || client.options.hideGui) return;
    AspectList aspects = ThaumcraftApiHelper.getObjectAspects(client.player.getMainHandItem(), client.level);
    int x = 8;
    for (Aspect aspect : aspects.getAspectsSorted()) {
        AspectRenderer.draw(graphics, aspect, x, 8, aspects.getAmount(aspect), 0);
        x += AspectRenderer.TAG_SIZE + 2;
    }
});
```
<!-- /snippet -->

The geometry, in GUI pixels from the top left corner `(x, y)` you pass, is a contract (`AspectRenderer` has a constant for each
number and a unit test pins them):

| Part | Where |
|---|---|
| the tag | a 16 by 16 square: the glyph of the aspect tinted with its colour (an `Aspect` always has a texture location; if the file is missing the square shows the game's missing-texture checkerboard) |
| `amount` | an `int` above 0: half size, white with a shadow, its right edge on the right edge of the tag (`x + 16`) and its top at `y + 11`, in front of the tag and of an item drawn there; no `+`, no separators |
| `bonus` | above 0: the research table's sparkle, a 16 by 16 frame over the top left corner (`x - 4, y - 4`), one of 16 frames by the tick of the player; above 1 also the number at half size centred on `x + 4` with its top at `y + 3` |

`amount` is a whole number. The original `drawTag` took a `float` and showed up to two decimals (`DecimalFormat("#######.##")`
in the audited legacy source), so a fraction of an essentia showed there and does not here; the top of the number is the whole
pixel `y + 11` where that source starts it at `y + 11.5` (recorded in `docs/audit/KNOWN_DEVIATIONS.md`, section 9).

The renderer does not clip, scale to the screen or show a tooltip: the hover area is the 16 pixel square, and a larger tag is
a scaled pose stack around `(x, y)`. Blending is left enabled and the colour is reset to white. As with the particles,
the numbers are tested and the pixels were not inspected on a frame.

## Not public API

- Golems had no API in 1.7.10 and have none now; `ThaumcraftEntities.GOLEM` only recognises the entity.
- The Thaumcraft 6 shaped classes of `thaumcraft.api` (`theorycraft`, `ResearchEntry`/`ResearchStage`,
  `ScanningManager`, `IWandFocus`) are compatibility shells for add-ons compiled against 1.12; they are not wired into
  Thaumcraft 4 gameplay. Use the TC4 classes in these guides.
- Everything outside `thaumcraft.api` (`thaumcraft.common`, `thaumcraft.client`) may change without notice.
