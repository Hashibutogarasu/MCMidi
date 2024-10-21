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

public class SoundFontManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundFontManager.class);
    private static final String SOUNDFONT_DIRECTORY = "soundfont";
    private static final String SOUNDFONT_EXTENTION = ".sf2";
    private final Map<Identifier, Optional<Sequence>> midis = Maps.newConcurrentMap();
    private final DataFixer dataFixer;
    private ResourceManager resourceManager;
    private final Path generatedPath;
    private final List<Provider> providers;
    private final RegistryEntryLookup<Block> blockLookup;
    private static final ResourceFinder SOUNDFONT_NBT_RESOURCE_FINDER = new ResourceFinder(SOUNDFONT_DIRECTORY, SOUNDFONT_EXTENTION);

    public SoundFontManager(ResourceManager resourceManager, LevelStorage.Session session, DataFixer dataFixer, RegistryEntryLookup<Block> blockLookup){
        this.resourceManager = resourceManager;
        this.dataFixer = dataFixer;
        this.generatedPath = session.getDirectory(WorldSavePath.GENERATED).normalize();
        this.blockLookup = blockLookup;
        ImmutableList.Builder<Provider> builder = ImmutableList.builder();
        builder.add(new Provider(this::loadSoundFontFromFile, this::streamSoundFontsFromFile));
        if (SharedConstants.isDevelopment) {
            builder.add(new Provider(this::loadSoundFontFromGameTestFile, this::streamSoundFontsFromGameTestFile));
        }

        builder.add(new Provider(this::loadSoundFontFromResource, this::streamSoundFontsFromResource));
        this.providers = builder.build();
    }

    public Stream<Identifier> streamSoundFontsFromResource() {
        Stream<Identifier> finder = SOUNDFONT_NBT_RESOURCE_FINDER.findResources(this.resourceManager).keySet().stream();
        return finder.map(SOUNDFONT_NBT_RESOURCE_FINDER::toResourceId);
    }

    public Stream<Identifier> streamSoundFontsFromGameTestFile() {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList<>();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.generatedPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator<Path> pathIterator = directoryStream.iterator();

                    while(pathIterator.hasNext()) {
                        Path path = pathIterator.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve(SOUNDFONT_DIRECTORY);
                        Objects.requireNonNull(list);
                        this.streamSoundFonts(path2, string, SOUNDFONT_EXTENTION, list::add);
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

    public Optional<File> loadSoundFontFromResource(Identifier id) {
        Identifier identifier = SOUNDFONT_NBT_RESOURCE_FINDER.toResourcePath(id);
        return this.loadSoundFont(() -> {
            return this.resourceManager.open(identifier);
        }, (throwable) -> {
            LOGGER.error("Couldn't load midi {}", id, throwable);
        });
    }

    public Stream<Identifier> streamSoundFontsFromFile() {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Stream.empty();
        } else {
            try {
                List<Identifier> list = new ArrayList<>();
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.generatedPath, (pathx) -> {
                    return Files.isDirectory(pathx, new LinkOption[0]);
                });

                try {
                    Iterator<Path> var3 = directoryStream.iterator();

                    while(var3.hasNext()) {
                        Path path = var3.next();
                        String string = path.getFileName().toString();
                        Path path2 = path.resolve(SOUNDFONT_DIRECTORY);
                        Objects.requireNonNull(list);
                        this.streamSoundFonts(path2, string, SOUNDFONT_EXTENTION, list::add);
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

    public void streamSoundFonts(Path directory, String namespace, String fileExtension, Consumer<Identifier> idConsumer) {
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

    public Optional<File> loadSoundFontFromGameTestFile(Identifier id) {
        return this.loadSoundFontFromSnbt(id, Paths.get(MidiTestUtil.getTestMidisDirectoryName));
    }

    public Optional<File> loadSoundFontFromSnbt(Identifier id, Path path) {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path2 = PathUtil.getResourcePath(path, id.getPath(), ".snbt");

            try {
                BufferedReader bufferedReader = Files.newBufferedReader(path2);

                Optional<File> sequenceOptional;
                try {
                    String string = IOUtils.toString(bufferedReader);
                    sequenceOptional = Optional.of(this.createSoundFont(NbtHelper.fromNbtProviderString(string)));
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

    public File createSoundFont(NbtCompound nbtCompound) throws InvalidMidiDataException, IOException {
        File tempFile = File.createTempFile(SOUNDFONT_DIRECTORY, SOUNDFONT_EXTENTION);
        Files.write(tempFile.toPath(), nbtCompound.getByteArray(SOUNDFONT_DIRECTORY));
        return tempFile;
    }

    public Optional<File> loadSoundFontFromFile(Identifier id) {
        if (!Files.isDirectory(this.generatedPath, new LinkOption[0])) {
            return Optional.empty();
        } else {
            Path path = this.getSoundFontPath(id, SOUNDFONT_EXTENTION);
            return this.loadSoundFont(() -> {
                return new FileInputStream(path.toFile());
            }, (throwable) -> {
                LOGGER.error("Couldn't load midi from {}", path, throwable);
            });
        }
    }

    public Optional<File> loadSoundFont(SoundFontFileOpener opener, Consumer<Throwable> exceptionConsumer) {
        try {
            InputStream inputStream = opener.open();

            Optional<File> sequenceOptional;
            try {
                InputStream inputStream2 = new FixedBufferInputStream(inputStream);

                try {
                    sequenceOptional = Optional.of(this.readSoundFont(inputStream2));
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

    public File readSoundFont(InputStream midiIInputStream) throws IOException, InvalidMidiDataException {
        File tempFile = File.createTempFile(SOUNDFONT_DIRECTORY, SOUNDFONT_EXTENTION);
        Files.copy(midiIInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    public Path getSoundFontPath(Identifier id, String extension) {
        if (id.getPath().contains("//")) {
            throw new InvalidIdentifierException("Invalid resource path: " + String.valueOf(id));
        } else {
            try {
                Path path = this.generatedPath.resolve(id.getNamespace());
                Path path2 = PathUtil.getResourcePath(path, id.getPath(), extension);
                if (path2.startsWith(this.generatedPath) && PathUtil.isNormal(path2) && PathUtil.isAllowedName(path2)) {
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
    public interface SoundFontFileOpener {
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

    public record SoundFont(String path) {
        public String getName() {
            return this.path.substring(this.path.lastIndexOf("/") + 1);
        }
    }
}
