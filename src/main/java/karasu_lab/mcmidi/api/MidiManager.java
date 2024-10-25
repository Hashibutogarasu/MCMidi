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
    private static final String MIDI_DIRECTORY = "midi";
    private static final String MIDI_EXTENSION = ".midi";
    private final Map<Identifier, Optional<Sequence>> midis = Maps.newConcurrentMap();
    private final DataFixer dataFixer;
    private ResourceManager resourceManager;
    private Path midisPath;
    private final List<Provider> providers;
    private final RegistryEntryLookup<Block> blockLookup;
    private static final ResourceFinder MIDI_NBT_RESOURCE_FINDER = new ResourceFinder(MIDI_DIRECTORY, MIDI_EXTENSION);

    public MidiManager(ResourceManager resourceManager, LevelStorage.Session session, DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup){
        this.resourceManager = resourceManager;
        this.dataFixer = dataFixer;
        this.midisPath = Path.of(MIDI_DIRECTORY).toAbsolutePath();
        try {
            this.midisPath = this.midisPath.toRealPath();
        }
        catch (IOException exception){
            LOGGER.error("Failed to resolve real path for midi directory", exception);
        }
        this.blockLookup = blockLookup;
        ImmutableList.Builder<Provider> builder = ImmutableList.builder();
        builder.add(new Provider(this::loadMidiFromFile, this::streamMidisFromFile));
        if (SharedConstants.isDevelopment) {
            builder.add(new Provider(this::loadMidiFromGameTestFile, this::streamMidisFromGameTestFile));
        }

        builder.add(new Provider(this::loadMidiFromResource, this::streamMidisFromResource));
        this.providers = builder.build();
    }

    public Stream<Identifier> streamMidisFromResource() {
        Stream<Identifier> finder = MIDI_NBT_RESOURCE_FINDER.findResources(this.resourceManager).keySet().stream();
        return finder.map(MIDI_NBT_RESOURCE_FINDER::toResourceId);
    }

    public Stream<Identifier> streamMidisFromGameTestFile() {
        if (!Files.isDirectory(this.midisPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList<>();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.midisPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator<Path> pathIterator = directoryStream.iterator();

                    while(pathIterator.hasNext()) {
                        Path path = pathIterator.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve(MIDI_DIRECTORY);
                        Objects.requireNonNull(list);
                        this.streamMidis(path2, string, MIDI_EXTENSION, list::add);
                    }
                } catch (Throwable throwable1) {
                    if (directoryStream != null) {
                        try {
                            directoryStream.close();
                        } catch (Throwable throwable2) {
                            throwable1.addSuppressed(throwable2);
                        }
                    }

                    throw throwable1;
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

    public Optional<File> loadMidiFromResource(Identifier id) {
        Identifier identifier = MIDI_NBT_RESOURCE_FINDER.toResourcePath(id);
        return this.loadMidi(() -> {
            return this.resourceManager.open(identifier);
        }, (throwable) -> {
            LOGGER.error("Couldn't load midi {}", id, throwable);
        });
    }

    public Stream<Identifier> streamMidisFromFile() {
        if (!Files.isDirectory(this.midisPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList<>();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.midisPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator<Path> var3 = directoryStream.iterator();

                    while(var3.hasNext()) {
                        Path path = var3.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve(MIDI_DIRECTORY);
                        Objects.requireNonNull(list);
                        this.streamMidis(path2, string, MIDI_EXTENSION, list::add);
                    }
                } catch (Throwable throwable1) {
                    if (directoryStream != null) {
                        try {
                            directoryStream.close();
                        } catch (Throwable throwable2) {
                            throwable1.addSuppressed(throwable2);
                        }
                    }

                    throw throwable1;
                }

                if (directoryStream != null) {
                    directoryStream.close();
                }

                return list.stream();
            } catch (IOException exception) {
                return Stream.empty();
            }
        }
    }

    public void streamMidis(Path directory, String namespace, String fileExtension, Consumer<Identifier> idConsumer) {
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
                    } catch (InvalidIdentifierException exception) {
                        LOGGER.error("Invalid location while listing folder {} contents", directory, exception);
                    }

                });
            } catch (Throwable throwable1) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable throwable2) {
                        throwable1.addSuppressed(throwable2);
                    }
                }

                throw throwable1;
            }

            if (stream != null) {
                stream.close();
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to list folder {} contents", directory, exception);
        }
    }

    public String toRelativePath(Path root, Path path) {
        return root.relativize(path).toString().replace(File.separator, "/");
    }

    public Optional<File> loadMidiFromGameTestFile(Identifier id) {
        return this.loadMidiFromSnbt(id, Paths.get(MidiTestUtil.getTestMidisDirectoryName));
    }

    public Optional<File> loadMidiFromSnbt(Identifier id, Path path) {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path2 = PathUtil.getResourcePath(path, id.getPath(), ".snbt");

            try {
                BufferedReader bufferedReader = Files.newBufferedReader(path2);

                Optional<File> sequenceOptional;
                try {
                    String string = IOUtils.toString(bufferedReader);
                    sequenceOptional = Optional.of(this.createMidi(NbtHelper.fromNbtProviderString(string)));
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

                return sequenceOptional;
            } catch (NoSuchFileException exception) {
                return Optional.empty();
            } catch (CommandSyntaxException | IOException | InvalidMidiDataException exception) {
                LOGGER.error("Couldn't load structure from {}", path2, exception);
                return Optional.empty();
            }
        }
    }

    public File createMidi(NbtCompound nbtCompound) throws InvalidMidiDataException, IOException {
        File tempFile = File.createTempFile(MIDI_DIRECTORY, MIDI_EXTENSION);
        Files.write(tempFile.toPath(), nbtCompound.getByteArray(MIDI_DIRECTORY));
        return tempFile;
    }

    public Optional<File> loadMidiFromFile(Identifier id) {
        if (!Files.isDirectory(this.midisPath, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path = this.getMidiPath(id, MIDI_EXTENSION);
            LOGGER.info("Loading midi from {}", path);
            return this.loadMidi(() -> {
                return new FileInputStream(path.toFile());
            }, (throwable) -> {
                LOGGER.error("Couldn't load midi from {}", path, throwable);
            });
        }
    }

    public Optional<File> loadMidi(MidiFileOpener opener, Consumer<Throwable> exceptionConsumer) {
        try {
            InputStream inputStream = opener.open();

            Optional<File> sequenceOptional;
            try {
                InputStream inputStream2 = new FixedBufferInputStream(inputStream);

                try {
                    sequenceOptional = Optional.of(this.readMidi(inputStream2));
                } catch (Throwable throwable1) {
                    try {
                        inputStream2.close();
                    } catch (Throwable throwable2) {
                        throwable1.addSuppressed(throwable2);
                    }

                    throw throwable1;
                }

                inputStream2.close();
            } catch (Throwable throwablee1) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable2) {
                        throwablee1.addSuppressed(throwable2);
                    }
                }

                throw throwablee1;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return sequenceOptional;
        } catch (FileNotFoundException exception) {
            return Optional.empty();
        } catch (Throwable exception) {
            exceptionConsumer.accept(exception);
            return Optional.empty();
        }
    }

    public File readMidi(InputStream midiIInputStream) throws IOException, InvalidMidiDataException {
        File tempFile = File.createTempFile(MIDI_DIRECTORY, MIDI_EXTENSION);
        Files.copy(midiIInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    public Path getMidiPath(Identifier id, String extension) {
        if (id.getPath().contains("//")) {
            throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(id));
        } else {
            try {
                Path path = this.midisPath.resolve(id.getNamespace());
                Path path2 = PathUtil.getResourcePath(path, id.getPath(), extension);
                if (path2.startsWith(this.midisPath) && PathUtil.isNormal(path2) && PathUtil.isAllowedName(path2)) {
                    return path2;
                } else {
                    throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(path2));
                }
            } catch (InvalidPathException exception) {
                throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(id), exception);
            }
        }
    }

    @FunctionalInterface
    public interface MidiFileOpener {
        InputStream open() throws IOException;
    }

    static record Provider(Function<Identifier, Optional<File>> loader, Supplier<Stream<Identifier>> lister) {
        Provider(Function<Identifier, Optional<File>> loader, Supplier<Stream<Identifier>> lister) {
            this.loader = loader;
            this.lister = lister;
        }

        public Function<Identifier, Optional<File>> loader() {
            return this.loader;
        }

        public Supplier<Stream<Identifier>> lister() {
            return this.lister;
        }
    }
}
