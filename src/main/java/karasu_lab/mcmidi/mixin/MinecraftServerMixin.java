package karasu_lab.mcmidi.mixin;

import com.mojang.datafixers.DataFixer;
import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.api.MidiManager;
import net.minecraft.block.Block;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin{
	@Shadow @Final protected SaveProperties saveProperties;
	@Shadow @Final private CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries;

	@Inject(at = @At(value = "TAIL"), method = "<init>")
	private void init(Thread serverThread, LevelStorage.Session session, ResourcePackManager resourcePackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci) {
		RegistryEntryLookup<Block> registryEntryLookup = this.combinedDynamicRegistries.getCombinedRegistryManager().get(RegistryKeys.BLOCK).getReadOnlyWrapper().withFeatureFilter(this.saveProperties.getEnabledFeatures());
		MCMidi.midiManager = new MidiManager(saveLoader.resourceManager(), session, dataFixer, registryEntryLookup);
	}
}
