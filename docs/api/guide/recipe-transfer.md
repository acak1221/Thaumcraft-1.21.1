# Recipe transfer (alpha.14)

Thaumcraft owns recipe arrangement. The Thaumonomicon's `+` button and the optional EMI
workbench handler send only a recipe identifier, the current menu ID and a maximum-fill
flag. The server resolves the recipe and moves existing ingredients; it never accepts
client-supplied stacks, creates output or spends wand vis during arrangement.

The native arcane workbench supports vanilla shaped/shapeless recipes, fixed native/API
arcane recipes and the canonical wand/sceptre variants shown by EMI. Shift requests as
many complete sets as fit. Only the player's main inventory/hotbar and the nine matrix
cells are sources; wand, armor and offhand are excluded. Previous matrix contents return
to the inventory. Missing ingredients, incomplete research or insufficient return space
leave both inventories unchanged.

From the standalone book, the server opens the closest reachable native arcane workbench.
From an already open compatible menu, transfer uses that menu. No storage addon is
required; recipes for crucibles, infusion and other machine types are not workbench fills.

## Addon menus

Implement `thaumcraft.api.crafting.transfer.RecipeTransferTarget` on the actual open
`AbstractContainerMenu`:

1. `recipeTransferContents()` exposes matrix cells 0–8.
2. `canTransferRecipe(actor)` validates the current actor/session, live block, reach and
   any external storage permissions. It is checked again before commit.
3. `recipeTransferSourceSnapshot(actor)` returns ordered copied source cells; the default
   is the player's 36 main inventory/hotbar cells. External sources may override it.
4. `applyRecipeTransfer(actor, plan)` compares all expected matrix/source cells, then
   atomically replaces both with the planned cells before publishing notifications.

`RecipeTransferPlan` defensively copies stack lists on construction and access. Return
false without changing anything when the expected state or authority is stale. Return
true once committed even if a later notification fails or retires the menu. A simulated
storage extraction is not sufficient: an external storage adapter must guarantee paid,
conserved ingredients and atomic commit itself.

Client handlers can call `RecipeTransferNetworking.request(recipeId, maximum)` with
their currently open compatible menu. The common server engine has no Refined Storage
dependency. It rechecks research and the actual recipe predicate before committing.
Custom dynamic recipe classes need an explicit supported template; a display-only
recipe does not authorize a transfer.
