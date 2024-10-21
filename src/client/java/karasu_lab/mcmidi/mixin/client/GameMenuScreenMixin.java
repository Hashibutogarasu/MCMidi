package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.api.networking.MidiS2CPacket;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.chaiware.midi4j.Midi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        try{
            Midi midi = MidiS2CPacket.getMidi().asMidi();

            if(midi.isPlaying()){
                midi.togglePause();
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void close() {
        try{
            Midi midi = MidiS2CPacket.getMidi().asMidi();
            if(!midi.isPlaying()){
                midi.togglePause();
            }
        } catch (Exception ignored) {

        }

        super.close();
    }
}
