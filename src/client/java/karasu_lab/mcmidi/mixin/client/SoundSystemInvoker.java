package karasu_lab.mcmidi.mixin.client;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundManager.class)
public interface SoundSystemInvoker {
    @Accessor("soundSystem")
    public SoundSystem getSoundSystem();
}
