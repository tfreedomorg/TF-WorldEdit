package me.totalfreedom.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TFWorldEditExtension extends JavaPlugin {
    
    private static TFWorldEditExtension instance;
    private SelectionChangeListener selectionListener;
    private LimitCommandInterceptor limitInterceptor;
    private WorldEditOperationInterceptor operationInterceptor;
    private boolean worldEditReady = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Check if WorldEdit is loaded
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            getLogger().severe("WorldEdit is not installed! This plugin requires WorldEdit.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
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
        
        // Register operation interceptor
        operationInterceptor = new WorldEditOperationInterceptor(this);
        operationInterceptor.register();
        
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
        if (operationInterceptor != null) {
            operationInterceptor.unregister();
        }
        
        instance = null;
        getLogger().info("TF-WorldEdit Extension disabled!");
    }
    
    public static TFWorldEditExtension getInstance() {
        return instance;
    }
}

