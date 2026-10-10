package com.github.eileen.tlmkeyboardmusic;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Declares the datapack registry that holds the keyboard's music.
 * <p>
 * Entries live at {@code data/<namespace>/tlmkeyboardmusic/keyboard_track/<name>.json}; the doubled
 * namespace is how NeoForge lays out non-vanilla datapack registries.
 * <p>
 * No network codec is supplied, so the registry stays server-side. The server picks the track and
 * sends only the chosen sound to clients.
 */
public final class KeyboardMusicRegistries {
    public static final ResourceKey<Registry<KeyboardTrack>> KEYBOARD_TRACKS = ResourceKey.createRegistryKey(
            ResourceLocation.fromNamespaceAndPath(TlmKeyboardMusic.MOD_ID, "keyboard_track"));

    private KeyboardMusicRegistries() {
    }

    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(KEYBOARD_TRACKS, KeyboardTrack.CODEC);
    }
}
