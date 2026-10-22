package thaumcraft.assets;

import dev.architectury.registry.registries.RegistrySupplier;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The registry ids the port really registers, read from the registration classes by reflection.
 *
 * <p>Every registration class lives in {@code thaumcraft.common.config} and keeps its entries in
 * {@link RegistrySupplier} fields; the families that register in a loop (the resources, the shards,
 * the crystals, the golem pieces, the coloured candles) keep them in {@code Map<String,
 * RegistrySupplier<...>>} fields. Both shapes are read here, so a new registration is covered
 * without touching this file.</p>
 */
public final class RegistryInventory {
    /** The package that holds every registration class of the port. */
    private static final String CONFIG_PACKAGE = "thaumcraft.common.config";

    static {
        // The registration classes touch the vanilla registries (the sounds of an item, a block
        // of the game) while they fill their entries, so the game has to be bootstrapped first.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private RegistryInventory() { }

    /** Every registered item id, sorted. */
    public static List<String> itemIds() { return ids(Item.class); }

    /** Every registered block id, sorted. */
    public static List<String> blockIds() { return ids(Block.class); }

    /** Every registered entity type id, sorted. */
    public static List<String> entityIds() { return ids(EntityType.class); }

    private static List<String> ids(Class<?> wanted) {
        TreeSet<String> found = new TreeSet<>();
        for (Class<?> owner : registrationClasses()) {
            for (Field field : owner.getDeclaredFields()) {
                collect(field, wanted, found);
            }
        }
        return List.copyOf(found);
    }

    /** The registry entries of one field: a single supplier or a map of suppliers. */
    private static void collect(Field field, Class<?> wanted, TreeSet<String> found) {
        if (RegistrySupplier.class.isAssignableFrom(field.getType())) {
            if (holds(field, wanted)) found.add(id(field));
            return;
        }
        if (!Map.class.isAssignableFrom(field.getType())) return;
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) return;
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 2 || !(arguments[1] instanceof ParameterizedType value)) return;
        Type[] valueArguments = value.getActualTypeArguments();
        if (valueArguments.length != 1 || raw(valueArguments[0]) == null || !raw(valueArguments[0]).equals(wanted)) return;
        Object map;
        try {
            field.setAccessible(true);
            map = field.get(null);
        } catch (IllegalAccessException | RuntimeException unreadable) {
            return;
        }
        if (!(map instanceof Map<?, ?> entries)) return;
        for (Object entry : entries.values()) {
            if (entry instanceof RegistrySupplier<?> supplier) found.add(supplier.getId().getPath());
        }
    }

    /** True when the field holds a registry entry of the wanted type. */
    private static boolean holds(Field field, Class<?> wanted) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) return false;
        Type[] arguments = parameterized.getActualTypeArguments();
        return arguments.length == 1 && raw(arguments[0]) != null && wanted.isAssignableFrom(raw(arguments[0]));
    }

    /** The raw class of a type argument, also when the argument is itself parameterised. */
    private static Class<?> raw(Type type) {
        if (type instanceof Class<?> raw) return raw;
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) return raw;
        return null;
    }

    /** The registry id of a registry entry field. */
    private static String id(Field field) {
        try {
            field.setAccessible(true);
            RegistrySupplier<?> supplier = (RegistrySupplier<?>) field.get(null);
            return supplier.getId().getPath();
        } catch (IllegalAccessException e) {
            throw new AssertionError("cannot read " + field, e);
        }
    }

    /** Every class of the registration package, found on the test classpath. */
    private static List<Class<?>> registrationClasses() {
        List<String> names = new ArrayList<>();
        try {
            for (String file : ResourceTree.filesOnAnyRoot(CONFIG_PACKAGE.replace('.', '/'))) {
                if (!file.endsWith(".class") || file.contains("$")) continue;
                names.add(CONFIG_PACKAGE + "." + file.substring(0, file.length() - ".class".length()).replace('/', '.'));
            }
        } catch (Exception unreadable) {
            throw new AssertionError("cannot list the registration classes", unreadable);
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String name : names) {
            try {
                // Initialise on purpose: the registry entries are static fields of the
                // class, so the <clinit> that fills them has to run before they are read.
                classes.add(Class.forName(name, true, RegistryInventory.class.getClassLoader()));
            } catch (Throwable broken) {
                throw new AssertionError("cannot initialise the registration class " + name, broken);
            }
        }
        return classes;
    }
}
