package com.example.myplugin;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Keyed;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SimpleRecipeUnlocker extends JavaPlugin implements Listener {

    private FoliaLib foliaLib;
    private Set<NamespacedKey> allRecipeKeys = new HashSet<>();

    // Locales (messages) loaded from locales.yml
    private String msgSelfUnlock;
    private String msgOtherUnlockSender;
    private String msgOtherUnlockTarget;
    private String msgJoin;
    private String msgPlayerNotFound;
    private String msgSpecifyPlayer;
    private String msgReloadDone;

    private File localesFile;
    private FileConfiguration localesCfg;

    // Basic configurable settings (from config.yml)
    private boolean settingUnlockOnJoin = true;
    private boolean settingCacheOnEnable = true;
    private boolean settingBroadcastUnlock = false;
    private boolean settingLogUndiscoverable = false;
    // Permission constants
    private static final String PERM_USE = "simplerecipeunlocker.use";
    private static final String PERM_RELOAD = "simplerecipeunlocker.reload";
    private static final String PERM_OTHER = "simplerecipeunlocker.other";
    private static final String PERM_AUTOUNLOCK = "simplerecipeunlocker.autounlock";
    private static final String PERM_BROADCAST = "simplerecipeunlocker.broadcast";


    // Recipes that cannot be discovered (special recipes) cached after first attempt
    private final Set<NamespacedKey> nonDiscoverableKeys = new HashSet<>();

    @Override
    public void onEnable() {
        foliaLib = new FoliaLib(this);
        saveDefaultConfig();
        loadPluginSettings();
        setupLocales();
        getLogger().info("SimpleRecipeUnlocker enabled! Preparing recipe list... (FoliaLib: " + 
            (foliaLib.isFolia() ? "Folia" : foliaLib.isPaper() ? "Paper" : "Spigot") + " detected)");
        getServer().getPluginManager().registerEvents(this, this);
        if (settingCacheOnEnable) {
            // Use FoliaLib's scheduler for recipe caching
            foliaLib.getScheduler().runNextTick(task -> {
                cacheAllRecipes();
                if (settingUnlockOnJoin) {
                    for (Player player : getServer().getOnlinePlayers()) {
                        foliaLib.getScheduler().runAtEntity(player, entityTask -> unlockAllRecipesFor(player));
                    }
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
        getLogger().info("SimpleRecipeUnlocker disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("unlockrecipes")) {
            return false; // Not our command
        }
        if (!sender.hasPermission(PERM_USE)) {
            sender.sendMessage(color("&cKeine Berechtigung."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(PERM_RELOAD)) {
                sender.sendMessage(color("&cKeine Berechtigung."));
                return true;
            }
            reloadConfig();
            loadPluginSettings();
            reloadLocales();
            sender.sendMessage(color(msgReloadDone + " (&7config.yml & locales.yml neu geladen)"));
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                int newly = unlockAllRecipesFor(player);
                sender.sendMessage(format(msgSelfUnlock, player, newly));
            } else {
                sender.sendMessage(color(msgSpecifyPlayer));
            }
            return true;
        }
        Player target = getServer().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(format(msgPlayerNotFound.replace("{input}", args[0]), null, 0));
                return true;
            }
            // If unlocking for someone else, require other permission
            if (sender instanceof Player pSender && !pSender.getUniqueId().equals(target.getUniqueId())) {
                if (!sender.hasPermission(PERM_OTHER)) {
                    sender.sendMessage(color("&cKeine Berechtigung für andere Spieler."));
                    return true;
                }
            }
            int newly = unlockAllRecipesFor(target);
            sender.sendMessage(format(msgOtherUnlockSender, target, newly));
            if (sender != target) {
                target.sendMessage(format(msgOtherUnlockTarget, target, newly));
            }
            return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (settingUnlockOnJoin && p.hasPermission(PERM_AUTOUNLOCK)) {
            int newly = unlockAllRecipesFor(p);
            if (newly > 0) {
                p.sendMessage(format(msgJoin, p, newly));
                if (settingBroadcastUnlock && p.hasPermission(PERM_BROADCAST)) {
                    // Avoid deprecated broadcast method by iterating players
                    String msg = format(msgOtherUnlockSender, p, newly);
                    for (Player pl : getServer().getOnlinePlayers()) {
                        foliaLib.getScheduler().runAtEntity(pl, task -> pl.sendMessage(msg));
                    }
                }
                getLogger().info("Unlocked " + newly + " recipes for " + p.getName());
            } else {
                // No new recipes; remain silent to player per requirement
                getLogger().fine("No new recipes to unlock for " + p.getName());
            }
        }
    }

    private void cacheAllRecipes() {
        try {
            Set<NamespacedKey> keys = new HashSet<>();
            Iterator<Recipe> it = getServer().recipeIterator();
            while (it.hasNext()) {
                Recipe r = it.next();
                if (r instanceof Keyed keyed) {
                    keys.add(keyed.getKey());
                }
            }
            allRecipeKeys = keys;
            getLogger().info("Cached " + allRecipeKeys.size() + " total crafting recipes.");
        } catch (Exception ex) {
            getLogger().severe("Failed to cache recipes: " + ex.getMessage());
        }
    }

    private int unlockAllRecipesFor(Player player) {
        if (allRecipeKeys.isEmpty()) {
            cacheAllRecipes();
        }
        List<NamespacedKey> candidates = new ArrayList<>();
        for (NamespacedKey key : allRecipeKeys) {
            if (nonDiscoverableKeys.contains(key)) continue; // skip known special recipes
            if (!player.hasDiscoveredRecipe(key)) {
                candidates.add(key);
            }
        }
        if (candidates.isEmpty()) return 0;

        int actuallyDiscovered = player.discoverRecipes(candidates); // server returns count

        // Identify recipes still not discovered after attempt -> mark as non-discoverable
        if (actuallyDiscovered < candidates.size()) {
            int before = nonDiscoverableKeys.size();
            for (NamespacedKey key : candidates) {
                if (!player.hasDiscoveredRecipe(key)) {
                    nonDiscoverableKeys.add(key);
                }
            }
            if (settingLogUndiscoverable && nonDiscoverableKeys.size() > before) {
                getLogger().fine("Marked " + (nonDiscoverableKeys.size() - before) + " recipes as non-discoverable (special). Current total: " + nonDiscoverableKeys.size());
            }
        }
        return actuallyDiscovered;
    }

    private void loadMessages() {
    msgSelfUnlock = localesCfg.getString("messages.self_unlock", "&eYou have now unlocked all recipes! (&f{count}&e new)");
    msgOtherUnlockSender = localesCfg.getString("messages.other_unlock_sender", "&aUnlocked all recipes for &f{player}&a (&f{count}&a new)");
    msgOtherUnlockTarget = localesCfg.getString("messages.other_unlock_target", "&eAll recipes have been unlocked! (&f{count}&e new)");
    msgJoin = localesCfg.getString("messages.join", "&bAll recipes unlocked! (&f{count}&b new)");
    msgPlayerNotFound = localesCfg.getString("messages.player_not_found", "&cPlayer not found: {input}");
    msgSpecifyPlayer = localesCfg.getString("messages.specify_player", "&7Please specify a player: /unlockrecipes <player>");
    msgReloadDone = localesCfg.getString("messages.reload_done", "&aLocales reloaded.");
    }

    private void setupLocales() {
        if (localesFile == null) {
            localesFile = new File(getDataFolder(), "locales.yml");
        }
        if (!localesFile.exists()) {
            getDataFolder().mkdirs();
            saveResource("locales.yml", false);
        }
        localesCfg = YamlConfiguration.loadConfiguration(localesFile);
        loadMessages();
    }

    private void reloadLocales() {
        localesCfg = YamlConfiguration.loadConfiguration(localesFile);
        loadMessages();
    }

    private void loadPluginSettings() {
        settingUnlockOnJoin = getConfig().getBoolean("settings.unlock_on_join", true);
        settingCacheOnEnable = getConfig().getBoolean("settings.cache_on_enable", true);
    settingBroadcastUnlock = getConfig().getBoolean("settings.broadcast_unlock_on_join", false);
    settingLogUndiscoverable = getConfig().getBoolean("settings.log_undiscoverable", false);
    }

    private String format(String raw, Player target, int count) {
    if (raw == null || raw.isEmpty()) return "";
        String out = raw
                .replace("{player}", target != null ? target.getName() : "")
                .replace("{count}", String.valueOf(count));
        return color(out);
    }

    private String color(String s) {
        // ChatColor.translateAlternateColorCodes still used; warning suppression if marked deprecated in version
        @SuppressWarnings("deprecation")
        String colored = ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
        return colored;
    }
}
