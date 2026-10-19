# Basic golem model handoff — 2026-10-18

**Corrected source implemented and frozen; compilation/game visuals unverified.** This is only the model contribution to the first functional straw-golem unit, not a claim that the gameplay unit is complete. Only the new `common/src/main/java/thaumcraft/client/renderers/models/ModelGolem.java` and this note were written/revised. No pre-existing files, registration, bootstrap, entity/runtime/API, recipes/research, tests, Python, smoke tooling, assets, saves or source pins were edited. Other WIP golem sources/notes were not read; the completed `docs/analysis/2026-10-18-straw-golem-original.md` was read after the source-correction notification. No agents were spawned.

## Exact public contract

```java
public final class ModelGolem<T extends LivingEntity> extends EntityModel<T>
public ModelGolem(ModelPart root)
public static LayerDefinition createBodyLayer()
public void setActive(boolean active)
public void setCarrying(boolean carrying)
public void setBootupTimer(int bootupTimer)
public void setHealingTimer(int healingTimer)
public void setCombatTimer(int combatTimer)
public void setInactive(boolean inactive) // optional original menu-pause pose
public void prepareMobModel(T entity, float walkPhase, float walkSpeed, float partialTick)
public void setupAnim(T entity, float walkPhase, float walkSpeed, float ageInTicks, float headYaw, float headPitch)
public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay, int color)
```

Public final `ModelPart` fields retain the original names: `golemHead`, `golemBody`, `golemRightArm`, `golemLeftArm`, `golemRightLeg`, `golemLeftLeg`. Baked child names are respectively `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`. Root owns the layer location/registration and renderer; construct this model with that layer's baked root. No `EntityGolemBase` or new entity-specific API dependency exists.

The five primary pose setters match the root's stated scalar contract. Root feeds them **for each rendered entity, including zero/false values**, before normal native model preparation/setup:

```java
model.setActive(entity.getGolemCore() != -1);
model.setCarrying(!entity.getCarriedItem().isEmpty());
model.setBootupTimer(entity.getBootupTimer());
model.setHealingTimer(entity.getHealingTimer());
model.setCombatTimer(0); // Gather has no combat goal; use a real action countdown only if supplied.
```

These calls illustrate only the API names provided by root; the generic model itself never references them or a concrete entity type. `getCarriedItem()` is the root-provided snapshot, not an inventory mutation. Positive `bootupTimer` supplies the original head pitch in degrees without extra interpolation; negative values also select the dormant pose. `combatTimer` maps the original `getActionTimer()` countdown, not normalized native `attackTime`/`getAttackAnim()`; the latter cannot reproduce the original five-tick wave.

Optional `setInactive(boolean)` retains the active original's distinct menu-pause head-down pose while limbs remain animated. This is not the same as `setActive(false)`, which freezes all limbs for a coreless statue. It defaults to false; if used, root must also feed/clear it per entity using actual pause state. No new runtime getter is assumed. The previous `setActivationState`/`setActionTimer`/`setArmTimers` handoff API is superseded; no compatibility aliases or unused future-core framework remain.

`setHealingTimer` implements original primary-pass RGB `(0.5 + healing/10, 0.9 + healing/5, 0.5 + healing/10)`. Components are clamped like GL and encoded as a native packed per-vertex tint, modulating caller RGB while retaining caller alpha. No global GL color state leaks. **For a separate damage-overlay draw, root must supply healing timer zero**, as original damage pass2 does not apply this primary-skin tint; feed the real timer again for the next primary draw.

The native `prepareMobModel` override captures the legitimate render partial tick for countdown interpolation. Native living renderers call it before `setupAnim`; any direct model caller must do likewise. No partial tick is guessed from `ageInTicks`, no entity-specific getter is called, and there are no world/player/input lookups. `ageInTicks` is unused, as in the original normal animation.

## Original anchors and adaptations

Authority is the **active** `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/models/entities/ModelGolem.java`, selected by `ClientProxy:471` and `RenderGolemBase:60,78,82–89` per the completed original review. The duplicate older `models/ModelGolem.java` is **not authoritative**; this corrects the initial handoff citation. The port stays in the root-requested modern `thaumcraft.client.renderers.models` package. Geometry already matched the active source and was not changed by this correction.

