package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.api.midi.MyReciever;
import karasu_lab.mcmidi.api.networking.MidiS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.chaiware.midi4j.MidiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MidiControlCenter extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiControlCenter.class);
    private String playingPath = "";
    private ExtendedMidi midi;
    private MidiInfo midiInfo;
    private final Screen parent;

    private static final Map<Integer, MidiNote> midiNoteMap = new HashMap<>();

    private static final MyReciever receiver = new MyReciever((message) -> {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if(currentScreen instanceof MidiControlCenter controlCenter){
            controlCenter.onRecieve(message.getA());
        }

        return 0;
    });

    private void onRecieve(MidiMessage message) {
        this.playingPath = MidiS2CPacket.getPlayingPath();
        if(message instanceof ShortMessage shortMessage){
            midiNoteMap.put(shortMessage.getChannel(), new MidiNote(shortMessage));
            midiNoteMap.entrySet().stream().sorted(Map.Entry.comparingByKey());
        }
    }

    public MidiControlCenter(){
        this(MinecraftClient.getInstance().currentScreen);
        midiNoteMap.clear();
    }

    public MidiControlCenter(Screen parent){
        this(Text.translatable("mcmidi.midi_control_center"), parent);
        midiNoteMap.clear();
    }

    protected MidiControlCenter(Text title, Screen parent) {
        super(title);
        this.parent = parent;
        ExtendedMidi extendedmidi = MidiS2CPacket.getMidi();

        if(extendedmidi != null){
            this.midi = extendedmidi;
            this.midiInfo = extendedmidi.getMidiInfo();
        }
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_soundfont_files"), button -> {
            if (this.client != null) {
                this.client.setScreen(new SoundFontManagerScreen(this));
            }
        }).dimensions((this.width / 2) - 205, this.height - 25, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_midi_files"), button -> {
            if (this.client != null) {
                this.client.setScreen(new MidiChooseScreen(this));
            }
        }).dimensions((this.width / 2) - 110, this.height - 25, 90, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_sound_controller"), button -> {
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.playingPath, this.width / 2, 16, 16777215);

        AtomicInteger offsetY = new AtomicInteger(30);

        Text channelText = Text.translatable("mcmidi.midi.channel");
        int channelTextWidth = this.textRenderer.getWidth(channelText);

        Text statusText = Text.translatable("mcmidi.midi.status");
        int statusTextWidth = this.textRenderer.getWidth(statusText);

        Text data1Text = Text.translatable("mcmidi.midi.data1");
        int data1TextWidth = this.textRenderer.getWidth(data1Text);

        Text data2Text = Text.translatable("mcmidi.midi.data2");
        int data2TextWidth = this.textRenderer.getWidth(data2Text);

        int offsetX1 = context.drawText(this.textRenderer, channelText, (this.width/ 2) - 200, offsetY.get(), 16777215, true);
        int offsetX2 = context.drawText(this.textRenderer, statusText, offsetX1 + channelTextWidth + 10, offsetY.get(), 16777215, true);
        int offsetX3 = context.drawText(this.textRenderer, data1Text, offsetX2 + statusTextWidth + 10, offsetY.get(), 16777215, true);
        int offsetX4 = context.drawText(this.textRenderer, data2Text, offsetX3 + data1TextWidth + 10, offsetY.get(), 16777215, true);

        offsetY.set(offsetY.get() + 10);
        if(!midiNoteMap.isEmpty()){
            try{
                midiNoteMap.forEach((integer, midiMessage) -> {
                    String channelFormatted = String.format("%02d" , integer);
                    String statusFormatted = String.format("%02d", midiMessage.getStatus());

                    int data1 = midiMessage.getData1();
                    int data2 = midiMessage.getData2();

                    String data2Value = String.valueOf(data2);

                    for (NoteStatus value : NoteStatus.values()) {
                        if(value.condition.apply(data2)){
                            data2Value = value.name;
                        }
                    }

                    Text data1Styled = Text.literal(String.valueOf(data1)).setStyle(Style.EMPTY.withColor(getColor(data1).getRGB()));
                    Text data2Styled = Text.literal(String.valueOf(data2Value)).setStyle(Style.EMPTY.withColor(getColor(data2).getRGB()));

                    context.drawText(this.textRenderer, channelFormatted, offsetX1 - channelTextWidth, offsetY.get(), Colors.WHITE, true);
                    context.drawText(this.textRenderer, statusFormatted, offsetX2 - statusTextWidth, offsetY.get(), Colors.WHITE, true);
                    context.drawText(this.textRenderer, data1Styled, offsetX3 - data1TextWidth, offsetY.get(), Colors.WHITE, true);
                    context.drawText(this.textRenderer, data2Styled, offsetX4 - data2TextWidth, offsetY.get(), Colors.WHITE, true);

                    offsetY.set(offsetY.get() + 10);
                });
            }
            catch (ConcurrentModificationException ignored){

            }
        }
    }

    private Color getColor(int a){
        int r = 0, g = 0, b = 0;

        if (a >= 0 && a <= 30) {
            r = 255;
            g = (int) (255 * (a / 30.0));
        } else if (a >= 31 && a <= 60) {
            g = 255;
            r = (int) (255 * ((60 - a) / 30.0));
        } else if (a >= 61 && a <= 90) {
            g = 255;
            b = (int) (255 * ((a - 60) / 30.0));
        } else if (a >= 91 && a <= 120) {
            b = 255;
            g = (int) (255 * ((120 - a) / 30.0));
        }

        if(r > 255){
            r = 255;
        }
        if(g > 255){
            g = 255;
        }
        if(b > 255){
            b = 255;
        }

        return new Color(r, g, b);
    }

    public static MyReciever getReceiver(){
        return receiver;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if(this.parent != null && midi != null){
            if (this.client != null) {
                super.close();
                this.client.setScreen(this);
            }
            this.midi.stop();
        }

        if (this.client != null) {
            this.client.setScreen(this.parent);
        }

    }

    private enum NoteStatus{
        ON(integer -> integer == 32 || integer == 100, "ON"),
        OFF(integer -> integer == 0, "OFF"),
        NONE(integer -> integer == 64, "NONE"),
        ON_OTHER(integer -> integer != 32 && integer != 100 && integer != 64 && integer != 0, "ON"),
        UNKNOWN(integer -> false, "UNKNOWN");

        private final Function<Integer, Boolean> condition;
        private final String name;

        NoteStatus(Function<Integer, Boolean> condition, String name){
            this.condition = condition;
            this.name = name;
        }
    }

    private record MidiNote(ShortMessage shortMessage) {
        public int getChennel(){
            return shortMessage.getChannel();
        }

        public int getStatus(){
            return shortMessage.getStatus();
        }

        public int getCommand(){
            return shortMessage.getCommand();
        }

        public byte[] getMessage(){
            return shortMessage.getMessage();
        }

        public int getData1(){
            return shortMessage.getData1();
        }

        public int getData2(){
            return shortMessage.getData2();
        }
    }
}
