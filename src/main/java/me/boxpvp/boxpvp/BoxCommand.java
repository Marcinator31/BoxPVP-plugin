package me.boxpvp.boxpvp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoxCommand implements CommandExecutor, TabCompleter {

    private final BoxPvP plugin;
    private static final List<String> SUBS = Arrays.asList(
            "wand", "create", "delete", "list", "pos1", "pos2",
            "reset", "resetblocks", "resetitems", "reload", "info");
    private static final List<String> NAME_SUBS = Arrays.asList(
            "delete", "reset", "resetblocks", "resetitems", "info");

    public BoxCommand(BoxPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("boxpvp.admin")) {
            msg(sender, "messages.no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand":        return handleWand(sender);
            case "pos1":        return handlePos(sender, 1);
            case "pos2":        return handlePos(sender, 2);
            case "create":      return handleCreate(sender, args);
            case "delete":      return handleDelete(sender, args);
            case "list":        return handleList(sender);
            case "reset":       return handleReset(sender, args);
            case "resetblocks": return handleResetBlocks(sender, args);
            case "resetitems":  return handleResetItems(sender, args);
            case "info":        return handleInfo(sender, args);
            case "reload":
                plugin.getZoneManager().reload();
                msg(sender, "messages.reloaded");
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "messages.players-only");
            return true;
        }
        player.getInventory().addItem(new ItemStack(plugin.getWandMaterial()));
        msg(sender, "messages.wand-given");
        return true;
    }

    private boolean handlePos(CommandSender sender, int which) {
        if (!(sender instanceof Player player)) {
            msg(sender, "messages.players-only");
            return true;
        }
        Location loc = player.getLocation();
        if (which == 1) {
            WandListener.setPos1(player, loc);
            sendLoc(player, "messages.pos1-set", loc);
        } else {
            WandListener.setPos2(player, loc);
            sendLoc(player, "messages.pos2-set", loc);
        }
        return true;
    }

    // /boxpvp create <name> [interval]
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg(sender, "messages.players-only");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp create <name> [interval-seconds]");
            return true;
        }
        Location p1 = WandListener.getPos1(player);
        Location p2 = WandListener.getPos2(player);
        if (p1 == null || p2 == null) {
            msg(sender, "messages.no-selection");
            return true;
        }
        if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            sender.sendMessage(ChatColor.RED + "Both points must be in the same world.");
            return true;
        }

        String name = args[1];
        int interval = plugin.getConfig().getInt("default-reset-interval", 300);
        if (args.length >= 3) {
            try {
                interval = Integer.parseInt(args[2]);
                if (interval < 1) interval = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Interval must be a number (seconds).");
                return true;
            }
        }

        boolean ok = plugin.getZoneManager().createZone(
                name, p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(),
                interval);

        if (!ok) {
            msg(sender, "messages.zone-exists");
            return true;
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String m = plugin.getConfig().getString("messages.zone-created", "")
                .replace("{zone}", name)
                .replace("{interval}", String.valueOf(interval));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + m));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp delete <name>");
            return true;
        }
        boolean ok = plugin.getZoneManager().deleteZone(args[1]);
        if (ok) {
            sendNamed(sender, "messages.zone-deleted", args[1]);
        } else {
            sendNamed(sender, "messages.zone-not-found", args[1]);
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        var zones = plugin.getZoneManager().getZones().values();
        if (zones.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No zones defined.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "BoxPvP zones:");
        for (Zone z : zones) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + z.getName()
                    + ChatColor.GRAY + " (" + z.getWorldName() + ", "
                    + z.getIntervalSeconds() + "s, next reset in "
                    + z.getSecondsUntilReset() + "s)");
        }
        return true;
    }

    // Full reset (blocks + items) and restart countdown
    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp reset <name>");
            return true;
        }
        Zone zone = plugin.getZoneManager().getZone(args[1]);
        if (zone == null) {
            sendNamed(sender, "messages.zone-not-found", args[1]);
            return true;
        }
        plugin.getZoneManager().resetZone(zone);
        zone.resetCountdown();
        sender.sendMessage(ChatColor.GREEN + "Zone '" + zone.getName() + "' manually reset.");
        return true;
    }

    // Manual: blocks only
    private boolean handleResetBlocks(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp resetblocks <name>");
            return true;
        }
        Zone zone = plugin.getZoneManager().getZone(args[1]);
        if (zone == null) {
            sendNamed(sender, "messages.zone-not-found", args[1]);
            return true;
        }
        boolean ok = plugin.getZoneManager().resetBlocks(zone);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "World for zone '" + zone.getName() + "' is not loaded.");
            return true;
        }
        sendNamed(sender, "messages.blocks-reset", zone.getName());
        return true;
    }

    // Manual: items only
    private boolean handleResetItems(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp resetitems <name>");
            return true;
        }
        Zone zone = plugin.getZoneManager().getZone(args[1]);
        if (zone == null) {
            sendNamed(sender, "messages.zone-not-found", args[1]);
            return true;
        }
        int removed = plugin.getZoneManager().clearItems(zone);
        if (removed < 0) {
            sender.sendMessage(ChatColor.RED + "World for zone '" + zone.getName() + "' is not loaded.");
            return true;
        }
        sendNamed(sender, "messages.items-reset", zone.getName());
        sender.sendMessage(ChatColor.GRAY + "Removed " + removed + " item(s).");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /boxpvp info <name>");
            return true;
        }
        Zone z = plugin.getZoneManager().getZone(args[1]);
        if (z == null) {
            sendNamed(sender, "messages.zone-not-found", args[1]);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Zone " + z.getName());
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + z.getWorldName());
        sender.sendMessage(ChatColor.GRAY + "Min: " + ChatColor.WHITE
                + z.getMinX() + ", " + z.getMinY() + ", " + z.getMinZ());
        sender.sendMessage(ChatColor.GRAY + "Max: " + ChatColor.WHITE
                + z.getMaxX() + ", " + z.getMaxY() + ", " + z.getMaxZ());
        sender.sendMessage(ChatColor.GRAY + "Interval: " + ChatColor.WHITE + z.getIntervalSeconds() + "s");
        sender.sendMessage(ChatColor.GRAY + "Next reset in: " + ChatColor.WHITE
                + z.getSecondsUntilReset() + "s");
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "=== BoxPvP ===");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp wand " + ChatColor.GRAY + "- get the selection wand");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp pos1|pos2 " + ChatColor.GRAY + "- set a point at your position");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp create <name> [sec] " + ChatColor.GRAY + "- create a zone");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp delete <name> " + ChatColor.GRAY + "- delete a zone");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp reset <name> " + ChatColor.GRAY + "- reset blocks + items now");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp resetblocks <name> " + ChatColor.GRAY + "- reset only blocks");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp resetitems <name> " + ChatColor.GRAY + "- clear only items");
        s.sendMessage(ChatColor.YELLOW + "/boxpvp list | info <name> | reload");
    }

    // ---- helpers ----

    private void msg(CommandSender s, String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String m = plugin.getConfig().getString(path, "");
        s.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + m));
    }

    private void sendNamed(CommandSender s, String path, String name) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String m = plugin.getConfig().getString(path, "").replace("{zone}", name);
        s.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + m));
    }

    private void sendLoc(Player p, String path, Location loc) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String m = plugin.getConfig().getString(path, "")
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()));
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + m));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && NAME_SUBS.contains(args[0].toLowerCase())) {
            for (Zone z : plugin.getZoneManager().getZones().values()) {
                if (z.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(z.getName());
            }
        }
        return out;
    }
}
