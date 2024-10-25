package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.api.SoundFontManager;
import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
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
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class SoundFontManagerScreen extends GameOptionsScreen implements IScreen {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundFontManagerScreen.class);
    private static final String SOUNDFONT_DIRECTORY = "soundfont";
    private static final String SOUNDFONT_EXTENTION = ".sf2";

    private SoundFontOptionListWidget soundFontOptionListWidget;
    private final ModConfig config;

    public SoundFontManagerScreen(Screen parent) {
        this(parent, MinecraftClient.getInstance().options);
    }

    public SoundFontManagerScreen(Screen parent, GameOptions gameOptions) {
        super(parent, gameOptions, Text.translatable("mcmidi.options.title"));
        this.layout.setFooterHeight(53);
        this.config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        ExtendedMidi current = ExtendedMidi.getCurrent();

        if(current != null){
            current.stop();
            current.clear();
        }
    }

    protected void initBody(){
        this.soundFontOptionListWidget = this.layout.addBody(new SoundFontOptionListWidget(this.client));
    }

    protected void initFooter(){
        DirectionalLayoutWidget directionalLayoutWidget = (this.layout.addFooter(DirectionalLayoutWidget.vertical())).spacing(8);
        DirectionalLayoutWidget directionalLayoutWidget2 = directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
        directionalLayoutWidget2.add(ButtonWidget.builder(Text.translatable("text.mcmidi.opensoundfontdirectory"), (button) -> {
            this.openSoundFontDirectory();
        }).build());
        directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
            this.onDone();
        }).build());
    }

    @Override
    protected void initTabNavigation() {
        super.initTabNavigation();
        this.soundFontOptionListWidget.position(this.width, this.layout);
    }

    @Override
    protected void addOptions() {

    }

    private void onDone(){
        SoundFontOptionListWidget.SoundFontEntry selected = this.soundFontOptionListWidget.getSelectedOrNull();

        if(selected != null){
            File file = new File("soundfont/" + selected.soundFont.path());
            if(!file.exists()){
                LOGGER.error("Soundfont file does not exist use default");
                config.soundFontPath = "";
                return;
            }

            config.soundFontPath = file.getAbsolutePath();
        }

        AutoConfig.getConfigHolder(ModConfig.class).save();

        this.close();
    }

    @Override
    public void close() {
        super.close();
    }

    public void openSoundFontDirectory() {
        File file = new File(SOUNDFONT_DIRECTORY);
        if (!file.exists()) {
            file.mkdirs();
        }

        Util.getOperatingSystem().open(file);
    }

    public List<String> getLocalSoundFonts() {
        File file = new File(SOUNDFONT_DIRECTORY);

        if (!file.exists()) {
            file.mkdirs();
        }

        List<String> soundfonts = new ArrayList<>();
        var listfiles = file.listFiles((dir, name) -> name.endsWith(SOUNDFONT_EXTENTION));
        if(listfiles != null){
            for (File listFile : listfiles) {
                soundfonts.add(listFile.getName());
            }
        }

        return soundfonts;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private class SoundFontOptionListWidget extends AlwaysSelectedEntryListWidget<SoundFontOptionListWidget.SoundFontEntry> {
        public SoundFontOptionListWidget(MinecraftClient minecraftClient) {
            super(minecraftClient, SoundFontManagerScreen.this.width, SoundFontManagerScreen.this.height - 33 - 53, 33, 18);

            List<String> soundfonts = SoundFontManagerScreen.this.getLocalSoundFonts();

            for (String soundfont : soundfonts) {
                SoundFontEntry entry = new SoundFontEntry(new SoundFontManager.SoundFont(soundfont));
                addEntry(entry);

                if (soundfont.equals(SoundFontManagerScreen.this.config.soundFontPath)) {
                    this.setSelected(entry);
                }
            }

            if (this.getSelectedOrNull() != null) {
                this.centerScrollOn(this.getSelectedOrNull());
            }
        }

        public void addSoundFontEntry(String path){
            addEntry(new SoundFontEntry(new SoundFontManager.SoundFont(path)));
        }

        public void clearSoundFontEntries(){
            clearEntries();
        }

        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        public class SoundFontEntry extends AlwaysSelectedEntryListWidget.Entry<SoundFontEntry>{
            private final SoundFontManager.SoundFont soundFont;
            private long clickTime;

            public SoundFontEntry(SoundFontManager.SoundFont soundFont) {
                this.soundFont = soundFont;
            }

            @Override
            public Text getNarration() {
                return Text.translatable("narrator.select", this.soundFont.getName());
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                TextRenderer textRenderer = SoundFontManagerScreen.this.textRenderer;
                Text soundFontName = Text.literal(this.soundFont.getName());
                int width = SoundFontManagerScreen.SoundFontOptionListWidget.this.width / 2;
                int height = y + entryHeight / 2;

                context.drawCenteredTextWithShadow(textRenderer, soundFontName, width, height - 9 / 2, -1);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (KeyCodes.isToggle(keyCode)) {
                    this.onPressed();
                    SoundFontManagerScreen.this.onDone();
                    return true;
                } else {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }

            private void onPressed() {
                SoundFontManagerScreen.SoundFontOptionListWidget.this.setSelected(this);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                this.onPressed();
                if (Util.getMeasuringTimeMs() - this.clickTime < 250L) {
                    SoundFontManagerScreen.this.onDone();
                }

                this.clickTime = Util.getMeasuringTimeMs();
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
    }
}
