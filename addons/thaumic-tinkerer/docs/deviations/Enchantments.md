# Enchantments: deviations from Thaumic Tinkerer 1.7.10

Group Enchantments: the fourteen enchantments (Ascent Boost, Flaming Touch, Disintegrate, Dispersed Strikes, Final Strike,
Focused Strikes, Pounce, Quick Draw, Shatter, Shockwave, Slow Fall, Tunnel, Valiance, Vampirism), `ModEnchantmentHandler`,
the Enchanter's table of costs and rules (`EnchantmentManager`), the Osmotic Enchanter (block, block entity, menu, the two
packets, screen, renderer) and the fourteen research entries of the enchantments. Specification: the 1.7.10 sources in
`/home/user/gtnewhorizons/thaumictinkerer` (commit in `NOTICE.md`). Everything not listed here is ported as it was:
research keys, coordinates, aspects and parents, the costs of all 40 table rows, the formulas and their quirks (section 11),
the pillar rules, the recipe and the hardness of the Enchanter. Requests to the API and to the lead are in
`docs/api-requests/Enchantments.md`.

Reading key: **forced** means 1.21.1 cannot do it the old way; **fix** means the original crashed, lost data or was
exploitable; **choice** means a free decision of the port; **limit** means something the port cannot reproduce exactly.

None of this has been seen running: the group was written and compiled (`tools/compile.sh`, with and without `-g`) and its
JUnit tests were run by hand with a JUnit launcher, but no game, no Gradle build and no GameTest was run. Every statement
about what happens in the game below is a reading of the code, not an observation. Section 12 lists what is unverified.

## 1. The enchantments are data (forced)

| 1.7.10 | 1.21.1 |
|---|---|
| `Enchantment` subclasses with numeric ids from the config (220 to 225 and 166 to 173) | one JSON file per enchantment, `data/thaumictinkerer/enchantment/<name>.json`; identity is the registry key `thaumictinkerer:<name>`. `LibEnchantIDs` keeps the 1.7.10 field names but holds `ResourceKey<Enchantment>`; the ids of the config are gone |
| `EnchantmentMod(id, maxLevel, type)`, weight 0, `canApplyAtEnchantingTable = false` | `max_level` of the file (Ascent Boost 4, Slow Fall 3, Flaming Touch 1, Disintegrate 1, Quick Draw 2, Vampirism 2, the other seven 5), weight 1 and `effects: {}`; the enchantments are in none of `in_enchanting_table`, `tradeable`, `on_random_loot` (so, as in 1.7.10, only the Enchanter gives them), and they are in `minecraft:treasure` |
| `EnumEnchantmentType` (weapon, digger, bow, armor_legs, armor_feet) and `canApply` overrides | the item tags `data/thaumictinkerer/tags/item/enchantable/<name>.json`: `#minecraft:swords` (Vampirism, Final Strike, Valiance, Focused and Dispersed Strikes), `#minecraft:pickaxes/shovels/axes` (Shatter, Tunnel, Disintegrate), `#minecraft:leg_armor` (Ascent Boost, Pounce), `#minecraft:enchantable/bow` (Quick Draw), explicit boot lists (Slow Fall, Shockwave), explicit axes plus `#minecraft:pickaxes/shovels` (Flaming Touch). Items of Thaumcraft and of this add-on are listed with `required: false` |
| `canApplyTogether` of each enchantment | `exclusive_set` of the file, and the rules of `EnchantmentManager` (section 5); both are asked by the Enchanter, only the exclusive sets by an anvil |
| `canApply` excluding the Elemental Axe (Flaming Touch), the Cursed Spirit's Blade (Vampirism), the gem boots (Slow Fall, Shockwave) | the tags simply do not list them (the gem boots are in `#minecraft:foot_armor`, so the boot tags are explicit lists); `EnchantmentManager.allowsItem` repeats the exclusion in code, for a data pack that adds them |

The names are the lang keys `enchantment.thaumictinkerer.<name>` of the converted 1.7.10 lang files (checked by
`EnchantmentResourcesTest` for en_us and ru_ru). No new lang key was needed, so there is no `lang-extra/enchantments.*.json`.

