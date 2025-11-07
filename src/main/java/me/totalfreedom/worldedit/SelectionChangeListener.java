package me.totalfreedom.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionChangeListener {
    
    private final TFWorldEditExtension plugin;
    private final Map<UUID, RegionSnapshot> lastSelections = new HashMap<>();
    private BukkitTask checkTask;
    private boolean registered = false;
    
    public SelectionChangeListener(TFWorldEditExtension plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        if (registered) {
            return;
        }
        
        // Poll for selection changes every tick
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkSelections, 1L, 1L);
        
        registered = true;
        plugin.getLogger().info("Selection change listener registered");
    }
    
    public void unregister() {
        if (!registered) {
            return;
        }
        
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        
        lastSelections.clear();
        registered = false;
        plugin.getLogger().info("Selection change listener unregistered");
    }
    
    private void checkSelections() {
        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        
        // Get all online Bukkit players and check their sessions
        for (org.bukkit.entity.Player bukkitPlayer : Bukkit.getOnlinePlayers()) {
            try {
                // Convert Bukkit Player to WorldEdit Player
                // Use WorldEdit's platform manager to get the player
                Player wePlayer = null;
                try {
                    // Get the Bukkit platform and use it to match the player
                    com.sk89q.worldedit.extension.platform.Platform platform = WorldEdit.getInstance()
                            .getPlatformManager()
                            .getPlatforms()
                            .stream()
                            .filter(p -> p.getConfiguration() != null)
                            .findFirst()
                            .orElse(null);
                    
                    // Use WorldEdit plugin instance to wrap the player
                    org.bukkit.plugin.Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
                    if (wePlugin instanceof com.sk89q.worldedit.bukkit.WorldEditPlugin) {
                        com.sk89q.worldedit.bukkit.WorldEditPlugin weBukkitPlugin = 
                                (com.sk89q.worldedit.bukkit.WorldEditPlugin) wePlugin;
                        wePlayer = weBukkitPlugin.wrapPlayer(bukkitPlayer);
                    }
                } catch (Exception e) {
                    // Ignore - can't convert player
                }
                
                if (wePlayer == null) {
                    continue;
                }
                
                UUID uuid = wePlayer.getUniqueId();
                
                try {
                    Region region = sessionManager
                            .get(wePlayer)
                            .getSelection(wePlayer.getWorld());
                    
                    RegionSnapshot current = new RegionSnapshot(
                            region.getMinimumPoint(),
                            region.getMaximumPoint()
                    );
                    
                    RegionSnapshot last = lastSelections.get(uuid);
                    
                    if (last == null || !last.equals(current)) {
                        // Selection changed
                        lastSelections.put(uuid, current);
                        WorldEditHandler.selectionChanged(wePlayer);
                    }
                } catch (IncompleteRegionException e) {
                    // No selection, remove from tracking
                    if (lastSelections.remove(uuid) != null) {
                        // Selection was cleared - fire event
                        try {
                            WorldEditHandler.selectionChanged(wePlayer);
                        } catch (Exception ex) {
                            // Ignore - selection is incomplete
                        }
                    }
                } catch (Exception e) {
                    // Ignore errors for individual players
                }
            } catch (Exception e) {
                // Ignore errors for individual players
            }
        }
    }
    
    private static class RegionSnapshot {
        private final com.sk89q.worldedit.math.BlockVector3 min;
        private final com.sk89q.worldedit.math.BlockVector3 max;
        
        public RegionSnapshot(com.sk89q.worldedit.math.BlockVector3 min, com.sk89q.worldedit.math.BlockVector3 max) {
            this.min = min;
            this.max = max;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RegionSnapshot)) {
                return false;
            }
            RegionSnapshot other = (RegionSnapshot) obj;
            return min.equals(other.min) && max.equals(other.max);
        }
        
        @Override
        public int hashCode() {
            return min.hashCode() * 31 + max.hashCode();
        }
    }
}

