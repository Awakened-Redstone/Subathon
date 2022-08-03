package com.awakenedredstone.subathon.util;

import java.util.*;
import java.util.stream.Collectors;

public class RandomCollection<T> {
    private final NavigableMap<Double, T> map = new TreeMap<>();
    private final Map<T, Double> weights = new HashMap<>();
    private final Random random;
    private double total = 0;

    public RandomCollection() {
        this(new Random());
    }

    public RandomCollection(Random random) {
        this.random = random;
    }

    public Map<T, Double> percentages() {
        Map<T, Double> percents = new HashMap<>();
        return weights.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() * 100 / total))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public double calculatePercentage(double obtained, double total) {
        return obtained * 100 / total;
    }

    public RandomCollection<T> add(double weight, T result) {
        if (weight <= 0) return this;
        weights.put(result, weight);
        total += weight;
        map.put(total, result);
        return this;
    }

    public T next() {
        double value = random.nextDouble() * total;
        return map.higherEntry(value).getValue();
    }
}