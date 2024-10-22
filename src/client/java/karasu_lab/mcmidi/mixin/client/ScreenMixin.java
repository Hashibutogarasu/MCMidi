package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.api.networking.MidiS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement implements Drawable {
    @Shadow public abstract boolean shouldPause();

    @Inject(method = "render", at = @At("RETURN"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(MinecraftClient.getInstance().currentScreen instanceof TitleScreen || MinecraftClient.getInstance().currentScreen instanceof CreditsScreen){
            try{
                var extendedmidi = MidiS2CPacket.getMidi();
                if(extendedmidi != null){
                    MidiS2CPacket.clearMidi();
                }
            } catch (Exception ignored) {

            }
        }
    }
}
