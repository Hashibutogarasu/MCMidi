package karasu_lab.mcmidi.api;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiPlayer {
    public void play() {

    }

    public void stop() {
        System.out.println("MidiPlayer: stop");
    }

    public void pause() {
        System.out.println("MidiPlayer: pause");
    }

    public void resume() {
        System.out.println("MidiPlayer: resume");
    }

    public boolean uploadMidi(String path) {
        return true;
    }


}