Consequences kept in mind: Mending, Sweeping Edge, Depth Strider, Soul Speed and the other enchantments of later versions
are not in the table (the table is the 1.7.10 one, section 4), so the Enchanter does not offer them. The numeric ids of
1.7.10 are not migrated; neither are 1.7.10 items or worlds.

## 2. Damage: Pounce, Final Strike, Valiance, Focused and Dispersed Strikes (forced, workaround)

1.7.10 changed `event.ammount` in `LivingHurtEvent`. Architectury's `EntityEvent.LIVING_HURT` can only veto a hit (and a veto
makes `Player.attack` treat the blow as a miss), so the amount cannot be changed there. `AttackBoost` instead arms the
attack just before it runs: from `PlayerEvent.ATTACK_ENTITY` (and from `AttackBoost.beforeAttack`, for attacks that do not
come from the network) it works out the factor of the enchantments, solves the attack formula of 1.21.1 for the base damage
that gives that factor and puts a transient `ADD_MULTIPLIED_TOTAL` modifier on the player's `ATTACK_DAMAGE`. The modifier is
removed by the `LIVING_HURT` event of the victim, and at the latest by the next tick of the player.

- **The numbers are the original's**: Pounce `x(1 + .25 L)` when the block under the feet is air, Final Strike `x3` when
  `L > nextInt(19)`, Valiance `x(1 + .1 L)` below half health, Focused Strikes `/2` then `+= .5 * max(s, 5) * amount * L`,
  Dispersed Strikes `x(1 + .6 L + s/5)` then `/(1 + .25 L + 2.5 s)` (integer division in `s/5`), in this order, with the
  counter `s` of the original (section 11). The factor multiplies the damage that reaches the victim, before armor, as
  `LivingHurtEvent` did.
- **Players only (limit).** The original applied the factor to any living attacker with an enchanted item in the hand;
  the port only to players. A zombie with an enchanted sword (only possible with commands or other mods) is not boosted.
  Vampirism is not affected: it works for any living attacker.
- **Not for attacks that do not go through `Player.attack`.** Arrows, thorns and the like were boosted by the original when
  the shooter's held item had the enchantments; they are not. The Animation Tablet's fake player calls `attack` without the
  network event: `AttackBoost.beforeAttack(player, target)` and `afterAttack(player)` have to be called around its attack
  (request to the lead in `docs/api-requests/Enchantments.md`, section 5). Until then a tablet does not get the factor.
- **The counter moves when the attack starts**, not when the damage lands, so a blow that the victim's invulnerability frames
  swallow still advances the counter of Focused and Dispersed Strikes.
- **A critical hit** multiplies the boosted damage like any other (the attribute is solved against the formula with the
  critical factor in it).

## 3. Vampirism (choice)

Heals the attacker by the level (any living attacker, as in 1.7.10) and plays `thaumcraft:zap` at the victim, from
`EntityEvent.LIVING_HURT`. That event is posted at the start of `LivingEntity.hurt`, before the game decides whether the blow
deals damage; 1.7.10's `LivingHurtEvent` came after. To stay close to "healed when damage was dealt" the port does not heal
when the amount is zero or less, when the victim is dying or invulnerable to the source, or when the victim is inside its
invulnerability frames (`invulnerableTime > 10`), where a blow normally deals nothing. A blow that is stronger than the one
that started the frames and would deal the difference is not counted either.

## 4. Movement and shooting enchantments (forced, workarounds)

- **Ascent Boost.** There is no jump event. The client looks at the tick of its own player: a tick that began on the ground,
  with the jump key held, and ended in the air moving up is a jump; the rest of the multiplied jump (`(L + 2) / 2`, not while
  sneaking) is added to the motion that is left, and the first tick's rise is moved at once, so the path is the one of the
  multiplied jump (`EnchantmentClientHandler`). The server does not simulate a player's jump (it checks the positions the
  client sends); a server that is stricter than vanilla about jump height would refuse it. Not seen in play.
