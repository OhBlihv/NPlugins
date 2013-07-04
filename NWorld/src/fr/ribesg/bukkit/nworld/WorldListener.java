package fr.ribesg.bukkit.nworld;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;

/**
 * TODO
 *
 * @author Ribesg
 */
public class WorldListener implements Listener {

    private final NWorld plugin;

    public WorldListener(final NWorld instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(final WorldLoadEvent event) {
        final World w = event.getWorld();
        if (!plugin.getWorldMap().containsKey(w.getName()) || !plugin.getSpawnMap().containsKey(w.getName())) {
            plugin.getPluginConfig().loadWorldFromConfig(w);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldUnload(final WorldUnloadEvent event) {
        try {
            plugin.getPluginConfig().writeConfig();
        } catch (final IOException e) {
            // TODO
            e.printStackTrace();
        }
        final World w = event.getWorld();
        plugin.getWorldMap().remove(w.getName());
        plugin.getSpawnMap().remove(w.getName());
    }

    public NWorld getPlugin() {
        return plugin;
    }
}