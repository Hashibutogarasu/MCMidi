package karasu_lab.mcmidi.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import karasu_lab.mcmidi.MCMidi;
import karasu_lab.mcmidi.api.networking.SequencePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntityDataObject;
import net.minecraft.command.StorageDataObject;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;

public class MidiCommand {
    public static final List<Function<String, DataCommand.ObjectType>> OBJECT_TYPE_FACTORIES;

    public MidiCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register((CommandManager.literal("midi").requires((source) -> {
            return source.hasPermissionLevel(2);
        }))
                .then((CommandManager.literal("load").then(CommandManager.argument("path", StringArgumentType.string())
                        .executes(MidiCommand::executeLoadCommand)))
                )
                .then((CommandManager.literal("play").then(CommandManager.argument("targets", EntityArgumentType.entities())
                                .then(CommandManager.argument("path", StringArgumentType.string())
                                        .executes(MidiCommand::playMidiCommand)))
                ))
                .then((CommandManager.literal("pause").then(CommandManager.argument("targets", EntityArgumentType.entities())
                        .executes(MidiCommand::pauseMidiCommand)))
                )
                .then((CommandManager.literal("stop").then(CommandManager.argument("targets", EntityArgumentType.entities())
                        .executes(MidiCommand::stopMidiCommand))
                )
        ));

    }

    private static int executeLoadCommand(CommandContext<ServerCommandSource> context){
        Identifier id = MCMidi.id("midi/" + StringArgumentType.getString(context, "path"));
        Optional<File> sequence = MCMidi.midiManager.loadMidiFromFile(id);
        sequence.ifPresentOrElse(sequence1 -> {
            context.getSource().sendFeedback(() -> {
                return Text.literal("Loaded MIDI file: " + id);
            }, true);
        }, () -> {
            context.getSource().sendError(Text.literal("Failed to load MIDI file: " + id));
        });

        return sequence.isPresent() ? 1 : 0;
    }

    private static int playMidiCommand(CommandContext<ServerCommandSource> context){
        Identifier id = MCMidi.id("midi/" + StringArgumentType.getString(context, "path"));
        MCMidi.midiManager.loadMidiFromFile(id).ifPresent(file -> {
            List<Byte> byteList = new ArrayList<>();

            try {
                byte[] data = Files.readAllBytes(file.toPath());
                for (byte b : data) {
                    byteList.add(b);
                }
            } catch (IOException ignored) {
            }

            NbtCompound nbt = new NbtCompound();
            nbt.putByteArray("data", byteList);
            nbt.putString("state", SequencePayload.MidiPlayerState.PLAYING.getName());

            ServerPlayNetworking.send((ServerPlayerEntity) context.getSource().getEntity(), new SequencePayload(nbt, new File(MCMidi.midiManager.getMidiPath(id, ".midi").toUri())));
        });
        return 0;
    }

    private static int pauseMidiCommand(CommandContext<ServerCommandSource> context){
        return 0;
    }

    private static int stopMidiCommand(CommandContext<ServerCommandSource> context){
        return 0;
    }

    static {
        OBJECT_TYPE_FACTORIES = List.of(EntityDataObject.TYPE_FACTORY, BlockDataObject.TYPE_FACTORY, StorageDataObject.TYPE_FACTORY);
    }
}
