package karasu_lab.mcmidi.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLangProviders {
    public static void addProviders(FabricDataGenerator.Pack pack) {
        pack.addProvider(EnglishLang::new);
        pack.addProvider(JapaneseLangProvider::new);
    }

    public static class JapaneseLangProvider extends FabricLanguageProvider {
        protected JapaneseLangProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(dataOutput, "ja_jp", registryLookup);
        }

        @Override
        public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder translationBuilder) {

        }
    }

    public static class EnglishLang extends FabricLanguageProvider {
        protected EnglishLang(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
            super(dataOutput, "en_us", registryLookup);
        }

        @Override
        public void generateTranslations(RegistryWrapper.WrapperLookup registryLookup, TranslationBuilder translationBuilder) {
            translationBuilder.add("options.midi", "MIDI Settings");
            translationBuilder.add("text.autoconfig.mcmidi.title", "MCMidi");
            translationBuilder.add("text.autoconfig.mcmidi.option.midiVolume", "MIDI Volume");
        }
    }
}