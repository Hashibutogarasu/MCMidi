package karasu_lab.mcmidi.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.*;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MidiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiManager.class);
    private static final String MID_DIRECTORY = "mid";
    private static final String MIDIS_DIRECTORY = "midis";
    private static final String MID_EXTENSION = ".mid";
    private static final String MIDI_EXTENSION = ".midi";
    private final Map<Identifier, Optional<Sequence>> midis = Maps.newConcurrentMap();
    private final DataFixer dataFixer;
    private ResourceManager resourceManager;
    private final Path generatedPath;
    private final List<Provider> providers;
    private final RegistryEntryLookup<Block> blockLookup;
    private static final ResourceFinder MIDI_NBT_RESOURCE_FINDER = new ResourceFinder("midi", ".nbt");

    public MidiManager(ResourceManager resourceManager, LevelStorage.Session session, DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup){
        this.resourceManager = resourceManager;
        this.dataFixer = dataFixer;
        this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
        this.blockLookup = blockLookup;
        ImmutableList.Builder<Provider> builder = ImmutableList.builder();
        builder.add(new Provider(this::loadMidiFromFile, this::streamMidisFromFile));
        if (SharedConstants.isDevelopment) {
            builder.add(new Provider(this::loadMidiFromGameTestFile, this::streamMidisFromGameTestFile));
        }

        builder.add(new Provider(this::loadMidiFromResource, this::streamMidisFromResource));
        this.providers = builder.build();
    }

    private Stream<Identifier> streamMidisFromResource() {
        Stream<Identifier> var10000 = MIDI_NBT_RESOURCE_FINDER.findResources(this.resourceManager).keySet().stream();
        ResourceFinder resourcefinder = MIDI_NBT_RESOURCE_FINDER;
        return var10000.map(resourcefinder::toResourceId);
    }

    private Stream<Identifier> streamMidisFromGameTestFile() {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.generatedPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator var3 = directoryStream.iterator();

                    while(var3.hasNext()) {
                        Path path = (Path)var3.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve("structures");
                        Objects.requireNonNull(list);
                        this.streamMidis(path2, string, ".nbt", list::add);
                    }
                } catch (Throwable var8) {
                    if (directoryStream != null) {
                        try {
                            directoryStream.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (directoryStream != null) {
                    directoryStream.close();
                }

                return list.stream();
            } catch (IOException var9) {
                return Stream.empty();
            }
        }
    }

    private Optional<Sequence> loadMidiFromResource(Identifier id) {
        Identifier identifier = MIDI_NBT_RESOURCE_FINDER.toResourcePath(id);
        return this.loadMidi(() -> {
            return this.resourceManager.open(identifier);
        }, (throwable) -> {
            LOGGER.error("Couldn't load midi {}", id, throwable);
        });
    }

    private Stream<Identifier> streamMidisFromFile() {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList<>();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.generatedPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator var3 = directoryStream.iterator();

                    while(var3.hasNext()) {
                        Path path = (Path)var3.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve("structures");
                        Objects.requireNonNull(list);
                        this.streamMidis(path2, string, ".midi", list::add);
                    }
                } catch (Throwable var8) {
                    if (directoryStream != null) {
                        try {
                            directoryStream.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (directoryStream != null) {
                    directoryStream.close();
                }

                return list.stream();
            } catch (IOException var9) {
                return Stream.empty();
            }
        }
    }

    private void streamMidis(Path directory, String namespace, String fileExtension, Consumer<Identifier> idConsumer) {
        int i = fileExtension.length();
        Function<String, String> function = (filename) -> {
            return filename.substring(0, filename.length() - i);
        };

        try {
            Stream<Path> stream = Files.find(directory, Integer.MAX_VALUE, (path, attributes) -> {
                return attributes.isRegularFile() && path.toString().endsWith(fileExtension);
            }, new FileVisitOption[0]);

            try {
                stream.forEach((path) -> {
                    try {
                        idConsumer.accept(Identifier.of(namespace, (String)function.apply(this.toRelativePath(directory, path))));
                    } catch (InvalidIdentifierException var7) {
                        InvalidIdentifierException invalidIdentifierException = var7;
                        LOGGER.error("Invalid location while listing folder {} contents", directory, invalidIdentifierException);
                    }

                });
            } catch (Throwable var11) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                    }
                }

                throw var11;
            }

            if (stream != null) {
                stream.close();
            }
        } catch (IOException var12) {
            IOException iOException = var12;
            LOGGER.error("Failed to list folder {} contents", directory, iOException);
        }
    }

    private String toRelativePath(Path root, Path path) {
        return root.relativize(path).toString().replace(File.separator, "/");
    }

    private Optional<Sequence> loadMidiFromGameTestFile(Identifier id) {
        return this.loadMidiFromSnbt(id, Paths.get(MidiTestUtil.getTestMidisDirectoryName));
    }

    private Optional<Sequence> loadMidiFromSnbt(Identifier id, Path path) {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path2 = PathUtil.getResourcePath(path, id.getPath(), ".snbt");

            try {
                BufferedReader bufferedReader = Files.newBufferedReader(path2);

                Optional var6;
                try {
                    String string = IOUtils.toString(bufferedReader);
                    var6 = Optional.of(this.createMidi(NbtHelper.fromNbtProviderString(string)));
                } catch (Throwable var8) {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                return var6;
            } catch (NoSuchFileException var9) {
                return Optional.empty();
            } catch (CommandSyntaxException | IOException | InvalidMidiDataException var10) {
                Exception exception = var10;
                LOGGER.error("Couldn't load structure from {}", path2, exception);
                return Optional.empty();
            }
        }
    }

    private Sequence createMidi(NbtCompound nbtCompound) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(new ByteArrayInputStream(nbtCompound.getByteArray("midi")));
    }

    private Optional<Sequence> loadMidiFromFile(Identifier id) {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path = this.getMidiPath(id, ".midi");
            return this.loadMidi(() -> {
                return new FileInputStream(path.toFile());
            }, (throwable) -> {
                LOGGER.error("Couldn't load structure from {}", path, throwable);
            });
        }
    }

    private Optional<Sequence> loadMidi(MidiFileOpener opener, Consumer<Throwable> exceptionConsumer) {
        try {
            InputStream inputStream = opener.open();

            Optional var5;
            try {
                InputStream inputStream2 = new FixedBufferInputStream(inputStream);

                try {
                    var5 = Optional.of(this.readMidi(inputStream2));
                } catch (Throwable var9) {
                    try {
                        inputStream2.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                inputStream2.close();
            } catch (Throwable var10) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var7) {
                        var10.addSuppressed(var7);
                    }
                }

                throw var10;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var5;
        } catch (FileNotFoundException var11) {
            return Optional.empty();
        } catch (Throwable var12) {
            Throwable throwable = var12;
            exceptionConsumer.accept(throwable);
            return Optional.empty();
        }
    }

    private Sequence readMidi(InputStream midiIInputStream) throws IOException, InvalidMidiDataException {
        return MidiSystem.getSequence(midiIInputStream);
    }

    private Path getMidiPath(Identifier id, String extension) {
        if (id.getPath().contains("//")) {
            throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(id));
        } else {
            try {
                Path path = this.generatedPath.resolve(id.getNamespace());
                Path path2 = path.resolve("midis");
                Path path3 = PathUtil.getResourcePath(path2, id.getPath(), extension);
                if (path3.startsWith(this.generatedPath) && PathUtil.isNormal(path3) && PathUtil.isAllowedName(path3)) {
                    return path3;
                } else {
                    throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(path3));
                }
            } catch (InvalidPathException var6) {
                InvalidPathException invalidPathException = var6;
                throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(id), invalidPathException);
            }
        }
    }

    @FunctionalInterface
    interface MidiFileOpener {
        InputStream open() throws IOException;
    }

    static record Provider(Function<Identifier, Optional<Sequence>> loader, Supplier<Stream<Identifier>> lister) {
        Provider(Function<Identifier, Optional<Sequence>> loader, Supplier<Stream<Identifier>> lister) {
            this.loader = loader;
            this.lister = lister;
        }

        public Function<Identifier, Optional<Sequence>> loader() {
            return this.loader;
        }

        public Supplier<Stream<Identifier>> lister() {
            return this.lister;
        }
    }
}
