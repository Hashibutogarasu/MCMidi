package karasu_lab.mcmidi.screen;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.mixin.client.SoundSystemInvoker;
import karasu_lab.mcmidi.options.MidiSound;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class MidiOptionsScreen extends Screen {
    private final SimpleOption<Double> midiVolume;

    @Nullable
    protected OptionListWidget body;

    public MidiOptionsScreen(){
        this(Text.translatable("options.mcmidi"));
    }

    protected MidiOptionsScreen(Text title) {
        super(title);

        this.midiVolume = createSoundVolumeOption("options.midi", MidiSound.MIDI);
    }

    @Override
    protected void init() {
        super.init();
        if(body != null){
            body.addSingleOptionEntry(this.midiVolume);
        }
    }

    private SimpleOption<Double> createSoundVolumeOption(String key, MidiSound category) {
        return new SimpleOption<>(key, SimpleOption.emptyTooltip(), MidiOptionsScreen::getPercentValueOrOffText, SimpleOption.DoubleSliderCallbacks.INSTANCE, 1.0, (value) -> {
            float f = (float)(double)value;
            ((SoundSystemInvoker)MinecraftClient.getInstance().getSoundManager()).getSoundSystem().updateSoundVolume((SoundCategory.MASTER), f);
        });
    }

    private static Text getPercentValueOrOffText(Text text, Object object) {
        return object instanceof Double ? getPercentValueOrOffText(text, object) : ScreenTexts.OFF;
    }

    private static Text getPercentValueOrOffText(Text prefix, double value) {
        return value == 0.0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getPercentValueText(prefix, value);
    }

    public static Text getGenericValueText(Text prefix, Text value) {
        return Text.translatable("options.generic_value", new Object[]{prefix, value});
    }

    private static Text getPercentValueText(Text prefix, double value) {
        return Text.translatable("options.percent_value", new Object[]{prefix, (int)(value * 100.0)});
    }
}
