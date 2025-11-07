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
        
        for (com.sk89q.worldedit.extension.platform.Actor actor : sessionManager.getAll()) {
            if (!(actor instanceof Player)) {
                continue;
            }
            
            Player wePlayer = (Player) actor;
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
                    // Selection was cleared - fire event with null region
                    // We'll handle this in the handler
                    try {
                        WorldEditHandler.selectionChanged(wePlayer);
                    } catch (Exception ex) {
                        // Ignore - selection is incomplete
                    }
                }
            } catch (Exception e) {
                // Ignore errors for individual players
            }
        }
    }
    
    private static class RegionSnapshot {
        private final com.sk89q.worldedit.Vector min;
        private final com.sk89q.worldedit.Vector max;
        
        public RegionSnapshot(com.sk89q.worldedit.Vector min, com.sk89q.worldedit.Vector max) {
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

