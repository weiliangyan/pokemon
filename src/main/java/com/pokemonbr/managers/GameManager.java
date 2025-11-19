package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏管理器
 * 管理所有游戏实例的生命周期
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class GameManager {

    private final Main plugin;

    // 游戏UUID -> 游戏实例
    private Map<String, Game> activeGames = new ConcurrentHashMap<>();

    // 玩家UUID -> 游戏UUID（快速查找玩家在哪个游戏中）
    private Map<UUID, String> playerGameMap = new ConcurrentHashMap<>();

    // 游戏主循环任务
    private BukkitTask gameLoopTask;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<>();
        this.playerGameMap = new HashMap<>();

        // 启动游戏主循环
        startGameLoop();
    }

    /**
     * 启动游戏主循环
     */
    private void startGameLoop() {
        gameLoopTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Game game : new ArrayList<>(activeGames.values())) {
                updateGame(game);
            }
        }, 20L, 20L); // 每秒执行一次
    }

    /**
     * 更新游戏状态
     * @param game 游戏实例
     */
    private void updateGame(Game game) {
        switch (game.getState()) {
            case PREPARING:
                // 准备阶段已在createGame中处理
                break;

            case INVINCIBILITY:
                // 处理无敌时间倒计时
                int remaining = game.decrementInvincibilityTime();
                if (remaining <= 0) {
                    endInvincibility(game);
                } else if (remaining <= 5) {
                    game.broadcastMessage(getMessage("game.invincibility-remaining")
                            .replace("{time}", String.valueOf(remaining)));
                }
                break;

            case PLAYING:
                // 检查游戏是否结束
                if (game.isGameOver()) {
                    endGame(game);
                } else {
                    // 对边界外的玩家造成伤害
                    plugin.getBorderShrinkManager().damagePlayersOutsideBorder(game);

                    // 定期检查存活玩家的宝可梦数量（每30秒检查一次）
                    if (game.getGameTime() % 600 == 0) { // 30秒 = 600 ticks
                        checkAllPlayersPokemonCount(game);
                    }
                }
                break;

            case FINAL_STAGE:
                // 最终阶段，检查游戏是否结束
                if (game.isGameOver()) {
                    endGame(game);
                } else {
                    // 对边界外的玩家造成伤害
                    plugin.getBorderShrinkManager().damagePlayersOutsideBorder(game);

                    // 最终阶段更频繁检查（每15秒检查一次）
                    if (game.getGameTime() % 300 == 0) { // 15秒 = 300 ticks
                        checkAllPlayersPokemonCount(game);
                    }
                }
                break;

            case ENDING:
                // 结束处理已在endGame中完成
                break;

            case FINISHED:
                // 清理游戏
                cleanupGame(game);
                break;
        }
    }

    /**
     * 创建游戏实例
     * @param queue 匹配队列
     * @param gameWorld 游戏世界（已由QueueManager分配）
     * @return 游戏UUID
     */
    public String createGame(MatchQueue queue, World gameWorld) {
        plugin.getLogger().info("§a正在为队列 #" + queue.getQueueId() + " 创建游戏实例...");
        plugin.getLogger().info("§7使用世界: " + gameWorld.getName());

        // 生成游戏UUID
        String gameUuid = UUID.randomUUID().toString();

        // 验证世界
        if (gameWorld == null) {
            plugin.getLogger().severe("§c游戏世界为 null！");
            // 返回玩家到大厅
            returnPlayersToLobby(queue.getPlayers());
            return null;
        }

        // 创建游戏实例
        Game game = new Game(gameUuid, queue.getQueueId(), gameWorld, queue.getWorldConfigName(), queue.getPlayers());
        activeGames.put(gameUuid, game);

        // 记录玩家-游戏映射
        for (UUID uuid : queue.getPlayers()) {
            playerGameMap.put(uuid, gameUuid);
        }

        plugin.getLogger().info("§a游戏实例已创建: " + gameUuid);
        plugin.getLogger().info("§a参与玩家数: " + game.getTotalPlayerCount());

        // 开始游戏
        startGame(game);

        return gameUuid;
    }

    /**
     * 开始游戏
     * @param game 游戏实例
     */
    private void startGame(Game game) {
        game.setState(GameState.PREPARING);

        // 填充战利品箱
        if (plugin.getConfig().getBoolean("global.fill-on-start", true)) {
            plugin.getLootChestManager().fillAllChests(game.getGameWorld());
        }

        // 传送玩家到出生点
        teleportPlayersToSpawns(game);

        // 广播游戏开始消息
        broadcastGameStart(game);

        // 发放初始物品
        giveInitialItems(game);

        // 开始无敌时间
        startInvincibility(game);
    }

    /**
     * 传送玩家到出生点
     * @param game 游戏实例
     */
    private void teleportPlayersToSpawns(Game game) {
        // 获取世界配置
        FileConfiguration worldConfig = plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        if (worldConfig == null) {
            plugin.getLogger().warning("§c未找到世界配置: " + game.getWorldConfigName() + "，使用世界出生点");
            teleportToWorldSpawn(game);
            return;
        }

        // 读取出生点模式
        String mode = worldConfig.getString("spawn.mode", "random");
        plugin.getLogger().info("§7出生点模式: " + mode + " (配置: " + game.getWorldConfigName() + ")");

        if ("random".equals(mode)) {
            // 随机出生
            teleportPlayersRandomly(game, worldConfig);
        } else if ("fixed-diagonal".equals(mode) || "fixed".equals(mode)) {
            // 固定对角出生或单点出生
            teleportPlayersFixed(game, worldConfig);
        } else {
            plugin.getLogger().warning("§c未知的出生点模式: " + mode + "，使用世界出生点");
            teleportToWorldSpawn(game);
        }
    }

    /**
     * 传送所有玩家到世界出生点
     */
    private void teleportToWorldSpawn(Game game) {
        Location defaultSpawn = game.getGameWorld().getSpawnLocation();
        for (UUID uuid : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(defaultSpawn);
            }
        }
    }

    /**
     * 随机传送玩家
     */
    private void teleportPlayersRandomly(Game game, FileConfiguration worldConfig) {
        // 从世界配置读取随机范围
        int minX = worldConfig.getInt("spawn.random-range.min-x", -200);
        int maxX = worldConfig.getInt("spawn.random-range.max-x", 200);
        int minZ = worldConfig.getInt("spawn.random-range.min-z", -200);
        int maxZ = worldConfig.getInt("spawn.random-range.max-z", 200);
        int y = worldConfig.getInt("spawn.random-range.y", 100);

        Random random = new Random();
        for (UUID uuid : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                int x = minX + random.nextInt(maxX - minX + 1);
                int z = minZ + random.nextInt(maxZ - minZ + 1);
                Location spawn = new Location(game.getGameWorld(), x + 0.5, y, z + 0.5);
                player.teleport(spawn);
            }
        }
    }

    /**
     * 固定位置传送玩家
     */
    private void teleportPlayersFixed(Game game, FileConfiguration worldConfig) {
        // 从世界配置读取固定出生点
        if (worldConfig.contains("spawn.location")) {
            double x = worldConfig.getDouble("spawn.location.x", 0.0);
            double y = worldConfig.getDouble("spawn.location.y", 100.0);
            double z = worldConfig.getDouble("spawn.location.z", 0.0);
            float yaw = (float) worldConfig.getDouble("spawn.location.yaw", 0.0);
            float pitch = (float) worldConfig.getDouble("spawn.location.pitch", 0.0);

            Location spawn = new Location(game.getGameWorld(), x, y, z, yaw, pitch);
            for (UUID uuid : game.getAlivePlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.teleport(spawn);
                }
            }
        } else {
            plugin.getLogger().warning("§c配置中未找到 spawn.location，使用世界出生点");
            teleportToWorldSpawn(game);
        }
    }

    // 注意：出生点逻辑已迁移到 WorldConfigManager 中管理
