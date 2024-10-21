package karasu_lab.mcmidi.config;

import karasu_lab.mcmidi.MCMidi;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = MCMidi.MOD_ID)
public class ModConfig implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int midiVolume = 100;

    public String soundFontPath = "";
}
