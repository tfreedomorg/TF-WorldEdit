package me.totalfreedom.worldedit;

import com.google.common.base.Function;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public class WorldEditHandler {
    
    private static Logger logger;
    private static Function<org.bukkit.entity.Player, Boolean> superAdminProvider;
    private static boolean initialized = false;
    
    public static void initialize(TFWorldEditExtension plugin) {
        if (initialized) {
            return;
        }
        logger = plugin.getLogger();
        initialized = true;
    }
    
    public static void selectionChanged(Player wePlayer) {
        if (!initialized) {
            return;
        }
        
        final org.bukkit.entity.Player player = getPlayer(wePlayer);
        if (player == null) {
            return;
        }
        
        final Region region;
        try {
            region = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wePlayer)
                    .getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException ex) {
            return;
        }
        
        final World world = Bukkit.getWorld(wePlayer.getWorld().getName());
        if (world == null) {
            return;
        }
        
        final SelectionChangedEvent event = new SelectionChangedEvent(
                player,
                world,
                new Vector(
                        region.getMinimumPoint().getX(),
                        region.getMinimumPoint().getY(),
                        region.getMinimumPoint().getZ()
                ),
                new Vector(
                        region.getMaximumPoint().getX(),
                        region.getMaximumPoint().getY(),
                        region.getMaximumPoint().getZ()
                )
        );
        
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wePlayer)
                    .getRegionSelector(wePlayer.getWorld())
                    .clear();
        }
    }
    
    public static int limitChanged(Player wePlayer, int limit, @Nullable String targetName) {
        if (!initialized) {
            return limit;
        }
        
        final int failCondition = -10;
        final int defaultCondition = (limit >= 1 && limit <= 10000 ? limit : failCondition);
        final org.bukkit.entity.Player player = getPlayer(wePlayer);
        
        if (player == null) {
            return defaultCondition;
        }
        
        final org.bukkit.entity.Player target;
        if (targetName == null) {
            target = player;
        } else {
            target = getPlayer(targetName);
        }
        
        if (target == null) {
            return defaultCondition;
        }
        
        final LimitChangedEvent event = new LimitChangedEvent(player, target, limit);
        Bukkit.getPluginManager().callEvent(event);
        
        return event.isCancelled() ? failCondition : event.getLimit();
    }
    
    @SuppressWarnings("unchecked")
    public static boolean isSuperAdmin(Player wePlayer) {
        if (!initialized) {
            return false;
        }
        
        final org.bukkit.entity.Player player = getPlayer(wePlayer);
        if (player == null) {
            return false;
        }
        
        if (superAdminProvider == null) {
            final Plugin tfm = getTFM();
            if (tfm == null) {
                return false;
            }
            
            Object provider = null;
            for (RegisteredServiceProvider<?> serv : Bukkit.getServicesManager().getRegistrations(tfm)) {
                if (Function.class.isAssignableFrom(serv.getService())) {
                    provider = serv.getProvider();
                }
            }
            
            if (provider == null) {
                warning("Could not obtain SuperAdmin service provider!");
                return false;
            }
            
            superAdminProvider = (Function<org.bukkit.entity.Player, Boolean>) provider;
        }
        
        return superAdminProvider.apply(player);
    }
    
    public static org.bukkit.entity.Player getPlayer(Player wePlayer) {
        final org.bukkit.entity.Player player = Bukkit.getPlayer(wePlayer.getUniqueId());
        
        if (player == null) {
            debug("Could not resolve Bukkit player: " + wePlayer.getName());
            return null;
        }
        
        return player;
    }
    
    public static org.bukkit.entity.Player getPlayer(String match) {
        match = match.toLowerCase();
        
        org.bukkit.entity.Player found = null;
        int delta = Integer.MAX_VALUE;
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(match)) {
                int curDelta = player.getName().length() - match.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) {
                    break;
                }
            }
        }
        
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().contains(match)) {
                return player;
            }
        }
        return found;
    }
    
    public static World getWorld(com.sk89q.worldedit.world.World world) {
        return Bukkit.getWorld(world.getName());
    }
    
    public static Plugin getTFM() {
        final Plugin tfm = Bukkit.getPluginManager().getPlugin("TotalFreedomMod");
        if (tfm == null) {
            logger.warning("Could not resolve plugin: TotalFreedomMod");
        }
        return tfm;
    }
    
    public static void debug(String debug) {
        if (logger != null) {
            logger.info("[DEBUG] " + debug);
        }
    }
    
    public static void warning(String warning) {
        if (logger != null) {
            logger.warning(warning);
        }
    }
    
    public static void info(String info) {
        if (logger != null) {
            logger.info(info);
        }
    }
}

