/**
 * The client-only part of the Thaumcraft API: the particles that 1.7.10 add-ons drew through {@code Thaumcraft.proxy}
 * ({@link thaumcraft.api.client.ThaumcraftClientFX}) and the tag of an aspect in a screen
 * ({@link thaumcraft.api.client.AspectRenderer}).
 *
 * <p>Nothing here changes the world. {@code ThaumcraftClientFX} may be called from shared code (a server level is a no-op);
 * {@code AspectRenderer} names client classes and belongs in client code only.</p>
 */
package thaumcraft.api.client;
