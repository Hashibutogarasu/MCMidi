package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public abstract boolean shouldPause();

    @Unique
    private ExtendedMidi current;

    @Unique
    private long position = 0;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
    public void init(CallbackInfo ci){
        if(shouldPause()){
            this.current = ExtendedMidi.getCurrent();

            if(this.current != null){
                this.position = this.current.getPosition();
                this.current.setPosition(this.position);
                this.current.pause();
            }
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    public void close(CallbackInfo ci){
        if(shouldPause()){
            if(this.current != null){
                this.current.play();
                this.current.setPosition(this.position);
            }
        }
    }
}