- **Slow Fall.** On the client (which simulates the fall of its player) the vertical motion is divided by `1 + .33 L` and
  the fall distance reduced by `L/3` to at least 2.9, exactly as the original, with the cloud particle. The server
  does not simulate the fall: it counts the fall distance from the client's reports, has no motion to look at, and so reduces
  the fall distance alone when it is 2.9 or more, the player is not sneaking and not on the ground (`PLAYER_PRE`). Remote
  players do not show the cloud particles. Highest level on the four armor slots, as before.
- **Shockwave.** There is no fall event. At the end of the tick of a server player (`PLAYER_POST`) the fall distance of the last
  tick in the air is remembered; the first tick on the ground is the landing. A fall of more than 3 blocks hurts every
  other living entity in the box of 10 blocks to every side by `.1 L d` fall damage, as before. Players that can fly
  (`mayfly`) do not trigger it, as `EntityPlayer.fall` did not run for them in 1.7.10. A fall that ends in water or by a teleport has
  its fall distance reset and is not a landing (as far as the code of 1.7.10 shows, its fall event did not fire either).
- **Quick Draw.** 1.7.10 lowered `itemInUseCount` while a bow was drawn; the draw clock of 1.21.1 cannot be written. When the
  arrow enters the world (`EntityEvent.ADD`) its speed is scaled to what the longer draw would have given, using the same
  simulation of the original's per-tick loop (`EnchantmentFormulas.quickDrawBonusTicks`); a full draw also makes it a
  critical arrow. **Limits:** the bow does not look drawn any faster (the pose and the sound come from the real clock), and a
  draw so short that the game's own minimum power (0.1) is not reached releases no arrow at all, because the game decides that
  before the arrow exists.

## 5. Rules of the Enchanter (choice)

`EnchantmentManager.rules` holds the `canApplyTogether` rules of the fourteen enchantments as they were (Slow Fall: not
Feather Falling; Flaming Touch and Disintegrate: only Unbreaking and Repair; Dispersed and Focused Strikes: Unbreaking,
Repair, Knockback and Looting; Final Strike and Valiance: Unbreaking, Repair, Sharpness and Smite; Quick Draw: not Punch;
Shatter: not Efficiency and Disintegrate; Tunnel: not Shatter and Efficiency; Shockwave: not Feather Falling and Slow Fall;
Vampirism: not Fire Aspect and Knockback). 1.7.10 asked both enchantments of every pair, so a rule of an enchantment that is
already chosen also forbids a new one; `rulesForbid` does the same. The exclusive sets of the JSON files say the same for an
anvil, and `EnchantmentResourcesTest` checks that the two agree for every pair of enchantments that fit the same kind of item
(swords, tools, boots, bows, leggings), Mending and Sweeping Edge included although the table does not list them.

The table has the 40 rows of `EnchantmentManager.initEnchantmentData` (24 vanilla enchantments, Haste and Repair of
Thaumcraft, the fourteen of this group), with their costs `level 1 aspects x level x (1 + .2 level)`, cut to whole numbers
per aspect, and their research. The research of the TT enchantments from Focused Strikes to Pounce in the table is
`TTENCH_VAMPIRISM` in the original, not the entry of their own name; this was kept (it is a copy-and-paste habit of 1.7.10).
Lure's and Luck of the Sea's `BEAST` cost is never charged because the Enchanter only collects the six primals, as before.

**Button order (choice).** The original listed the buttons in the iteration order of a `HashMap<Integer, ...>` keyed by the
enchantment ids, which for the small ids of 1.7.10 was close to ascending id. The port lists them in the order of the table
(vanilla armor, sword, tool and bow enchantments, Lure, Luck of the Sea, Haste, Repair, then the fourteen). At most sixteen are
shown, in one or two rows of eight, as before.

## 6. The Osmotic Enchanter

