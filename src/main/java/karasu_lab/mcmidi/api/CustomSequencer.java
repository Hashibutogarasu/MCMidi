package karasu_lab.mcmidi.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtType;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.visitor.NbtElementVisitor;

import javax.sound.midi.Sequencer;
import java.io.DataOutput;
import java.io.IOException;

public abstract class CustomSequencer extends NbtCompound implements Sequencer {
    @Override
    public void write(DataOutput output) throws IOException {

    }

    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

    @Override
    public void accept(NbtElementVisitor visitor) {

    }

    @Override
    public NbtScanner.Result doAccept(NbtScanner visitor) {
        return null;
    }
}
