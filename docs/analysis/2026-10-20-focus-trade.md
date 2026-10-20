# Equal Trade — original 4.2.3.5 contract

Oracle: `ItemFocusTrade`, `ServerTickEvents`, `ConfigRecipes:250`, read outside this repository.
Sneak remembers a block item; left click queues one swap, right click spreads through exposed
matching blocks with 26 neighbours and life `3 + Enlarge`. Architect selects the connected plane.
Base costs are terra/ordo/perditio 5 centivis each; Silk adds one centivis of all six primals.
One successful swap per level tick consumes one actual material and discounted vis, then delivers
the source's native Fortune/Silk drops without experience. Creative requires sufficient vis but
spends neither vis nor material. Held wand, slot, selection, source and protection are checked
again before payment. Callbacks cannot reuse the reserved material or prepaid wand charge.

Native adaptations: queue runs at level PRE instead of legacy server END; original block metadata
is represented by the selected block's item/default state. Copying modern WATERLOGGED/double-slab
world state would create resources and is deliberately excluded. Doors, beds and double plants
remain unavailable until their multiple-cell placement topology is supported. Native protection
uses Fabric interaction/break callbacks and the Architectury placement event; no bridge means refusal.
The original persisted selection now uses BlockState NBT inside the wand custom-data component.

`TradeRuntimeChecks` exercises native holder/selection, funding, callbacks, source drops, dry slabs,
creative behavior, real level-tick spreading and Architect selection. Its current result is pending
the integrated build. Client targeting/Architect visual parity and wider native placement are pending.
