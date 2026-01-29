package fakemade.redstoneSignalDisplay;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RedstoneSignalDisplay extends JavaPlugin {

    private DisplayManager displayManager;
    private BukkitTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        displayManager = new DisplayManager(this);


        getServer().getPluginManager().registerEvents(new RedstoneListener(this, displayManager), this);

        int refreshInterval = getConfig().getInt("refresh-interval-ticks", 10);
        refreshTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                displayManager.refreshForPlayer(player);
            }
        }, 20L, refreshInterval);

        getLogger().info("Плагин загружен. Сигнал редстоуна отображается над блоками.");
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (displayManager != null) {
            displayManager.removeAllDisplays();
        }
        getLogger().info("Плагин выгружен.");
    }
}
