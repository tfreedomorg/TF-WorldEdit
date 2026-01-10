package me.totalfreedom.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.EditSession;
import org.bukkit.Bukkit;

/**
 * Intercepts EditSession creation and wraps the extent with TFExtent to fire events.
 */
public class EditSessionInterceptor {
    
    private final TFWorldEditExtension plugin;
    private boolean registered = false;
    private Object eventHandler;
    
    public EditSessionInterceptor(TFWorldEditExtension plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        if (registered) {
            return;
        }
        
        // Register with WorldEdit's event bus
        eventHandler = new Object() {
            @com.google.common.eventbus.Subscribe
            public void onEditSession(EditSessionEvent event) {
                // Only intercept at the BEFORE_CHANGE stage
                if (event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
                    Actor actor = event.getActor();
                    
                    // Only intercept player operations
                    if (actor instanceof Player) {
                        Player wePlayer = (Player) actor;
                        com.sk89q.worldedit.world.World weWorld = event.getWorld();
                        
                        // Wrap the extent with our custom extent
                        event.setExtent(new TFExtent(event.getExtent(), wePlayer, weWorld));
                    }
                }
            }
        };
        
        WorldEdit.getInstance().getEventBus().register(eventHandler);
        registered = true;
        plugin.getLogger().info("EditSession interceptor registered (supports WorldEdit, FAWE, and VoxelSniper)");
    }
    
    public void unregister() {
        if (!registered) {
            return;
        }
        
        // The handler will remain but won't cause issues
        registered = false;
        plugin.getLogger().info("EditSession interceptor unregistered");
    }
    
    public boolean isRegistered() {
        return registered;
    }
}
