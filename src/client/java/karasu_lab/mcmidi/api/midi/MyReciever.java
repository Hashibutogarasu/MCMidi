package karasu_lab.mcmidi.api.midi;

import oshi.util.tuples.Pair;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import java.util.function.Function;

public class MyReciever implements Receiver {
    private final Function<Pair<MidiMessage, Long>, Integer> onControlChange;

    public MyReciever(Function<Pair<MidiMessage, Long>, Integer> onControlChange){
        this.onControlChange = onControlChange;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        onControlChange.apply(new Pair<>(message, timeStamp));
    }

    @Override
    public void close() {

    }
}
