package one.eim.randomcontrol;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static one.eim.randomcontrol.Utils.clazz;
import static one.eim.randomcontrol.Utils.sneakyThrows;
import static one.eim.randomcontrol.Utils.requireNonNullElseGet;

public final class Compatibility {
    private static final boolean hasRandomSource;
    private static final MethodHandle createRandom;
    private static final MethodHandle randomSetSeed;
    private static final Object sharedRandom;
    private static final MethodHandle craftEntityGetHandle;
    private static final MethodHandle entityGetRandom;
    private static final MethodHandle entitySetRandom;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        final @NotNull Class<?> entityClass = requireNonNullElseGet(
                clazz("net.minecraft.world.entity.Entity"), // >1.17.1
                () -> sneakyThrows(() -> Class.forName(Bukkit.getServer().getClass().getName() // <1.16
                        .replace("org.bukkit.craftbukkit", "net.minecraft.server")
                        .replace("CraftServer", "Entity")))
        );

        final @Nullable Class<?> randomSourceClass = clazz("net.minecraft.util.RandomSource");

        hasRandomSource = randomSourceClass != null;

        createRandom = hasRandomSource
                ? sneakyThrows(() -> lookup.unreflect(
                Arrays.stream(randomSourceClass.getDeclaredMethods())
                        .filter(m -> m.getReturnType() == randomSourceClass && m.getParameterCount() == 0 && Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
                        .findFirst()
                        .orElseThrow(() -> new ExceptionInInitializerError("Failed to locate RandomSource RandomSource.create() method"))
        ))
                : sneakyThrows(() -> lookup.findConstructor(Random.class, MethodType.methodType(void.class)));

        randomSetSeed = hasRandomSource
                ? sneakyThrows(() -> lookup.unreflect(
                Arrays.stream(randomSourceClass.getDeclaredMethods())
                        .filter(m -> m.getReturnType() == void.class && m.getParameterCount() == 1 && m.getParameterTypes()[0] == long.class && Modifier.isPublic(m.getModifiers()))
                        .findFirst()
                        .orElseThrow(() -> new ExceptionInInitializerError("Failed to locate RandomSource#setSeed(long) method"))
        ))
                : sneakyThrows(() -> lookup.unreflect(Random.class.getMethod("setSeed", long.class)));

        Field randomField;
        try {
            randomField = entityClass.getDeclaredField("random");
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError("Failed to find Random on Entity. No fields with type RandomSource/Random. Not Paper?");
        }

        randomField.setAccessible(true);

        sharedRandom = sneakyThrows(() -> entityClass.getField("SHARED_RANDOM").get(null));

        final Class<?> craftEntityClass = Objects.requireNonNull(
                clazz(Bukkit.getServer().getClass().getName().replace("CraftServer", "entity.CraftEntity")),
                "Can not find o.b.c.v.entity.CraftEntity - not Paper?"
        );

        craftEntityGetHandle = sneakyThrows(() -> lookup.unreflect(craftEntityClass.getDeclaredMethod("getHandle")));

        entityGetRandom = sneakyThrows(() -> lookup.unreflectGetter(randomField));
        entitySetRandom = sneakyThrows(() -> lookup.unreflectSetter(randomField));
    }

    public static void updateRandom(final org.bukkit.entity.Entity entity, final @Nullable Long seed) {
        try {
            final Object wrapped = craftEntityGetHandle.invoke(entity);

            if (entityGetRandom.invoke(wrapped) != sharedRandom) {
                return;
            }

            final Object random = createRandom.invoke();
            if (seed != null) {
                randomSetSeed.invoke(random, seed);
            }

            entitySetRandom.invoke(wrapped, random);
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to update random on entity " + entity.getName() + " " + entity.getLocation() + " " + entity.getUniqueId(), t);
        }
    }
}