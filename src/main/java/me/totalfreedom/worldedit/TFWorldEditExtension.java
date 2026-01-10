package me.totalfreedom.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TFWorldEditExtension extends JavaPlugin {
    
    private static TFWorldEditExtension instance;
    private SelectionChangeListener selectionListener;
    private LimitCommandInterceptor limitInterceptor;
    private EditSessionInterceptor editSessionInterceptor;
    private boolean worldEditReady = false;
    
    private boolean faweEnabled = false;
    private boolean voxelSniperEnabled = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Detect available plugins
        faweEnabled = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
        voxelSniperEnabled = Bukkit.getPluginManager().isPluginEnabled("FastAsyncVoxelSniper");
        boolean worldEditEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldEdit") || faweEnabled;
        
        if (!worldEditEnabled) {
            getLogger().severe("Neither WorldEdit nor FastAsyncWorldEdit is installed! This plugin requires one of them.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        if (faweEnabled) {
            getLogger().info("FastAsyncWorldEdit detected");
        } else if (worldEditEnabled) {
            getLogger().info("WorldEdit detected");
        }
        
        if (voxelSniperEnabled) {
            getLogger().info("FastAsyncVoxelSniper detected");
        }
        
        getLogger().info("Waiting for WorldEdit to initialize...");
        
        // Wait for WorldEdit to be ready
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @com.google.common.eventbus.Subscribe
            public void onPlatformReady(PlatformReadyEvent event) {
                if (!worldEditReady) {
                    worldEditReady = true;
                    initialize();
                }
            }
        });
        
        // If WorldEdit is already ready, initialize immediately
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!worldEditReady && WorldEdit.getInstance().getPlatformManager().getPlatforms().size() > 0) {
                worldEditReady = true;
                initialize();
            }
        }, 20L); // Wait 1 second
    }
    
    private void initialize() {
        getLogger().info("Initializing TF-WorldEdit Extension...");
        
        // Initialize WorldEditHandler
        WorldEditHandler.initialize(this);
        
        // Register selection change listener
        selectionListener = new SelectionChangeListener(this);
        selectionListener.register();
        
        // Register limit command interceptor
        limitInterceptor = new LimitCommandInterceptor(this);
        limitInterceptor.register();
        
        // Register EditSession interceptor (replaces command-based interception)
        // This catches WorldEdit, FAWE, and VoxelSniper operations
        editSessionInterceptor = new EditSessionInterceptor(this);
        editSessionInterceptor.register();
        
        getLogger().info("TF-WorldEdit Extension enabled!");
    }
    
    @Override
    public void onDisable() {
        if (selectionListener != null) {
            selectionListener.unregister();
        }
        if (limitInterceptor != null) {
            limitInterceptor.unregister();
        }
        if (editSessionInterceptor != null) {
            editSessionInterceptor.unregister();
        }
        
        instance = null;
        getLogger().info("TF-WorldEdit Extension disabled!");
    }
    
    public static TFWorldEditExtension getInstance() {
        return instance;
    }
    
    /**
     * Check if FastAsyncWorldEdit is enabled.
     */
    public boolean isFAWE() {
        return faweEnabled;
    }
    
    /**
     * Check if FastAsyncVoxelSniper is enabled.
     */
    public boolean isVoxelSniperEnabled() {
        return voxelSniperEnabled;
    }
}