- **Model lines 32–57:** normal `ModelGolem(false)` geometry, seven original boxes on a 128x128 layer. Normal part offset is Y30, legs Y48. Preserved exact UVs/boxes, mirrored left limbs, the left/right leg pivot asymmetry X5/X-4, and the waist's `0.5` cube deformation. This is not a vanilla humanoid/iron-golem substitute. The alternate `true` preview offset is outside this normal-entity model.
- **Lines 60–80:** model rendering owns the uniform **0.4 scale**. Root must not apply a second 0.4 scale to this mesh. Explicit draw order remains head, body, right leg, left leg, right arm, left arm. Native packed color/light/overlay replace global GL color state.
- **Lines 83–124:** original dormant/inactive/bootup head branches and yaw/pitch conversion by `57.295776`; period-13 triangular walking, right leg negative and left leg positive.
- **Lines 91–94:** original healing RGB is implemented in `renderToBuffer`, with native eight-bit packed-color quantization replacing global GL color state.
- **Lines 133–168:** for the plain Gather slice, shared action countdown precedes carrying, then walking. Shared action pitch is `-2 + 1.5 * triangle(combatTimer - partialTick, 5)`. Carry pitch is **-1 radian on both arms**. Walking arms are `(-0.2 + 1.5*triangle) * walkSpeed` on the right and `(-0.2 - 1.5*triangle) * walkSpeed` on the left. The exact triangle helper is `(abs(phase % period - period*0.5) - period*0.25) / (period*0.25)`, not cosine/sine animation. Dormant/ordinary/action/carry branches assign both arm pitches; unsupported independent-arm counters are not fabricated from native attacks.
- Original two-stage `setLivingAnimations`/`setRotationAngles` animation is consolidated in native `setupAnim`, retaining the dormant override and ordinary pose priorities. The normal primary model does not need an `advanced` flag; no speculative advanced/upgrades framework was added.

Original `RenderGolemBase` lines 78 and 285–289 select **`thaumcraft:textures/models/golem_straw.png`**. The retained `common/src/main/resources/assets/thaumcraft/textures/models/golem_straw.png` has a 128x128 PNG header and exactly matches the original JAR entry's SHA-256. It was not modified, rescaled, converted or UV-remapped. Material selection stays with root's renderer.

Entity-wide walking sway (original renderer 160–166), carried-item rendering (170–267), damage overlays, core/upgrade/accessory geometry, the core6-specific arm spread (model 125–128), separate period-20 arm counters for other actions (151–157), and other material/gameplay units are outside this owned model contribution and are not claimed complete. Renderer-owned item/layer transforms do not inherit the model's internal 0.4 pose because rendering balances its own push/pop.

## SHA-256 and verification boundary

| Input/output | SHA-256 |
| --- | --- |
| Corrected `common/src/main/java/thaumcraft/client/renderers/models/ModelGolem.java` | `86eb173e7281ed7cb93511a91c82e76052c22ce7935c293e4ca9e0ed57023755` |
| Authoritative active oracle `models/entities/ModelGolem.java` | `d098fc46204842bef84ff135e9d0c709ce219c58614d101603f1adcaf18551f7` |
| Oracle `entity/RenderGolemBase.java` | `57335bc2378c0517c79b42b66de36b54f8eb085807da773be57e6fd6598adb3f` |
| Retained straw PNG and original JAR entry | `22061ca098038af515241fbfe69d17178b41cecd4c8ac526c26820b2ab45273a` |

Readback/static comparison, texture hashing/header inspection and read-only cached 1.21.1 API bytecode inspection were performed. The native API confirms `EntityModel` preparation/setup and the 1.21.1 packed-color `renderToBuffer` signature plus the cube builder/deformation overloads. No Gradle, compiler, Java/game launch, test, smoke, world or visual run occurred. Root owns actual Fabric compilation after entity/layer/renderer integration; final gameplay checks remain deferred, not passed. Owned source is frozen at the hash above; no further edits after handoff.
