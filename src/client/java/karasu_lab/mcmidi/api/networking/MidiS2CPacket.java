package karasu_lab.mcmidi.api.networking;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;

public class MidiS2CPacket {
    private static String playingPath;
    private static ExtendedMidi midi;

    public static void recieve(SequencePayload payload, ClientPlayNetworking.Context context) {
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
            midi = new ExtendedMidi(bytes, Identifier.of(path));
        } catch (Exception e) {
            if(current != null){
                current.clear();
            }
            MCMidi.LOGGER.error("Failed to load MIDI file: {}", nbt.getString("path"));
            MCMidi.LOGGER.error(e.getMessage());

            return;
        }

        playingPath = path;
        midi.saveToLocal(bytes, path);

        if(loopCount > 0){
            midi.setLoopCount(loopCount);
        }

        if(startTick > 0){
            midi.setStartTick(startTick);
        }

        midi.playAsync();

        MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(Text.literal(path));
    }

    public static String getPlayingPath() {
        return playingPath;
    }

    public static void setPlayingPath(String path) {
        playingPath = path;
    }


    @Nullable
    public static ExtendedMidi getMidi() {
        return midi;
    }

    public static void clearMidi() {
        midi.stop();
        midi = null;
        playingPath = null;
    }

    public static void setMidi(ExtendedMidi extendedMidi) {
        midi = extendedMidi;
    }
}
