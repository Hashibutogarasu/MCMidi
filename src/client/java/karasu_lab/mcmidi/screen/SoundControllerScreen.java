package karasu_lab.mcmidi.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SoundControllerScreen extends Screen {
    private final Screen parent;

    public SoundControllerScreen(Screen parent) {
        this(Text.translatable("mcmidi.midi_sound_controller"), parent);
    }

    protected SoundControllerScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 16777215);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
