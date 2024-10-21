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
import org.chaiware.midi4j.Midi;
import org.chaiware.midi4j.MidiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MidiControlCenter extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiControlCenter.class);
    private String playingPath = "";
    private Midi midi;
    private MidiInfo midiInfo;

    private static final Map<Integer, Integer> midiStatusMap = new HashMap<>();
    private static final Map<Integer, Pair<Integer, Integer>> midiChannelMap = new HashMap<>();

    private static final MyReciever receiver = new MyReciever((message) -> {
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if(currentScreen instanceof MidiControlCenter controlCenter){
            controlCenter.onRecieve(message.getA(), message.getB());
        }

        onRecieveStatic(message.getA(), message.getB());

        return 0;
    });

    private static void onRecieveStatic(MidiMessage message, long timeStamp){

    }

    private void onRecieve(MidiMessage message, long timeStamp) {
        this.playingPath = MidiS2CPacket.getPlayingPath();
        if(message instanceof ShortMessage shortMessage){
            midiChannelMap.put(shortMessage.getChannel(), new Pair<>(shortMessage.getData1(), shortMessage.getData2()));
            midiStatusMap.put(shortMessage.getChannel(), shortMessage.getStatus());

            midiChannelMap.entrySet().stream().sorted(Map.Entry.comparingByKey());
        }
    }

    public MidiControlCenter(){
        this(Text.translatable("mcmidi.midi_control_center"));

        midiChannelMap.clear();
        midiStatusMap.clear();
    }

    protected MidiControlCenter(Text title) {
        super(title);
        ExtendedMidi extendedmidi = MidiS2CPacket.getMidi();

        if(extendedmidi != null){
            this.midi = extendedmidi.asMidi();
            this.midiInfo = extendedmidi.getMidiInfo();
        }
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(Text.translatable("mcmidi.midi_control_center.open_midi_files"), button -> {
            if (this.client != null) {
                this.client.setScreen(new MidiChooseScreen(this));
            }
        }).dimensions((this.width / 2) - 100, this.height - 25, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 17, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer, this.playingPath, this.width / 2, 27, 16777215);

        AtomicInteger offsetY = new AtomicInteger(40);
        AtomicInteger lastOffsetX = new AtomicInteger();
        if(!midiStatusMap.isEmpty()){
            try{
                midiStatusMap.forEach((integer, integer2) -> {
                    String formatted = String.format("%02d", integer);
                    Text channel = Text.literal("Channel " + formatted);
                    int x = this.textRenderer.getWidth(channel) + 10;

                    String statusFormatted = String.format("%03d", integer2);
                    context.drawText(this.textRenderer, channel, (this.width / 2) - 200, offsetY.get(), 16777215, true);
                    var lastoffsetX = context.drawText(this.textRenderer, Text.literal("Status: " + statusFormatted),((this.width / 2) - 200) + x, offsetY.get(), 16777215, true);
                    lastOffsetX.set(lastoffsetX);

                    offsetY.addAndGet(10);
                });
            }
            catch (ConcurrentModificationException e){
                LOGGER.error(e.getMessage());
            }
        }

        if(!midiChannelMap.isEmpty()){
            AtomicInteger offsetY2 = new AtomicInteger(40);
            try{
                midiChannelMap.forEach((integer, pair) -> {
                    String data1 = String.format("%03d", pair.getA());
                    String data2;

                    Style styleA;
                    Style styleB;

                    int a = pair.getA();
                    Color color = getColor(a);

                    styleA = Style.EMPTY.withColor(color.getRGB());

                    int b = pair.getB();
                    if(b == 100){
                        data2 = "ON";
                        styleB = Style.EMPTY.withColor(Colors.GREEN);
                    }
                    else if(b == 0){
                        data2 = "OFF";
                        styleB = Style.EMPTY.withColor(Colors.RED);
                    }
                    else if(b == 64){
                        data2 = "NONE";
                        styleB = Style.EMPTY.withColor(Colors.YELLOW);
                    }
                    else{
                        data2 = "ON";
                        styleB = Style.EMPTY.withColor(getColor(b).getRGB());
                    }

                    Text styledTextA = Text.literal(data1).setStyle(styleA);
                    Text styledTextB = Text.literal(data2).setStyle(styleB);

                    int lastoffsetX = context.drawText(this.textRenderer, styledTextA,lastOffsetX.get() + 10, offsetY2.get(), 16777215, true);
                    context.drawText(this.textRenderer, styledTextB, lastoffsetX + 10, offsetY2.get(), 16777215, true);

                    offsetY2.addAndGet(10);
                });
            }
            catch (ConcurrentModificationException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private Color getColor(int a){
        Color color = new Color(0, 0, 0);

        if(a < 50){
            int g = (int)(5.1 * a);
            if(g > 255){
                g = 255;
            }
            color = new Color(255, g, 0);
        }
        else if(a < 100){
            int r = (int)(510 - 5.1 * a);
            if(r > 255){
                r = 255;
            }
            color = new Color(r, 255, 0);
        }
        else if(a < 150){
            int b = (int)(5.1 * a);
            if(b > 255){
                b = 255;
            }
            color = new Color(0, 255, b);
        }
        else if(a < 200){
            int g = (int)(510 - 5.1 * a);
            if(g > 255){
                g = 255;
            }
            color = new Color(0, g, 255);
        }

        return color;
    }

    public static MyReciever getReceiver(){
        return receiver;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