- **Block.** `BaseEntityBlock`, hardness 5.0, blast resistance 1200 (the 2000 of 1.7.10 in today's scale, the number of the
  enchanting table), stone sound, needs
  a pickaxe of any tier (`mineable/tile_blocks/pickaxe`, no `needs_*` tag), drops itself and drops the tool and the wand it
  holds when broken. The box is 12 pixels high like the enchanting table (`setBlockBounds(0, 0, 0, 1, .75, 1)`), the model is
  that box with the three original textures (`textures/block/enchanter_0..2.png`), `noOcclusion`.
- **Opening.** A right click opens the menu with or without an item in the hand, as `onBlockActivated` did. The screen is a
  `MenuType` (`ContainerEnchanter`, an extended menu that carries the position) instead of a GUI id.
- **Slots.** `SlotTool` (one non-stacking damageable item, no book; locked while it works) and `SlotWand` (a wand, not a
  staff, whose cap's base cost modifier is at most one) are the 1.7.10 slots. The block entity is a `WorldlyContainer` with no
  accessible face, so hoppers never touch it. Shift-click follows `transferStackInSlot`: books are refused, a wand goes to the
  wand slot, a tool to the tool slot, anything from the two slots goes to the inventory.
- **Choice of enchantments is kept on the server and sent with the block entity update packet.** The keys changed:
  `enchants` is a list of enchantment ids as strings (1.7.10 `enchantsIntArray` held numeric ids), `levelsIntArray`,
  `totalAspects`, `currentAspects`, `working` and `Items` keep their names; the slot list is the `ContainerHelper` format with
  1.21.1 stacks. A tag where the two lists differ in length is mended on loading (levels are cut or filled with 1).
- **The work runs on the server only (forced).** 1.7.10 ran `updateEntity` on both sides, so the client counted
  `currentAspects` itself, with its own random choice of aspect, and played the particles. The port ticks on the server
  and sends the state every other tick of the work, and at once for the last step, so the bars on the screen move in steps
  of two ticks.
- **Vis.** One whole vis per tick from the wand: an aspect that the job still needs and that the wand holds at least 100 of
  (that is one vis) is chosen at random, `WandHelper.consumeAllVisCrafting(wand, null, 1 of the aspect, true)` pays
  it (with the discount of the wand's cap, as the original's private copy of the method did), and `currentAspects` grows
  by one. The original ignored a failed payment; the port counts the unit only if the payment succeeded (it cannot fail with
  a slot that only takes caps that do not raise the cost). The pillar used for the effect is the one with the index of the
  chosen aspect among the aspects that could be paid, a quirk of the original that was kept.
- **Pillars.** Within 4 blocks horizontally, at the height of the table: two to twelve Obsidian Totems then a Nitor; the
  first six found in the order x then z. A field of fewer than six stops the work and loses the vis paid so far (the
  vis taken from the wand stays taken).
- **Effects (A8).** The essentia trail is `ThaumcraftFX.essentiaTrail` from the table to the pillar; the two magic runes of
  `Thaumcraft.proxy.blockRunes` are vanilla `ENCHANT` particles around the pillar's top and above the table (`docs/api-requests/Enchantments.md` section 7); the
  sounds are `thaumcraft:egidle` (one in five of the effects, at the pillar) and `thaumcraft:wand` when the job is done.
- **Packets (fix / hardening).** The payloads carry the block position and the enchantment's registry id. The server
  resolves the block entity only if the sender has the menu of exactly that enchanter open (the original trusted any
  position), refuses an enchantment that is not in the table, refuses to add to a tool that is enchanted already, and `start`
  needs a tool in the slot and clears `currentAspects` (the original kept the vis of a stopped job, so a job could be
  started half paid). Level changes clamp to `[1, max level]` of the registry's data as before.
- **Research gate.** `canEnchantmentBeUsed` is `ThaumcraftApiHelper.isResearchComplete(player, key)` for the table's research,
  on the server when the packet arrives and on the client when the buttons are listed.
- **Recipe and research** are the original's: the infusion recipe (instability 15, Magic 50, Energy 20, Eldritch 20, Void 20,
  Mind 10; enchanting table in the middle, five Obsidian Tiles, two Thaumium Ingots, Spellbinding Cloth) and the entry
  `ENCHANTER` (5, 4), complexity 5, parent `SPELL_CLOTH`, pages 0, 1, 2 and the recipe.

