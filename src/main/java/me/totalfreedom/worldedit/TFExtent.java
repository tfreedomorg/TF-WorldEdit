package me.totalfreedom.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Custom Extent wrapper that intercepts block changes and fires WorldEditOperationEvent.
 * This allows us to intercept all WorldEdit/FAWE/VoxelSniper operations at the block level.
 */
public class TFExtent extends AbstractDelegateExtent {
    
    private final Player wePlayer;
    private final com.sk89q.worldedit.world.World weWorld;
    private boolean eventFired = false;
    private boolean cancelled = false;
    private BlockVector3 minPos = null;
    private BlockVector3 maxPos = null;
    
    public TFExtent(Extent extent, Player wePlayer, com.sk89q.worldedit.world.World weWorld) {
        super(extent);
        this.wePlayer = wePlayer;
        this.weWorld = weWorld;
    }
    
    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block) throws WorldEditException {
        // Track bounds for the event
        if (minPos == null) {
            minPos = pos;
        } else {
            int minX = Math.min(minPos.x(), pos.x());
            int minY = Math.min(minPos.y(), pos.y());
            int minZ = Math.min(minPos.z(), pos.z());
            minPos = BlockVector3.at(minX, minY, minZ);
        }
        
        if (maxPos == null) {
            maxPos = pos;
        } else {
            int maxX = Math.max(maxPos.x(), pos.x());
            int maxY = Math.max(maxPos.y(), pos.y());
            int maxZ = Math.max(maxPos.z(), pos.z());
            maxPos = BlockVector3.at(maxX, maxY, maxZ);
        }
        
        // Fire event on first block change
        if (!eventFired) {
            eventFired = true;
            cancelled = fireOperationEvent();
        }
        
        // If cancelled, don't apply the change
        if (cancelled) {
            return false;
        }
        
        // Apply the change
        return super.setBlock(pos, block);
    }
    
    private boolean fireOperationEvent() {
        org.bukkit.entity.Player bukkitPlayer = WorldEditHandler.getPlayer(wePlayer);
        if (bukkitPlayer == null) {
            return false;
        }
        
        World world = WorldEditHandler.getWorld(weWorld);
        if (world == null) {
            return false;
        }
        
        // Determine source type
        WorldEditOperationEvent.SourceType sourceType = determineSourceType();
        
        // Create vectors for bounds (or null if no blocks changed yet)
        Vector minVector = null;
        Vector maxVector = null;
        if (minPos != null && maxPos != null) {
            minVector = new Vector(minPos.x(), minPos.y(), minPos.z());
            maxVector = new Vector(maxPos.x(), maxPos.y(), maxPos.z());
        }
        
        // Fire the event
        WorldEditOperationEvent event = new WorldEditOperationEvent(
                bukkitPlayer,
                world,
                minVector,
                maxVector,
                WorldEditOperationEvent.OperationType.OTHER,
                null, // No command string available from Extent
                sourceType
        );
        
        Bukkit.getPluginManager().callEvent(event);
        
        return event.isCancelled();
    }
    
    private WorldEditOperationEvent.SourceType determineSourceType() {
        // Try to detect the source using reflection
        try {
            // Check if FAWE is available
            Class.forName("com.fastasyncworldedit.core.FaweAPI");
            
            // Check if this is a VoxelSniper operation
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (className.contains("voxelsniper") || className.contains("VoxelSniper")) {
                    return WorldEditOperationEvent.SourceType.VOXELSNIPER;
                }
                if (className.contains("fastasyncworldedit") && !className.contains("voxelsniper")) {
                    return WorldEditOperationEvent.SourceType.FAWE;
                }
            }
            
            // If FAWE is available but no specific source detected, assume FAWE
            return WorldEditOperationEvent.SourceType.FAWE;
        } catch (ClassNotFoundException e) {
            // WorldEdit
            return WorldEditOperationEvent.SourceType.WORLDEDIT;
        }
    }
}
