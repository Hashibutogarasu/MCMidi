package karasu_lab.mcmidi.api.midi;

import karasu_lab.mcmidi.config.ModConfig;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.io.*;

public class ExtendedMidi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedMidi.class);
    private final ModConfig config;
    private final byte[] bytes;
    private final Identifier identifier;

    private final Sequencer sequencer;
    private final Sequence sequence;
    private final Synthesizer synthesizer;

    private static ExtendedMidi current;

    private long position;

    public ExtendedMidi(byte[] bytes, Identifier identifier) throws Exception {
        this.config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        this.identifier = identifier;

        this.bytes = bytes;

        this.sequencer = MidiSystem.getSequencer();
        this.sequence = MidiSystem.getSequence(new ByteArrayInputStream(this.bytes));

        this.synthesizer = MidiSystem.getSynthesizer();

        current = this;
    }

    @Nullable
    public static ExtendedMidi getCurrent() {
        assert current != null;
        return current;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public static Synthesizer getSynthesizer() {
        return current.synthesizer;
    }

    public void saveToLocal(byte[] bytes, String path) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        try {
            File file = new File(path);
            boolean result = file.createNewFile();
            if (result) {
                MidiSystem.write(MidiSystem.getSequence(new ByteArrayInputStream(bytes)), 1, file);
            }
        } catch (InvalidMidiDataException | IOException ignored) {

        }
    }

    public void play() {
        try {

            if (!this.sequencer.isOpen()) {
                this.sequencer.setSequence(this.sequence);
                this.synthesizer.open();
                this.sequencer.open();
                this.setSoundFont(config.soundFontPath);

                this.sequencer.getTransmitter().setReceiver(new MyReciever(midiMessageLongPair -> {
                    if (MinecraftClient.getInstance().currentScreen instanceof MidiControlCenter controlCenter) {
                        controlCenter.onRecieve(midiMessageLongPair.getA());
                    }

                    return 0;
                }));
            }

            if (!this.sequencer.isRunning()) {
                this.sequencer.start();
                LOGGER.info("Playing MIDI: {}", this.identifier);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to play MIDI: {}", this.identifier);
            LOGGER.error(e.getMessage());
        }
    }

    public void stop() {
        LOGGER.info("Stopping current midi: {}", getCurrent().getIdentifier());
        if (this.sequencer.isOpen()) {
            this.sequencer.stop();
            this.sequencer.close();
            this.synthesizer.close();
        }
    }

    public void clear() {
        if (current != null) {
            current.stop();
            current = null;
        }
    }

    public static void clearAll() {
        if (current != null) {
            try {
                current.stop();

                if (current.sequencer != null && current.sequencer.isOpen()) {
                    current.sequencer.stop();
                    current.sequencer.close();
                }

                if (current.synthesizer != null && current.synthesizer.isOpen()) {
                    current.synthesizer.close();
                }

                LOGGER.info("All MIDI resources cleared");
            } catch (Exception e) {
                LOGGER.error("Error clearing MIDI resources", e);
            } finally {
                current = null;
            }
        }
    }

    public static void pauseCurrent() {
        var current = ExtendedMidi.getCurrent();
        if (current != null) {
            current.pause();
        }
    }

    public void stopCurrent() {
        var current = ExtendedMidi.getCurrent();
        if (current != null) {
            current.stop();
        }
    }

    public long getPosition() {
        var current = ExtendedMidi.getCurrent();
        if (current != null) {
            return current.sequencer.getTickPosition();
        }
        return 0;
    }

    public static void updatePosition() {
        var current = ExtendedMidi.getCurrent();
        if (current != null && current.sequencer.isOpen()) {
            current.position = current.sequencer.getTickPosition();
        }
    }

    public void setPosition(long position) {
        LOGGER.info("Setting position to: {}", position);
        this.position = position;
        this.sequencer.setTickPosition(this.position);
    }

    public void pause() {
        if (this.sequencer.isOpen() && this.sequencer.isRunning()) {
            this.sequencer.stop();
            LOGGER.info("Pausing MIDI: {}", this.identifier);
        }
    }

    public void setPotision(long potision) {
        this.sequencer.setTickPosition(potision);
    }

    public void setSoundFont(String path) {
        if (path == null || path.isEmpty()) {
            LOGGER.info("Using default soundfont");
            useDefaultSoundFont();
            return;
        }

        File file = new File(path);

        if (!file.exists() || file.isDirectory()) {
            LOGGER.error("Soundfont file does not exist use default");
            useDefaultSoundFont();
            return;
        }

        Sequencer sequencer = this.sequencer;
        Synthesizer synthesizer = this.synthesizer;

        try {
            for (Transmitter tm : sequencer.getTransmitters()) {
                tm.close();
            }

            synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
            synthesizer.loadAllInstruments(MidiSystem.getSoundbank(file));
            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
        } catch (InvalidMidiDataException | IOException | MidiUnavailableException e) {
            LOGGER.error("Failed to load soundfont: {}", path);
            useDefaultSoundFont();
        }
    }

    private void useDefaultSoundFont() {
        try {
            Sequencer sequencer = this.sequencer;
            Synthesizer synthesizer = this.synthesizer;

            for (Transmitter tm : sequencer.getTransmitters()) {
                tm.close();
            }

            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
            LOGGER.info("Default soundfont loaded");
        } catch (MidiUnavailableException e) {
            LOGGER.error("Failed to load default soundfont", e);
        }
    }

    public void setLoopCount(int count) {
        this.sequencer.setLoopCount(count);
    }

    public void setStartTick(long tick) {
        this.sequencer.setLoopStartPoint(tick);
    }

    public boolean isPlaying() {
        return this.sequencer.isOpen() && this.sequencer.isRunning();
    }

    public Text getPlayingPath() {
        if (this.identifier == null) {
            return Text.translatable("mcmidi.text.no_midi");
        }
        return Text.literal(this.identifier.getPath());
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public float getBPM() {
        return this.sequencer.getTempoInBPM();
    }
}
