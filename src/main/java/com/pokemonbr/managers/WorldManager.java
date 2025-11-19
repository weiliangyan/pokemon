package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 世界管理器
 * 管理游戏世界的复制和恢复
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class WorldManager {

    private final Main plugin;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 备份游戏世界
     * @param worldName 世界名称
     * @return 是否成功
     */
    public boolean backupWorld(String worldName) {
        if (!plugin.getConfig().getBoolean("world-restore.enabled", true)) {
            return false;
        }

        try {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.exists()) {
                plugin.getLogger().warning("§c世界文件夹不存在: " + worldName);
                return false;
            }

            String templatePath = plugin.getConfig().getString("world-restore.template-path", worldName + "_template");
            File templateFolder = new File(Bukkit.getWorldContainer(), templatePath);

            // 删除旧备份
            if (templateFolder.exists()) {
                deleteDirectory(templateFolder.toPath());
            }

            // 创建新备份
            plugin.getLogger().info("§e正在备份世界 " + worldName + "...");
            copyDirectory(worldFolder.toPath(), templateFolder.toPath());
            plugin.getLogger().info("§a世界备份完成！");

            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("§c备份世界失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 还原游戏世界
     * @param worldName 世界名称
     * @return 是否成功
     */
    public boolean restoreWorld(String worldName) {
        if (!plugin.getConfig().getBoolean("world-restore.enabled", true)) {
            return false;
        }

        try {
            plugin.getLogger().info("§e正在还原世界 " + worldName + "...");

            // 1. 卸载世界
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                // 传送所有玩家到默认世界
                World defaultWorld = Bukkit.getWorlds().get(0);
                world.getPlayers().forEach(player -> player.teleport(defaultWorld.getSpawnLocation()));

                // 卸载世界
                if (!Bukkit.unloadWorld(world, false)) {
                    plugin.getLogger().warning("§c无法卸载世界 " + worldName);
                    return false;
                }
            }

            // 2. 等待一段时间确保世界完全卸载
            Thread.sleep(2000);

            // 3. 删除世界文件夹
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (worldFolder.exists()) {
                deleteDirectory(worldFolder.toPath());
            }

            // 4. 从模板复制世界
            String templatePath = plugin.getConfig().getString("world-restore.template-path", worldName + "_template");
            File templateFolder = new File(Bukkit.getWorldContainer(), templatePath);

            if (!templateFolder.exists()) {
                plugin.getLogger().warning("§c未找到世界模板: " + templatePath);
                plugin.getLogger().warning("§e正在创建世界备份...");

                // 重新加载世界并备份
                Bukkit.createWorld(new WorldCreator(worldName));
                return backupWorld(worldName);
            }

            copyDirectory(templateFolder.toPath(), worldFolder.toPath());

            // 5. 重新加载世界
            Bukkit.createWorld(new WorldCreator(worldName));

            plugin.getLogger().info("§a世界还原完成！");

            // 6. 如果启用了箱子刷新，填充箱子
            if (plugin.getConfig().getBoolean("world-restore.refresh-chests", true)) {
                World restoredWorld = Bukkit.getWorld(worldName);
                if (restoredWorld != null) {
                    // 延迟填充，确保世界完全加载
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getLootChestManager().fillAllChests(restoredWorld);
                    }, 40L); // 2秒后填充
                }
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("§c还原世界失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 异步还原世界
     * @param worldName 世界名称
     */
    public void restoreWorldAsync(String worldName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            restoreWorld(worldName);
        });
    }

    /**
     * 复制文件夹
     * @param source 源文件夹
     * @param target 目标文件夹
     * @throws IOException IO异常
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));

                    // 跳过 session.lock 和 uid.dat 文件
                    String fileName = sourcePath.getFileName().toString();
                    if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                        return;
                    }

                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("§c复制文件失败: " + sourcePath + " -> " + e.getMessage());
                }
            });
        }
    }

    /**
     * 删除文件夹
     * @param path 文件夹路径
     * @throws IOException IO异常
     */
    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            // 跳过 session.lock
                            if (!p.getFileName().toString().equals("session.lock")) {
                                Files.deleteIfExists(p);
                            }
                        } catch (IOException e) {
                            plugin.getLogger().warning("§c删除文件失败: " + p + " -> " + e.getMessage());
                        }
                    });
        }
    }
}
