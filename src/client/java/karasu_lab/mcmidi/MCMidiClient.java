package karasu_lab.mcmidi;

import karasu_lab.mcmidi.api.networking.ModClientNetworking;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.Sequencer;

public class MCMidiClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(MCMidiClient.class);
	public static Sequencer CLIENT_SEQUENCER;

	KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.mcmidi.open", // The translation key of the keybinding's name
		InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
		GLFW.GLFW_KEY_R, // The keycode of the key
        "category.mcmidi.keybinds" // The translation key of the keybinding's category.
	));

	@Override
	public void onInitializeClient() {
		ModClientNetworking.registerS2CPackets();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.wasPressed()) {
				client.setScreen(new MidiControlCenter());
			}
		});
	}
}