## 7. The screen

`GuiEnchanting` and its four buttons follow `GuiEnchanting` and `GuiButton*` of 1.7.10 pixel for pixel (positions, the frames to
the right of the screen, the level buttons, the six aspect bars drawn with the aspect colour and their tooltips with
`current/total`). Differences:

- **Buttons are rebuilt when something changes** (the tool, the choice, the offered list, `working`), where 1.7.10 rebuilt them
  every tick. The list of offered enchantments depends on the player's research, which is read on the client.
- **Icons** are the 1.7.10 textures `textures/enchant/*.png`, 32 by 32 (35 files) or 512 by 512 (8 files, the ones of this
  add-on); the original drew either onto 16 by 16 with the texture coordinates 0 to 1. `EnchantIcons` reads the width from the
  PNG header once and does the same.
- **Names and tooltips** use the description component of the data (`enchantment.thaumictinkerer.<name>`; the vanilla and
  Thaumcraft enchantments their own) and the 1.7.10 keys `ttmisc.startEnchant`, `ttmisc.baseCost`, `ttmisc.clickToRemove`.
  The level numeral is `enchantment.level.<n>`, which exists up to 10 in the vanilla lang files.

## 8. Renderer

`RenderTileEnchanter` places the tool and the wand with the transforms of the 1.7.10 renderer (the tool lying on the table
at 0.7 scale, the wand tilted and floating with `cos(time/1000)` and `cos(time/500)`). The item renderer draws the model of
each item; 1.7.10 extruded the flat icon of the item with `ItemRenderer.renderItemIn2D`. The offsets were carried over by
arithmetic, not by looking at the result: they may need a correction when the first screenshot exists.

## 9. Mining: Shatter, Tunnel, Disintegrate, Flaming Touch (forced, workaround)

1.7.10 changed `event.newSpeed` in `PlayerEvent.BreakSpeed` and cancelled it for blocks the tool must not break. There is no
such event in Architectury. `BreakSpeedControl` keeps a transient modifier on the player's `BLOCK_BREAK_SPEED` attribute that
fits the block under the crosshair: once per tick, on the server, from the tool in the main hand and `player.pick(...)`. The
attribute is synchronised to the client like every attribute, so the client's prediction of the break and the server's
decision use the same speed.

- **One tick of lag** after the crosshair moves to another kind of block (the modifier is from the previous tick).
- **Factors** are the original's: Shatter `x3L` on blocks of hardness above 20 and `x.8` on the others; Tunnel `x(1 + .2 L)`
  when the player's yaw is within 50 degrees of the yaw remembered on the tool at the last break and `x.3` when it is not
  (nothing without a remembered yaw); Disintegrate on blocks of hardness 1.5 or less, and Flaming Touch on "wood", make
  the block break at once, any other block cannot be broken with the tool.
- **"Break at once"** is a factor of 1024 (the largest value the attribute allows; 1.7.10 used `Float.MAX_VALUE`). Together with
  the speed of the tool it is enough for every block of hardness up to 3 even in the air; harder wooden blocks (doors are 3)
  with a tool that does not match may take two ticks.
- **"Cannot be broken"** is a factor of 0 (no progress) plus a veto in `BlockEvent.BREAK` for a survival-mode player, so that
  a fake player (the tablet) or a client that claims to have finished is refused too. Creative players are never refused, as
  in 1.7.10 where creative did not ask for the speed.
- **Wear.** 1.7.10 called `damageItem(1)` every time the speed event fired, that is every tick of the dig; with a speed of
  `MAX_VALUE` that is once or twice per block. The port takes one more point of wear per block broken with Disintegrate or
  Flaming Touch.
- **Hardness** is read at the block's own position; the original asked `getBlockHardness(world, 0, 0, 0)` for Shatter, which
  for almost every block is the same number.
- **"Wood"** (`Material.wood`) is a block with a wooden sound type (wood, nether wood, cherry wood, bamboo wood, stem) or one of
  the vanilla tags for logs, planks, wooden fences, doors, trapdoors, slabs, stairs and signs. 1.7.10's wood also held the
  bookshelf, chests, the crafting table and jukebox (all have the wooden sound).
