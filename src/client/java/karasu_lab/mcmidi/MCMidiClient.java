package karasu_lab.mcmidi;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.api.networking.ModClientNetworking;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

public class MCMidiClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(MCMidiClient.class);
	public static Sequencer CLIENT_SEQUENCER;

	KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.mcmidi.open",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_COMMA,
			"category.mcmidi.keybinds"));

	@Override
	public void onInitializeClient() {
		ModClientNetworking.registerS2CPackets();

		try {
			CLIENT_SEQUENCER = MidiSystem.getSequencer(false);
		} catch (MidiUnavailableException ignored) {

		}

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ExtendedMidi.clearAll();
			LOGGER.info("World joined - clearing all MIDIs");
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.wasPressed()) {
				client.setScreen(new MidiControlCenter());
			}
		});
	}
}