package fr.ribesg.bukkit.ntheendagain;

import fr.ribesg.bukkit.ncore.lang.MessageId;
import fr.ribesg.bukkit.ncore.utils.Utils;
import fr.ribesg.bukkit.ntheendagain.world.EndChunk;
import fr.ribesg.bukkit.ntheendagain.world.EndChunks;
import fr.ribesg.bukkit.ntheendagain.world.EndWorldHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

public class TheEndAgainListener implements Listener {

    /**
     * Players that did less than threshold % of total damages
     * have no chance to receive the Egg with custom handling
     */
    private final static float  threshold = 0.15f;
    private final static Random rand      = new Random();

    private final NTheEndAgain plugin;

    public TheEndAgainListener(final NTheEndAgain instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEnderDragonDeath(final EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            final World endWorld = event.getEntity().getWorld();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(endWorld.getName()));
            if (handler == null) {
                return;
            } else {
                final Config config = handler.getConfig();
                switch (config.getEdExpHandling()) {
                    case 0:
                        event.setDroppedExp(config.getEdExpReward());
                        break;
                    case 1:
                        event.setDroppedExp(0);
                        HashMap<String, Long> dmgMap = null;
                        try {
                            dmgMap = new HashMap<>(handler.getDragons().get(event.getEntity().getUniqueId()));
                        } catch (final NullPointerException e) {
                            return;
                        }

                        // We ignore offline players
                        final Iterator<Entry<String, Long>> it = dmgMap.entrySet().iterator();
                        Entry<String, Long> e;
                        while (it.hasNext()) {
                            e = it.next();
                            if (plugin.getServer().getPlayerExact(e.getKey()) == null) {
                                it.remove();
                            }
                        }

                        // Get total damages done to the ED by Online players
                        long totalDamages = 0;
                        for (final Long v : dmgMap.values()) {
                            totalDamages += v;
                        }

                        // Give exp to players
                        for (final Entry<String, Long> entry : dmgMap.entrySet()) {
                            final Player p = plugin.getServer().getPlayerExact(entry.getKey());
                            final int reward = (int) (config.getEdExpReward() * entry.getValue() / totalDamages);
                            p.giveExp(reward);
                            plugin.sendMessage(p, MessageId.theEndAgain_receivedXP, Integer.toString(reward));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        // EnderDragon damaged by Player
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            Player player = null;
            if (event.getDamager().getType() == EntityType.PLAYER) {
                player = (Player) event.getDamager();
            } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter().getType() == EntityType.PLAYER) {
                player = (Player) ((Projectile) event.getDamager()).getShooter();
            } else {
                return;
            }
            final EnderDragon dragon = (EnderDragon) event.getEntity();
            final World endWorld = dragon.getWorld();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(endWorld.getName()));
            if (handler != null) {
                handler.playerHitED(dragon.getUniqueId(), player.getName(), event.getDamage());
            }
        }

        // Player damaged by EnderDragon
        else if (event.getEntityType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.ENDER_DRAGON) {
            final World endWorld = event.getEntity().getWorld();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(endWorld.getName()));
            if (handler != null) {
                event.setDamage(Math.round(event.getDamage() * handler.getConfig().getEdDamageMultiplier()));

                if (handler.getConfig().getEdPushesPlayers() == 1) {
                    // Simulate ED pushing player
                    final Vector velocity = event.getDamager().getLocation().toVector();
                    velocity.subtract(event.getEntity().getLocation().toVector());
                    velocity.normalize().multiply(-1);
                    if (velocity.getY() < 0.05f) {
                        velocity.setY(0.05f);
                    }
                    if (rand.nextFloat() < 0.025f) {
                        velocity.setY(10);
                    }
                    velocity.normalize().multiply(1.75f);
                    Bukkit.getScheduler().runTask(plugin, new BukkitRunnable() {

                        @Override
                        public void run() {
                            event.getEntity().setVelocity(velocity);
                        }
                    }
                                                 );
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnderDragonCreatePortal(final EntityCreatePortalEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            final World endWorld = event.getEntity().getWorld();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(endWorld.getName()));
            if (handler != null) {
                final Config config = handler.getConfig();
                final int pH = config.getEdPortalSpawn();
                final int eH = config.getEdEggHandling();
                final Location deathLocation = event.getEntity().getLocation();

                /* Explanation of algorithm (1a and 1b are likely to be the same loop)
                 *   if (pH == 0 && eH == 0) {
                 *       Nothing to do
                 *   } else if (pH == 0 && eH == 1) {
                 *       (1a) Cancel Egg spawn
                 *       (2)  Distribute egg correctly
                 *   } else if (pH == 1 && eH == 0) {
                 *       (1b) Cancel every block but egg
                 *   } else if (pH == 1 && eH == 1) {
                 *       (2)  Distribute egg correctly
                 *       (3)  Cancel event
                 *   } else if (pH == 2 && eH == 0) {
                 *       (3)  Cancel event
                 *   } else if (pH == 2 && eH == 1) {
                 *       (2)  Distribute egg correctly
                 *       (3)  Cancel event
                 *   }
                 */

                // (1a)
                final boolean cancelEgg = pH == 0 && eH == 1;

                // (1b)
                final boolean cancelPortalBlocks = pH == 1 && eH == 0;

                // (2)
                final boolean customEggHandling = eH == 1;

                // (3)
                final boolean cancelEvent = pH == 1 && eH == 1 || pH == 2;

                if (cancelEgg || cancelPortalBlocks) {
                    BlockState eggBlock = null;
                    for (final BlockState b : event.getBlocks()) {
                        if (b.getType() == Material.DRAGON_EGG) {
                            if (cancelEgg) {
                                b.setType(Material.AIR);
                            }
                            eggBlock = b;
                        } else if (cancelPortalBlocks) {
                            b.setType(Material.AIR);
                        }
                    }
                    if (eggBlock != null) {
                        final Chunk c = eggBlock.getChunk();
                        for (int x = c.getX() - 2; x <= c.getX() + 2; x++) {
                            for (int z = c.getZ() - 2; z <= c.getZ() + 2; z++) {
                                c.getWorld().refreshChunk(x, z);
                            }
                        }
                    }
                }

                if (customEggHandling) {
                    // % of total damages done to the ED ; Player name
                    TreeMap<Float, String> ratioMap = new TreeMap<>();
                    long totalDamages = 0;
                    for (final Entry<String, Long> e : handler.getDragons().get(event.getEntity().getUniqueId()).entrySet()) {
                        totalDamages += e.getValue();
                    }
                    for (final Entry<String, Long> e : handler.getDragons().get(event.getEntity().getUniqueId()).entrySet()) {
                        ratioMap.put((float) ((double) e.getValue() / (double) totalDamages), e.getKey());
                    }

                    // Remove entries for Players whom done less damages than threshold
                    final Iterator<Entry<Float, String>> it = ratioMap.entrySet().iterator();
                    while (it.hasNext()) {
                        final Entry<Float, String> e = it.next();
                        if (e.getKey() <= threshold) {
                            it.remove();
                        }
                    }

                    // Update ratio according to removed parts of total (was 1 obviously)
                    float remainingRatioTotal = 0f;
                    for (final float f : ratioMap.keySet()) {
                        // Computing new total (should be <=1)
                        remainingRatioTotal += f;
                    }
                    if (remainingRatioTotal != 1) {
                        final TreeMap<Float, String> newRatioMap = new TreeMap<>();
                        for (final Entry<Float, String> e : ratioMap.entrySet()) {
                            newRatioMap.put(e.getKey() * 1 / remainingRatioTotal, e.getValue());
                        }
                        ratioMap = newRatioMap;
                    }

                    // Now we will take a random player, the best fighter has the best chance to be choosen
                    float rand = new Random().nextFloat();
                    String playerName = null;
                    for (final Entry<Float, String> e : ratioMap.entrySet()) {
                        if (rand < e.getKey()) {
                            playerName = e.getValue();
                            break;
                        }
                        rand -= e.getKey();
                    }
                    if (playerName == null) { // Security
                        endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
                    } else {
                        final Player p = Bukkit.getServer().getPlayerExact(playerName);
                        if (p == null) {
                            endWorld.dropItem(deathLocation, new ItemStack(Material.DRAGON_EGG));
                        } else {
                            final HashMap<Integer, ItemStack> notGiven = p.getInventory().addItem(new ItemStack(Material.DRAGON_EGG));
                            if (notGiven.size() > 0) {
                                p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DRAGON_EGG));
                                plugin.sendMessage(p, MessageId.theEndAgain_droppedDragonEgg);
                            } else {
                                plugin.sendMessage(p, MessageId.theEndAgain_receivedDragonEgg);
                            }
                        }
                    }
                }

                event.setCancelled(cancelEvent);

                // Forget about this dragon
                handler.getDragons().remove(event.getEntity().getUniqueId());
                handler.getLoadedDragons().remove(event.getEntity().getUniqueId());
                handler.decrementDragonCount();

                // Handle on-ED-death regen/respawn

                boolean willRespawn = false;
                if (config.getRespawnType() == 1) {
                    willRespawn = true;
                } else if (config.getRespawnType() == 2 && handler.getNumberOfAliveEnderDragons() == 0) {
                    willRespawn = true;
                }
                if (willRespawn) {
                    Bukkit.getScheduler().runTaskLater(plugin, new BukkitRunnable() {

                        @Override
                        public void run() {
                            if (config.getRegenType() == 1) {
                                handler.regen();
                            }
                            handler.respawnDragons();
                        }
                    }, 20 * config.getRandomRespawnTimer()
                                                      );
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEndChunkLoad(final ChunkLoadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            final String worldName = event.getWorld().getName();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(worldName));
            if (handler != null) {
                final EndChunks chunks = handler.getChunks();
                final Chunk chunk = event.getChunk();
                EndChunk endChunk = chunks.getChunk(worldName, chunk.getX(), chunk.getZ());
                if (endChunk != null && endChunk.hasToBeRegen()) {
                    for (final Entity e : chunk.getEntities()) {
                        if (e.getType() == EntityType.ENDER_DRAGON) {
                            final EnderDragon ed = (EnderDragon) e;
                            if (handler.getDragons().containsKey(ed.getUniqueId())) {
                                handler.getDragons().remove(ed.getUniqueId());
                                handler.getLoadedDragons().remove(ed.getUniqueId());
                                handler.decrementDragonCount();
                            }
                        }
                        e.remove();
                    }
                    final int x = endChunk.getX(), z = endChunk.getZ();
                    event.getWorld().regenerateChunk(x, z);
                    endChunk.setToBeRegen(false);
                    Bukkit.getScheduler().runTaskLater(plugin, new BukkitRunnable() {

                        @Override
                        public void run() {
                            event.getWorld().refreshChunk(x, z);
                        }
                    }, 100L
                                                      );
                } else {
                    if (endChunk == null) {
                        endChunk = chunks.addChunk(chunk);
                    }
                    for (final Entity e : chunk.getEntities()) {
                        if (e.getType() == EntityType.ENDER_DRAGON) {
                            final EnderDragon ed = (EnderDragon) e;
                            if (!handler.getDragons().containsKey(ed.getUniqueId())) {
                                ed.setMaxHealth(handler.getConfig().getEdHealth());
                                ed.setHealth(ed.getMaxHealth());
                                handler.getDragons().put(ed.getUniqueId(), new HashMap<String, Long>());
                                handler.getLoadedDragons().add(ed.getUniqueId());
                                handler.incrementDragonCount();
                            }
                        } else if (e.getType() == EntityType.ENDER_CRYSTAL) {
                            endChunk.addCrystalLocation(e);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEndChunkUnload(final ChunkUnloadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            final String worldName = event.getWorld().getName();
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(worldName));
            if (handler != null) {
                for (final Entity e : event.getChunk().getEntities()) {
                    if (e.getType() == EntityType.ENDER_DRAGON) {
                        final EnderDragon ed = (EnderDragon) e;
                        handler.getLoadedDragons().remove(ed.getUniqueId());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnderDragonSpawn(final CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(event.getLocation().getWorld().getName()));
            if (handler == null) {
                return;
            } else if (handler.getNumberOfAliveEnderDragons() >= handler.getConfig().getRespawnNumber()) {
                event.setCancelled(true);
            } else {
                if (event.getSpawnReason() != SpawnReason.CUSTOM && event.getSpawnReason() != SpawnReason.SPAWNER_EGG) {
                    event.setCancelled(true);
                } else if (!handler.getDragons().containsKey(event.getEntity().getUniqueId())) {
                    handler.getDragons().put(event.getEntity().getUniqueId(), new HashMap<String, Long>());
                    handler.getLoadedDragons().add(event.getEntity().getUniqueId());
                    event.getEntity().setMaxHealth(handler.getConfig().getEdHealth());
                    event.getEntity().setHealth(event.getEntity().getMaxHealth());
                    handler.incrementDragonCount();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEnderDragonRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            // TODO Config thing for this
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(final WorldLoadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            plugin.getLogger().info("Additional End world detected: handling " + event.getWorld().getName());
            final EndWorldHandler handler = new EndWorldHandler(plugin, event.getWorld());
            try {
                handler.loadConfig();
                handler.loadChunks();
                plugin.getWorldHandlers().put(Utils.toLowerCamelCase(event.getWorld().getName()), handler);
                handler.init();
            } catch (final IOException e) {
                plugin.getLogger().severe("An error occured, stacktrace follows:");
                e.printStackTrace();
                plugin.getLogger().severe("This error occured when NTheEndAgain tried to load " + e.getMessage() + ".yml");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWorldUnload(final WorldUnloadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            plugin.getLogger().info("Handling " + event.getWorld().getName() + " unload");
            final EndWorldHandler handler = plugin.getHandler(Utils.toLowerCamelCase(event.getWorld().getName()));
            if (handler != null) {
                handler.unload();
            }
        }
    }
}