# TF-WorldEdit Extension

A WorldEdit extension plugin for TotalFreedom servers that adds:
- **SelectionChangedEvent** - Fires when a player's WorldEdit selection changes
- **LimitChangedEvent** - Fires when a player's block change limit is modified
- **WorldEditOperationEvent** - Fires before WorldEdit operations execute (e.g., //set, //replace, //fill)
- **Super admin bypass** for disallowed blocks (requires TotalFreedomMod)

## Requirements

- WorldEdit 7.3.x
- Paper/Spigot 1.21.x
- TotalFreedomMod (optional, for super admin features)

## Installation

1. Place `TF-WorldEdit.jar` in your `plugins/` folder
2. Ensure WorldEdit is installed
3. Restart your server

## Features

### Events

#### SelectionChangedEvent
Fires when a player's WorldEdit selection changes. Can be cancelled to prevent the selection.

```java
@EventHandler
public void onSelectionChange(SelectionChangedEvent event) {
    Player player = event.getPlayer();
    Vector min = event.getMinVector();
    Vector max = event.getMaxVector();
    World world = event.getWorld();
    // Handle selection change
    
    // Cancel the selection if needed
    // event.setCancelled(true);
}
```

#### LimitChangedEvent
Fires when a player's block change limit is modified via `//limit`. Can be cancelled or the limit can be modified.

```java
@EventHandler
public void onLimitChange(LimitChangedEvent event) {
    Player player = event.getPlayer();
    Player target = event.getTarget();
    int limit = event.getLimit();
    
    // Modify the limit
    event.setLimit(5000);
    
    // Or cancel the change
    // event.setCancelled(true);
}
```

#### WorldEditOperationEvent
Fires before a WorldEdit operation executes (e.g., //set, //replace, //fill). Can be cancelled to prevent the operation.

**Important:** This event intercepts at the command level. For comprehensive protection, you may also need to listen to `BlockBreakEvent` and `BlockPlaceEvent` at the Bukkit level, as some WorldEdit operations may bypass command events.

```java
@EventHandler
public void onWorldEditOperation(WorldEditOperationEvent event) {
    Player player = event.getPlayer();
    World world = event.getWorld();
    Vector min = event.getMinVector();
    Vector max = event.getMaxVector();
    WorldEditOperationEvent.OperationType type = event.getOperationType();
    String command = event.getCommand();
    
    // Check if operation is in a protected area
    if (isInProtectedArea(world, min, max)) {
        event.setCancelled(true);
        player.sendMessage("§cYou cannot perform WorldEdit operations in this area.");
    }
}
```

### Super Admin Bypass

If TotalFreedomMod is installed, super admins can use blocks that are in WorldEdit's disallowed blocks list. This is handled automatically through the `WorldEditHandler.isSuperAdmin()` method.

## Building

```bash
./gradlew build
```

The JAR will be in `build/libs/TF-WorldEdit-2.0.0.jar`

## Development

This extension was created to modernize TF-WorldEdit (a fork of WorldEdit 6.1.7) by:
1. Extracting unique features from the fork
2. Creating a standalone extension plugin
3. Making it compatible with WorldEdit 7.3.x and Minecraft 1.21.x

## License

This plugin follows the same license as WorldEdit (LGPL v3).

