package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.*;
import java.awt.*;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MidiControlCenter extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiControlCenter.class);
    private final Screen parent;

    private static final Map<Integer, MidiNote> midiNoteMap = new HashMap<>();
    private static final Map<Integer, Instrument> instrumentMap = new HashMap<>();

    private Instrument[] instruments;

    private static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    private int maxInstrumentNameWidth = 0;

    public MidiControlCenter() {
        this(MinecraftClient.getInstance().currentScreen);
        midiNoteMap.clear();
    }

    public MidiControlCenter(Screen parent) {
        this(Text.translatable("mcmidi.midi_control_center"), parent);
        midiNoteMap.clear();
    }

    protected MidiControlCenter(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    private void loadAllInstruments() {
        try {
            ExtendedMidi current = ExtendedMidi.getCurrent();
            if (current != null) {
                Synthesizer synthesizer = ExtendedMidi.getSynthesizer();
                if (synthesizer != null) {
                    this.instruments = synthesizer.getDefaultSoundbank().getInstruments();

                    if (this.instruments != null) {
                        LOGGER.info("Loading all instruments: {}", this.instruments.length);
                        for (int i = 0; i < this.instruments.length && i < 16; i++) {

                            if (this.instruments[i] != null) {
                                instrumentMap.put(i, this.instruments[i]);
                                LOGGER.info("Loaded instrument {} for channel {}", this.instruments[i].getName(), i);

                                String channelFormatted = String.format("Ch%d: %s", i, this.instruments[i].getName());
                                int width = this.textRenderer != null ? this.textRenderer.getWidth(channelFormatted)
                                        : channelFormatted.length() * 6;

                                if (width > maxInstrumentNameWidth) {
                                    maxInstrumentNameWidth = width;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load instruments", e);
        }
    }

    public void onRecieve(MidiMessage message) {
        this.instruments = ExtendedMidi.getSynthesizer().getDefaultSoundbank().getInstruments();

        if (message instanceof ShortMessage shortMessage) {
            midiNoteMap.put(shortMessage.getChannel(), new MidiNote(shortMessage));

            int channel = shortMessage.getChannel();
            if (instruments != null && channel < instruments.length && instruments[channel] != null) {
                Instrument instrument = instruments[channel];
                instrumentMap.put(channel, instrument);

                String channelFormatted = String.format("Ch%d: %s", channel, instrument.getName());
                int width = this.textRenderer != null ? this.textRenderer.getWidth(channelFormatted)
                        : channelFormatted.length() * 6;

                if (width > maxInstrumentNameWidth) {
                    maxInstrumentNameWidth = width;
                }
            }

            midiNoteMap.entrySet().stream().sorted(Map.Entry.comparingByKey());
        }
    }

    @Override
    protected void init() {

        loadAllInstruments();

        super.init();

        addDrawableChild(
                ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_soundfont_files"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(new SoundFontManagerScreen(this));
                    }
                }).dimensions((this.width / 2) - 205, this.height - 25, 90, 20).build());

        addDrawableChild(
                ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_midi_files"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(new MidiChooseScreen(this));
                    }
                }).dimensions((this.width / 2) - 110, this.height - 25, 90, 20).build());

        addDrawableChild(
                ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_sound_controller"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(new SoundControllerScreen(this));
                    }
                }).dimensions((this.width / 2) - 15, this.height - 25, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.close"), button -> {
            this.close();
        }).dimensions((this.width / 2) + 80, this.height - 25, 110, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 16777215);

        ExtendedMidi midi = ExtendedMidi.getCurrent();
        if (midi != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, midi.getPlayingPath(), this.width / 2, 16, 16777215);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("mcmidi.text.no_midi"),
                    this.width / 2, 16, 16777215);
        }

        AtomicInteger offsetY = new AtomicInteger(30);

        Text channelText = Text.translatable("mcmidi.midi.channel");
        int channelTextWidth = this.textRenderer.getWidth(channelText);

        Text statusText = Text.translatable("mcmidi.midi.status");
        int statusTextWidth = this.textRenderer.getWidth(statusText);

        Text data1Text = Text.translatable("mcmidi.midi.data1");
        int data1TextWidth = this.textRenderer.getWidth(data1Text);

        Text data2Text = Text.translatable("mcmidi.midi.data2");
        int data2TextWidth = this.textRenderer.getWidth(data2Text);

        String pos = "0";
        String bpm = "";

        if (midi != null) {
            pos = String.format("%.2f", (float) midi.getPosition() / 1000);
            bpm = String.format("%.2f", midi.getBPM());
        }

        float leftMarginRatio = 0.15f;
        float columnSpacingRatio = 0.05f;

        int leftMargin = (int) (this.width * leftMarginRatio);
        int columnSpacing = (int) (this.width * columnSpacingRatio);

        Text postext = Text.literal("Posision: " + pos);

        Text bpmText = Text.literal("BPM: " + bpm);

        int channelColumnWidth = Math.max(maxInstrumentNameWidth, channelTextWidth);

        int column1X = leftMargin;
        int column2X = column1X + channelColumnWidth + columnSpacing;
        int column3X = column2X + statusTextWidth + columnSpacing;
        int column4X = column3X + data1TextWidth + columnSpacing;
        int infoX = column4X + data2TextWidth + columnSpacing;

        context.drawText(this.textRenderer, channelText, column1X, offsetY.get(), 16777215, true);
        context.drawText(this.textRenderer, statusText, column2X, offsetY.get(), 16777215, true);
        context.drawText(this.textRenderer, data1Text, column3X, offsetY.get(), 16777215, true);
        context.drawText(this.textRenderer, data2Text, column4X, offsetY.get(), 16777215, true);
        context.drawText(this.textRenderer, postext, infoX, offsetY.get(), 16777215, true);
        context.drawText(this.textRenderer, bpmText, infoX, offsetY.get() + 10, 16777215, true);
        offsetY.set(offsetY.get() + 10);
        if (!instrumentMap.isEmpty()) {
            try {
                instrumentMap.forEach((integer, instrument) -> {
                    if (instrument != null) {
                        String channelFormatted = String.format("Ch%d: %s", integer, instrument.getName());

                        MidiNote midiNote = midiNoteMap.get(integer);
                        String statusFormatted = "-";
                        Text data1Styled = Text.literal("-");
                        Text data2Styled = Text.literal("-");

                        if (midiNote != null) {
                            statusFormatted = String.format("%02d", midiNote.getStatus());

                            if (midiNote.getCommand() == ShortMessage.NOTE_ON) {
                                int key = midiNote.getData1();
                                int octave = (key / 12) - 1;
                                int note = key % 12;
                                String noteName = NOTE_NAMES[note];
                                int velocity = minorize(midiNote.getData2());

                                data1Styled = Text.literal(noteName + octave)
                                        .setStyle(Style.EMPTY.withColor(getColor(noteName).getRGB()));
                                data2Styled = Text.literal(String.valueOf(velocity));
                            } else if (midiNote.getCommand() == ShortMessage.NOTE_OFF) {
                                data1Styled = Text.literal("OFF").setStyle(Style.EMPTY.withColor(Colors.RED));
                                data2Styled = Text.literal("0");
                            }
                        }

                        context.drawText(this.textRenderer, channelFormatted, column1X,
                                offsetY.get(), Colors.WHITE, true);
                        context.drawText(this.textRenderer, statusFormatted, column2X,
                                offsetY.get(),
                                Colors.WHITE, true);
                        context.drawText(this.textRenderer, data1Styled, column3X, offsetY.get(),
                                Colors.WHITE, true);
                        context.drawText(this.textRenderer, data2Styled, column4X, offsetY.get(),
                                Colors.WHITE, true);

                        offsetY.set(offsetY.get() + 10);
                    }
                });
            } catch (ConcurrentModificationException ignored) {

            }
        }
    }

    private int minorize(int origin) {
        int minorized = origin;
        int note = (origin % 12);
        if (note == 4 || note == 9 || note == 11) {
            minorized--;
        }
        return minorized;
    }

    private Color getColor(String data) {
        return switch (data) {
            case "C" -> new Color(255, 0, 0);
            case "C#" -> new Color(255, 165, 0);
            case "D" -> new Color(255, 255, 0);
            case "D#" -> new Color(0, 255, 0);
            case "E" -> new Color(0, 0, 255);
            case "F" -> new Color(75, 0, 130);
            case "F#" -> new Color(238, 130, 238);
            case "G" -> new Color(255, 192, 203);
            case "G#" -> new Color(255, 255, 255);
            case "A" -> new Color(211, 211, 211);
            case "A#" -> new Color(128, 128, 128);
            case "B" -> new Color(0, 0, 0);
            default -> Color.WHITE;
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ExtendedMidi midi = ExtendedMidi.getCurrent();
        if (this.parent != null && midi != null) {
            if (this.client != null) {
                super.close();
                this.client.setScreen(this);
            }
            midi.stop();
        }

        super.close();
    }

    private enum NoteStatus {
        ON(integer -> integer == 32 || integer == 100, "ON"),
        OFF(integer -> integer == 0, "OFF"),
        NONE(integer -> integer == 64, "NONE"),
        ON_OTHER(integer -> integer != 32 && integer != 100 && integer != 64 && integer != 0, "ON"),
        UNKNOWN(integer -> false, "UNKNOWN");

        private final Function<Integer, Boolean> condition;
        private final String name;

        NoteStatus(Function<Integer, Boolean> condition, String name) {
            this.condition = condition;
            this.name = name;
        }
    }

    private record MidiNote(ShortMessage shortMessage) {
        public int getChennel() {
            return shortMessage.getChannel();
        }

        public int getStatus() {
            return shortMessage.getStatus();
        }

        public int getCommand() {
            return shortMessage.getCommand();
        }

        public byte[] getMessage() {
            return shortMessage.getMessage();
        }

        public int getData1() {
            return shortMessage.getData1();
        }

        public int getData2() {
            return shortMessage.getData2();
        }
    }
}
