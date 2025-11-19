package com.pokemonbr.managers;

import com.pokemonbr.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 * 管理所有配置文件的加载和保存
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class ConfigManager {

    private final Main plugin;
    private final Map<String, FileConfiguration> configs;

    // 配置文件名称
    private static final String[] CONFIG_FILES = {
            "config.yml",
            "messages.yml",
            "rewards.yml"
    };

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
    }

    /**
     * 加载所有配置文件
     */
    public void loadConfigs() {
        // 保存默认配置文件（仅在文件不存在时）
        for (String fileName : CONFIG_FILES) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                try {
                    // 尝试从JAR包保存默认文件
                    plugin.saveResource(fileName, false);
                } catch (IllegalArgumentException e) {
                    // 如果JAR包中没有该文件，创建空文件
                    plugin.getLogger().info("§eJAR包中未找到 " + fileName + "，创建空配置文件");
                    createEmptyConfigFile(file, fileName);
                }
            }
        }

        // 加载配置文件到内存
        for (String fileName : CONFIG_FILES) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (file.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                configs.put(fileName, config);
                plugin.getLogger().info("§a已加载配置文件: " + fileName);
            } else {
                plugin.getLogger().warning("§c配置文件不存在: " + fileName);
            }
        }
    }

    /**
     * 创建空的配置文件
     */
    private void createEmptyConfigFile(File file, String fileName) {
        try {
            // 确保父目录存在
            file.getParentFile().mkdirs();

            // 创建新文件
            if (file.createNewFile()) {
                plugin.getLogger().info("§a已创建空配置文件: " + fileName);

                // 为特定文件写入默认内容
                FileConfiguration config = new YamlConfiguration();
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("§c创建配置文件失败: " + fileName);
            e.printStackTrace();
        }
    }

    /**
     * 创建默认的世界设置配置
     */
    private void createDefaultWorldSettings(FileConfiguration config) {
        config.set("template.world-name", "world_pbr_template");
        config.set("template.world-prefix", "world_pbr_");
        config.set("template.start-index", 1);

        config.set("management.auto-delete-after-game", true);
        config.set("management.delete-delay", 10);
        config.set("management.auto-create-new-world", true);

        config.set("copy.mode", "async");
        config.set("copy.folders", new String[]{"region", "data", "datapacks"});
        config.set("copy.files", new String[]{"level.dat", "uid.dat"});

        config.set("debug.enabled", false);

        plugin.getLogger().info("§a已创建默认世界设置配置");
    }

    /**
     * 保存所有配置文件
     */
    public void saveConfigs() {
        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            try {
                File file = new File(plugin.getDataFolder(), entry.getKey());
                entry.getValue().save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("§c保存配置文件失败: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    /**
     * 重载所有配置文件
     */
    public void reloadConfigs() {
        configs.clear();
        loadConfigs();
        plugin.getLogger().info("§a配置文件已重载");
    }

    /**
     * 获取指定配置文件
     * @param fileName 文件名
     * @return FileConfiguration
     */
    public FileConfiguration getConfig(String fileName) {
        return configs.getOrDefault(fileName, plugin.getConfig());
    }

    /**
     * 获取主配置文件
     * @return FileConfiguration
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }

    /**
     * 获取消息配置文件
     * @return FileConfiguration
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }

    /**
     * 获取奖励配置文件
     * @return FileConfiguration
     */
    public FileConfiguration getRewardsConfig() {
        return getConfig("rewards.yml");
    }

    // 注意：出生点配置已移动到各个世界独立配置文件中
// 通过 WorldConfigManager.getSpawnConfig(worldName) 获取
}
