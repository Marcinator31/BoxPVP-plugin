package me.boxpvp.boxpvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZoneManager {

    private final BoxPvP plugin;
    private final Map<String, Zone> zones = new HashMap<>();
    private BukkitTask schedulerTask;

    public ZoneManager(BoxPvP plugin) {
        this.plugin = plugin;
    }

    public Map<String, Zone> getZones() {
        return zones;
    }

    public Zone getZone(String name) {
        return zones.get(name.toLowerCase());
    }

    // ---------- Load / Save ----------

    public void loadZones() {
        zones.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("zones");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection z = sec.getConfigurationSection(key);
            if (z == null) continue;
            try {
                Zone zone = new Zone(
                        key,
                        z.getString("world"),
                        z.getInt("x1"), z.getInt("y1"), z.getInt("z1"),
                        z.getInt("x2"), z.getInt("y2"), z.getInt("z2"),
                        z.getInt("interval", plugin.getConfig().getInt("default-reset-interval", 300))
                );
                zones.put(key.toLowerCase(), zone);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load zone '" + key + "': " + e.getMessage());
            }
        }
    }

    public void saveZones() {
        plugin.getConfig().set("zones", null);
        for (Zone zone : zones.values()) {
            String base = "zones." + zone.getName();
            plugin.getConfig().set(base + ".world", zone.getWorldName());
            plugin.getConfig().set(base + ".x1", zone.getMinX());
            plugin.getConfig().set(base + ".y1", zone.getMinY());
            plugin.getConfig().set(base + ".z1", zone.getMinZ());
            plugin.getConfig().set(base + ".x2", zone.getMaxX());
            plugin.getConfig().set(base + ".y2", zone.getMaxY());
            plugin.getConfig().set(base + ".z2", zone.getMaxZ());
            plugin.getConfig().set(base + ".interval", zone.getIntervalSeconds());
        }
        plugin.saveConfig();
    }

    public boolean createZone(String name, String world,
                              int x1, int y1, int z1,
                              int x2, int y2, int z2, int interval) {
        if (zones.containsKey(name.toLowerCase())) return false;
        Zone zone = new Zone(name, world, x1, y1, z1, x2, y2, z2, interval);
        zones.put(name.toLowerCase(), zone);
        saveZones();
        return true;
    }

    public boolean deleteZone(String name) {
        Zone removed = zones.remove(name.toLowerCase());
        if (removed != null) {
            saveZones();
            return true;
        }
        return false;
    }

    // ---------- Scheduler ----------

    public void startScheduler() {
        if (schedulerTask != null) schedulerTask.cancel();
        // runs every second (20 ticks)
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        int warningSeconds = plugin.getConfig().getInt("warning-seconds", 10);
        for (Zone zone : zones.values()) {
            long remaining = zone.getSecondsUntilReset() - 1;
            zone.setSecondsUntilReset(remaining);

            if (remaining == warningSeconds) {
                broadcastWarning(zone, warningSeconds);
            }

            if (remaining <= 0) {
                resetZone(zone);
                zone.resetCountdown();
            }
        }
    }

    private void broadcastWarning(Zone zone, int seconds) {
        String prefix = color(plugin.getConfig().getString("messages.prefix", ""));
        String msg = color(plugin.getConfig().getString("messages.warning", "")
                .replace("{zone}", zone.getName())
                .replace("{seconds}", String.valueOf(seconds)));
        Bukkit.broadcastMessage(prefix + msg);
    }

    // ---------- Reset logic ----------

    /** Full reset: blocks + items (respecting clear-items-on-reset). Broadcasts reset-done. */
    public void resetZone(Zone zone) {
        World world = zone.getWorld();
        if (world == null) {
            plugin.getLogger().warning("World '" + zone.getWorldName() + "' for zone '"
                    + zone.getName() + "' is not loaded. Reset skipped.");
            return;
        }

        resetBlocks(zone, world);

        if (plugin.getConfig().getBoolean("clear-items-on-reset", true)) {
            clearItems(zone, world);
        }

        String prefix = color(plugin.getConfig().getString("messages.prefix", ""));
        String msg = color(plugin.getConfig().getString("messages.reset-done", "")
                .replace("{zone}", zone.getName()));
        if (!msg.trim().isEmpty()) {
            Bukkit.broadcastMessage(prefix + msg);
        }
    }

    /** Resets only the configured blocks in the zone. Returns false if world unloaded. */
    public boolean resetBlocks(Zone zone) {
        World world = zone.getWorld();
        if (world == null) {
            plugin.getLogger().warning("World '" + zone.getWorldName() + "' for zone '"
                    + zone.getName() + "' is not loaded.");
            return false;
        }
        resetBlocks(zone, world);
        return true;
    }

    private void resetBlocks(Zone zone, World world) {
        Set<Material> toRemove = getRemovableBlocks();
        for (int x = zone.getMinX(); x <= zone.getMaxX(); x++) {
            for (int y = zone.getMinY(); y <= zone.getMaxY(); y++) {
                for (int z = zone.getMinZ(); z <= zone.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (toRemove.contains(block.getType())) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    /** Clears dropped items in the zone. Returns number removed, or -1 if world unloaded. */
    public int clearItems(Zone zone) {
        World world = zone.getWorld();
        if (world == null) {
            plugin.getLogger().warning("World '" + zone.getWorldName() + "' for zone '"
                    + zone.getName() + "' is not loaded.");
            return -1;
        }
        return clearItems(zone, world);
    }

    private int clearItems(Zone zone, World world) {
        int count = 0;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            if (zone.contains(item.getLocation())) {
                item.remove();
                count++;
            }
        }
        return count;
    }

    private Set<Material> getRemovableBlocks() {
        Set<Material> set = new HashSet<>();
        List<String> list = plugin.getConfig().getStringList("blocks-to-remove");
        for (String s : list) {
            Material m = Material.matchMaterial(s);
            if (m != null) {
                set.add(m);
            } else {
                plugin.getLogger().warning("Unknown material in blocks-to-remove: " + s);
            }
        }
        return set;
    }

    public void reload() {
        plugin.reloadConfig();
        loadZones();
        startScheduler();
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
