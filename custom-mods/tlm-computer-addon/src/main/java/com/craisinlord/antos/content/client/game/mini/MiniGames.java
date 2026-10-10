package com.craisinlord.antos.content.client.game.mini;

import com.craisinlord.antos.api.client.game.ComputerGame;
import com.craisinlord.antos.api.client.game.ComputerGameRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Registers the built-in single-player games shown in the Games app. */
public final class MiniGames {
    private MiniGames() {}

    public static void register() {
        add("coin_miner", "COIN MINER", CoinMinerSession::new);
        add("paddle_ball", "PADDLE BALL", PaddleBallSession::new);
        add("tic_tac_toe", "TIC-TAC-TOE", TicTacToeSession::new);
        add("minesweeper", "MINESWEEPER", MinesweeperSession::new);
        add("checkers", "CHECKERS", CheckersSession::new);
    }

    private static void add(String path, String title, Supplier<ComputerGame.Session> factory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("antos", path);
        if (ComputerGameRegistry.get(id) != null) return;
        ComputerGameRegistry.register(new ComputerGame() {
            @Override public ResourceLocation id() { return id; }
            @Override public ResourceLocation diskId() { return null; }
            @Override public boolean includedByDefault() { return true; }
            @Override public String title() { return title; }
            @Override public Session create() { return factory.get(); }
        });
    }
}
