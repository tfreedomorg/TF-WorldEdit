package me.totalfreedom.worldedit;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

/**
 * Event fired before a WorldEdit operation executes (e.g., //set, //replace, //fill).
 * This event can be cancelled to prevent the operation from executing.
 */
public class WorldEditOperationEvent extends PlayerEvent implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final World world;
    private final Vector minVector;
    private final Vector maxVector;
    private final OperationType operationType;
    private final String command;
    private final SourceType sourceType;
    
    public WorldEditOperationEvent(Player player, World world, Vector minVector, Vector maxVector, 
                                   OperationType operationType, String command) {
        this(player, world, minVector, maxVector, operationType, command, SourceType.WORLDEDIT);
    }
    
    /**
     * Constructor for Extent-based operations
     */
    public WorldEditOperationEvent(Player player, World world, Vector minVector, Vector maxVector, 
                                   OperationType operationType, String command, SourceType sourceType) {
        super(player);
        this.world = world;
        this.minVector = minVector;
        this.maxVector = maxVector;
        this.operationType = operationType;
        this.command = command;
        this.sourceType = sourceType != null ? sourceType : SourceType.WORLDEDIT;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Vector getMinVector() {
        return minVector;
    }
    
    public Vector getMaxVector() {
        return maxVector;
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public String getCommand() {
        return command;
    }
    
    public SourceType getSourceType() {
        return sourceType;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    /**
     * Enum representing different types of WorldEdit operations.
     */
    public enum OperationType {
        SET("set"),
        REPLACE("replace"),
        FILL("fill"),
        DRAIN("drain"),
        FIXLAVA("fixlava"),
        FIXWATER("fixwater"),
        REMOVE("remove"),
        REMOVENEAR("removenear"),
        REPAIR("repair"),
        SMOOTH("smooth"),
        OVERLAY("overlay"),
        STACK("stack"),
        MOVE("move"),
        COPY("copy"),
        CUT("cut"),
        PASTE("paste"),
        ROTATE("rotate"),
        FLIP("flip"),
        UNDO("undo"),
        REDO("redo"),
        CLEARCLIPBOARD("clearclipboard"),
        OTHER("other");
        
        private final String name;
        
        OperationType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public static OperationType fromCommand(String command) {
            if (command == null || command.isEmpty()) {
                return OTHER;
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
            String commandName = parts.length > 0 ? parts[0] : cmd;
            
            for (OperationType type : values()) {
                if (type.name.equalsIgnoreCase(commandName)) {
                    return type;
                }
            }
            
            return OTHER;
        }
    }
    
    /**
     * Enum representing the plugin source of the operation.
     */
    public enum SourceType {
        /**
         * WorldEdit
         */
        WORLDEDIT,
        
        /**
         * FastAsyncWorldEdit
         */
        FAWE,
        
        /**
         * FastAsyncVoxelSniper
         */
        VOXELSNIPER,
        
        /**
         * Direct API usage
         */
        API
    }
}

