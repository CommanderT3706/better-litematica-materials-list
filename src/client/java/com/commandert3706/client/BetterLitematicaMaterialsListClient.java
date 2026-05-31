package com.commandert3706.client;

import com.commandert3706.client.gui.screen.LitematicSelectionScreen;
import com.commandert3706.client.gui.screen.MaterialListScreen;
import com.commandert3706.client.logic.MaterialTrackerManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BetterLitematicaMaterialsListClient implements ClientModInitializer {
    public static KeyMapping selectSchematicKey;
    public static KeyMapping openMaterialListKey;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
        selectSchematicKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.blml.select_schematic",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_Z,
                        KeyMapping.Category.MISC
                )
        );

        openMaterialListKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.blml.material_list",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_X,
                        KeyMapping.Category.MISC
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (selectSchematicKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new LitematicSelectionScreen(Component.literal("Select a Schematic")));
                }
            }

            while (openMaterialListKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new MaterialListScreen(Component.literal("Material List")));
                }
            }
         });

        MaterialTrackerManager.loadGlobalSettings();
	}
}