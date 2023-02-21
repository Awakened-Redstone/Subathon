package com.awakenedredstone.subathon.core.effect.process;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.Utils;
import com.google.common.reflect.ClassPath;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class EffectRegistry {
    private static final List<String> packages = new ArrayList<>();
    public static final Map<Identifier, Effect> registry = new TreeMap<>();

    public static void registerPackage(String packageName) {
        packages.add(packageName);
    }

    public static void init() {
        for (String packageName : packages) {
            try {
                for (Class<? extends Effect> clazz : findAllClasses(packageName).stream().filter(Effect.class::isAssignableFrom).map(clazz -> (Class<? extends Effect>) clazz).toList()) {
                    if (clazz.isAnnotationPresent(RegisterEffect.class)) {
                        var annotation = clazz.getAnnotation(RegisterEffect.class);
                        if (Identifier.isValid(annotation.value())) {
                            try {
                                Identifier identifier = new Identifier(annotation.value());
                                registry.put(identifier, clazz.getConstructor(Identifier.class).newInstance(identifier));
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Subathon.LOGGER.error("Failed to read package \"" + packageName + "\"", e);
            }
        }
    }

    private static Set<Class<?>> findAllClasses(String packageName) throws IOException {
        return ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName().equalsIgnoreCase(packageName))
                .map(classInfo -> Utils.getClass(classInfo.getName()))
                .collect(Collectors.toSet());
    }
}