- **Tunnel's yaw difference is wrapped** to 0..180 degrees. 1.7.10's `rotationYaw` was unbounded and the original compared the
  raw numbers; the 1.21.1 server stores the player's yaw wrapped to [-180, 180], where a raw difference would call two nearly
  equal yaws on either side of the seam 360 degrees apart (fix).
- The modifier is on the player's attribute for the tools of this group only; other mods that add their own
  `BLOCK_BREAK_SPEED` modifiers multiply with it.

## 10. Research entries

The fourteen entries are registered through `ResearchHelper.queue` from the group's `ready()`: secondary, complexity 2, one
text page `"0"`, at the coordinates, with the aspects and the parents of 1.7.10 `ResearchHelper.initResearch` (Ascent Boost
6/2, Slow Fall 7/3, Flaming Touch 7/5, Disintegrate 6/6, Quick Draw 4/6, Vampirism 3/5, Focused Strikes 2/7, Dispersed Strikes
1/6, Final Strike 0/8 with two parents, Pounce 7/0, Shatter 5/8, Shockwave 9/2, Tunnel 9/6, Valiance 1/4). The Enchanter's
own entry comes with its block (section 6). The `PERIPHERALS` entry of the same 1.7.10 method belongs to the Machines group.

## 11. Values checked against the original on purpose

- Strike counter: a new target (or the first) gives 0; the same target gives `max(counter + 1, 5)`, so 5 from the second blow on
  (the original wrote `max` where `min` was meant; kept). Keys `TTEnchantLastTarget`, `TTEnchantLastTarget2` (the two halves
  of the UUID), `TTEnchantSuccessiveStrike` on the weapon, in its custom data.
- Focused Strikes divides by two *before* adding; the added term uses `max(counter, 5)`.
- Final Strike's roll is `nextInt(19)`; level 5 wins 5 of 19.
- Slow Fall: threshold `2.9` as a `double` comparison (`fallDistance >= 2.9`), reduced fall distance `max(2.9, d - L/3)`, the
  motion limit `-0.0784000015258789`, the divisor `1 + L * 0.33` (the float literal of the original).
- Shockwave: `d > 3`, box of 10 to each side, damage `.1 L d`, the faller excluded.
- Ascent Boost: `(L + 2) / 2`, not while sneaking.
- Quick Draw: `speedup = L / 6`, plus one when the clock is a multiple of `6 - L % 6`, never below 2; the simulation runs from
  72000.
- Disintegrate: hardness `<= 1.5`; Flaming Touch: wood only; Tunnel: 50 degrees, `1 + .2 L` and `.3`; Shatter: hardness
  above 20 gives `3 L`, otherwise `.8`.
- Costs: `i * (1 + i * 0.2)` times the level-1 aspects; the amounts are cut to integers per aspect.
- The Enchanter's blast resistance and hardness: 5.0 and 1200 (2000 in 1.7.10).

## 12. What is not verified

- Nothing of this group was run in the game: not the GameTests in `EnchantmentsGameTests` (they compile; their first run may
  show a wrong assumption of a test as well as a defect), not the screen, the renderer, the particles and sounds, the attribute
  mechanisms (damage, dig speed), the client jump hook, nor the multiplayer behaviour of the packets.
- The JUnit tests (`EnchantmentFormulasTest`, `EnchantmentManagerTest`, `EnchantmentResourcesTest`,
  `EnchanterPacketsAndIconsTest`; 52 tests) were compiled with `javac` and run with the JUnit launcher from the Gradle cache
  (all passed), not through Gradle. `EnchantmentResourcesTest` parses the fourteen JSON files with the real
  `Enchantment.DIRECT_CODEC` and the tag files with `TagFile.CODEC`.
- The attack arithmetic assumes the 1.21.1 `Player.attack` formula as read from the decompiled classes; a change of the formula
  in another Minecraft version breaks the factor silently (the unit test only covers the solver).