// 通过 plugin.getWorldConfigManager().getSpawnLocations(worldName) 获取

    /**
     * 获取随机出生点（在区域内）
     * @param world 世界
     * @return 随机出生点
     */
    private Location getRandomSpawnInArea(World world) {
        Random random = new Random();

        int minX = plugin.getConfig().getInt("spawn.random-area.min-x", -200);
        int maxX = plugin.getConfig().getInt("spawn.random-area.max-x", 200);
        int minZ = plugin.getConfig().getInt("spawn.random-area.min-z", -200);
        int maxZ = plugin.getConfig().getInt("spawn.random-area.max-z", 200);
        int y = plugin.getConfig().getInt("spawn.random-area.y", 100);

        int x = minX + random.nextInt(maxX - minX + 1);
        int z = minZ + random.nextInt(maxZ - minZ + 1);

        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * 广播游戏开始消息
     * @param game 游戏实例
     */
    private void broadcastGameStart(Game game) {
        game.broadcastMessage(getMessage("game.started"));

        List<String> infoLines = plugin.getConfigManager().getMessagesConfig()
                .getStringList("game.started-info");

        for (String line : infoLines) {
            String message = ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{players}", String.valueOf(game.getTotalPlayerCount()))
                    .replace("{world}", game.getGameWorld().getName())
                    .replace("{size}", String.valueOf(plugin.getConfig().getInt("shrink.initial-size")))
                    .replace("{invincibility}", String.valueOf(plugin.getConfig().getInt("spawn.invincibility-duration")));

            game.broadcastMessage(message);
        }
    }

    /**
     * 发放初始物品
     * @param game 游戏实例
     */
    private void giveInitialItems(Game game) {
        // 获取世界配置
        FileConfiguration worldConfig = plugin.getWorldConfigManager().getWorldConfig(game.getWorldConfigName());

        List<String> itemList;

        // 检查是否覆盖全局配置
        if (worldConfig != null && worldConfig.getBoolean("initial-items.override-global", false)) {
            // 使用世界专属配置
            itemList = worldConfig.getStringList("initial-items.items");
            plugin.getLogger().info("§7使用世界专属初始物品配置: " + game.getWorldConfigName());
        } else {
            // 使用全局配置
            itemList = plugin.getConfig().getStringList("spawn.initial-items");
        }

        for (UUID uuid : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            player.getInventory().clear();

            for (String itemString : itemList) {
                String[] parts = itemString.split(":");
                if (parts.length != 2) continue;

                try {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);

                    player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, amount));
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无效的初始物品配置: " + itemString);
                }
            }

            // 设置玩家速度
            if (plugin.getConfig().getBoolean("global.speed-control-enabled", false)) {
                float walkSpeed = (float) plugin.getConfig().getDouble("global.walk-speed", 0.2);
                float flySpeed = (float) plugin.getConfig().getDouble("global.fly-speed", 0.1);
                player.setWalkSpeed(walkSpeed);
                player.setFlySpeed(flySpeed);
            }
        }

        game.broadcastMessage(getMessage("game.items-given"));
    }

    /**
     * 开始无敌时间
     * @param game 游戏实例
     */
    private void startInvincibility(Game game) {
        int duration = plugin.getConfig().getInt("spawn.invincibility-duration", 30);
        game.setInvincibilityTime(duration);
        game.setState(GameState.INVINCIBILITY);

        game.broadcastMessage(getMessage("game.invincibility-start")
                .replace("{time}", String.valueOf(duration)));
    }

    /**
     * 结束无敌时间
     * @param game 游戏实例
     */
    private void endInvincibility(Game game) {
        game.setState(GameState.PLAYING);
        game.broadcastMessage(getMessage("game.invincibility-end"));

        // 启动缩圈系统
        plugin.getBorderShrinkManager().initializeShrink(game);
    }

    /**
     * 结束游戏
     * @param game 游戏实例
     */
    private void endGame(Game game) {
        game.setState(GameState.ENDING);
        game.setEndTime(System.currentTimeMillis());

        // 广播游戏结束
        broadcastGameEnd(game);

        // 播放胜利特效
        plugin.getVictoryEffectManager().playVictoryEffects(game);

        // 发放奖励
        plugin.getRewardManager().processGameRewards(game);

        // 保存游戏记录到数据库
        // TODO: 实现游戏记录保存

        // 延迟清理游戏
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            game.setState(GameState.FINISHED);
        }, 200L); // 10秒后清理
    }

    /**
     * 广播游戏结束消息
     * @param game 游戏实例
     */
    private void broadcastGameEnd(Game game) {
        List<GamePlayer> topThree = game.getTopThreePlayers();

        game.broadcastMessage(getMessage("victory.winner-broadcast"));

        List<String> infoLines = plugin.getConfigManager().getMessagesConfig()
                .getStringList("victory.winner-info");

        String first = topThree.size() > 0 ? topThree.get(0).getName() : "无";
        String second = topThree.size() > 1 ? topThree.get(1).getName() : "无";
        String third = topThree.size() > 2 ? topThree.get(2).getName() : "无";

        for (String line : infoLines) {
            String message = ChatColor.translateAlternateColorCodes('&', line)
                    .replace("{first}", first)
                    .replace("{second}", second)
                    .replace("{third}", third);

            game.broadcastMessage(message);
        }
    }

    /**
     * 清理游戏
     * @param game 游戏实例
     */
    private void cleanupGame(Game game) {
        // 停止缩圈系统
        plugin.getBorderShrinkManager().stopShrink(game);

        // 传送所有玩家回大厅
        returnPlayersToLobby(game.getPlayers().keySet());

        // 移除玩家-游戏映射
        for (UUID uuid : game.getPlayers().keySet()) {
            playerGameMap.remove(uuid);
        }

        // 移除游戏实例
        activeGames.remove(game.getGameUuid());

        plugin.getLogger().info("§a游戏 " + game.getGameUuid() + " 已清理");

        // 通知QueueManager清理世界（新的世界队列模式）
        int queueId = game.getQueueId();
        plugin.getQueueManager().onGameEnd(queueId);
        plugin.getLogger().info("§7已通知QueueManager清理队列 #" + queueId + " 的世界");
    }

    /**
     * 传送玩家回大厅
     * @param playerUuids 玩家UUID集合
     */
    private void returnPlayersToLobby(Set<UUID> playerUuids) {
        String worldName = plugin.getConfig().getString("queue.lobby-world", "world");
        World lobbyWorld = Bukkit.getWorld(worldName);

        if (lobbyWorld == null) {
            plugin.getLogger().severe("§c大厅世界不存在: " + worldName);
            return;
        }

        double x = plugin.getConfig().getDouble("queue.lobby-location.x", 0.5);
        double y = plugin.getConfig().getDouble("queue.lobby-location.y", 100.0);
        double z = plugin.getConfig().getDouble("queue.lobby-location.z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("queue.lobby-location.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("queue.lobby-location.pitch", 0.0);

        Location lobby = new Location(lobbyWorld, x, y, z, yaw, pitch);

        for (UUID uuid : playerUuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(lobby);
            }
        }
    }

    /**
     * 获取玩家所在游戏
     * @param player 玩家
     * @return 游戏实例 或 null
     */
    public Game getPlayerGame(Player player) {
        String gameUuid = playerGameMap.get(player.getUniqueId());
        return gameUuid != null ? activeGames.get(gameUuid) : null;
    }

    /**
     * 检查玩家是否在游戏中
     * @param player 玩家
     * @return 是否在游戏中
     */
    public boolean isInGame(Player player) {
        return playerGameMap.containsKey(player.getUniqueId());
    }

    /**
     * 获取消息
     * @param key 消息键
     * @return 消息
     */
    private String getMessage(String key) {
        String prefix = plugin.getConfigManager().getMessagesConfig().getString("prefix", "");
        String message = plugin.getConfigManager().getMessagesConfig().getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * 停止所有进行中的游戏
     */
    public void stopAllGames() {
        for (Game game : new ArrayList<>(activeGames.values())) {
            endGame(game);
        }

        if (gameLoopTask != null) {
            gameLoopTask.cancel();
        }

        plugin.getLogger().info("§a所有游戏已停止");
    }

    /**
     * 检查所有存活玩家的宝可梦数量
     * @param game 游戏实例
     */
    private void checkAllPlayersPokemonCount(Game game) {
        // 异步执行检查，避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (UUID playerId : new ArrayList<>(game.getAlivePlayers())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    // 调用 PixelmonBattleListener 的检查方法
                    plugin.getPixelmonBattleListener().checkPlayerPokemonCount(player);
                }
            }
        });
    }

    /**
     * 获取所有活跃游戏
     * @return 游戏列表
     */
    public Collection<Game> getActiveGames() {
        return activeGames.values();
    }
}
