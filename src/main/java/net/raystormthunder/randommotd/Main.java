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

    private void ensureConfigFilesExist() {
        try {
            // Ensure the config folder exists
            Path configFolderPath = Paths.get(CONFIG_FOLDER);
            if (!Files.exists(configFolderPath)) {
                Files.createDirectories(configFolderPath);
                System.out.println("Created config folder at " + CONFIG_FOLDER);
            } else if (!Files.isDirectory(configFolderPath)) {
                throw new IOException(CONFIG_FOLDER + " exists but is not a directory.");
            }

            // Ensure the message-list file exists
            Path messageListPath = Paths.get(MESSAGE_LIST_FILE);
            if (!Files.exists(messageListPath)) {
                List<String> defaultMotds = Arrays.asList(
                        "Welcome Server",
                        "Test",
                        "Change these strings!"
                );
                Files.write(messageListPath, defaultMotds, StandardOpenOption.CREATE_NEW);
                System.out.println("Created default message list at " + MESSAGE_LIST_FILE);
            } else if (!Files.isRegularFile(messageListPath)) {
                throw new IOException(MESSAGE_LIST_FILE + " exists but is not a file.");
            }

            // Ensure the timer file exists
            Path timerFilePath = Paths.get(TIMER_FILE);
            if (!Files.exists(timerFilePath)) {
                List<String> defaultTimer = Arrays.asList(
                        "#Change MOTD timer set below",
                        "hours=0",
                        "minutes=1",
                        "seconds=0"
                );
                Files.write(timerFilePath, defaultTimer, StandardOpenOption.CREATE_NEW);
                System.out.println("Created default timer file at " + TIMER_FILE);
            } else if (!Files.isRegularFile(timerFilePath)) {
                throw new IOException(TIMER_FILE + " exists but is not a file.");
            }

        } catch (IOException e) {
            System.err.println("Failed to create config files: " + e.getMessage());
        }
    }


    private void loadMotds() {
        try {
            motds = Files.lines(Paths.get(MESSAGE_LIST_FILE))
                    .filter(line -> !line.trim().isEmpty()) // Skip empty lines
                    .collect(Collectors.toList());

            if (motds.isEmpty()) {
                System.err.println("No valid MOTDs found in the message list file. Using defaults.");
                motds = Arrays.asList("Welcome Server", "Test", "Change these strings!");
            } else {
                System.out.println("Loaded MOTDs: " + motds);
            }
        } catch (IOException e) {
            System.err.println("Failed to load MOTDs from message list file: " + e.getMessage());
        }
    }

    private void loadTimerInterval() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(TIMER_FILE));
            long hours = 0, minutes = 0, seconds = 0;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("hours=")) {
                    hours = Long.parseLong(line.split("=")[1].trim());
                } else if (line.startsWith("minutes=")) {
                    minutes = Long.parseLong(line.split("=")[1].trim());
                } else if (line.startsWith("seconds=")) {
                    seconds = Long.parseLong(line.split("=")[1].trim());
                }
            }

            // Calculate the interval in milliseconds
            timerInterval = (hours * 3600 + minutes * 60 + seconds) * 1000;

            if (timerInterval <= 0) {
                throw new IllegalArgumentException("Timer interval must be greater than 0.");
            }

            System.out.println("Loaded timer interval: " + timerInterval + " ms");
        } catch (Exception e) {
            System.err.println("Failed to load timer interval. Using default: 60000 ms");
            timerInterval = 60000; // Fallback to default
        }
    }

    private void updateMotd() {
        if (motds.isEmpty()) {
            System.err.println("MOTD list is empty. Skipping update.");
            return;
        }

        // Pick a random MOTD
        String motd = motds.get(new Random().nextInt(motds.size()));

        // Replace \u00a7 or \\u00a7 with § for color formatting
        motd = motd.replace("\\u00a7", "§").replace("\u00a7", "§");

        // Dynamically update the server MOTD
        if (server != null) {
            server.setMotd(motd);
            System.out.println("Updated MOTD to: " + motd);
        } else {
            System.err.println("Server instance not available to update MOTD.");
        }
    }

    public void setServerInstance(MinecraftServer server) {
        this.server = server;
    }
}
