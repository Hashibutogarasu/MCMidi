package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.options.MidiSound;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Shadow private boolean started;

    @Inject(at = @At("HEAD"), method = "updateSoundVolume")
    private void updateSoundVolume(SoundCategory category, float volume, CallbackInfo ci) {
        if(started){
            if ((Object)category instanceof MidiSound midiSound) {
                MCMidi.LOGGER.info("Playing MIDI sound at volume {}", volume);

                ci.cancel();
            }
        }
    }
}
