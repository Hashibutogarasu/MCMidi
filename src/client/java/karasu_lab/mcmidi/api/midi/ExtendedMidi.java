package karasu_lab.mcmidi.api.midi;

import karasu_lab.mcmidi.config.ModConfig;
import karasu_lab.mcmidi.mixin.client.MidiAccessor;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import me.shedaniel.autoconfig.AutoConfig;
import org.chaiware.midi4j.Midi;
import org.chaiware.midi4j.MidiInfo;
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
    private final MidiAccessor midiAccessor;

    public ExtendedMidi(File file) throws Exception {
        String pathToMidiFile = file.getAbsolutePath();
        this.midi = new CustomMidi(pathToMidiFile);
        this.midiInfo = new MidiInfo(pathToMidiFile);
        this.pathToMidiFile = pathToMidiFile;
        this.config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        this.midiAccessor = (MidiAccessor)this.asMidi();
    }

    public void setReceiver(Receiver receiver) throws Exception {
        getSequencer().getTransmitter().setReceiver(receiver);
    }

    public void saveToLocal(byte[] bytes, String path){
        if(bytes == null || bytes.length == 0){
            return;
        }

        try {
            File file = new File(path);
            boolean result = file.createNewFile();
            if(result){
                MidiSystem.write(MidiSystem.getSequence(new ByteArrayInputStream(bytes)), 1, file);
            }
        } catch (InvalidMidiDataException | IOException ignored) {

        }
    }

    public void play(){
        try{
            this.getSequencer().open();
            this.getSequencer().setSequence(MidiSystem.getSequence(new File(this.pathToMidiFile)));
            this.setSoundFont(config.soundFontPath);
            this.setReceiver(MidiControlCenter.getReceiver());
            this.asMidi().play();
            LOGGER.info("Playing MIDI file: {}", this.pathToMidiFile);
        }
        catch (Exception e) {
            LOGGER.error("Failed to play MIDI file: {}", this.pathToMidiFile);
            LOGGER.error(e.getMessage());
        }
    }

    public Sequencer getSequencer() {
        return this.midiAccessor.getSequencer();
    }

    public Synthesizer getSynthesizer() {
        return ((MidiAccessor)this.asMidi()).getSynthesizer();
    }

    public void stop() {
        if(this.midi == null){
            LOGGER.error("Midi is null");
            return;
        }

        Sequencer sequencer = this.getMidiAccessor().getSequencer();
        Synthesizer synthesizer = this.getMidiAccessor().getSynthesizer();

        LOGGER.info("Stopping MIDI playback");

        try{
            if(sequencer != null){
                if(sequencer.isOpen() || sequencer.isRunning()){
                    if(synthesizer.isOpen()){
                        sequencer.getReceiver().close();
                        sequencer.stop();
                        sequencer.close();
                        removeTracks();

                        synthesizer.close();
                        synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
                    }
                }
            }
        }
        catch (Exception e){
            LOGGER.error("Failed to stop MIDI playback");
            LOGGER.error(e.getMessage());
        }
    }

    private void removeTracks(){
        Sequencer sequencer = getSequencer();
        Sequence sequence = sequencer.getSequence();

        if(sequence != null){
            Track[] tracks = sequence.getTracks();
            for(Track track: tracks){
                sequence.deleteTrack(track);
            }
        }
    }

    public void setSoundFont(String path) {
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

    public MidiAccessor getMidiAccessor() {
        return midiAccessor;
    }
}
