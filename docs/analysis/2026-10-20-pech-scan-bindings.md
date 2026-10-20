# Native Pech scan binding

The newly registered `thaumcraft:pech` now resolves to original entity name `Thaumcraft.Pech` in `EntityAspectBindings`. This activates the existing original aspect rows without replacing the public registration list or introducing a native-specific reward formula.

Authority is external TC4 4.2.3.5 `common/config/ConfigAspects.java:84` through86. Its exact `PechType` BYTE predicates are:

| Saved type | Original complete aspect set |
| --- | --- |
| 0 | MAN2, MAGIC2, EXCHANGE2, GREED2 |
| 1 | MAN2, MAGIC2, EXCHANGE2, WEAPON2 |
| 2 | MAN2, MAGIC4, EXCHANGE2 |

The weapon reward belongs to type1 and the four-magic reward to type2, even though the native gameplay variants are named Forager/Mage/Stalker. Those original values are retained. No unconditional Pech aspect row exists, so other signed byte values receive no fallback reward. The real `EntityPech.addAdditionalSaveData` writes `PechType` with `putByte`; generic scan predicate handling reads that actual saved entity data.

`PechScanRuntimeChecks.run` creates three registered Pech entities, puts them in the native world index and selects their actual variant state through `setPechType`. It verifies each entity's saved BYTE, exact complete aspect set, distinct original persistent scan signature and native entity-ID session identity. Public scan DTO resolution reads the indexed entities; the production `completeScan` receiver pays the original cumulative RP and refuses duplicates without changing knowledge. An actual type3 entity is rejected. The fixture declares all parent aspects discovered with empty RP pools and restores knowledge/removes its entities afterward. It does not inject scan registrations, expected aspect output or rewards.

The test is supplied for the coordinator's subsequent compile/runtime run. It does not claim an inspected client or a separate held-thaumometer timing replay; that timing is covered by the existing general entity scan tests.

## Actual Pech focus item

Original `ConfigAspects.java:436` registers a complete direct tag for `new ItemStack(ConfigItems.itemFocusPech)`: MAGIC5, POISON5, ENTROPY5, ELDRITCH5 and WEAPON5, in that order. `ConfigItems.java:361` through362 identifies the original `ItemFocusPech`/`FocusPech` item; the native identity is the implemented `thaumcraft:focus_pech`. The original Minecraft1.7.10 `ItemStack(Item)` constructor bytecode delegates to count1 and metadata0, so this registration uses exact metadata0, without a wildcard.

The bounded early bindings resource adds only this original row436 and retains every previous source/hash/provenance field and registration order. Its existing provenance test separately asserts the new exact row, all five amounts, direct-versus-complex classification and metadata0; the historical ALL90 digest remains unchanged for the older rows. The resource now has99 rows,118 direct native identities and10 complex identities. No aspect tags are invented for clusters, Mana Beans, loot bags, the Focus Pouch or the Hellbat focus.
