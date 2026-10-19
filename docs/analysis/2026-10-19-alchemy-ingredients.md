# Alchemy infusion ingredient predicates — FINAL, 2026-10-19

Implemented source prerequisite for the [Alchemy6 contract](2026-10-19-golem-alchemy.md), **not compiled/tested or accepted content**. Changed only `common/src/main/java/thaumcraft/common/crafting/InfusionComponentPredicate.java`, `InfusionStackRecipe.java`, the existing `common/src/test/java/thaumcraft/infusion/InfusionComponentPredicateTest.java`, and this new note. `ItemJar.isEmptyIngredient` was read only; no Matrix/InfusionMatching/runtime/Entity/EssentiaTransfers/client/registry/recipe/resource or other-document changes.

## Explicit JSON / validation

For ROOT's separately owned recipe whose pedestal array is normal jar, water potion, water potion, water potion:
```json
"component_predicates": [
  {"component": 0, "empty_jar": true},
  {"component": 1, "water_potion": true},
  {"component": 2, "water_potion": true},
  {"component": 3, "water_potion": true}
]
```
This is a format fragment, **not a registered/placeholder Alchemy recipe**. Existing declarations such as `{"component":4,"damage":0}` remain ordinary exact native damage requirements in recipes with that index. Omitted flags default false; absent damage is not implicit zero.

- A declaration must specify damage or one true typed flag. Bare/false-only declarations, both typed flags, duplicate/out-of-range indices, negative explicit JSON damage and invalid declared ingredient variants are rejected. The existing4096 component/count/index bounds, nonnegative native integer damage range and damage<declared maximum checks remain. Scalar declarations are checked without item bootstrap; item compatibility is checked against every ingredient alternative during recipe/session predicate validation.
- **water_potion:** all declared alternatives must be exactly **Items.POTION**. Offered stock must be that item and its **entire POTION_CONTENTS equal `new PotionContents(Potions.WATER)`**. Missing/empty contents, another potion, effects, custom color, glass/splash/lingering bottles fail; no bottle consumption/reward behavior is added beyond ordinary pedestal consumption. Other unrelated item components are not a new equality restriction.
- **empty_jar:** all alternatives and offered stock must be **ItemJar backed by a non-void BlockJar**; offered stock must also pass unchanged **`ItemJar.isEmptyIngredient(offered)`**. Filled, labelled, malformed/unknown meaningful jar payloads fail. Harmless custom names/lore and empty-state payloads remain governed by that existing helper, not a new pristine-all-NBT policy.
- Damage is still an AND requirement when present, with unchanged declared native DAMAGE/max-damage validation. Normal potion/jar ingredient variants are not damageable, so adding damage to either typed requirement is invalid at ingredient validation; the scalar declaration codec alone does not initialize items to determine this. There is no general NBT/component predicate engine or new ingredient serializer/registry.

## Compatibility / selected ritual snapshots

- Paired record is now `InfusionComponentPredicate(Ingredient, Optional<Integer> damage, boolean waterPotion, boolean emptyJar)`. Existing **one-/two-argument** constructors default both flags false. Paired CODEC stores optional `water_potion`/`empty_jar`, both false by default; old selected-ingredient entries still decode with exactly their old restrictions.
- `Declaration(int component,int damage)` remains; `.damage()` is still **int**. New four-argument declaration uses **−1 only for internal absent damage with a true typed flag**, e.g. `new Declaration(1,-1,true,false)`. `optionalDamage()` converts that sentinel to empty. JSON uses an optional **nonnegative** damage codec, so even `damage:-1` alongside a true flag is rejected; encoding typed-only declarations omits damage. All-false/−1 declarations remain invalid.
- `InfusionStackRecipe.componentDamage(index)` returns empty for typed-only declarations. Its **six-/seven-/eight-argument constructors remain unchanged**. `selectedComponents()` validates indices and pairs all restrictions with the original component **before consumption**; initial recipe matching and the existing later Matrix selection/consumption both use that paired predicate's `matches`. No index drift or late rule lookup against a shortened component list.
- Recipe stream declarations now carry **index VarInt, damage VarInt (internal−1 allowed only as above), water boolean, jar boolean**, followed by the unchanged central input-damage encoding. Decode validates full declarations/recipe and retains existing allocation/count bounds. Both native login endpoints require the updated build; no cross-version packet compatibility is promised. No recipe/session schema version, journal or saved cast/state was introduced.
- **Central `input_damage`, its admission-only semantics, Matrix snapshots and `InfusionMatching.centralStillMatches` were not changed.** There is no new central pristine recheck during running-ritual retention. Legacy unflagged ingredients retain existing fuzzy-log/NBT behavior; damage-only Bore rules retain their previous meaning.

## ROOT book hook / remaining work

Public pure hook **`InfusionComponentPredicate.displayStack(ItemStack ingredientPreview)`** returns a fresh **count1 water potion with actual native WATER contents** when waterPotion is true; all other requirements return `ingredientPreview.copy()`. No mutation, stock transfer or client-class dependency. ROOT's book display should iterate **`recipe.selectedComponents()`** and pass each chosen ingredient preview through **`required.displayStack(preview)`** before icon/tooltip rendering; using `recipe.components()`/default potion previews alone cannot show the water requirement. Normal empty-jar previews already need no special transformation. No client display code was edited here.

Only the existing registry-independent declaration JUnit source was extended: typed JSON/NBT round trips, omission/default compatibility, invalid/conflicting flags, sentinel rejection and mixed-rule duplicate indices; existing negative/index/count cases retained. **Not run.** Native item matching, paired selected-session and recipe stream round trips, book output and integration remain unexecuted. No Gradle/compiler/Java/game/tests/Python/worlds/agents or smoke/proof framework was run/added. ROOT owns actual Fabric compilation after all author finals, recipe JSON/book integration and later runtime checks; this prerequisite registers no Alchemy item/recipe and is not worker/content completion or a PASS.
