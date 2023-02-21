package com.awakenedredstone.subathon.util;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedRandom<T> {
    private final NavigableMap<Double, T> map = new TreeMap<>();
    private final Map<T, Double> weights = new HashMap<>();
    private final Random random;
    private double total = 0;

    public WeightedRandom() {
        this(new Random());
    }

    public WeightedRandom(Random random) {
        this.random = random;
    }

    public Map<T, Double> percentages() {
        return weights.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue() * 100 / total))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public double percentage(T type) {
        return weights.getOrDefault(type, 0d) * 100 / total;
    }

    public double getWeight(T type) {
        return weights.getOrDefault(type, 0d);
    }

    public double getTotal() {
        return total;
    }

    public double calculatePercentage(double obtained, double total) {
        return obtained * 100 / total;
    }

    public WeightedRandom<T> add(double weight, T result) {
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