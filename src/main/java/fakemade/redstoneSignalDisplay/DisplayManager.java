package fakemade.redstoneSignalDisplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager {

    private final RedstoneSignalDisplay plugin;
    private final double displayRadius;
    private final double displayHeight;
    private final TextColor textColor;
    private final int verticalRange; // Диапазон по Y

    // Кэш: игрок → (блок → сущность)
    private final Map<UUID, Map<Location, TextDisplay>> displays = new ConcurrentHashMap<>();

    // Кэш последних позиций игроков для оптимизации
    private final Map<UUID, Location> lastKnownLocations = new ConcurrentHashMap<>();

    public DisplayManager(RedstoneSignalDisplay plugin) {
        this.plugin = plugin;
        this.displayRadius = plugin.getConfig().getDouble("display-radius", 10.0);
        this.displayHeight = plugin.getConfig().getDouble("display-height", 0.35);
        this.verticalRange = plugin.getConfig().getInt("vertical-range", 5);

        String colorStr = plugin.getConfig().getString("text-color", "#FF5555");
        this.textColor = colorStr.equalsIgnoreCase("default") ?
                NamedTextColor.WHITE : TextColor.fromHexString(colorStr);
    }

    /**
     * Обновляет отображение для блока у игрока
     */
    public void updateDisplay(Player player, Location blockLoc, int signal) {
        TextDisplay existing = getDisplay(player, blockLoc);
        if (existing != null && !existing.isDead()) {
            existing.text(Component.text(String.valueOf(signal)).color(textColor));
            return;
        }

        // Создаём новую голограмму
        Location displayLoc = blockLoc.clone().add(0.5, displayHeight, 0.5);
        TextDisplay display = player.getWorld().spawn(displayLoc, TextDisplay.class, td -> {
            td.text(Component.text(String.valueOf(signal)).color(textColor));
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setShadowRadius(0.08f);
            td.setLineWidth(150);
            td.setBillboard(TextDisplay.Billboard.CENTER);
            td.setGravity(false);
            td.setPersistent(false);
        });

        displays.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(blockLoc, display);
    }

    private TextDisplay getDisplay(Player player, Location blockLoc) {
        Map<Location, TextDisplay> playerDisplays = displays.get(player.getUniqueId());
        return (playerDisplays != null) ? playerDisplays.get(blockLoc) : null;
    }


    public void removeAllForPlayer(Player player) {
        Map<Location, TextDisplay> playerDisplays = displays.remove(player.getUniqueId());
        lastKnownLocations.remove(player.getUniqueId());
        if (playerDisplays == null) return;

        for (TextDisplay display : playerDisplays.values()) {
            if (!display.isDead()) {
                try {
                    display.remove();
                } catch (Exception ignored) {}
            }
        }
    }

    public void removeAllDisplays() {
        for (Map<Location, TextDisplay> playerDisplays : displays.values()) {
            for (TextDisplay display : playerDisplays.values()) {
                if (!display.isDead()) {
                    try {
                        display.remove();
                    } catch (Exception ignored) {}
                }
            }
        }
        displays.clear();
        lastKnownLocations.clear();
    }

    public void refreshForPlayer(Player player) {
        Location playerLoc = player.getLocation();

        // Оптимизация: пропускаем обновление если игрок почти не двигался
        Location lastLoc = lastKnownLocations.get(player.getUniqueId());
        if (lastLoc != null &&
                lastLoc.getWorld() == playerLoc.getWorld() &&
                lastLoc.distanceSquared(playerLoc) < 1.0) { // Меньше 1 блока
            return;
        }
        lastKnownLocations.put(player.getUniqueId(), playerLoc.clone());

        double radiusSq = displayRadius * displayRadius;
        Set<Location> scannedBlocks = new HashSet<>();

        // Удаляем голограммы вне радиуса
        Map<Location, TextDisplay> playerDisplays = displays.get(player.getUniqueId());
        if (playerDisplays != null) {
            playerDisplays.keySet().removeIf(blockLoc -> {
                if (playerLoc.getWorld() != blockLoc.getWorld() ||
                        playerLoc.distanceSquared(blockLoc) > radiusSq) {
                    TextDisplay display = playerDisplays.get(blockLoc);
                    if (display != null && !display.isDead()) {
                        try {
                            display.remove();
                        } catch (Exception ignored) {}
                    }
                    return true;
                }
                return false;
            });
        }

        // УЛУЧШЕННОЕ СКАНИРОВАНИЕ: проверяем Y-координаты
        int centerX = playerLoc.getBlockX();
        int centerY = playerLoc.getBlockY();
        int centerZ = playerLoc.getBlockZ();
        int radius = (int) Math.ceil(displayRadius);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Проверяем расстояние по горизонтали сразу
                if (x * x + z * z > radiusSq) continue;

                // Сканируем по вертикали
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    Location blockLoc = new Location(
                            playerLoc.getWorld(),
                            centerX + x,
                            centerY + y,
                            centerZ + z
                    );

                    // Проверяем полное расстояние
                    if (playerLoc.distanceSquared(blockLoc) > radiusSq) {
                        continue;
                    }

                    Block block = blockLoc.getBlock();
                    if (block.getType() == Material.REDSTONE_WIRE) {
                        scannedBlocks.add(blockLoc);
                        try {
                            RedstoneWire wire = (RedstoneWire) block.getBlockData();
                            int signal = wire.getPower();
                            updateDisplay(player, blockLoc, signal);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Ошибка при обновлении дисплея: " + e.getMessage());
                        }
                    }
                }
            }
        }

        // Удаляем дисплеи для блоков, которые больше не редстоун
        if (playerDisplays != null) {
            playerDisplays.keySet().removeIf(blockLoc -> {
                if (!scannedBlocks.contains(blockLoc)) {
                    TextDisplay display = playerDisplays.get(blockLoc);
                    if (display != null && !display.isDead()) {
                        try {
                            display.remove();
                        } catch (Exception ignored) {}
                    }
                    return true;
                }
                return false;
            });
        }
    }
}