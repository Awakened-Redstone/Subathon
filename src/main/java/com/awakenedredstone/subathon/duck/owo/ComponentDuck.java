package com.awakenedredstone.subathon.duck.owo;

public interface ComponentDuck {
    void subathon$registerUpdateListener(Listener listener);

    @FunctionalInterface
    interface Listener {
        void update(float delta, int mouseX, int mouseY);
    }
}