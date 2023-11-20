package net.endercube.EndercubeCommon;

import net.endercube.EndercubeCommon.blocks.Sign;
import net.endercube.EndercubeCommon.blocks.Skull;
import net.endercube.EndercubeCommon.utils.ConfigUtils;
import net.endercube.EndercubeCommon.utils.DatabaseWrapper;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.PlayerProvider;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import redis.clients.jedis.JedisPooled;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class EndercubeGame {
    private final EventNode<Event> EVENTNODE;
    private static final Logger LOGGER;
    private CommentedConfigurationNode config;
    private PlayerProvider PLAYER_PROVIDER;
    private boolean databaseEnabled = true;
    private String databaseNamespace;
    private ConfigUtils configUtils;
    private DatabaseWrapper redisDatabaseWrapper;
    private JedisPooled databaseJedisPool;

    // Initializes the logger, only on the first initialization of this class
    static {
        LOGGER = LoggerFactory.getLogger(EndercubeGame.class);
    }

    public EndercubeGame() {
        // Create out event node to add to later
        EVENTNODE = EventNode.type("listeners", EventFilter.ALL);
    }

    /**
     * Set the custom player to be used
     *
     * @param playerProvider The custom player to use
     * @return The builder
     */
    public EndercubeGame setPlayer(@Nullable PlayerProvider playerProvider) {
        PLAYER_PROVIDER = playerProvider;
        return this;
    }

    /**
     * Add an event to the event tree
     *
     * @param listener The listener
     * @return The builder
     */
    public EndercubeGame addEvent(EventListener<?> listener) {
        EVENTNODE.addListener(listener);
        return this;
    }

    /**
     * Add an event to the event tree
     *
     * @param eventType The type of event to listen for
     * @param listener  The listener
     * @return The builder
     */
    public <E extends Event> EndercubeGame addEvent(@NotNull Class<E> eventType, @NotNull Consumer<E> listener) {
        EVENTNODE.addListener(eventType, listener);
        return this;
    }

    /**
     * Should we enable the database? Default: true
     *
     * @param enableSQL a boolean to say if the database is enabled
     * @return The builder
     */
    public EndercubeGame useDatabase(boolean enableSQL) {
        databaseEnabled = enableSQL;

        return this;
    }

    /**
     * Set the namespace for the database
     *
     * @param name The name to use
     * @return The builder
     */
    public EndercubeGame setDatabaseNamespace(String name) {
        databaseNamespace = name;

        return this;
    }

    /**
     * The main class that starts the server
     */
    public void build() {
        this.initConfig();

        // Server Initialization
        MinecraftServer minecraftServer = MinecraftServer.init();

        // Add our event node
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addChild(EVENTNODE);

        // Register block handlers
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from("minecraft:sign"), Sign::new);
        MinecraftServer.getBlockManager().registerHandler(NamespaceID.from("minecraft:skull"), Skull::new);
        LOGGER.debug("Set block handlers");

        // Set encryption
        EncryptionMode encryptionMode;
        try {
            encryptionMode = EncryptionMode.valueOf(configUtils.getOrSetDefault(config.node("connection", "mode"), "online").toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Cannot read encryption mode from config, falling back to ONLINE");
            encryptionMode = EncryptionMode.ONLINE;
        }
        initEncryption(encryptionMode, configUtils.getOrSetDefault(config.node("connection", "velocitySecret"), ""));


        // Start the server
        int port = Integer.parseInt(configUtils.getOrSetDefault(config.node("connection", "port"), "25565"));
        minecraftServer.start("0.0.0.0", port);
        LOGGER.info("Starting server on port " + port + " with " + encryptionMode + " encryption");

        MinecraftServer.getConnectionManager().setPlayerProvider(PLAYER_PROVIDER);
        LOGGER.debug("Set player provider");

        if (databaseEnabled) {
            String DBHostname = configUtils.getOrSetDefault(config.node("database", "redis", "hostname"), "localhost");
            int DBPort = Integer.parseInt(configUtils.getOrSetDefault(config.node("database", "redis", "port"), "6379"));
            databaseJedisPool = new JedisPooled(DBHostname, DBPort);
            redisDatabaseWrapper = new DatabaseWrapper(databaseJedisPool, databaseNamespace);
        }

    }

    enum EncryptionMode {
        ONLINE,
        VELOCITY
    }

    private void initEncryption(EncryptionMode mode, String velocitySecret) {
        switch (mode) {
            case ONLINE -> MojangAuth.init();
            case VELOCITY -> {
                if (!Objects.equals(velocitySecret, "")) {
                    VelocityProxy.enable(velocitySecret);
                }
            }
        }
    }

    private void initConfig() {
        // Create config directories
        if (!Files.exists(getPath("config/worlds/"))) {
            LOGGER.info("Creating configuration files");

            try {
                Files.createDirectories(getPath("config/worlds/"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .path(getPath("config/config.conf"))
                .build();

        try {
            config = loader.load();
        } catch (ConfigurateException e) {
            LOGGER.error("An error occurred while loading config.conf: " + e.getMessage());
            LOGGER.error(Arrays.toString(e.getStackTrace()));
            MinecraftServer.stopCleanly();
        }

        // Init a ConfigUtils class
        configUtils = new ConfigUtils(loader, config);
    }

    public static Path getPath(String path) {
        try {
            return Path.of(new File(EndercubeGame.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getPath()).getParent().resolve(path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull CommentedConfigurationNode getConfig() {
        return config;
    }

    public @NotNull ConfigUtils getConfigUtils() {
        return configUtils;
    }

    public @Nullable DatabaseWrapper getRedisDatabaseWrapper() {
        if (!databaseEnabled) {
            return null;
        }
        return redisDatabaseWrapper;
    }

    public @Nullable JedisPooled getDatabaseJedisPool() {
        if (!databaseEnabled) {
            return null;
        }
        return databaseJedisPool;
    }
}
