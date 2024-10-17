package karasu_lab.mcmidi;

import karasu_lab.mcmidi.api.MidiManager;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCMidi implements ModInitializer {
	public static final String MOD_ID = "mcmidi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MidiManager midiManager;

	@Override
	public void onInitialize() {

    }
}