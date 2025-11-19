package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 多世界配置管理器
 * 支持每个世界独立的yml配置
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class MultiWorldConfigManager {

    private final Main plugin;
    private final Map<String, FileConfiguration> worldConfigs; // 世界名 -> 配置文件
    private final File worldsDir; // worlds目录

    public MultiWorldConfigManager(Main plugin) {
        this.plugin = plugin;
        this.worldConfigs = new HashMap<>();
        this.worldsDir = new File(plugin.getDataFolder(), "worlds");

        // 创建worlds目录
        if (!worldsDir.exists()) {
            try {
                worldsDir.mkdirs();
                plugin.getLogger().info("§a已创建worlds目录: " + worldsDir.getAbsolutePath());
            } catch (Exception e) {
                plugin.getLogger().severe("§c创建worlds目录失败: " + e.getMessage());
            }
        }

        // 首次运行：从JAR包中释放默认世界配置文件
        extractDefaultWorldConfigs();

        // 加载所有世界配置
        loadWorldConfigs();
    }

    /**
     * 从JAR包中释放默认世界配置文件
     */
    private void extractDefaultWorldConfigs() {
        // 需要释放的世界配置文件列表
        String[] defaultWorldConfigs = {
            "flyfortress.yml",
            "flyfortress_clean.yml",
            "standard_arena.yml"
        };

        for (String configName : defaultWorldConfigs) {
            File targetFile = new File(worldsDir, configName);
            if (!targetFile.exists()) {
                try {
                    // 确保父目录存在
                    worldsDir.mkdirs();

                    // 从JAR包中读取资源（路径包含worlds/前缀）
                    // saveResource会自动保存到 plugins/PokemonBattleRoyale/worlds/ 目录
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
     * 加载所有世界的配置文件
     */
    private void loadWorldConfigs() {
        worldConfigs.clear();

        // 遍历worlds目录下的所有yml文件
        if (!worldsDir.exists()) {
            return;
        }

        try {
            File[] files = worldsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String worldName = file.getName().replace(".yml", "");
                    if (!worldName.isEmpty()) {
                        FileConfiguration config = loadWorldConfig(file);
                        if (config != null) {
                            worldConfigs.put(worldName, config);
                            plugin.getLogger().info("§a已加载世界配置: " + worldName + ".yml");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("§c加载世界配置文件失败: " + e.getMessage());
        }

        plugin.getLogger().info("§a已加载 " + worldConfigs.size() + " 个世界配置");
    }

    /**
     * 加载单个世界配置文件
     */
    private FileConfiguration loadWorldConfig(File yamlFile) {
        if (yamlFile == null || !yamlFile.exists()) {
            return null;
        }

        try {
            return YamlConfiguration.loadConfiguration(yamlFile);
        } catch (Exception e) {
            plugin.getLogger().severe("§c加载配置文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存所有世界配置文件
     */
    public void saveWorldConfigs() {
        for (Map.Entry<String, FileConfiguration> entry : worldConfigs.entrySet()) {
            saveWorldConfig(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 保存单个世界配置文件
     */
    public void saveWorldConfig(String worldName, FileConfiguration config) {
        File configFile = new File(worldsDir, worldName + ".yml");
        try {
            config.save(configFile);
            plugin.getLogger().info("§a已保存世界配置: " + worldName + ".yml");
        } catch (IOException e) {
            plugin.getLogger().severe("§c保存世界配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取世界配置
     */
    public FileConfiguration getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    /**
     * 创建示例世界配置文件
     */
    public void createExampleWorldConfig(String worldName) {
        File configFile = new File(worldsDir, worldName + ".yml");
        if (configFile.exists()) {
            return; // 文件已存在，不创建
        }

        try {
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            FileConfiguration config = new YamlConfiguration();
            config.set("description", "世界 " + worldName + " 的配置文件");
            config.set("config.spawnpoint.x", 0.5);
            config.set("config.spawnpoint.y", 100);
            config.set("config.spawnpoint.z", 0.5);
            config.set("config.worldborder.x", 200);
            config.set("config.worldborder.z", 200);
            config.set("config.pvp-enabled", true);
            config.set("keepInventoryOnDeath", false);
            config.set("dropExp", false);

            config.set("configs.enchantments.enabled", true);
            config.set("configs.pvp-access.enabled", true);
            config.set("configs.pvp-in-gold-name-restrict", false);
            config.set("configs.pvp-in-hold-name-restrict", false);

            config.save(configFile);
            plugin.getLogger().info("§a已创建示例配置: " + configFile.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("§c创建示例配置失败: " + e.getMessage());
        }
    }

    /**
     * 检查世界配置文件中的配置项
     */
    public boolean hasConfigValue(String worldName, String path, String defaultValue) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null && config.contains(path) && config.getString(path, defaultValue) != null;
    }

    /**
     * 获取配置值
     */
    public String getConfigValue(String worldName, String path) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null ? config.getString(path) : null;
    }

    /**
     * 设置配置值
     */
    public void setConfigValue(String worldName, String path, String value) {
        FileConfiguration config = getWorldConfig(worldName);
        if (config != null) {
            config.set(path, value);
            saveWorldConfig(worldName, config);
        }
    }

    /**
     * 获取数值配置项
     */
    public int getConfigInt(String worldName, String path, int defaultValue) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }

    /**
     * 获取布尔配置项
     */
    public boolean getConfigBoolean(String worldName, String path, boolean defaultValue) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    /**
     * 获取字符串列表配置项
     */
    public List<String> getConfigStringList(String worldName, String path, List<String> defaultValue) {
        List<String> defaultValueList = new ArrayList<>();
        FileConfiguration config = getWorldConfig(worldName);
        if (config != null && config.contains(path) && config.isList(path)) {
            defaultValueList = new ArrayList<>(config.getStringList(path));
            return defaultValueList;
        }
        return defaultValueList;
    }

    /**
     * 获取浮点数配置项
     */
    public double getConfigDouble(String worldName, String path, double defaultValue) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null ? config.getDouble(path, defaultValue) : defaultValue;
    }

    /**
     * 获取配置部分
     */
    public ConfigurationSection getConfigSection(String worldName, String section) {
        FileConfiguration config = getWorldConfig(worldName);
        return config != null ? config.getConfigurationSection(section) : null;
    }

    /**
     * 获取所有已加载的世界名称
     */
    public Set<String> getLoadedWorlds() {
        return new HashSet<>(worldConfigs.keySet());
    }

    /**
     * 重载所有世界配置
     */
    public void reloadConfigs() {
        loadWorldConfigs();
        plugin.getLogger().info("§a已重载所有世界配置");
    }

    /**
     * 检查世界配置是否存在
     */
    public boolean worldConfigExists(String worldName) {
        return worldConfigs.containsKey(worldName);
    }
}