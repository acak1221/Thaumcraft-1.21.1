/**
 * What add-ons read from, and change in, four Thaumcraft machines: the Arcane Ear, the Brain in a Jar, the
 * Deconstruction Table and the Arcane Bore.
 *
 * <p>Not a 1.7.10 package. The ComputerCraft and OpenComputers peripherals of Thaumic Tinkerer read the public fields of
 * the internal block entities ({@code TileSensor.note}, {@code TileJarBrain.xp}, {@code TileDeconstructionTable.aspect},
 * {@code TileArcaneBore.hasFocus} ...); the block entities of this port implement these interfaces instead, so an add-on
 * asks {@code level.getBlockEntity(pos) instanceof IArcaneEar ear}. Each interface holds exactly what those peripherals
 * read or wrote. Thaumcraft's own block entities are the only implementations; Thaumcraft never asks an add-on's block
 * entity for them.</p>
 *
 * <p>The getters read the block entity's own state and change nothing; on a client they read the copy the server syncs.
 * Call them on the thread that owns the level (the server thread for a server block entity).</p>
 */
package thaumcraft.api.machines;
