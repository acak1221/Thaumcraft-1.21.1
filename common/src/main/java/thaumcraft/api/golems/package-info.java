/**
 * Read and edit Thaumcraft's golems from an add-on: what a golem is (material, core, accessories, health), where it is,
 * its home and its markers.
 *
 * <p>Not a 1.7.10 package: golems had no API in TC4, and add-ons such as Thaumic Tinkerer's Golem Connector read the
 * internal {@code EntityGolemBase} directly. {@link thaumcraft.api.golems.GolemApi} is the entry point; it answers
 * immutable {@link thaumcraft.api.golems.GolemView} snapshots and changes the home and the markers through the golem's
 * own code paths.</p>
 */
package thaumcraft.api.golems;
