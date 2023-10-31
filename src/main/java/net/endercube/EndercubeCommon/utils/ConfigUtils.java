package net.endercube.EndercubeCommon.utils;

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

public class ConfigUtils {

    private final Logger logger;
    private final HoconConfigurationLoader LOADER;
    private final CommentedConfigurationNode CONFIG;

    /**
     * Reads config from disk
     *
     * @param CONFIG The {@link HoconConfigurationLoader} to use to save your config
     * @param LOADER The {@link HoconConfigurationLoader} to use to save your config
     */
    public ConfigUtils(HoconConfigurationLoader LOADER, CommentedConfigurationNode CONFIG) {
        this.logger = LoggerFactory.getLogger(SQLWrapper.class);
        this.LOADER = LOADER;
        this.CONFIG = CONFIG;
    }

    /**
     * Saves unwritten changes to config
     */
    public void saveConfig() {
        try {
            LOADER.save(CONFIG);
        } catch (final ConfigurateException e) {
            logger.error("Unable to save your messages configuration! Sorry! " + e.getMessage());
            MinecraftServer.stopCleanly();
        }
    }

    /**
     * @param node  The {@link ConfigurationNode} to write to
     * @param value The value to write to that node
     * @return a {@link String} containing what the value currently is
     */
    public String getOrSetDefault(ConfigurationNode node, String value) {
        if (node.getString() == null) {
            node.raw(value);
            logger.info("Setting config");
            saveConfig();
            return value;
        }

        return node.getString();
    }

    /**
     * Reads an {@link ItemStack} from config
     *
     * @param configNode The {@link ConfigurationNode} to read from
     * @return {@link ItemStack}
     */
    @Nullable
    public ItemStack getItemStackFromConfig(ConfigurationNode configNode) {
        String materialString = configNode.node("material").getString();
        String name = configNode.node("name").getString();

        if (materialString == null) {
            materialString = "minecraft:barrier";
            logger.warn("Please set a material for the map above");
        }
        if (name == null) {
            name = "Please set a name in config for this";
            logger.warn("Please set a name for the map above");
        }

        Material material = Material.fromNamespaceId(materialString);

        if (material == null) {
            logger.warn("The material, " + materialString + " in config of the map above is invalid");
            return null;
        }

        return ItemStack.of(material)
                .withDisplayName(MiniMessage.miniMessage().deserialize(name));
    }

    /**
     * Gets a single {@link Pos} from config
     *
     * @param configNode The {@link ConfigurationNode} to read from
     * @return {@link Pos}
     */
    @Nullable
    public Pos getPosFromConfig(ConfigurationNode configNode) {
        Float[] pointList;
        try {
            pointList = configNode.get(new TypeToken<>() {
            });
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        if (pointList == null) {
            return null;
        }

        if (pointList.length == 3) {
            return new Pos(pointList[0], pointList[1], pointList[2]);
        }

        if (pointList.length == 5) {
            return new Pos(pointList[0], pointList[1], pointList[2], pointList[3], pointList[4]);
        }

        logger.warn("Position value in config's length is out of bounds");
        return null;
    }

    /**
     * Returns a list of {@link Pos} from config
     *
     * @param configNode The {@link ConfigurationNode} to read from
     * @return A list of {@link Pos}
     */
    @Nullable
    public Pos[] getPosListFromConfig(ConfigurationNode configNode) {
        List<Pos> outArrayList = new ArrayList<>();

        // Loop through the list at the specific node and add it to our out array list
        for (ConfigurationNode currentNode : configNode.childrenList()) {
            outArrayList.add(getPosFromConfig(currentNode));
        }
        return outArrayList.toArray(new Pos[0]);
    }
}
