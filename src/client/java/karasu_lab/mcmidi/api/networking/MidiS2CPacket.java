package karasu_lab.mcmidi.api.networking;

import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.MCMidiClient;
import karasu_lab.mcmidi.api.midi.ExtendedMidi;
import karasu_lab.mcmidi.screen.MidiControlCenter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import javax.sound.midi.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class MidiS2CPacket {
    private static final Sequencer sequencer = MCMidiClient.CLIENT_SEQUENCER;
    private static MidiDevice device;
    private static String playingPath;
    private static ExtendedMidi midi;

    public static void recieve(SequencePayload payload, ClientPlayNetworking.Context context) {
        NbtCompound nbt = payload.nbt();

        String path = nbt.getString("path");
        SequencePayload.MidiPlayerState state = Arrays.stream(SequencePayload.MidiPlayerState.values()).filter(state1 -> state1.getName().equals(nbt.getString("state"))).toList().getFirst();

        if(state.equals(SequencePayload.MidiPlayerState.STOPPING)){
            try {
                midi.stop();
            } catch (Exception ignored) {

            }
            return;
        }

        if(path == null || path.isEmpty()){
            MCMidi.LOGGER.info("No path provided in MIDI packet");
            return;
        }

        Optional<MidiDevice> device = Arrays.stream(MidiSystem.getMidiDeviceInfo()).toList().stream().map(info -> {
            try {
                return MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                return null;
            }
        }).filter(Objects::nonNull).filter(midiDevice -> midiDevice.getDeviceInfo().getName().contains("VirtualMIDISynth #1")).findFirst();

        device.ifPresent(midiDevice -> {
            MidiS2CPacket.device = midiDevice;
        });

        byte[] bytes = nbt.getByteArray("data");

        try {
            if(midi != null){
                midi.stop();
            }
            midi = new ExtendedMidi(bytes, path);
        } catch (Exception e) {
            MCMidi.LOGGER.error("Failed to load MIDI file: {}", nbt.getString("path"));
            MCMidi.LOGGER.error(e.getMessage());

            return;
        }

        playingPath = path;
        midi.saveToLocal(bytes, path);
        midi.play();

        MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(Text.literal(path));
    }

    public static String getPlayingPath() {
        return playingPath;
    }

    @Nullable
    public static ExtendedMidi getMidi() {
        return midi;
    }
}
