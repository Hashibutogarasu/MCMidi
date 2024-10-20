package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.options.MidiSound;
import net.minecraft.sound.SoundCategory;

public interface SoundManagerAccessor {
    void updateMidiVolume(MidiSound category, float volume);
}
