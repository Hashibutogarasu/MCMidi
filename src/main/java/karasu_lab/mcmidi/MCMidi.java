package karasu_lab.mcmidi;

import karasu_lab.mcmidi.api.MidiManager;
import karasu_lab.mcmidi.api.SoundFontManager;
import karasu_lab.mcmidi.api.command.MidiCommand;
import karasu_lab.mcmidi.api.networking.ModNetworking;
import karasu_lab.mcmidi.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCMidi implements ModInitializer {
	public static final String MOD_ID = "mcmidi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MidiManager midiManager;
	public static SoundFontManager soundFontManager;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			MidiCommand.register(dispatcher, registryAccess);
		});

		ModNetworking.registerC2SPackets();
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    }

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}