package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 世界模板管理器
 * 负责从模板复制世界、删除世界等操作
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class WorldTemplateManager {

    private final Main plugin;
    private final File serverFolder;

    // 模板世界名称
    private String templateWorldName;

    // 世界副本前缀
    private String worldPrefix;

    // 当前世界计数器
    private int worldCounter;

    // 活跃的世界列表 <世界名称, World对象>
    private final Map<String, World> activeWorlds;

    // 正在复制的世界列表
    private final Set<String> copyingWorlds;

    public WorldTemplateManager(Main plugin) {
        this.plugin = plugin;
        this.serverFolder = plugin.getServer().getWorldContainer();
        this.activeWorlds = new HashMap<>();
        this.copyingWorlds = new HashSet<>();

        loadConfig();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        // 不再从 worlds.yml 读取配置，改为从 WorldConfigManager 读取
        plugin.getLogger().info("§a世界模板管理器已初始化");
        plugin.getLogger().info("§7世界模板将从各自的配置文件中读取");
    }

    /**
     * 检查模板世界是否存在
     * @return 是否存���
     */
    public boolean isTemplateExists() {
        File templateFolder = new File(serverFolder, templateWorldName);
        return templateFolder.exists() && templateFolder.isDirectory();
    }

    /**
     * 创建新的世界副本（异步）
     * @return CompletableFuture<World> 世界对象
     */
    public CompletableFuture<World> createWorldCopy() {
        if (!isTemplateExists()) {
            plugin.getLogger().severe("§c模板世界不存在: " + templateWorldName);
            return CompletableFuture.completedFuture(null);
        }

        // 生成新的世界名称
        String newWorldName = worldPrefix + worldCounter;
        worldCounter++;

        // 检查世界是否已在复制中
        if (copyingWorlds.contains(newWorldName)) {
            plugin.getLogger().warning("§c世界正在复制中: " + newWorldName);
            return CompletableFuture.completedFuture(null);
        }

        copyingWorlds.add(newWorldName);

        plugin.getLogger().info("§e正在复制世界: " + templateWorldName + " -> " + newWorldName);

        // 使用主配置文件的复制模式设置
        boolean asyncMode = plugin.getConfig().getString("world-copy.mode", "async").equalsIgnoreCase("async");

        if (asyncMode) {
            // 异步复制
            return CompletableFuture.supplyAsync(() -> {
                try {
                    copyWorld(templateWorldName, newWorldName);

                    // 在主线程加载世界
                    World world = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                        return loadWorld(newWorldName);
                    }).get();

                    if (world != null) {
                        activeWorlds.put(newWorldName, world);
                        plugin.getLogger().info("§a世界复制并加载成功: " + newWorldName);
                    }

                    copyingWorlds.remove(newWorldName);
                    return world;

                } catch (Exception e) {
                    plugin.getLogger().severe("§c世界复制失败: " + e.getMessage());
                    e.printStackTrace();
                    copyingWorlds.remove(newWorldName);
                    return null;
                }
            });
        } else {
            // 同步复制
            return CompletableFuture.completedFuture(createWorldCopySync(newWorldName));
        }
    }

    /**
     * 同步创建世界副本
     * @param newWorldName 新世界名称
     * @return World对象
     */
    private World createWorldCopySync(String newWorldName) {
        try {
            copyWorld(templateWorldName, newWorldName);
            World world = loadWorld(newWorldName);

            if (world != null) {
                activeWorlds.put(newWorldName, world);
                plugin.getLogger().info("§a世界复制并加载成功: " + newWorldName);
            }

            copyingWorlds.remove(newWorldName);
            return world;

        } catch (Exception e) {
            plugin.getLogger().severe("§c世界复制失败: " + e.getMessage());
            e.printStackTrace();
            copyingWorlds.remove(newWorldName);
            return null;
        }
    }

    /**
     * 复制世界文件
     * @param sourceWorldName 源世界名称
     * @param targetWorldName 目标世界名称
     */
    private void copyWorld(String sourceWorldName, String targetWorldName) throws IOException {
        File sourceFolder = new File(serverFolder, sourceWorldName);
        File targetFolder = new File(serverFolder, targetWorldName);

        if (!sourceFolder.exists()) {
            throw new IOException("源世界不存在: " + sourceWorldName);
        }

        if (targetFolder.exists()) {
            plugin.getLogger().warning("§e目标世界已存在，将被删除: " + targetWorldName);
            deleteWorldFiles(targetFolder);
        }

        // 创建目标文件夹
        targetFolder.mkdirs();

        // 使用默认的文件夹和文件列表（标准Minecraft世界结构）
        List<String> folders = Arrays.asList("region", "DIM-1", "DIM1", "data", "datapacks");
        List<String> files = Arrays.asList("level.dat", "level.dat_old");

        // 复制文件夹
        for (String folderName : folders) {
            File sourceSubFolder = new File(sourceFolder, folderName);
            File targetSubFolder = new File(targetFolder, folderName);

            if (sourceSubFolder.exists()) {
                copyFolder(sourceSubFolder.toPath(), targetSubFolder.toPath());
            }
        }

        // 复制文件
        for (String fileName : files) {
            File sourceFile = new File(sourceFolder, fileName);
            File targetFile = new File(targetFolder, fileName);

            if (sourceFile.exists()) {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        plugin.getLogger().info("§a世界文件复制完成: " + targetWorldName);
    }

    /**
     * 递归复制文件夹
     * @param source 源路径
     * @param target 目标路径
     */
    private void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 加载世界
     * @param worldName 世界名称
     * @return World对象
     */
    private World loadWorld(String worldName) {
        try {
            WorldCreator creator = new WorldCreator(worldName);

            // 从模板世界获取环境类型
            World templateWorld = Bukkit.getWorld(templateWorldName);
            if (templateWorld != null) {
                creator.environment(templateWorld.getEnvironment());
            }

            World world = creator.createWorld();

            if (world != null) {
                // 设置世界规则
                world.setAutoSave(false);
                plugin.getLogger().info("§a世界加载成功: " + worldName);
            }

            return world;

        } catch (Exception e) {
            plugin.getLogger().severe("§c世界加载失败: " + worldName);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除世界（异步）
     * @param worldName 世界名称
     */
    public CompletableFuture<Boolean> deleteWorld(String worldName) {
        // 使用主配置文件的删除延迟设置
        int deleteDelay = plugin.getConfig().getInt("world-management.delete-delay", 10);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 延迟删除
                Thread.sleep(deleteDelay * 1000L);

                // 在主线程卸载世界
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    World world = activeWorlds.remove(worldName);

                    if (world != null) {
                        // 踢出所有玩家
                        world.getPlayers().forEach(player -> {
                            String lobbyWorld = plugin.getConfig().getString("queue.lobby-world", "world");
                            World lobby = Bukkit.getWorld(lobbyWorld);
                            if (lobby != null) {
                                player.teleport(lobby.getSpawnLocation());
                            }
                        });

                        // 卸载世界
                        Bukkit.unloadWorld(world, false);
                        plugin.getLogger().info("§a世界已卸载: " + worldName);
                    }

                    return true;
                }).get();

                // 删除世界文件
                File worldFolder = new File(serverFolder, worldName);
                if (worldFolder.exists()) {
                    deleteWorldFiles(worldFolder);
                    plugin.getLogger().info("§a世界已删除: " + worldName);
                }

                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("§c世界删除失败: " + worldName);
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * 递归删除文件夹
     * @param folder 文件夹
     */
    private void deleteWorldFiles(File folder) throws IOException {
        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 获取活跃的世界
     * @param worldName 世界名称
     * @return World对象
     */
    public World getActiveWorld(String worldName) {
        return activeWorlds.get(worldName);
    }

    /**
     * 获取所有活跃的世界
     * @return 世界列表
     */
    public Collection<World> getAllActiveWorlds() {
        return activeWorlds.values();
    }

    /**
     * 检查世界是否在复制中
     * @param worldName 世界名称
     * @return 是否在复制
     */
    public boolean isCopying(String worldName) {
        return copyingWorlds.contains(worldName);
    }

    /**
     * 获取模板世界名称
     * @return 模板世界名称
     */
    public String getTemplateWorldName() {
        return templateWorldName;
    }

    /**
     * 清理所有世界
     */
    public void cleanup() {
        plugin.getLogger().info("§e正在清理所有游戏世界...");

        for (String worldName : new ArrayList<>(activeWorlds.keySet())) {
            deleteWorld(worldName);
        }
    }

    // ==================== 新增：多地图模板支持 ====================

    /**
     * 从地图池中随机选择一个世界配置
     * @return 世界配置名称，如果地图池为空返回null
     */
    public String selectRandomWorldConfig() {
        Map<String, Integer> worldWeights = plugin.getWorldConfigManager().getEnabledWorldsWithWeight();

        if (worldWeights.isEmpty()) {
            plugin.getLogger().warning("§c没有找到已启用的世界配置！");
            return null;
        }

        // 计算总权重
        int totalWeight = 0;
        for (int weight : worldWeights.values()) {
            totalWeight += weight;
        }

        // 基于权重随机选择
        Random random = new Random();
        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : worldWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                plugin.getLogger().info("§a从地图池随机选择: " + entry.getKey() + " (权重: " + entry.getValue() + ")");
                return entry.getKey();
            }
        }

        // 备选方案：返回第一个
        String fallback = worldWeights.keySet().iterator().next();
        plugin.getLogger().info("§e使用备选地图: " + fallback);
        return fallback;
    }

    /**
     * 从世界配置文件读取模板世界名称
     * @param worldConfigName 世界配置名称（例如: standard_arena）
     * @return 模板世界名称，如果配置不存在返回null
     */
    public String getTemplateWorldFromConfig(String worldConfigName) {
        // 使用 WorldConfigManager 获取模板世界名称
        String templateWorld = plugin.getWorldConfigManager().getTemplateWorldName(worldConfigName);

        if (templateWorld == null || templateWorld.isEmpty()) {
            plugin.getLogger().warning("§c世界配置 " + worldConfigName + " 中未指定 template-world");
            plugin.getLogger().warning("§c请检查配置文件 worlds/" + worldConfigName + ".yml 中的 world.template-world 字段");
            return null;
        }

        plugin.getLogger().info("§7世界配置 " + worldConfigName + " 使用模板: " + templateWorld);
        return templateWorld;
    }

    /**
     * 检查指定模板世界是否存在
     * @param templateWorldName 模板世界名称
     * @return 是否存在
     */
    public boolean isTemplateExists(String templateWorldName) {
        File templateFolder = new File(serverFolder, templateWorldName);
        return templateFolder.exists() && templateFolder.isDirectory();
    }

    /**
     * 使用指定的世界配置创建新的世界副本（异步）
     * @param worldConfigName 世界配置名称（例如: standard_arena, flyfortress）
     * @return CompletableFuture<World> 世界对象
     */
    public CompletableFuture<World> createWorldCopyFromConfig(String worldConfigName) {
        // 从配置文件读取模板世界名称
        String templateWorld = getTemplateWorldFromConfig(worldConfigName);
        if (templateWorld == null) {
            return CompletableFuture.completedFuture(null);
        }

        // 检查模板世界是否存在
        if (!isTemplateExists(templateWorld)) {
            plugin.getLogger().severe("§c模板世界不存在: " + templateWorld + " (配置: " + worldConfigName + ")");
            return CompletableFuture.completedFuture(null);
        }

        // 生成新的世界名称
        String newWorldName = worldPrefix + worldCounter;
        worldCounter++;

        // 检查世界是否已在复制中
        if (copyingWorlds.contains(newWorldName)) {
            plugin.getLogger().warning("§c世界正在复制中: " + newWorldName);
            return CompletableFuture.completedFuture(null);
        }

        copyingWorlds.add(newWorldName);

        plugin.getLogger().info("§e正在复制世界: " + templateWorld + " -> " + newWorldName + " (配置: " + worldConfigName + ")");

        // 使用主配置文件的复制模式设置
        boolean asyncMode = plugin.getConfig().getString("world-copy.mode", "async").equalsIgnoreCase("async");

        if (asyncMode) {
            // 异步复制
            return CompletableFuture.supplyAsync(() -> {
                try {
                    copyWorld(templateWorld, newWorldName);

                    // 在主线程加载世界
                    World world = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                        return loadWorldWithTemplate(newWorldName, templateWorld);
                    }).get();

                    if (world != null) {
                        activeWorlds.put(newWorldName, world);
                        plugin.getLogger().info("§a世界复制并加载成功: " + newWorldName + " (配置: " + worldConfigName + ")");
                    }

                    copyingWorlds.remove(newWorldName);
                    return world;

                } catch (Exception e) {
                    plugin.getLogger().severe("§c世界复制失败: " + e.getMessage());
                    e.printStackTrace();
                    copyingWorlds.remove(newWorldName);
                    return null;
                }
            });
        } else {
            // 同步复制
            return CompletableFuture.completedFuture(createWorldCopySyncWithTemplate(newWorldName, templateWorld, worldConfigName));
        }
    }

    /**
     * 同步创建世界副本（使用指定模板）
     * @param newWorldName 新世界名称
     * @param templateWorld 模板世界名称
     * @param worldConfigName 世界配置名称
     * @return World对象
     */
    private World createWorldCopySyncWithTemplate(String newWorldName, String templateWorld, String worldConfigName) {
        try {
            copyWorld(templateWorld, newWorldName);
            World world = loadWorldWithTemplate(newWorldName, templateWorld);

            if (world != null) {
                activeWorlds.put(newWorldName, world);
                plugin.getLogger().info("§a世界复制并加载成功: " + newWorldName + " (配置: " + worldConfigName + ")");
            }

            copyingWorlds.remove(newWorldName);
            return world;

        } catch (Exception e) {
            plugin.getLogger().severe("§c世界复制失败: " + e.getMessage());
            e.printStackTrace();
            copyingWorlds.remove(newWorldName);
            return null;
        }
    }

    /**
     * 加载世界（使用指定的模板世界）
     * @param worldName 世界名称
     * @param templateWorld 模板世界名称
     * @return World对象
     */
    private World loadWorldWithTemplate(String worldName, String templateWorld) {
        try {
            WorldCreator creator = new WorldCreator(worldName);

            // 从模板世界获取环境类型
            World templateWorldObj = Bukkit.getWorld(templateWorld);
            if (templateWorldObj != null) {
                creator.environment(templateWorldObj.getEnvironment());
            }

            World world = creator.createWorld();

            if (world != null) {
                // 设置世界规则
                world.setAutoSave(false);
                plugin.getLogger().info("§a世界加载成功: " + worldName);
            }

            return world;

        } catch (Exception e) {
            plugin.getLogger().severe("§c世界加载失败: " + worldName);
            e.printStackTrace();
            return null;
        }
    }
}
