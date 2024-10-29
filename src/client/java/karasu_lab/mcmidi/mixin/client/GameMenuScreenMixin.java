package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends AbstractParentElement implements Drawable {
    @Inject(method = "disconnect", at = @At("RETURN"))
    private void close(CallbackInfo ci) {
        var current = ExtendedMidi.getCurrent();
        if (current != null) {
            current.stopCurrent();
        }
    }
}
