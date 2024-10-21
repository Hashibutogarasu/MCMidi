package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.api.midi.MyReciever;
import karasu_lab.mcmidi.api.networking.MidiS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.chaiware.midi4j.Midi;
import org.chaiware.midi4j.MidiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.tuples.Pair;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MidiControlCenter extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiControlCenter.class);
    private String playingPath = "";
    private Midi midi;
    private MidiInfo midiInfo;

    private final Map<Integer, Integer> midiStatusMap = new HashMap<>();
    private final Map<Integer, Pair<Integer, Integer>> midiChannelMap = new HashMap<>();

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
        }
    }

    public MidiControlCenter(){
        this(Text.translatable("mcmidi.midi_control_center"));
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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 17, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer, this.playingPath, this.width / 2, 27, 16777215);

        AtomicInteger offsetY = new AtomicInteger(40);
        AtomicInteger lastOffsetX = new AtomicInteger();
        if(!this.midiStatusMap.isEmpty()){
            this.midiStatusMap.forEach((integer, integer2) -> {
                String formatted = String.format("%02d", integer);
                Text channel = Text.literal("Channel " + formatted);
                int x = this.textRenderer.getWidth(channel) + 10;

                String statusFormatted = String.format("%03d", integer2);
                context.drawText(this.textRenderer, channel, (this.width / 2) - 200, offsetY.get(), 16777215, true);
                var lastoffsetY = context.drawText(this.textRenderer, Text.literal("Status: " + statusFormatted),((this.width / 2) - 200) + x, offsetY.get(), 16777215, true);
                lastOffsetX.set(lastoffsetY);

                offsetY.addAndGet(10);
            });
        }

        if(!this.midiChannelMap.isEmpty()){
            AtomicInteger offsetY2 = new AtomicInteger(40);
            this.midiChannelMap.forEach((integer, pair) -> {
                String data1 = String.format("%03d", pair.getA());
                String data2 = String.format("%03d", pair.getB());

                int x = context.drawText(this.textRenderer, Text.literal("Data1: " + data1),lastOffsetX.get() + 10, offsetY2.get(), 16777215, true);
                context.drawText(this.textRenderer, Text.literal("Data2: " + data2),x + 10, offsetY2.get(), 16777215, true);

                offsetY2.addAndGet(10);
            });
        }
    }

    public static MyReciever getReceiver(){
        return receiver;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
