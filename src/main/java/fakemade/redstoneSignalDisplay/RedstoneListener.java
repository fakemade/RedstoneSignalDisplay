package fakemade.redstoneSignalDisplay;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class RedstoneListener implements Listener {

    private final RedstoneSignalDisplay plugin;
    private final DisplayManager displayManager; // Теперь инициализируется напрямую

    // КОНСТРУКТОР ИСПРАВЛЕН: принимаем оба параметра напрямую
    public RedstoneListener(RedstoneSignalDisplay plugin, DisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager; // Безопасная инициализация
    }

    @EventHandler
    public void onRedstoneChange(BlockRedstoneEvent event) {
        if (event.getBlock().getType() != Material.REDSTONE_WIRE) return;

        int newSignal = event.getNewCurrent();
        Location blockLoc = event.getBlock().getLocation();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() == blockLoc.getWorld() &&
                    player.getLocation().distanceSquared(blockLoc) <= 144) {
                displayManager.updateDisplay(player, blockLoc, newSignal);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        displayManager.refreshForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        displayManager.refreshForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        displayManager.removeAllForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        displayManager.removeAllForPlayer(event.getPlayer());
    }
}