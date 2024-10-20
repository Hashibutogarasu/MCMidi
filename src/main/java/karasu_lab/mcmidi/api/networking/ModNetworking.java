package karasu_lab.mcmidi.api.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModNetworking {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(SequencePayload.ID, SequencePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SequencePayload.ID, MidiC2SPacket::recieve);
    }
}
