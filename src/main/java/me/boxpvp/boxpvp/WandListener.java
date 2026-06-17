package me.boxpvp.boxpvp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    private final BoxPvP plugin;

    // the two selection points per player
    private static final Map<UUID, Location> pos1 = new HashMap<>();
    private static final Map<UUID, Location> pos2 = new HashMap<>();

    public WandListener(BoxPvP plugin) {
        this.plugin = plugin;
    }

    public static Location getPos1(Player p) { return pos1.get(p.getUniqueId()); }
    public static Location getPos2(Player p) { return pos2.get(p.getUniqueId()); }

    public static void setPos1(Player p, Location l) { pos1.put(p.getUniqueId(), l); }
    public static void setPos2(Player p, Location l) { pos2.put(p.getUniqueId(), l); }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // avoid double-firing (two hands)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (event.getItem() == null) return;
        if (event.getItem().getType() != plugin.getWandMaterial()) return;
        if (!player.hasPermission("boxpvp.admin")) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            setPos1(player, block.getLocation());
            send(player, "messages.pos1-set", block.getLocation());
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            setPos2(player, block.getLocation());
            send(player, "messages.pos2-set", block.getLocation());
        }
    }

    private void send(Player player, String path, Location loc) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString(path, "")
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }
}
