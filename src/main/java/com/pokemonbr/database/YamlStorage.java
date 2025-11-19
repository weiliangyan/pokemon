package com.pokemonbr.database;

import com.pokemonbr.Main;
import com.pokemonbr.models.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * YAML 本地文件存储实现
 * 使用 YAML 文件存储玩家数据
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class YamlStorage {

    private final Main plugin;
    private final File dataFolder;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    public YamlStorage(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    /**
     * 初始化 YAML 存储
     * @return 是否成功
     */
    public boolean initialize() {
        try {
            // 创建数据文件夹
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // 创建玩家数据文件
            playerDataFile = new File(dataFolder, "players.yml");
            if (!playerDataFile.exists()) {
                playerDataFile.createNewFile();
            }

            // 加载配置
            playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);

            plugin.getLogger().info("§a[YAML存储] 数据文件加载成功！");
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("§c[YAML存储] 初始化失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存玩家数据
     * @param playerData 玩家数据
     */
    public void savePlayerData(PlayerData playerData) {
        String uuid = playerData.getUuid().toString();
        String path = "players." + uuid + ".";

        // 检查是否是新数据（用于设置created_at）
        boolean isNew = !playerDataConfig.contains("players." + uuid);

        playerDataConfig.set(path + "name", playerData.getName());
        playerDataConfig.set(path + "total_games", playerData.getTotalGames());
        playerDataConfig.set(path + "total_points", playerData.getTotalPoints());
        playerDataConfig.set(path + "total_kills", playerData.getTotalKills());
        playerDataConfig.set(path + "total_wins", playerData.getTotalWins());
        playerDataConfig.set(path + "plays_today", playerData.getPlaysToday());
        playerDataConfig.set(path + "last_reset", playerData.getLastReset().getTime());

        // 首次创建时设置 created_at
        if (isNew) {
            playerDataConfig.set(path + "created_at", playerData.getCreatedAt().getTime());
        }

        // 每次保存更新 updated_at
        playerDataConfig.set(path + "updated_at", System.currentTimeMillis());

        saveConfig();
    }

    /**
     * 加载玩家数据
     * @param uuid 玩家UUID
     * @return 玩家数据
     */
    public PlayerData loadPlayerData(UUID uuid) {
        String path = "players." + uuid.toString() + ".";

        if (!playerDataConfig.contains("players." + uuid.toString())) {
            // 返回null,让PlayerDataManager创建新玩家数据
            return null;
        }

        // 从YAML加载数据
        String name = playerDataConfig.getString(path + "name", "Unknown");
        int totalGames = playerDataConfig.getInt(path + "total_games", 0);
        int totalPoints = playerDataConfig.getInt(path + "total_points", 0);
        int totalKills = playerDataConfig.getInt(path + "total_kills", 0);
        int totalWins = playerDataConfig.getInt(path + "total_wins", 0);
        int playsToday = playerDataConfig.getInt(path + "plays_today", 0);
        long lastResetTime = playerDataConfig.getLong(path + "last_reset", System.currentTimeMillis());
        long createdTime = playerDataConfig.getLong(path + "created_at", System.currentTimeMillis());
        long updatedTime = playerDataConfig.getLong(path + "updated_at", System.currentTimeMillis());

        // 创建 Timestamp 对象
        Timestamp lastReset = new Timestamp(lastResetTime);
        Timestamp createdAt = new Timestamp(createdTime);
        Timestamp updatedAt = new Timestamp(updatedTime);

        // 使用完整构造函数
        return new PlayerData(uuid, name, totalGames, totalPoints, totalKills,
                            totalWins, playsToday, lastReset, createdAt, updatedAt);
    }

    /**
     * 保存配置到文件
     */
    private void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("§c[YAML存储] 保存数据失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 关闭存储
     */
    public void close() {
        saveConfig();
        plugin.getLogger().info("§a[YAML存储] 数据已保存");
    }
}
