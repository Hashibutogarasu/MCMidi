package karasu_lab.mcmidi.api.midi;

import org.chaiware.midi4j.Midi;

public class CustomMidi extends Midi {
    public CustomMidi(String pathToMidiFile) throws Exception {
        super(pathToMidiFile);
    }
}
