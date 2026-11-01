# Add-on compatibility and fortress rendering — alpha.12

Porting an external TC4 add-on exposed missing native extension points and a visible fortress-model defect.

- Registered API crucible, arcane and fixed-output infusion views now participate in canonical object-aspect inference. Registration removal, readdition, order and relevant view values invalidate the recipe index. Custom infusion recipes opt in through `objectAspectRecipe()`; actor-dependent outputs and central-item patches remain excluded. No manual object tags substitute for recipe inference.
- `FocusUpgradeType` accepts explicit add-on registrations under unused stable IDs. The focal manipulator checks the real payer before admission and completes a fully paid upgrade through a single-use structural permit, without another null/offline-player research query.
- Fortress families can use their source durability and rarity through an additive constructor; armor may supply a source-specific ward share. Existing base defaults are unchanged.
- The existing explosive orb accepts the source add-on strengths up to5.1875 within a finite limit6; invalid runtime/NBT strengths remain rejected. The existing primal-wisp backend exposes a constructor-delegating factory for exact source velocity/lifetime bindings.
- `ModelFortressArmor.setPieceVisibility` now converts original `isHidden` to modern `visible`. Bare helmets hide infused goggles/masks; only the selected mask is shown; the proper two/three-piece ornaments appear.

The add-on is a separate project. Its code, textures, donor libraries and test mod are absent from this repository's production archives.

## Verification

Java21 common JUnit:2851 tests, zero failures/errors, five existing skips without donor JARs. Fabric build/archive isolation and559/559 fresh native GameTests pass. Python:155 tests, nine existing skips; resource validation and scope ledger pass. A test-only arcane-lamp fixture now waits for the real native lighting `POST_UPDATE` future with a bounded deadline, preserving all brightness/spread/removal assertions;130 accelerated test ticks were insufficient wall-clock time for the worker thread.

The separate Fabric add-on passes14 JUnit and37 native GameTests, including real API-recipe remainders, infusion component preservation, paid VisNet upgrades, effect ownership, flight persistence, source damage, teleport acknowledgement, consumable item events and original tree geometry. Its122 production classes compiled in the standalone project match the inspected development-client classes byte for byte. Rendered EN/RU acceptance and normal production-server loading are recorded in that separate project's verification report. Supplied fixtures do not establish a new survival acquisition or multiplayer playthrough.

After Fabric acceptance, NeoForge build/archive isolation,15JUnit and538/538 fresh native GameTests pass. The external add-on remains Fabric-only; the base checks do not establish a NeoForge add-on.
