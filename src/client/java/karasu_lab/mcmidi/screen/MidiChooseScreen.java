package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.api.networking.MidiS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class MidiChooseScreen extends GameOptionsScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiChooseScreen.class);
    private static final String MIDI_DIRECTORY = "midi";
    private static final String MIDI_EXTENTION = ".midi";

    private MidiListWidget midiFileListWidget;
    private final FileAlterationMonitor monitor;

    public MidiChooseScreen(Screen parent) {
        super(parent, MinecraftClient.getInstance().options, Text.translatable("mcmidi.options.title"));
        this.layout.setFooterHeight(53);

        File file = new File(MIDI_DIRECTORY);

        this.monitor = new FileAlterationMonitor(1000);
        FileAlterationObserver observer = new FileAlterationObserver(file);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                onChanged();
                super.onFileCreate(file);
            }

            @Override
            public void onFileChange(File file) {
                onChanged();
                super.onFileChange(file);
            }

            @Override
            public void onFileDelete(File file) {
                onChanged();
                super.onFileDelete(file);
            }

            void onChanged(){
                MidiChooseScreen.this.midiFileListWidget.clearSoundFontEntries();
                for (String localSoundFont : getLocalMidiFiles()) {
                    MidiChooseScreen.this.midiFileListWidget.addSoundFontEntry(localSoundFont);
                }
            }
        };

        observer.addListener(listener);
        this.monitor.addObserver(observer);
        try {
            this.monitor.start();
        } catch (Exception ignored) {

        }
    }

    @Override
    public void close() {
        try {
            this.monitor.stop();
        } catch (Exception ignored) {

        }
        super.close();
    }

    protected void initBody(){
        this.midiFileListWidget = this.layout.addBody(new MidiListWidget(this.client));
    }

    protected void initFooter(){
        DirectionalLayoutWidget directionalLayoutWidget = (this.layout.addFooter(DirectionalLayoutWidget.vertical())).spacing(8);
        DirectionalLayoutWidget directionalLayoutWidget2 = directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
        directionalLayoutWidget2.add(ButtonWidget.builder(Text.translatable("text.mcmidi.openmididirectory"), (button) -> {
            this.openSoundFontDirectory();
        }).build());
        directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
            this.onDone();
        }).build());
    }

    @Override
    protected void initTabNavigation() {
        super.initTabNavigation();
        this.midiFileListWidget.position(this.width, this.layout);
    }

    @Override
    protected void addOptions() {

    }

    private void onDone(){
        MidiListWidget.MidiEntry selected = this.midiFileListWidget.getSelectedOrNull();

        if(selected != null){
            File file = new File(MIDI_DIRECTORY + "/" + selected.path);
            try {
                var currentMidi = MidiS2CPacket.getMidi();
                if (currentMidi != null) {
                    currentMidi.stop();
                }
                MidiS2CPacket.setMidi(new ExtendedMidi(file));
                var extendedMidi = MidiS2CPacket.getMidi();
                MidiS2CPacket.setPlayingPath(selected.path);

                if (extendedMidi != null) {
                    extendedMidi.play();
                }
            } catch (Exception ignored) {

            }
        }

        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public void openSoundFontDirectory() {
        File file = new File(MIDI_DIRECTORY);
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getOperatingSystem().open(file);
    }

    public List<String> getLocalMidiFiles() {
        File file = new File(MIDI_DIRECTORY);

        if (!file.exists()) {
            file.mkdirs();
        }

        List<String> soundfonts = new ArrayList<>();
        var listfiles = file.listFiles((dir, name) -> name.endsWith(MIDI_EXTENTION));
        if(listfiles != null){
            for (File listFile : listfiles) {
                soundfonts.add(listFile.getName());
            }
        }

        return soundfonts;
    }

    private class MidiListWidget extends AlwaysSelectedEntryListWidget<MidiListWidget.MidiEntry> {
        public MidiListWidget(MinecraftClient minecraftClient) {
            super(minecraftClient, MidiChooseScreen.this.width, MidiChooseScreen.this.height - 33 - 53, 33, 18);

            List<String> paths = MidiChooseScreen.this.getLocalMidiFiles();

            for (String path : paths) {
                MidiEntry entry = new MidiEntry(path);
                addEntry(entry);

            }

            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn(this.getSelectedOrNull());
            }
        }

        public void addSoundFontEntry(String path){
            addEntry(new MidiEntry(path));
        }

        public void clearSoundFontEntries(){
            clearEntries();
        }

        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        public class MidiEntry extends AlwaysSelectedEntryListWidget.Entry<MidiEntry>{
            private final String path;
            private long clickTime;

            public MidiEntry(String path) {
                this.path = path;
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.path);
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                TextRenderer textRenderer = MidiChooseScreen.this.textRenderer;
                Text midifilepath = Text.literal(this.path);
                int width = MidiListWidget.this.width / 2;
                int height = y + entryHeight / 2;

                context.drawCenteredTextWithShadow(textRenderer, midifilepath, width, height - 9 / 2, -1);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (KeyCodes.isToggle(keyCode)) {
                    this.onPressed();
                    MidiChooseScreen.this.onDone();
                    return true;
                } else {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }

            private void onPressed() {
                MidiListWidget.this.setSelected(this);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                this.onPressed();
                if (Util.getMeasuringTimeMs() - this.clickTime < 250L) {
                    MidiChooseScreen.this.onDone();
                }

                this.clickTime = Util.getMeasuringTimeMs();
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}