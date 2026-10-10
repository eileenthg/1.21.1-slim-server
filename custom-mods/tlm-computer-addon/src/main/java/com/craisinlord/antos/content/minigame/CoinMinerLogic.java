package com.craisinlord.antos.content.minigame;

/** Idle clicker state: mine coins by hand, then buy upgrades that mine for you. */
public final class CoinMinerLogic {
    public static final int TICKS_PER_SECOND = 20;
    private static final double COST_GROWTH = 1.15D;

    public record Upgrade(String name, String description, double baseCost, double amount, boolean perClick) {}

    public static final Upgrade[] UPGRADES = {
            new Upgrade("PICKAXE", "+1 COIN PER CLICK", 10, 1, true),
            new Upgrade("ANT HELPER", "+1 COIN PER SEC", 50, 1, false),
            new Upgrade("DRILL", "+5 COINS PER SEC", 400, 5, false),
            new Upgrade("MINECART", "+25 COINS PER SEC", 3_000, 25, false),
            new Upgrade("REFINERY", "+150 COINS PER SEC", 25_000, 150, false)
    };

    private final int[] levels = new int[UPGRADES.length];
    private double coins;
    private double totalMined;

    public double coins() {
        return coins;
    }

    public double totalMined() {
        return totalMined;
    }

    public int level(int upgrade) {
        return levels[upgrade];
    }

    public double clickValue() {
        double value = 1;
        for (int i = 0; i < UPGRADES.length; i++) {
            if (UPGRADES[i].perClick()) value += UPGRADES[i].amount() * levels[i];
        }
        return value;
    }

    public double perSecond() {
        double value = 0;
        for (int i = 0; i < UPGRADES.length; i++) {
            if (!UPGRADES[i].perClick()) value += UPGRADES[i].amount() * levels[i];
        }
        return value;
    }

    public double cost(int upgrade) {
        return Math.ceil(UPGRADES[upgrade].baseCost() * Math.pow(COST_GROWTH, levels[upgrade]));
    }

    public boolean canAfford(int upgrade) {
        return coins >= cost(upgrade);
    }

    public void click() {
        coins += clickValue();
        totalMined += clickValue();
    }

    public void tick() {
        double gained = perSecond() / TICKS_PER_SECOND;
        coins += gained;
        totalMined += gained;
    }

    public boolean buy(int upgrade) {
        if (upgrade < 0 || upgrade >= UPGRADES.length || !canAfford(upgrade)) return false;
        coins -= cost(upgrade);
        levels[upgrade]++;
        return true;
    }

    /** Short number format: 950, 1.2K, 3.4M, 5.6B. */
    public static String format(double value) {
        if (value < 1_000) {
            return Long.toString((long) Math.floor(value));
        }
        String[] suffixes = {"K", "M", "B", "T"};
        double scaled = value;
        int index = -1;
        while (scaled >= 1_000 && index < suffixes.length - 1) {
            scaled /= 1_000;
            index++;
        }
        return String.format(java.util.Locale.ROOT, "%.1f%s", scaled, suffixes[index]);
    }
}
