package com.awakenedredstone.subathon.util;

import java.util.List;
import java.util.Random;

public class SubathonUtils {

    public static <T> T pickRandom(List<T> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    public static <T> T pickRandom(List<T> list) {
        return pickRandom(list, new Random());
    }
}
