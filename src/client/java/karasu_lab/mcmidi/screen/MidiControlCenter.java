package karasu_lab.mcmidi.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MidiControlCenter extends Screen {
    public MidiControlCenter(){
        this(Text.translatable("options.mcmidi"));
    }

    protected MidiControlCenter(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
    }
}
