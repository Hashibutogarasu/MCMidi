package karasu_lab.mcmidi.api.networking;

import karasu_lab.mcmidi.MCMidiClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class MidiS2CPacket {
    private static Sequencer sequencer = MCMidiClient.CLIENT_SEQUENCER;
    private static MidiDevice device;

    public static void recieve(SequencePayload payload, ClientPlayNetworking.Context context) {
        NbtCompound nbt = payload.nbt();

        SequencePayload.MidiPlayerState state = Arrays.stream(SequencePayload.MidiPlayerState.values()).filter(state1 -> state1.getName().equals(nbt.getString("state"))).toList().getFirst();

        try {
            if(sequencer == null){
                sequencer = MidiSystem.getSequencer(false);
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

            if(state.equals(SequencePayload.MidiPlayerState.STOPPING)){
                stop();
                return;
            }

            byte[] bytes = nbt.getByteArray("data");
            play(bytes);
        } catch (MidiUnavailableException e) {
            MCMidiClient.LOGGER.error("Failed to load midi");
            MCMidiClient.LOGGER.error(e.getMessage());
        }
    }

    public static void mute(){
        try {
            for (MidiChannel channel : MidiSystem.getSynthesizer().getChannels()) {
                if(!channel.getMute()){
                    channel.setMute(true);
                }
            }
        } catch (MidiUnavailableException ignored) {

        }
    }

    public static void unmute(){
        try {
            for (MidiChannel channel : MidiSystem.getSynthesizer().getChannels()) {
                if(channel.getMute()){
                    channel.setMute(false);
                }
            }
        } catch (MidiUnavailableException ignored) {

        }
    }

    public static void stop(){
       if(sequencer != null){
           if(sequencer.isOpen() || sequencer.isRunning()){
               sequencer.stop();
               sequencer.close();
               if(device != null && device.isOpen()){
                   device.close();
               }
           }
       }
    }

    public static void play(byte[] data) {
        if(device == null){
            return;
        }

        device.close();
        sequencer.close();

        try {
            sequencer.setSequence(new ByteArrayInputStream(data));
            sequencer.getTransmitter().setReceiver(device.getReceiver());
            device.open();
            sequencer.open();
            sequencer.start();
        } catch (InvalidMidiDataException | MidiUnavailableException | IOException e) {
            MCMidiClient.LOGGER.error("Failed to play midi");
        }
    }
}
