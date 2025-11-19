package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 世界配置管理器
 * 管理每个游戏世界的独立配置文件
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class WorldConfigManager {

    private final Main plugin;
    private final File worldsFolder;

    // 世界名称 -> 世界配置
    private final Map<String, FileConfiguration> worldConfigs;
    private final Map<String, File> worldConfigFiles;

    public WorldConfigManager(Main plugin) {
        this.plugin = plugin;
        this.worldConfigs = new HashMap<>();
        this.worldConfigFiles = new HashMap<>();

        // 创建 worlds 文件夹
        this.worldsFolder = new File(plugin.getDataFolder(), "worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
        }

        // 首次运行：从JAR包释放默认世界配置文件
        extractDefaultWorldConfigs();

        // 加载已存在的世界配置
        loadAllWorldConfigs();

        plugin.getLogger().info("§a世界配置管理器已初始化");
    }

    /**
     * 从JAR包中释放默认世界配置文件
     */
    private void extractDefaultWorldConfigs() {
        // 需要释放的世界配置文件列表
        String[] defaultWorldConfigs = {
            "flyfortress.yml",
            "standard_arena.yml"
        };

        for (String configName : defaultWorldConfigs) {
            File targetFile = new File(worldsFolder, configName);
            if (!targetFile.exists()) {
                try {
                    // 从JAR包中读取资源（路径包含worlds/前缀）
                    plugin.saveResource("worlds/" + configName, false);
                    plugin.getLogger().info("§a已从JAR包释放默认世界配置: " + configName);
                } catch (IllegalArgumentException e) {
                    // JAR包中没有该文件，跳过
                    plugin.getLogger().warning("§eJAR包中未找到默认配置: worlds/" + configName);
                } catch (Exception e) {
                    plugin.getLogger().severe("§c释放世界配置失败: " + configName + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 加载所有已存在的世界配置
     */
    private void loadAllWorldConfigs() {
        File[] files = worldsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("§7未找到已有的世界配置文件");
            return;
        }

        int enabledCount = 0;
        for (File file : files) {
            String worldName = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            worldConfigs.put(worldName, config);
            worldConfigFiles.put(worldName, file);

            // 检查是否启用
            boolean enabled = config.getBoolean("world.enabled", false);
            String status = enabled ? "§a[已启用]" : "§7[已禁用]";
            plugin.getLogger().info(status + " §7加载世界配置: " + worldName);

            if (enabled) {
                enabledCount++;
            }
        }

        plugin.getLogger().info("§a共加载 " + worldConfigs.size() + " 个世界配置，其中 " + enabledCount + " 个已启用");
    }

    /**
     * 为新世界创建配置文件
     * @param worldName 世界名称
     * @return 世界配置
     */
    public FileConfiguration createWorldConfig(String worldName) {
        File configFile = new File(worldsFolder, worldName + ".yml");

        // 如果已存在,直接返回
        if (configFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            worldConfigs.put(worldName, config);
            worldConfigFiles.put(worldName, configFile);
            return config;
        }

        // 从模板创建新配置
        FileConfiguration config = new YamlConfiguration();

        // 复制模板内容
        FileConfiguration template = loadTemplate();
        if (template != null) {
            for (String key : template.getKeys(true)) {
                config.set(key, template.get(key));
            }
        }

        // 设置世界名称
        config.set("world.name", worldName);
        config.set("world.alias", "&e&l" + worldName);

        // 保存配置
        try {
            config.save(configFile);
            worldConfigs.put(worldName, config);
            worldConfigFiles.put(worldName, configFile);

            plugin.getLogger().info("§a已为世界 " + worldName + " 创建配置文件");
        } catch (IOException e) {
            plugin.getLogger().severe("§c保存世界配置失败: " + e.getMessage());
        }

        return config;
    }

    /**
     * 加载配置模板
     * @return 模板配置
     */
    private FileConfiguration loadTemplate() {
        File templateFile = new File(plugin.getDataFolder(), "world-template.yml");

        if (!templateFile.exists()) {
            plugin.saveResource("world-template.yml", false);
        }

        return YamlConfiguration.loadConfiguration(templateFile);
    }

    /**
     * 获取世界配置
     * @param worldName 世界名称
     * @return 世界配置 或 null
     */
    public FileConfiguration getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    /**
     * 保存世界配置
     * @param worldName 世界名称
     */
    public void saveWorldConfig(String worldName) {
        FileConfiguration config = worldConfigs.get(worldName);
        File file = worldConfigFiles.get(worldName);

        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("§c保存世界配置失败: " + worldName);
            }
        }
    }

    /**
     * 获取世界别名（显示名称）
     * @param worldName 世界名称
     * @return 世界别名
     */
    public String getWorldAlias(String worldName) {
        FileConfiguration config = getWorldConfig(worldName);
        if (config == null) {
            return worldName;
        }

        String alias = config.getString("world.alias", worldName);
        return ChatColor.translateAlternateColorCodes('&', alias);
    }

    /**
     * 获取缩圈中心坐标
     * @param world 世界
     * @return 缩圈中心坐标
     */
    public Location getShrinkCenter(World world) {
        FileConfiguration config = getWorldConfig(world.getName());

        if (config == null) {
            // 使用全局配置
            return new Location(world,
                    plugin.getConfig().getDouble("shrink.center.x", 0.0),
                    world.getHighestBlockYAt(0, 0),
                    plugin.getConfig().getDouble("shrink.center.z", 0.0));
        }

        String mode = config.getString("shrink-center.mode", "fixed");

        if ("random".equals(mode)) {
            // 随机模式
            Random random = new Random();
            int minX = config.getInt("shrink-center.random.min-x", -200);
            int maxX = config.getInt("shrink-center.random.max-x", 200);
            int minZ = config.getInt("shrink-center.random.min-z", -200);
            int maxZ = config.getInt("shrink-center.random.max-z", 200);

            double x = minX + random.nextInt(maxX - minX + 1);
            double z = minZ + random.nextInt(maxZ - minZ + 1);
            double y = world.getHighestBlockYAt((int) x, (int) z);

            return new Location(world, x, y, z);
        } else {
            // 固定模式
            double x = config.getDouble("shrink-center.fixed.x", 0.0);
            double z = config.getDouble("shrink-center.fixed.z", 0.0);
            double y = world.getHighestBlockYAt((int) x, (int) z);

            return new Location(world, x, y, z);
        }
    }

    /**
     * 获取随机出生点
     * @param world 世界
     * @return 出生点坐标
     */
    public Location getRandomSpawn(World world) {
        FileConfiguration config = getWorldConfig(world.getName());
        Random random = new Random();

        if (config == null) {
            // 使用全局配置
            int minX = plugin.getConfig().getInt("spawn.random-area.min-x", -200);
            int maxX = plugin.getConfig().getInt("spawn.random-area.max-x", 200);
            int minZ = plugin.getConfig().getInt("spawn.random-area.min-z", -200);
            int maxZ = plugin.getConfig().getInt("spawn.random-area.max-z", 200);
            int y = plugin.getConfig().getInt("spawn.random-area.y", 100);

            double x = minX + random.nextInt(maxX - minX + 1) + 0.5;
            double z = minZ + random.nextInt(maxZ - minZ + 1) + 0.5;

            return new Location(world, x, y, z);
        }

        String mode = config.getString("spawn.mode", "random");

        if ("fixed".equals(mode)) {
            // 固定对角模式
            double x1 = config.getDouble("spawn.fixed-diagonal.corner1.x", 200.0);
            double y1 = config.getDouble("spawn.fixed-diagonal.corner1.y", 100.0);
            double z1 = config.getDouble("spawn.fixed-diagonal.corner1.z", 200.0);

            double x2 = config.getDouble("spawn.fixed-diagonal.corner2.x", -200.0);
            double y2 = config.getDouble("spawn.fixed-diagonal.corner2.y", 100.0);
            double z2 = config.getDouble("spawn.fixed-diagonal.corner2.z", -200.0);

            // 在两个对角之间随机
            double x = Math.min(x1, x2) + random.nextDouble() * Math.abs(x1 - x2);
            double y = Math.min(y1, y2) + random.nextDouble() * Math.abs(y1 - y2);
            double z = Math.min(z1, z2) + random.nextDouble() * Math.abs(z1 - z2);

            return new Location(world, x, y, z);
        } else {
            // 随机模式
            int minX = config.getInt("spawn.random-area.min-x", -200);
            int maxX = config.getInt("spawn.random-area.max-x", 200);
            int minZ = config.getInt("spawn.random-area.min-z", -200);
            int maxZ = config.getInt("spawn.random-area.max-z", 200);
            int y = config.getInt("spawn.random-area.y", 100);

            double x = minX + random.nextInt(maxX - minX + 1) + 0.5;
            double z = minZ + random.nextInt(maxZ - minZ + 1) + 0.5;

            return new Location(world, x, y, z);
        }
    }

    /**
     * 设置缩圈中心坐标（固定模式）
     * @param worldName 世界名称
     * @param x X坐标
     * @param z Z坐标
     */
    public void setShrinkCenter(String worldName, double x, double z) {
        FileConfiguration config = worldConfigs.get(worldName);

        if (config == null) {
            config = createWorldConfig(worldName);
        }

        config.set("shrink-center.mode", "fixed");
        config.set("shrink-center.fixed.x", x);
        config.set("shrink-center.fixed.z", z);

        saveWorldConfig(worldName);
    }

    /**
     * 设置固定出生点对角坐标
     * @param worldName 世界名称
     * @param corner1 第一个角
     * @param corner2 第二个角
     */
    public void setSpawnDiagonal(String worldName, Location corner1, Location corner2) {
        FileConfiguration config = worldConfigs.get(worldName);

        if (config == null) {
            config = createWorldConfig(worldName);
        }

        config.set("spawn.mode", "fixed");
        config.set("spawn.fixed-diagonal.corner1.x", corner1.getX());
        config.set("spawn.fixed-diagonal.corner1.y", corner1.getY());
        config.set("spawn.fixed-diagonal.corner1.z", corner1.getZ());

        config.set("spawn.fixed-diagonal.corner2.x", corner2.getX());
        config.set("spawn.fixed-diagonal.corner2.y", corner2.getY());
        config.set("spawn.fixed-diagonal.corner2.z", corner2.getZ());

        saveWorldConfig(worldName);
    }

    /**
     * 删除世界配置
     * @param worldName 世界名称
     */
    public void deleteWorldConfig(String worldName) {
        File file = worldConfigFiles.remove(worldName);
        worldConfigs.remove(worldName);

        if (file != null && file.exists()) {
            file.delete();
            plugin.getLogger().info("§a已删除世界配置: " + worldName);
        }
    }

    /**
     * 重载所有世界配置
     */
    public void reloadAll() {
        worldConfigs.clear();
        worldConfigFiles.clear();
        loadAllWorldConfigs();
        plugin.getLogger().info("§a已重载所有世界配置");
    }

    /**
     * 重载所有世界配置（别名方法）
     */
    public void reloadAllWorldConfigs() {
        reloadAll();
    }

    /**
     * 获取所有已启用的世界配置名称
     * @return 已启用的世界配置名称列表
     */
    public List<String> getEnabledWorldNames() {
        List<String> enabledWorlds = new ArrayList<>();

        for (Map.Entry<String, FileConfiguration> entry : worldConfigs.entrySet()) {
            if (entry.getValue().getBoolean("world.enabled", false)) {
                enabledWorlds.add(entry.getKey());
            }
        }

        return enabledWorlds;
    }

    /**
     * 获取所有已启用的世界配置（带权重）
     * @return Map<世界名称, 权重>
     */
    public Map<String, Integer> getEnabledWorldsWithWeight() {
        Map<String, Integer> worldWeights = new HashMap<>();

        for (Map.Entry<String, FileConfiguration> entry : worldConfigs.entrySet()) {
            FileConfiguration config = entry.getValue();
            if (config.getBoolean("world.enabled", false)) {
                int weight = config.getInt("world.weight", 100);
                worldWeights.put(entry.getKey(), weight);
            }
        }

        return worldWeights;
    }

    /**
     * 检查世界配置是否启用
     * @param worldConfigName 世界配置名称
     * @return 是否启用
     */
    public boolean isWorldEnabled(String worldConfigName) {
        FileConfiguration config = worldConfigs.get(worldConfigName);
        return config != null && config.getBoolean("world.enabled", false);
    }

    /**
     * 获取世界的模板世界名称
     * @param worldConfigName 世界配置名称
     * @return 模板世界名称
     */
    public String getTemplateWorldName(String worldConfigName) {
        FileConfiguration config = worldConfigs.get(worldConfigName);
        if (config == null) {
            return null;
        }
        return config.getString("world.template-world");
    }
}
