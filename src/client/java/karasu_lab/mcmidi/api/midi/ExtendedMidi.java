package karasu_lab.mcmidi.api.midi;

import karasu_lab.mcmidi.config.ModConfig;
import karasu_lab.mcmidi.mixin.client.MidiAccessor;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import me.shedaniel.autoconfig.AutoConfig;
import org.chaiware.midi4j.Midi;
import org.chaiware.midi4j.MidiInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class ExtendedMidi{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedMidi.class);
    private final String pathToMidiFile;
    private final CustomMidi midi;
    private final MidiInfo midiInfo;
    private final ModConfig config;

    public ExtendedMidi(File file) throws Exception {
        String pathToMidiFile = file.getAbsolutePath();
        this.midi = new CustomMidi(pathToMidiFile);
        this.midiInfo = new MidiInfo(pathToMidiFile);
        this.pathToMidiFile = pathToMidiFile;
        this.config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    public void setReceiver(Receiver receiver) throws Exception {
        getSequencer().getTransmitter().setReceiver(receiver);
    }

    @Nullable
    public String saveToLocal(byte[] bytes, String path){
        if(bytes == null || bytes.length == 0){
            return null;
        }

        try {
            File file = new File(path);
            boolean result = file.createNewFile();
            if(result){
                MidiSystem.write(MidiSystem.getSequence(new ByteArrayInputStream(bytes)), 1, file);
            }
        } catch (InvalidMidiDataException | IOException ignored) {

        }

        return path;
    }

    public void play(){
        try{
            this.getSequencer().open();
            this.getSequencer().setSequence(MidiSystem.getSequence(new File(this.pathToMidiFile)));
            this.setSoundFont(config.soundFontPath);
            this.setReceiver(MidiControlCenter.getReceiver());
            this.asMidi().play();
        }
        catch (Exception e) {
            LOGGER.error("Failed to play MIDI file: {}", this.pathToMidiFile);
            LOGGER.error(e.getMessage());
        }
    }

    public Sequencer getSequencer() {
        return ((MidiAccessor)this.asMidi()).getSequencer();
    }

    public Synthesizer getSynthesizer() {
        return ((MidiAccessor)this.asMidi()).getSynthesizer();
    }

    public void stop() {
        Sequencer sequencer = getSequencer();
        Synthesizer synthesizer = getSynthesizer();

        if(sequencer != null){
            if(sequencer.isOpen() || sequencer.isRunning()){
                sequencer.stop();
                sequencer.close();
                if(synthesizer != null && synthesizer.isOpen()){
                    synthesizer.close();
                }
            }
        }
    }

    public void setSoundFont(String path) throws Exception {
        File file = new File(path);

        if(!file.exists() || file.isDirectory()){
            LOGGER.error("Soundfont file does not exist: {}", path);
            return;
        }

        Sequencer sequencer = getSequencer();
        Synthesizer synthesizer = getSynthesizer();

        try {
            for(Transmitter tm: sequencer.getTransmitters()) {
                tm.close();
            }

            synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
            synthesizer.loadAllInstruments(MidiSystem.getSoundbank(file));
            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        } catch (InvalidMidiDataException | IOException | MidiUnavailableException e) {
            LOGGER.error("Failed to load soundfont: {}", path);
        }
    }

    public void setLoopCount(int count) {
        this.getSequencer().setLoopCount(count);
    }

    public void setStartTick(long tick) {
        this.getSequencer().setLoopStartPoint(tick);
    }

    public MidiInfo getMidiInfo() {
        return midiInfo;
    }

    public Midi asMidi() {
        return midi;
    }
}
