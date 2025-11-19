package com.pokemonbr.database;

import com.pokemonbr.Main;
import com.pokemonbr.models.PlayerData;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 玩家数据访问对象
 * 支持 YAML 和 MySQL 两种存储模式
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PlayerDAO {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    public PlayerDAO(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 加载玩家数据
     * @param uuid 玩家UUID
     * @return PlayerData 或 null
     */
    public PlayerData loadPlayerData(UUID uuid) {
        // 根据存储类型选择加载方式
        if (databaseManager.isYamlStorage()) {
            return databaseManager.getYamlStorage().loadPlayerData(uuid);
        } else {
            return loadPlayerDataFromMySQL(uuid);
        }
    }

    /**
     * 从 MySQL 加载玩家数据
     * @param uuid 玩家UUID
     * @return PlayerData 或 null
     */
    private PlayerData loadPlayerDataFromMySQL(UUID uuid) {
        String sql = "SELECT * FROM pbr_players WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlayerData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getInt("total_games"),
                        rs.getInt("total_points"),
                        rs.getInt("total_kills"),
                        rs.getInt("total_wins"),
                        rs.getInt("plays_today"),
                        rs.getTimestamp("last_reset"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                );
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("§c加载玩家数据失败: " + uuid);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 保存玩家数据
     * @param data 玩家数据
     * @return 是否成功
     */
    public boolean savePlayerData(PlayerData data) {
        // 根据存储类型选择保存方式
        if (databaseManager.isYamlStorage()) {
            databaseManager.getYamlStorage().savePlayerData(data);
            return true;
        } else {
            return savePlayerDataToMySQL(data);
        }
    }

    /**
     * 保存玩家数据到 MySQL
     * @param data 玩家数据
     * @return 是否成功
     */
    private boolean savePlayerDataToMySQL(PlayerData data) {
        String sql = "INSERT INTO pbr_players " +
                "(uuid, name, total_games, total_points, total_kills, total_wins, " +
                "plays_today, last_reset, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), " +
                "total_games = VALUES(total_games), " +
                "total_points = VALUES(total_points), " +
                "total_kills = VALUES(total_kills), " +
                "total_wins = VALUES(total_wins), " +
                "plays_today = VALUES(plays_today), " +
                "last_reset = VALUES(last_reset), " +
                "updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getName());
            stmt.setInt(3, data.getTotalGames());
            stmt.setInt(4, data.getTotalPoints());
            stmt.setInt(5, data.getTotalKills());
            stmt.setInt(6, data.getTotalWins());
            stmt.setInt(7, data.getPlaysToday());
            stmt.setTimestamp(8, data.getLastReset());
            stmt.setTimestamp(9, data.getCreatedAt());
            stmt.setTimestamp(10, data.getUpdatedAt());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("§c保存玩家数据失败: " + data.getUuid());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 异步保存玩家数据
     * @param data 玩家数据
     */
    public void savePlayerDataAsync(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerData(data));
    }

    /**
     * 获取积分排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    public List<PlayerData> getTopPoints(int limit) {
        // YAML 模式下暂不支持排行榜
        if (databaseManager.isYamlStorage()) {
            plugin.getLogger().warning("§eYAML 存储模式暂不支持排行榜功能，请使用 MySQL 模式");
            return new ArrayList<>();
        }

        return getTopPointsFromMySQL(limit);
    }

    /**
     * 从 MySQL 获取积分排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    private List<PlayerData> getTopPointsFromMySQL(int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM pbr_players ORDER BY total_points DESC LIMIT ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new PlayerData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getInt("total_games"),
                        rs.getInt("total_points"),
                        rs.getInt("total_kills"),
                        rs.getInt("total_wins"),
                        rs.getInt("plays_today"),
                        rs.getTimestamp("last_reset"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("§c获取积分排行榜失败");
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 获取胜场排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    public List<PlayerData> getTopWins(int limit) {
        // YAML 模式下暂不支持排行榜
        if (databaseManager.isYamlStorage()) {
            plugin.getLogger().warning("§eYAML 存储模式暂不支持排行榜功能，请使用 MySQL 模式");
            return new ArrayList<>();
        }

        return getTopWinsFromMySQL(limit);
    }

    /**
     * 从 MySQL 获取胜场排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    private List<PlayerData> getTopWinsFromMySQL(int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM pbr_players ORDER BY total_wins DESC LIMIT ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new PlayerData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getInt("total_games"),
                        rs.getInt("total_points"),
                        rs.getInt("total_kills"),
                        rs.getInt("total_wins"),
                        rs.getInt("plays_today"),
                        rs.getTimestamp("last_reset"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("§c获取胜场排行榜失败");
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 获取击败数排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    public List<PlayerData> getTopKills(int limit) {
        // YAML 模式下暂不支持排行榜
        if (databaseManager.isYamlStorage()) {
            plugin.getLogger().warning("§eYAML 存储模式暂不支持排行榜功能，请使用 MySQL 模式");
            return new ArrayList<>();
        }

        return getTopKillsFromMySQL(limit);
    }

    /**
     * 从 MySQL 获取击败数排行榜
     * @param limit 数量限制
     * @return 玩家数据列表
     */
    private List<PlayerData> getTopKillsFromMySQL(int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM pbr_players ORDER BY total_kills DESC LIMIT ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new PlayerData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getInt("total_games"),
                        rs.getInt("total_points"),
                        rs.getInt("total_kills"),
                        rs.getInt("total_wins"),
                        rs.getInt("plays_today"),
                        rs.getTimestamp("last_reset"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("§c获取击败数排行榜失败");
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 重置所有玩家的每日游玩次数
     */
    public void resetAllPlaysToday() {
        // YAML 模式下不支持批量重置
        if (databaseManager.isYamlStorage()) {
            plugin.getLogger().warning("§eYAML 存储模式暂不支持批量重置功能");
            return;
        }

        resetAllPlaysTodayInMySQL();
    }

    /**
     * 在 MySQL 中重置所有玩家的每日游玩次数
     */
    private void resetAllPlaysTodayInMySQL() {
        String sql = "UPDATE pbr_players SET plays_today = 0, last_reset = CURRENT_TIMESTAMP";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int affected = stmt.executeUpdate();
            plugin.getLogger().info("§a已重置 " + affected + " 个玩家的每日游玩次数");

        } catch (SQLException e) {
            plugin.getLogger().severe("§c重置每日游玩次数失败");
            e.printStackTrace();
        }
    }

    /**
     * 检查是否需要重置玩家今日游玩次数
     * @param data 玩家数据
     * @param resetHours 重置间隔小时数
     * @return 是否已重置
     */
    public boolean checkAndResetPlaysToday(PlayerData data, int resetHours) {
        long now = System.currentTimeMillis();
        long lastReset = data.getLastReset().getTime();
        long hoursPassed = (now - lastReset) / (1000 * 60 * 60);

        if (hoursPassed >= resetHours) {
            data.resetPlaysToday();
            savePlayerDataAsync(data);
            return true;
        }

        return false;
    }

    /**
     * 删除玩家数据
     * @param uuid 玩家UUID
     * @return 是否成功
     */
    public boolean deletePlayerData(UUID uuid) {
        // YAML 模式下暂不支持删除（需要手动删除文件中的条目）
        if (databaseManager.isYamlStorage()) {
            plugin.getLogger().warning("§eYAML 存储模式暂不支持删除功能，请手动编辑 players.yml 文件");
            return false;
        }

        return deletePlayerDataFromMySQL(uuid);
    }

    /**
     * 从 MySQL 删除玩家数据
     * @param uuid 玩家UUID
     * @return 是否成功
     */
    private boolean deletePlayerDataFromMySQL(UUID uuid) {
        String sql = "DELETE FROM pbr_players WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("§c删除玩家数据失败: " + uuid);
            e.printStackTrace();
            return false;
        }
    }
}
