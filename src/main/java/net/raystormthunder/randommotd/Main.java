package net.raystormthunder.randommotd;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main implements ModInitializer {
    private static final String CONFIG_FOLDER = "./config/random-motd";
    private static final String MESSAGE_LIST_FILE = CONFIG_FOLDER + "/message-list";
    private static final String TIMER_FILE = CONFIG_FOLDER + "/randomize-message-timer";

    private Path messageListPath;
    private Path timerFilePath;

    private List<String> motds = new ArrayList<>();
    private long timerInterval = 60000; // Default to 1 minute in milliseconds
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        System.out.println("Random MOTD Mod Initialized!");

        ensureConfigFilesExist();
        loadMotds();
        loadTimerInterval();

        // Use a timer to update the MOTD dynamically
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateMotd();
            }
        }, 0, timerInterval); // Use the interval from config
    }

    private Path resolveConfigFile(String base, List<String> defaultContent) throws IOException {
        Path folder = Paths.get(CONFIG_FOLDER);
        if (!Files.exists(folder)) Files.createDirectories(folder);
        if (!Files.isDirectory(folder)) throw new IOException(CONFIG_FOLDER + " exists but is not a directory.");

        Path txt = Paths.get(base + ".txt");
        Path raw = Paths.get(base);

        if (Files.exists(txt) && Files.isRegularFile(txt)) return txt;
        if (Files.exists(raw) && Files.isRegularFile(raw)) return raw;

        // Neither exists: create the .txt with defaults
        Files.write(txt, defaultContent, StandardOpenOption.CREATE_NEW);
        System.out.println("Created default config: " + txt);
        return txt;
    }

    private void ensureConfigFilesExist() {
        try {
            messageListPath = resolveConfigFile(
                    MESSAGE_LIST_FILE,
                    Arrays.asList("Welcome Server", "Test", "Change these strings!")
            );

            timerFilePath = resolveConfigFile(
                    TIMER_FILE,
                    Arrays.asList(
                            "#Change MOTD timer set below",
                            "hours=0",
                            "minutes=1",
                            "seconds=0"
                    )
            );

            // Optional: warn if both existed but we chose .txt
            Path preferredTxtMsg = Paths.get(MESSAGE_LIST_FILE + ".txt");
            Path rawMsg = Paths.get(MESSAGE_LIST_FILE);
            if (Files.exists(preferredTxtMsg) && Files.exists(rawMsg)) {
                System.out.println("Both message-list and message-list.txt exist. Using message-list.txt.");
            }

            Path preferredTxtTimer = Paths.get(TIMER_FILE + ".txt");
            Path rawTimer = Paths.get(TIMER_FILE);
            if (Files.exists(preferredTxtTimer) && Files.exists(rawTimer)) {
                System.out.println("Both randomize-message-timer and randomize-message-timer.txt exist. Using .txt.");
            }
        } catch (IOException e) {
            System.err.println("Failed to prepare config files: " + e.getMessage());
        }
    }

    private void loadMotds() {
        try {
            motds = Files.lines(messageListPath)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            if (motds.isEmpty()) {
                System.err.println("No valid MOTDs found in " + messageListPath + ". Using defaults.");
                motds = Arrays.asList("Welcome Server", "Test", "Change these strings!");
            } else {
                System.out.println("Loaded MOTDs from " + messageListPath + ": " + motds);
            }
        } catch (IOException e) {
            System.err.println("Failed to load MOTDs from " + messageListPath + ": " + e.getMessage());
        }
    }

    private void loadTimerInterval() {
        try {
            List<String> lines = Files.readAllLines(timerFilePath);
            long hours = 0, minutes = 0, seconds = 0;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.startsWith("hours=")) {
                    hours = Long.parseLong(line.substring("hours=".length()).trim());
                } else if (line.startsWith("minutes=")) {
                    minutes = Long.parseLong(line.substring("minutes=".length()).trim());
                } else if (line.startsWith("seconds=")) {
                    seconds = Long.parseLong(line.substring("seconds=".length()).trim());
                }
            }

            timerInterval = (hours * 3600 + minutes * 60 + seconds) * 1000;
            if (timerInterval <= 0) throw new IllegalArgumentException("Timer interval must be > 0.");

            System.out.println("Loaded timer interval (" + timerFilePath + "): " + timerInterval + " ms");
        } catch (Exception e) {
            System.err.println("Failed to load timer interval from " + timerFilePath + ". Using default: 60000 ms");
            timerInterval = 60000;
        }
    }

    private void updateMotd() {
        if (motds.isEmpty()) {
            System.err.println("MOTD list is empty. Skipping update.");
            return;
        }

        String motd = motds.get(new Random().nextInt(motds.size()));

        // Normalize § and newlines
        motd = motd.replace("\\u00a7", "§").replace("\u00a7", "§")
                .replace("\\u00A7", "§").replace("\u00A7", "§")
                .replace("\\n", "\n").replace("/n", "\n");

        if (server != null) {
            server.setMotd(motd);
            System.out.println("Updated MOTD to:\n" + motd);
        } else {
            System.err.println("Server instance not available to update MOTD.");
        }
    }

    public void setServerInstance(MinecraftServer server) {
        this.server = server;
    }
}
