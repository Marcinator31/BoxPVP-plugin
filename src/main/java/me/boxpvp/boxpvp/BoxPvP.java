package me.boxpvp.boxpvp;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class BoxPvP extends JavaPlugin {

    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.zoneManager = new ZoneManager(this);
        zoneManager.loadZones();
        zoneManager.startScheduler();

        BoxCommand cmd = new BoxCommand(this);
        getCommand("boxpvp").setExecutor(cmd);
        getCommand("boxpvp").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new WandListener(this), this);

        getLogger().info("BoxPvP enabled. Zones loaded: " + zoneManager.getZones().size());
    }

    @Override
    public void onDisable() {
        if (zoneManager != null) {
            zoneManager.saveZones();
        }
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public Material getWandMaterial() {
        String name = getConfig().getString("wand-item", "BLAZE_ROD");
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.BLAZE_ROD;
    }
}
