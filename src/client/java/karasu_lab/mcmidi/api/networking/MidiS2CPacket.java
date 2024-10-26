package karasu_lab.mcmidi.api.networking;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MidiS2CPacket {
    private static ExtendedMidi midi;

    private static final ExecutorService MIDI_PLAYER_POOL;

    static {
        final ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("MidiPlayerThread");
            return thread;
        };

        MIDI_PLAYER_POOL = Executors.newSingleThreadExecutor(threadFactory);
    }

    public static void recieve(SequencePayload payload, ClientPlayNetworking.Context context) {
        MIDI_PLAYER_POOL.submit(() -> recieveAsync(payload, context));
    }

    private static void recieveAsync(SequencePayload payload, ClientPlayNetworking.Context context) {
        NbtCompound nbt = payload.nbt();

        String path = nbt.getString("path");
        int loopCount = nbt.getInt("loopCount");
        int startTick = nbt.getInt("startTick");

        SequencePayload.MidiPlayerState state = Arrays.stream(SequencePayload.MidiPlayerState.values()).filter(state1 -> state1.getName().equals(nbt.getString("state"))).toList().getFirst();

        if(state.equals(SequencePayload.MidiPlayerState.STOPPING)){
            ExtendedMidi.getCurrent().stop();
            return;
        }

        if(path == null || path.isEmpty()){
            MCMidi.LOGGER.info("No path provided in MIDI packet");
            return;
        }

        byte[] bytes = payload.bytes();

        var current = ExtendedMidi.getCurrent();
        try {
            if(current != null){
                current.stop();
            }
            midi = new ExtendedMidi(bytes, Identifier.of(MCMidi.MOD_ID, path));
        } catch (Exception e) {
            if(current != null){
                current.clear();
            }
            MCMidi.LOGGER.error("Failed to load MIDI file: {}", nbt.getString("path"));
            MCMidi.LOGGER.error(e.getMessage());

            return;
        }

        if(loopCount > 0){
            midi.setLoopCount(loopCount);
        }

        if(startTick > 0){
            midi.setStartTick(startTick);
        }

        midi.play();

        MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(Text.literal(path));
    }

    @Nullable
    public static ExtendedMidi getMidi() {
        return midi;
    }

    public static void setMidi(ExtendedMidi extendedMidi) {
        midi = extendedMidi;
    }
}
