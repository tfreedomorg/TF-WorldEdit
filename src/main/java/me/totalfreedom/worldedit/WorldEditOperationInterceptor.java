package me.totalfreedom.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts WorldEdit operations and fires WorldEditOperationEvent before they execute.
 * 
 * NOTE: This intercepts at the command level. If WorldEdit operations bypass command events
 * or execute asynchronously, they may not be caught. For comprehensive protection, consider
 * also listening to BlockBreakEvent/BlockPlaceEvent at the Bukkit level.
 */
public class WorldEditOperationInterceptor implements Listener {
    
    private final TFWorldEditExtension plugin;
    private boolean registered = false;
    
    // Track cancelled commands per player to prevent execution
    private final Map<UUID, Set<String>> cancelledCommands = new ConcurrentHashMap<>();
    
    // Commands that modify blocks (operations we want to intercept)
    private static final Set<String> OPERATION_COMMANDS = new HashSet<>();
    
    static {
        OPERATION_COMMANDS.add("set");
        OPERATION_COMMANDS.add("replace");
        OPERATION_COMMANDS.add("fill");
        OPERATION_COMMANDS.add("drain");
        OPERATION_COMMANDS.add("fixlava");
        OPERATION_COMMANDS.add("fixwater");
        OPERATION_COMMANDS.add("remove");
        OPERATION_COMMANDS.add("removenear");
        OPERATION_COMMANDS.add("repair");
        OPERATION_COMMANDS.add("smooth");
        OPERATION_COMMANDS.add("overlay");
        OPERATION_COMMANDS.add("stack");
        OPERATION_COMMANDS.add("move");
        OPERATION_COMMANDS.add("copy");
        OPERATION_COMMANDS.add("cut");
        OPERATION_COMMANDS.add("paste");
        OPERATION_COMMANDS.add("rotate");
        OPERATION_COMMANDS.add("flip");
        OPERATION_COMMANDS.add("undo");
        OPERATION_COMMANDS.add("redo");
        OPERATION_COMMANDS.add("clearclipboard");
    }
    
