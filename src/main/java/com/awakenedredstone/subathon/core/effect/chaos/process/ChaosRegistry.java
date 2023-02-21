package com.awakenedredstone.subathon.core.effect.chaos.process;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.Utils;
import com.google.common.reflect.ClassPath;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class ChaosRegistry {
    private static final List<String> packages = new ArrayList<>();
    public static final Map<Identifier, Chaos> registry = new TreeMap<>();
    public static final Map<String, Identifier> classNameRegistry = new TreeMap<>();

    public  static void registerPackage(String packageName) {
        packages.add(packageName);
    }

    public static void init() {
        for (String packageName : packages) {
            try {
                for (Class<? extends Chaos> clazz : findAllClasses(packageName).stream().filter(Chaos.class::isAssignableFrom).map(clazz -> (Class<? extends Chaos>) clazz).toList()) {
                    if (clazz.isAnnotationPresent(RegisterChaos.class)) {
                        var annotation = clazz.getAnnotation(RegisterChaos.class);
                        if (Identifier.isValid(annotation.value())) {
                            try {
                                Identifier identifier = new Identifier(annotation.value());
                                registry.put(identifier, clazz.getConstructor().newInstance());
                                classNameRegistry.put(clazz.getName(), identifier);
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
