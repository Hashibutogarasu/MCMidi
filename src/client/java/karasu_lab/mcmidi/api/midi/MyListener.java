package karasu_lab.mcmidi.api.midi;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import java.util.function.Function;

public class MyListener implements MetaEventListener {
    private final Function<MetaMessage, Integer> onControlChange;

    public MyListener(Function<MetaMessage, Integer> onControlChange){
        this.onControlChange = onControlChange;
    }

    public void meta(MetaMessage meta) {
        onControlChange.apply(meta);
    }
}
