package me.totalfreedom.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.regex.Pattern;

public class LimitCommandInterceptor implements Listener {
    
    private final TFWorldEditExtension plugin;
    private static final Pattern LIMIT_COMMAND = Pattern.compile("^/(?:limit|/limit)\\s+(\\d+|-1)(?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);
    private boolean registered = false;
    
    public LimitCommandInterceptor(TFWorldEditExtension plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        if (registered) {
            return;
        }
        
        // Register Bukkit listener for command interception
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Also listen to WorldEdit's command event bus
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @com.google.common.eventbus.Subscribe
            public void onCommand(CommandEvent event) {
                handleCommand(event);
            }
        });
        
        registered = true;
        plugin.getLogger().info("Limit command interceptor registered");
    }
    
    public void unregister() {
        if (!registered) {
            return;
        }
        
        // Note: Can't easily unregister from EventBus, but that's okay
        registered = false;
        plugin.getLogger().info("Limit command interceptor unregistered");
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        java.util.regex.Matcher matcher = LIMIT_COMMAND.matcher(command);
        
        if (matcher.matches()) {
            int limit;
            try {
                limit = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return;
            }
            
            String targetName = matcher.group(2);
            
            // Get WorldEdit player
            org.bukkit.entity.Player bukkitPlayer = event.getPlayer();
            Player wePlayer = null;
            
            try {
                wePlayer = WorldEdit.getInstance()
                        .getPlatformManager()
                        .getPlatforms()
                        .stream()
                        .filter(p -> p.getConfiguration() != null)
                        .findFirst()
                        .map(p -> {
                            try {
                                return p.matchPlayer(bukkitPlayer);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null);
            } catch (Exception e) {
                // Ignore
            }
            
            if (wePlayer == null) {
                // Try direct conversion
                try {
                    wePlayer = WorldEdit.getInstance().wrapPlayer(bukkitPlayer);
                } catch (Exception e) {
                    return;
                }
            }
            
            if (wePlayer != null) {
                int newLimit = WorldEditHandler.limitChanged(wePlayer, limit, targetName);
                if (newLimit < -1) {
                    event.setCancelled(true);
                    bukkitPlayer.sendMessage("§cLimit change was cancelled.");
                }
            }
        }
    }
    
    private void handleCommand(CommandEvent event) {
        Actor actor = event.getActor();
        if (!(actor instanceof Player)) {
            return;
        }
        
        String command = event.getCommand();
        java.util.regex.Matcher matcher = LIMIT_COMMAND.matcher(command);
        
        if (matcher.matches()) {
            int limit;
            try {
                limit = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return;
            }
            
            String targetName = matcher.group(2);
            
            int newLimit = WorldEditHandler.limitChanged((Player) actor, limit, targetName);
            if (newLimit < -1) {
                event.setCancelled(true);
            }
        }
    }
}

