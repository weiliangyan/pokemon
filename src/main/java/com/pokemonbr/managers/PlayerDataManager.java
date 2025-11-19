package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.database.PlayerDAO;
import com.pokemonbr.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据管理器
 * 管理玩家数据的CRUD操作，包含缓存机制
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PlayerDataManager {

    private final Main plugin;
    private final PlayerDAO playerDAO;

    // 玩家数据缓存（UUID -> PlayerData）
    private Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.playerDAO = new PlayerDAO(plugin);
        this.playerCache = new HashMap<>();
    }

    /**
     * 加载玩家数据（带缓存）
     * @param uuid 玩家UUID
     * @return PlayerData
     */
    public PlayerData getPlayerData(UUID uuid) {
        // 先从缓存获取
        if (playerCache.containsKey(uuid)) {
            return playerCache.get(uuid);
        }

        // 从数据库加载
        PlayerData data = playerDAO.loadPlayerData(uuid);

        // 如果数据库中没有，创建新数据
        if (data == null) {
            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : "Unknown";
            data = new PlayerData(uuid, name);
            playerDAO.savePlayerDataAsync(data);
        }

        // 放入缓存
        playerCache.put(uuid, data);

        return data;
    }

    /**
     * 获取在线玩家数据
     * @param player 玩家
     * @return PlayerData
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    /**
     * 保存玩家数据
     * @param data 玩家数据
     */
    public void savePlayerData(PlayerData data) {
        // 更新缓存
        playerCache.put(data.getUuid(), data);

        // 异步保存到数据库
        playerDAO.savePlayerDataAsync(data);
    }

    /**
     * 移除玩家数据缓存
     * @param uuid 玩家UUID
     */
    public void removePlayerCache(UUID uuid) {
        // 保存后移除缓存
        PlayerData data = playerCache.remove(uuid);
        if (data != null) {
            playerDAO.savePlayerData(data);
        }
    }

    /**
     * 保存所有缓存的玩家数据
     */
    public void saveAllCachedData() {
        plugin.getLogger().info("§e正在保存所有玩家数据...");
        int count = 0;

        for (PlayerData data : playerCache.values()) {
            playerDAO.savePlayerData(data);
            count++;
        }

        plugin.getLogger().info("§a已保存 " + count + " 个玩家数据");
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        saveAllCachedData();
        playerCache.clear();
        plugin.getLogger().info("§a玩家数据缓存已清空");
    }

    /**
     * 检查并重置玩家每日游玩次数
     * @param player 玩家
     * @return 是否已重置
     */
    public boolean checkAndResetPlaysToday(Player player) {
        PlayerData data = getPlayerData(player);
        int resetHours = plugin.getConfig().getInt("play-limit.reset-hours", 24);

        return playerDAO.checkAndResetPlaysToday(data, resetHours);
    }

    /**
     * 获取玩家剩余游玩次数
     * @param player 玩家
     * @return 剩余次数
     */
    public int getRemainingPlays(Player player) {
        // 检查是否启用次数限制
        if (!plugin.getConfig().getBoolean("play-limit.enabled", true)) {
            return 999; // 无限制
        }

        PlayerData data = getPlayerData(player);

        // 先检查是否需要重置
        checkAndResetPlaysToday(player);

        // 获取玩家的最大次数
        int maxPlays = getMaxPlays(player);

        // 计算剩余次数
        return Math.max(0, maxPlays - data.getPlaysToday());
    }

    /**
     * 获取玩家的最大游玩次数
     * @param player 玩家
     * @return 最大次数
     */
    public int getMaxPlays(Player player) {
        // 检查VIP权限（从高到低）
        if (player.hasPermission("pbr.vip.plays.30")) {
            return 30;
        }
        if (player.hasPermission("pbr.vip.plays.20")) {
            return 20;
        }
        if (player.hasPermission("pbr.vip.plays.10")) {
            return 10;
        }

        // 默认次数
        return plugin.getConfig().getInt("play-limit.default-plays", 3);
    }

    /**
     * 消耗一次游玩次数
     * @param player 玩家
     * @return 是否成功
     */
    public boolean consumePlay(Player player) {
        int remaining = getRemainingPlays(player);

        if (remaining <= 0) {
            return false;
        }

        PlayerData data = getPlayerData(player);
        data.addPlaysToday(1);
        savePlayerData(data);

        return true;
    }

    /**
     * 获取PlayerDAO实例
     * @return PlayerDAO
     */
    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }
}
