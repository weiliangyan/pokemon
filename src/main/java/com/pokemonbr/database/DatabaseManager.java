package com.pokemonbr.database;

import com.pokemonbr.Main;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库管理器
 * 支持 YAML 本地存储和 MySQL 数据库存储
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;
    private YamlStorage yamlStorage;
    private String storageType;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据存储
     * @return 是否成功
     */
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        storageType = config.getString("storage.type", "YAML").toUpperCase();

        plugin.getLogger().info("§e正在初始化数据存储模式: §b" + storageType);

        // 根据配置选择存储模式
        if (storageType.equals("YAML")) {
            return initializeYaml();
        } else if (storageType.equals("MYSQL")) {
            return initializeMySQL();
        } else {
            plugin.getLogger().warning("§c未知的存储类型: " + storageType + ", 使用默认的 YAML 模式");
            storageType = "YAML";
            return initializeYaml();
        }
    }

    /**
     * 初始化 YAML 存储
     * @return 是否成功
     */
    private boolean initializeYaml() {
        yamlStorage = new YamlStorage(plugin);
        boolean success = yamlStorage.initialize();

        if (success) {
            plugin.getLogger().info("§a[存储模式] YAML 本地文件存储已启用");
        }

        return success;
    }

    /**
     * 初始化 MySQL 数据库
     * @return 是否成功
     */
    private boolean initializeMySQL() {
        try {
            FileConfiguration config = plugin.getConfig();

            // 读取数据库配置
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String database = config.getString("database.database", "minecraft");
            String username = config.getString("database.username", "root");
            String password = config.getString("database.password", "");
            boolean useSSL = config.getBoolean("database.useSSL", false);

            // 配置 HikariCP 连接池
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&autoReconnect=true&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);

            // 连接池配置 - 优化为更适合Minecraft服务器的设置
            hikariConfig.setMaximumPoolSize(15); // 增加最大连接数以应对高并发
            hikariConfig.setMinimumIdle(3); // 增加最小空闲连接数
            hikariConfig.setConnectionTimeout(20000); // 减少连接超时时间
            hikariConfig.setIdleTimeout(300000); // 减少空闲超时时间
            hikariConfig.setMaxLifetime(1200000); // 减少最大生命周期
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.setLeakDetectionThreshold(5000); // 添加连接泄漏检测

            // 性能优化配置
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

            // 创建数据源
            dataSource = new HikariDataSource(hikariConfig);

            // 测试连接
            try (Connection connection = dataSource.getConnection()) {
                plugin.getLogger().info("§a[存储模式] MySQL 数据库连接成功！");
            }

            // 创建表
            createTables();

            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("§c[存储模式] MySQL 数据库连接失败：" + e.getMessage());
            plugin.getLogger().warning("§e自动切换到 YAML 本地存储模式...");
            storageType = "YAML";
            return initializeYaml();
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() {
        // 创建玩家数据表
        createPlayerTable();

        // 创建游戏记录表
        createGameRecordTable();

        // 创建玩家游戏详情表
        createPlayerGameDetailsTable();

        plugin.getLogger().info("§a数据库表创建完成");
    }

    /**
     * 创建玩家数据表
     */
    private void createPlayerTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `pbr_players` (" +
                "`uuid` VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '玩家UUID'," +
                "`name` VARCHAR(16) NOT NULL COMMENT '玩家名称'," +
                "`total_games` INT NOT NULL DEFAULT 0 COMMENT '累计局数'," +
                "`total_points` INT NOT NULL DEFAULT 0 COMMENT '累计积分'," +
                "`total_kills` INT NOT NULL DEFAULT 0 COMMENT '累计击败数'," +
                "`total_wins` INT NOT NULL DEFAULT 0 COMMENT '累计第一名次数'," +
                "`plays_today` INT NOT NULL DEFAULT 0 COMMENT '今日游玩次数'," +
                "`last_reset` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次重置时间'," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "INDEX `idx_name` (`name`)," +
                "INDEX `idx_total_points` (`total_points`)," +
                "INDEX `idx_total_wins` (`total_wins`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家数据表';";

        executeUpdate(sql);
    }

    /**
     * 创建游戏记录表
     */
    private void createGameRecordTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `pbr_game_records` (" +
                "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '游戏ID'," +
                "`game_uuid` VARCHAR(36) NOT NULL UNIQUE COMMENT '游戏唯一标识'," +
                "`queue_id` INT NOT NULL COMMENT '队列ID'," +
                "`start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间'," +
                "`end_time` TIMESTAMP NULL COMMENT '结束时间'," +
                "`winner_uuid` VARCHAR(36) NULL COMMENT '胜利者UUID'," +
                "`total_players` INT NOT NULL COMMENT '参与人数'," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "INDEX `idx_game_uuid` (`game_uuid`)," +
                "INDEX `idx_queue_id` (`queue_id`)," +
                "INDEX `idx_start_time` (`start_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏记录表';";

        executeUpdate(sql);
    }

    /**
     * 创建玩家游戏详情表
     */
    private void createPlayerGameDetailsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `pbr_player_game_details` (" +
                "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID'," +
                "`game_id` INT NOT NULL COMMENT '游戏ID'," +
                "`player_uuid` VARCHAR(36) NOT NULL COMMENT '玩家UUID'," +
                "`rank` INT NOT NULL COMMENT '名次'," +
                "`kills` INT NOT NULL DEFAULT 0 COMMENT '击败数'," +
                "`points_earned` INT NOT NULL DEFAULT 0 COMMENT '获得积分'," +
                "`eliminated_by` VARCHAR(36) NULL COMMENT '淘汰者UUID'," +
                "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "INDEX `idx_game_id` (`game_id`)," +
                "INDEX `idx_player_uuid` (`player_uuid`)," +
                "INDEX `idx_rank` (`rank`)," +
                "FOREIGN KEY (`game_id`) REFERENCES `pbr_game_records`(`id`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家游戏详情表';";

        executeUpdate(sql);
    }

    /**
     * 执行SQL更新语句（CREATE/INSERT/UPDATE/DELETE）
     * @param sql SQL语句
     */
    private void executeUpdate(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("§c执行SQL失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     * @return Connection
     * @throws SQLException SQL异常
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("数据源未初始化");
        }
        return dataSource.getConnection();
    }

    /**
     * 关闭数据存储
     */
    public void close() {
        if (storageType.equals("YAML")) {
            if (yamlStorage != null) {
                yamlStorage.close();
            }
        } else if (storageType.equals("MYSQL")) {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                plugin.getLogger().info("§a数据库连接池已关闭");
            }
        }
    }

    /**
     * 获取当前存储类型
     * @return 存储类型 (YAML 或 MYSQL)
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * 是否使用 YAML 存储
     * @return 是否使用 YAML
     */
    public boolean isYamlStorage() {
        return "YAML".equals(storageType);
    }

    /**
     * 是否使用 MySQL 存储
     * @return 是否使用 MySQL
     */
    public boolean isMySQLStorage() {
        return "MYSQL".equals(storageType);
    }

    /**
     * 获取 YamlStorage 实例（仅 YAML 模式可用）
     * @return YamlStorage
     */
    public YamlStorage getYamlStorage() {
        return yamlStorage;
    }

    /**
     * 检查数据库连接是否有效
     * @return 是否有效
     */
    public boolean isConnected() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
