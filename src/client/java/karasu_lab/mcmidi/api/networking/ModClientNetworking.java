package karasu_lab.mcmidi.api.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModClientNetworking {
    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(SequencePayload.ID, SequencePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SequencePayload.ID, MidiS2CPacket::recieve);
    }
}
