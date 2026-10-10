package com.craisinlord.antos.content.minigame;

/** Shared AI strength setting for the built-in games. */
public enum Difficulty {
    EASY("EASY"),
    MEDIUM("MEDIUM"),
    HARD("HARD");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
