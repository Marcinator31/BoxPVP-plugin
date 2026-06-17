package me.boxpvp.boxpvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Zone {

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final int intervalSeconds;

    // runtime state (not persisted)
    private long secondsUntilReset;

    public Zone(String name, String worldName,
                int x1, int y1, int z1, int x2, int y2, int z2,
                int intervalSeconds) {
        this.name = name;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.intervalSeconds = intervalSeconds;
        this.secondsUntilReset = intervalSeconds;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getIntervalSeconds() { return intervalSeconds; }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public long getSecondsUntilReset() { return secondsUntilReset; }
    public void setSecondsUntilReset(long s) { this.secondsUntilReset = s; }
    public void resetCountdown() { this.secondsUntilReset = intervalSeconds; }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
