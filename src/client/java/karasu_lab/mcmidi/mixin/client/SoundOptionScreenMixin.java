package karasu_lab.mcmidi.mixin.client;

import karasu_lab.mcmidi.MCMidi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Mixin(GameOptions.class)
public abstract class SoundOptionScreenMixin {
    @Shadow @Final private Map<SoundCategory, SimpleOption<Double>> soundVolumeLevels;

    @Shadow protected abstract SimpleOption<Double> createSoundVolumeOption(String key, SoundCategory category);

    @Inject(at = @At("RETURN"), method = "<init>")
    private void init(MinecraftClient client, File optionsFile, CallbackInfo ci) {
        try {
            SoundCategory soundCategory = addEnumValue(SoundCategory.values(), "MIDI");
            this.soundVolumeLevels.put(soundCategory, this.createSoundVolumeOption("options.midi", soundCategory));
        } catch (Throwable throwable) {
            MCMidi.LOGGER.error("Failed to add MIDI sound category");
            MCMidi.LOGGER.error(throwable.getMessage());
        }
    }

    @Unique
    private static <T extends Enum<T>> T addEnumValue(T[] values, String name) throws ClassCastException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        //getenumconstants
        Field valuesField = Class.class.getDeclaredField("enumConstantDirectory");
        valuesField.setAccessible(true);
        Object[] valuesArray = (Object[]) valuesField.get(values[0].getClass());
        //create new array
        T[] newValues = (T[]) new Enum[valuesArray.length + 1];
        //copy values
        System.arraycopy(valuesArray, 0, newValues, 0, valuesArray.length);
        //add new value
        T newValue = (T) Enum.valueOf(values[0].getClass(), name);
        newValues[valuesArray.length] = newValue;
        //set new array
        valuesField.set(values[0].getClass(), newValues);
        return newValue;
    }
}
