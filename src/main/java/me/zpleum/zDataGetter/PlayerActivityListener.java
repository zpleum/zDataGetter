package me.zpleum.zDataGetter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Material;

import java.util.UUID;

public class PlayerActivityListener implements Listener {
    private final ZDataGetter plugin;

    public PlayerActivityListener(ZDataGetter plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayTimes().putIfAbsent(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Update player play time (mocked here)
        int timePlayed = 10; // Replace with actual calculation
        plugin.getPlayTimes().put(event.getPlayer().getUniqueId(), timePlayed);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        int commands = plugin.getCommandsUsed().getOrDefault(event.getPlayer().getUniqueId(), 0);
        plugin.getCommandsUsed().put(event.getPlayer().getUniqueId(), commands + 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();

        // ตรวจจับเฉพาะพืชที่ต้องการนับ
        if (blockType == Material.WHEAT || blockType == Material.CARROTS || blockType == Material.BEETROOTS ||
                blockType == Material.POTATOES || blockType == Material.MELON || blockType == Material.PUMPKIN) {

            UUID playerId = event.getPlayer().getUniqueId();
            int crops = plugin.getCropsHarvested().getOrDefault(playerId, 0);
            plugin.getCropsHarvested().put(playerId, crops + 1);
        }
    }
}
