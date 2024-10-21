package karasu_lab.mcmidi.mixin.client;

import org.chaiware.midi4j.Midi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

@Mixin(Midi.class)
public interface MidiAccessor {
    @Accessor(value = "sequencer", remap = false)
    Sequencer getSequencer();

    @Accessor(value = "synthesizer", remap = false)
    Synthesizer getSynthesizer();
}
