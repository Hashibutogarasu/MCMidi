package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import karasu_lab.mcmidi.screen.SoundFontManagerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Shadow protected abstract ButtonWidget createButton(Text message, Supplier<Screen> screenSupplier);

    @Shadow @Final private static Text LANGUAGE_TEXT;

    @Shadow @Final private GameOptions settings;

    @Unique
    private final Text MCMIDI_TEXT = Text.translatable("mcmidi.options.title");

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/GridWidget$Adder;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;", ordinal = 4))
    private <T extends Widget> T init(GridWidget.Adder adder, T widget) {
        MinecraftClient client = MinecraftClient.getInstance();
        adder.add(this.createButton(LANGUAGE_TEXT, () -> {
            return new LanguageOptionsScreen(this, this.settings, client.getLanguageManager());
        }));

        adder.add(this.createButton(MCMIDI_TEXT, MidiControlCenter::new));

        return null;
    }
}
