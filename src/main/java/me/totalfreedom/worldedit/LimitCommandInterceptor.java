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
            
            // Get WorldEdit player - use WorldEdit's session manager
            org.bukkit.entity.Player bukkitPlayer = event.getPlayer();
            Player wePlayer = null;
            com.sk89q.worldedit.bukkit.WorldEditPlugin weBukkitPlugin = null;
            
            try {
                // Get WorldEdit plugin instance
                org.bukkit.plugin.Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
                if (wePlugin instanceof com.sk89q.worldedit.bukkit.WorldEditPlugin) {
                    weBukkitPlugin = (com.sk89q.worldedit.bukkit.WorldEditPlugin) wePlugin;
                    // Use the plugin's API to wrap the player
                    wePlayer = weBukkitPlugin.wrapPlayer(bukkitPlayer);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get WorldEdit player: " + e.getMessage());
            }
            
            if (wePlayer == null || weBukkitPlugin == null) {
                return; // Can't get WorldEdit player, skip
            }
            
            // Check the limit through our event system
            int newLimit = WorldEditHandler.limitChanged(wePlayer, limit, targetName);
            
            plugin.getLogger().info("Limit command intercepted: original=" + limit + ", newLimit=" + newLimit);
            
            if (newLimit < -1) {
                // Event was cancelled or limit was invalid
                event.setCancelled(true);
                if (limit > 10000) {
                    bukkitPlayer.sendMessage("§cLimit change rejected: Maximum allowed limit is 10000.");
                } else {
                    bukkitPlayer.sendMessage("§cLimit change was cancelled.");
                }
                return;
            }
            
            // Always cancel and set the limit manually to ensure our validation is applied
            event.setCancelled(true);
            
            // Get the target session
            com.sk89q.worldedit.session.SessionManager sessionManager = 
                    WorldEdit.getInstance().getSessionManager();
            com.sk89q.worldedit.LocalSession targetSession;
            
            if (targetName != null) {
                // Find session for target player
                org.bukkit.entity.Player targetBukkit = WorldEditHandler.getPlayer(targetName);
                if (targetBukkit != null) {
                    try {
                        Player targetWePlayer = weBukkitPlugin.wrapPlayer(targetBukkit);
                        if (targetWePlayer != null) {
                            targetSession = sessionManager.get(targetWePlayer);
                        } else {
                            targetSession = sessionManager.get(wePlayer);
                        }
                    } catch (Exception e) {
                        targetSession = sessionManager.get(wePlayer);
                    }
                } else {
                    bukkitPlayer.sendMessage("§cCould not find player: " + targetName);
                    return;
                }
            } else {
                targetSession = sessionManager.get(wePlayer);
            }
            
            // Set the limit directly (this is the validated/modified limit from the event)
            targetSession.setBlockChangeLimit(newLimit);
            
            // Send confirmation message
            if (newLimit != -1) {
                bukkitPlayer.sendMessage("§aBlock change limit set to " + newLimit + ".");
            } else {
                bukkitPlayer.sendMessage("§aBlock change limit set to unlimited.");
            }
        }
    }
    
    private void handleCommand(CommandEvent event) {
        Actor actor = event.getActor();
        if (!(actor instanceof Player)) {
            return;
        }
        
        // Get command string from the event using reflection
        // CommandEvent API varies, so we use reflection to find the correct method
        String command = null;
        try {
            // Try to find a method that returns the command string
            java.lang.reflect.Method[] methods = event.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.contains("command") || methodName.equals("getcommandstring") 
                    || methodName.equals("getcommand")) 
                    && method.getParameterCount() == 0 
                    && method.getReturnType() == String.class) {
                    try {
                        command = (String) method.invoke(event);
                        if (command != null && !command.isEmpty()) {
                            break;
                        }
                    } catch (Exception e) {
                        // Try next method
                    }
                }
            }
        } catch (Exception e) {
            // Can't get command via reflection, skip this handler
            return;
        }
        
        if (command == null || command.isEmpty()) {
            return;
        }
        
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

