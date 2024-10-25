package karasu_lab.mcmidi.api.networking;

import karasu_lab.mcmidi.MCMidi;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SequencePayload(NbtCompound nbt, byte[] bytes) implements CustomPayload {
    public static final Identifier IDENTIFIER = MCMidi.id("midi_packet");
    public static final CustomPayload.Id<SequencePayload> ID = new CustomPayload.Id<>(IDENTIFIER);
    public static PacketCodec<RegistryByteBuf, SequencePayload> CODEC = new PacketCodec<>() {
        public SequencePayload decode(RegistryByteBuf byteBuf) {
            NbtCompound nbt = PacketByteBuf.readNbt(byteBuf);
            byte[] bytes = byteBuf.readByteArray();
            return new SequencePayload(nbt, bytes);
        }

        public void encode(RegistryByteBuf byteBuf, SequencePayload payload) {
            NbtCompound nbt = payload.nbt();
            PacketByteBuf.writeNbt(byteBuf, nbt);
            PacketByteBuf.writeByteArray(byteBuf, payload.bytes());
        }
    };

    public static PacketCodec<RegistryByteBuf, SequencePayload> getCODEC() {
        return CODEC;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static enum MidiPlayerState {
        LOADING("loading"),
        PLAYING("playing"),
        PAUSING("pausing"),
        CHANGE_SOUNDFONT("change_soundfont"),
        STOPPING("stopping");

        private final String name;

        MidiPlayerState(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