    public WorldEditOperationInterceptor(TFWorldEditExtension plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        if (registered) {
            return;
        }
        
        // Register Bukkit listener for command interception
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Register with WorldEdit's event bus to intercept commands
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @com.google.common.eventbus.Subscribe
            public void onCommand(CommandEvent event) {
                handleCommand(event);
            }
        });
        
        registered = true;
        plugin.getLogger().info("WorldEdit operation interceptor registered");
    }
    
    public void unregister() {
        // Note: Can't easily unregister from EventBus, but that's okay
        cancelledCommands.clear();
        registered = false;
        plugin.getLogger().info("WorldEdit operation interceptor unregistered");
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        String commandName = extractCommandName(command);
        
        // Check if this is a WorldEdit operation command
        if (!isWorldEditCommand(commandName)) {
            return;
        }
        
        // Check if this command was cancelled
        UUID playerId = event.getPlayer().getUniqueId();
        Set<String> cancelled = cancelledCommands.get(playerId);
        if (cancelled != null && cancelled.contains(command.toLowerCase())) {
            event.setCancelled(true);
            cancelled.remove(command.toLowerCase());
            if (cancelled.isEmpty()) {
                cancelledCommands.remove(playerId);
            }
            return;
        }
        
        // Intercept WorldEdit commands at Bukkit level
        // Get WorldEdit player and selection
        org.bukkit.entity.Player bukkitPlayer = event.getPlayer();
        Player wePlayer = null;
        
        try {
            org.bukkit.plugin.Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
            if (wePlugin instanceof com.sk89q.worldedit.bukkit.WorldEditPlugin) {
                com.sk89q.worldedit.bukkit.WorldEditPlugin weBukkitPlugin = 
                        (com.sk89q.worldedit.bukkit.WorldEditPlugin) wePlugin;
                wePlayer = weBukkitPlugin.wrapPlayer(bukkitPlayer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get WorldEdit player: " + e.getMessage());
            return;
        }
        
        if (wePlayer == null) {
            return;
        }
        
        // Get the player's selection
        Region region = null;
        try {
            region = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wePlayer)
                    .getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            // No selection, but some commands might still work (like //undo, //redo)
            if (requiresSelection(commandName)) {
                return; // Let WorldEdit handle the error
            }
        }
        
        // Fire the operation event
        boolean shouldCancel = fireOperationEvent(wePlayer, region, command, commandName);
        
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }
    
    private boolean isWorldEditCommand(String commandName) {
        // WorldEdit commands typically start with // or /
        return isOperationCommand(commandName);
    }
    
    private void handleCommand(CommandEvent event) {
        Actor actor = event.getActor();
        if (!(actor instanceof Player)) {
            return; // Only intercept player commands
        }
        
        Player wePlayer = (Player) actor;
        
        // Get the command string
        String command = getCommandString(event);
        if (command == null || command.isEmpty()) {
            return;
        }
        
        // Check if this is an operation command
        String commandName = extractCommandName(command);
        if (!isOperationCommand(commandName)) {
            return; // Not an operation command, skip
        }
        
        // Get the player's selection
        Region region;
        try {
            region = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wePlayer)
                    .getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            // No selection, but some commands might still work (like //undo, //redo)
            // For commands that require a selection, we'll skip them
            if (requiresSelection(commandName)) {
                return;
            }
            // For commands that don't require selection, we'll fire the event with null region
            fireOperationEvent(wePlayer, null, command, commandName);
            return;
        }
        
        // Fire the operation event
        fireOperationEvent(wePlayer, region, command, commandName);
    }
    
    private boolean fireOperationEvent(Player wePlayer, Region region, String command, String commandName) {
        org.bukkit.entity.Player bukkitPlayer = WorldEditHandler.getPlayer(wePlayer);
        if (bukkitPlayer == null) {
            return false;
        }
        
        World world = WorldEditHandler.getWorld(wePlayer.getWorld());
        if (world == null) {
            return false;
        }
        
        Vector minVector = null;
        Vector maxVector = null;
        
        if (region != null) {
            minVector = new Vector(
                    region.getMinimumPoint().x(),
                    region.getMinimumPoint().y(),
                    region.getMinimumPoint().z()
            );
            maxVector = new Vector(
                    region.getMaximumPoint().x(),
                    region.getMaximumPoint().y(),
                    region.getMaximumPoint().z()
            );
        }
        
        WorldEditOperationEvent.OperationType operationType = 
                WorldEditOperationEvent.OperationType.fromCommand(commandName);
        
        WorldEditOperationEvent event = new WorldEditOperationEvent(
                bukkitPlayer,
                world,
                minVector,
                maxVector,
                operationType,
                command
        );
        
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return true;
        }
        
        return false;
    }
    
    private String getCommandString(CommandEvent event) {
        // Try to get the command string from the event
        // CommandEvent API in WorldEdit 7.3.x may vary
        try {
            // Try common method names
            java.lang.reflect.Method[] methods = event.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.equals("getcommand") || methodName.equals("getcommandstring") 
                    || methodName.equals("getinput")) 
                    && method.getParameterCount() == 0 
                    && method.getReturnType() == String.class) {
                    try {
                        String result = (String) method.invoke(event);
                        if (result != null && !result.isEmpty()) {
                            return result;
                        }
                    } catch (Exception e) {
                        // Try next method
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get command string from CommandEvent: " + e.getMessage());
        }
        
        return null;
    }
    
    private String extractCommandName(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        
        String cmd = command.toLowerCase().trim();
        // Remove leading slashes
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        
        // Split by space to get the command name
        String[] parts = cmd.split("\\s+");
        return parts.length > 0 ? parts[0] : cmd;
    }
    
    private boolean isOperationCommand(String commandName) {
        return OPERATION_COMMANDS.contains(commandName.toLowerCase());
    }
    
    private boolean requiresSelection(String commandName) {
        // Commands that don't require a selection
        Set<String> noSelectionCommands = new HashSet<>();
        noSelectionCommands.add("undo");
        noSelectionCommands.add("redo");
        noSelectionCommands.add("clearclipboard");
        
        return !noSelectionCommands.contains(commandName.toLowerCase());
    }
}

