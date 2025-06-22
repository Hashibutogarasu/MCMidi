package karasu_lab.mcmidi.screen;

import com.mojang.authlib.minecraft.client.MinecraftClient;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SoundControllerScreen extends Screen implements IScreen {
    private final Screen parent;
    private ButtonWidget playButton;
    private ButtonWidget pauseButton;
    private ButtonWidget stopButton;

    public SoundControllerScreen(Screen parent) {
        this(Text.translatable("mcmidi.midi_sound_controller"), parent);
    }

    protected SoundControllerScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.playButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("mcmidi.sound_controller.play"),
                button -> {
                    ExtendedMidi current = ExtendedMidi.getCurrent();
                    if (current != null) {
                        current.play();
                    }
                }).dimensions((this.width / 2) - 110, this.height - 30, 70, 20).build());

        this.pauseButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("mcmidi.sound_controller.pause"),
                button -> {
                    ExtendedMidi current = ExtendedMidi.getCurrent();
                    if (current != null) {
                        current.pause();
                    }
                }).dimensions((this.width / 2) - 35, this.height - 30, 70, 20).build());

        this.stopButton = this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("mcmidi.sound_controller.stop"),
                button -> {
                    ExtendedMidi current = ExtendedMidi.getCurrent();
                    if (current != null) {
                        current.stop();
                    }
                }).dimensions((this.width / 2) + 40, this.height - 30, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.back"),
                button -> this.close()).dimensions((this.width / 2) - 100, this.height - 60, 200, 20).build());

        this.updateButtonStates();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 16777215);

        ExtendedMidi current = ExtendedMidi.getCurrent();
        if (current != null) {
            Text infoText = Text.translatable("mcmidi.sound_controller.now_playing");
            context.drawText(this.textRenderer, infoText, (this.width / 2) - this.textRenderer.getWidth(infoText) / 2,
                    30, 16777215, true);

            Text midiName = current.getPlayingPath();
            context.drawText(this.textRenderer, midiName, (this.width / 2) - this.textRenderer.getWidth(midiName) / 2,
                    45, 16777215, true);

            String stateText = current.isPlaying() ? "mcmidi.sound_controller.state.playing"
                    : "mcmidi.sound_controller.state.paused";
            Text state = Text.translatable(stateText);
            context.drawText(this.textRenderer, state, (this.width / 2) - this.textRenderer.getWidth(state) / 2, 60,
                    16777215, true);

            Text bpmText = Text.literal("BPM: " + current.getBPM());
            context.drawText(this.textRenderer, bpmText, (this.width / 2) - this.textRenderer.getWidth(bpmText) / 2, 75,
                    16777215, true);
        } else {
            Text noMidiText = Text.translatable("mcmidi.sound_controller.no_midi");
            context.drawText(this.textRenderer, noMidiText,
                    (this.width / 2) - this.textRenderer.getWidth(noMidiText) / 2, 50, 16777215, true);
        }

        this.updateButtonStates();
    }

    private void updateButtonStates() {
        ExtendedMidi current = ExtendedMidi.getCurrent();
        boolean hasMidi = current != null;
        boolean isPlaying = hasMidi && current.isPlaying();

        this.playButton.active = hasMidi && !isPlaying;
        this.pauseButton.active = hasMidi && isPlaying;
        this.stopButton.active = hasMidi;
    }

    @Override
    public void playCurrent() {
        ExtendedMidi current = ExtendedMidi.getCurrent();
        if (current != null && !current.isPlaying()) {
            current.play();
        }
    }

    @Override
    public void close() {
        this.playCurrent();
        if (parent != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
