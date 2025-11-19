package com.pokemonbr.managers;

import com.pokemonbr.Main;
import com.pokemonbr.models.MatchQueue;
import com.pokemonbr.models.QueueState;
import com.pokemonbr.models.QueueType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 匹配队列管理器
 * 管理基于世界的玩家匹配队列
 * 支持三种队列类型: 普通队列、VIP队列、管理员队列
 * 每个队列绑定一个独立的世界实例
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class QueueManager {

    private final Main plugin;
    private final WorldTemplateManager worldTemplateManager;

    // 队列ID -> 队列对象
    private Map<Integer, MatchQueue> queues = new ConcurrentHashMap<>();

    // 玩家UUID -> 队列ID（快速查找玩家在哪个队列）
    private Map<UUID, Integer> playerQueueMap = new ConcurrentHashMap<>();

    // 队列类型 -> 队列索引计数器 (用于自动扩展队列)
    private final Map<QueueType, Integer> queueIndexCounters;

    // 倒计时任务
    private BukkitTask countdownTask;

    // 队列ID计数器
    private int queueIdCounter = 1;

    // 正在创建的队列类型集合（防止并发创建）
    private final Set<QueueType> creatingQueues = ConcurrentHashMap.newKeySet();

    public QueueManager(Main plugin, WorldTemplateManager worldTemplateManager) {
        this.plugin = plugin;
        this.worldTemplateManager = worldTemplateManager;
        this.queues = new HashMap<>();
        this.playerQueueMap = new HashMap<>();
        this.queueIndexCounters = new HashMap<>();

        // 初始化队列索引计数器
        for (QueueType type : QueueType.values()) {
            queueIndexCounters.put(type, 1);
        }

        // 启动倒计时任务
        startCountdownTask();

        plugin.getLogger().info("§a队列管理器已初始化（多队列类型模式）");
    }

    /**
     * 启动倒计时任务
     */
    private void startCountdownTask() {
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (MatchQueue queue : queues.values()) {
                if (queue.getState() == QueueState.COUNTDOWN) {
                    int remaining = queue.decrementCountdown();

                    // 广播倒计时
                    if (remaining > 0) {
                        broadcastToQueue(queue, getMessage("queue.countdown")
                                .replace("{time}", String.valueOf(remaining)));

                        // 显示准备倒计时Title (最后5秒)
                        if (remaining <= 5) {
                            for (UUID uuid : queue.getPlayers()) {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    String title = getMessage("queue.ready-title");
                                    String subtitle = getMessage("queue.ready-subtitle")
                                            .replace("{time}", String.valueOf(remaining));
                                    player.sendTitle(title, subtitle, 10, 20, 10);
                                }
                            }
                        }
                    } else {
                        // 倒计时结束，开始游戏
                        startGame(queue);
                    }
                }
            }
        }, 20L, 20L); // 每秒执行一次
    }

    /**
     * 玩家加入队列
     * 根据权限自动分配到对应队列类型
     * 如果队列满了，自动扩展新队列
     * @param player 玩家
     * @return 是否成功
     */
    public boolean joinQueue(Player player) {
        // 检查玩家是否已在队列中
        if (isInQueue(player)) {
            sendMessage(player, "queue.already-in-queue");
            return false;
        }

        // 检查游玩次数（仅对计分模式队列）
        QueueType targetQueueType = determineQueueType(player);
        if (targetQueueType.isRanked()) {
            int remaining = plugin.getPlayerDataManager().getRemainingPlays(player);
            if (remaining <= 0) {
                sendMessage(player, "queue.no-plays-left");
                return false;
            }
        }

        // 查找可用队列
        MatchQueue availableQueue = findAvailableQueue(targetQueueType);

        // 如果没有可用队列，检查是否正在创建中
        if (availableQueue == null) {
            if (creatingQueues.contains(targetQueueType)) {
                // 有同类型队列正在创建中，提示等待
                sendMessage(player, "queue.preparing-world");
                sendMessage(player, "queue.world-creation-in-progress");
                return true;
            }

            sendMessage(player, "queue.preparing-world");
            createNewQueueAsync(player, targetQueueType);
            return true;
        }

        // 添加到队列
        if (availableQueue.addPlayer(player)) {
            playerQueueMap.put(player.getUniqueId(), availableQueue.getQueueId());

            // 发送消息
            sendMessage(player, "queue.joined",
                    "{queue}", availableQueue.getFullQueueName(),
                    "{current}", String.valueOf(availableQueue.getPlayerCount()),
                    "{max}", String.valueOf(availableQueue.getMaxPlayers()));

            // 如果是计分模式，显示剩余次数
            if (targetQueueType.isRanked()) {
                int remaining = plugin.getPlayerDataManager().getRemainingPlays(player);
                sendMessage(player, "queue.plays-remaining",
                        "{plays}", String.valueOf(remaining));
            }

            // 检查是否达到最小人数
            if (availableQueue.hasMinPlayers() && availableQueue.getState() == QueueState.WAITING) {
                startCountdown(availableQueue);
            }

            return true;
        }

        return false;
    }

    /**
     * 根据权限确定队列类型
     * 优先级: 管理员 > VIP > 普通
     * @param player 玩家
     * @return 队列类型
     */
    private QueueType determineQueueType(Player player) {
        // 检查管理员权限
        if (player.hasPermission(QueueType.ADMIN.getPermission())) {
            return QueueType.ADMIN;
        }
        // 检查VIP权限
        if (player.hasPermission(QueueType.VIP.getPermission())) {
            return QueueType.VIP;
        }
        // 默认普通队列
        return QueueType.NORMAL;
    }

    /**
     * 查找可用队列（指定队列类型）
     * @param queueType 队列类型
     * @return 可用队列 或 null
     */
    private MatchQueue findAvailableQueue(QueueType queueType) {
        for (MatchQueue queue : queues.values()) {
            // 队列必须: 类型匹配、未满、等待中、已绑定世界
            if (queue.getQueueType() == queueType &&
                !queue.isFull() &&
                queue.getState() == QueueState.WAITING &&
                queue.hasWorld()) {
                return queue;
            }
        }
        return null;
    }

    /**
     * 异步创建新队列并分配世界
     * @param player 首个玩家
     * @param queueType 队列类型
     */
    private void createNewQueueAsync(Player player, QueueType queueType) {
        // 标记此类型队列正在创建中
        creatingQueues.add(queueType);

        int minPlayers = plugin.getConfig().getInt("queue.min-players", 2);
        String queueConfigPath = "queue.queues." + queueType.getConfigKey();
        int maxPlayers = plugin.getConfig().getInt(queueConfigPath + ".max-players", 20);
        String displayName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(queueConfigPath + ".display-name", queueType.getDisplayName()));

        // 获取当前队列索引并递增
        int queueIndex = queueIndexCounters.get(queueType);
        queueIndexCounters.put(queueType, queueIndex + 1);

        // 创建新队列
        int queueId = queueIdCounter++;
        MatchQueue newQueue = new MatchQueue(queueId, queueType, queueIndex, displayName, minPlayers, maxPlayers);
        queues.put(queueId, newQueue);

        // ⚡ 从地图池随机选择地图
        String worldConfigName = worldTemplateManager.selectRandomWorldConfig();
        if (worldConfigName != null) {
            newQueue.setWorldConfigName(worldConfigName);
            plugin.getLogger().info("§e队列 #" + queueId + " 使用地图配置: " + worldConfigName);
        } else {
            plugin.getLogger().warning("§c队列 #" + queueId + " 未选择地图配置，将使用默认配置");
        }

        plugin.getLogger().info("§e正在为" + newQueue.getFullQueueName() + " (#" + queueId + ") 创建世界副本...");

        // 异步复制世界
        CompletableFuture<World> worldFuture;
        if (worldConfigName != null) {
            // 使用指定的世界配置创建世界
            worldFuture = worldTemplateManager.createWorldCopyFromConfig(worldConfigName);
        } else {
            // 使用默认配置创建世界
            worldFuture = worldTemplateManager.createWorldCopy();
        }

        worldFuture.thenAccept(world -> {
            if (world == null) {
                // 世界创建失败
                Bukkit.getScheduler().runTask(plugin, () -> {
                    creatingQueues.remove(queueType);
                    queues.remove(queueId);
                    sendMessage(player, "queue.world-creation-failed");
                    plugin.getLogger().severe("§c" + newQueue.getFullQueueName() + " 世界创建失败！");
                });
                return;
            }

            // 在主线程绑定世界并添加玩家
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    newQueue.bindWorld(world);
                    plugin.getLogger().info("§a" + newQueue.getFullQueueName() + " 已绑定世界: " + world.getName());

                    // 添加玩家到队列
                    if (newQueue.addPlayer(player)) {
                        playerQueueMap.put(player.getUniqueId(), queueId);

                        sendMessage(player, "queue.joined",
                                "{queue}", newQueue.getFullQueueName(),
                                "{current}", String.valueOf(newQueue.getPlayerCount()),
                                "{max}", String.valueOf(newQueue.getMaxPlayers()));

                        // 如果是计分模式，显示剩余次数
                        if (queueType.isRanked()) {
                            int remaining = plugin.getPlayerDataManager().getRemainingPlays(player);
                            sendMessage(player, "queue.plays-remaining",
                                    "{plays}", String.valueOf(remaining));
                        }
                    } else {
                        sendMessage(player, "queue.join-failed");
                    }
                } finally {
                    // 无论成功与否，都要移除创建标记
                    creatingQueues.remove(queueType);
                }
            });
        }).exceptionally(throwable -> {
            // 处理异步操作中的异常
            Bukkit.getScheduler().runTask(plugin, () -> {
                creatingQueues.remove(queueType);
                queues.remove(queueId);
                sendMessage(player, "queue.world-creation-failed");
                plugin.getLogger().severe("§c" + newQueue.getFullQueueName() + " 世界创建时发生异常: " + throwable.getMessage());
                throwable.printStackTrace();
            });
            return null;
        });
    }

    /**
     * 玩家退出队列
     * @param player 玩家
     * @return 是否成功
     */
    public boolean leaveQueue(Player player) {
        Integer queueId = playerQueueMap.remove(player.getUniqueId());

        if (queueId == null) {
            sendMessage(player, "queue.not-in-queue");
            return false;
        }

        MatchQueue queue = queues.get(queueId);
        if (queue != null) {
            queue.removePlayer(player);

            // 如果人数不足，取消倒计时
            if (queue.getState() == QueueState.COUNTDOWN && !queue.hasMinPlayers()) {
                cancelCountdown(queue);
            }
        }

        sendMessage(player, "queue.left");
        return true;
    }


    /**
     * 开始倒计时
     * @param queue 队列
     */
    private void startCountdown(MatchQueue queue) {
        int countdownTime = plugin.getConfig().getInt("queue.match-countdown", 30);
        queue.setState(QueueState.COUNTDOWN);
        queue.setCountdown(countdownTime);

        broadcastToQueue(queue, getMessage("queue.countdown")
                .replace("{time}", String.valueOf(countdownTime)));
    }

    /**
     * 取消倒计时
     * @param queue 队列
     */
    private void cancelCountdown(MatchQueue queue) {
        queue.setState(QueueState.WAITING);
        queue.setCountdown(0);

        broadcastToQueue(queue, getMessage("queue.countdown-cancelled"));
    }

    /**
     * 开始游戏
     * @param queue 队列
     */
    private void startGame(MatchQueue queue) {
        queue.setState(QueueState.PLAYING);

        // 检查世界是否存在
        if (!queue.hasWorld()) {
            plugin.getLogger().severe("§c" + queue.getFullQueueName() + " 未绑定世界，无法开始游戏！");
            resetQueue(queue);
            return;
        }

        // 广播匹配成功
        broadcastToQueue(queue, getMessage("queue.match-found"));

        // 消耗玩家游玩次数 (仅计分模式)
        if (queue.isRankedMode()) {
            for (UUID uuid : queue.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    plugin.getPlayerDataManager().consumePlay(player);
                }
            }
        } else {
            // 娱乐模式提示不消耗次数
            broadcastToQueue(queue, ChatColor.YELLOW + "娱乐模式: 本局不消耗游玩次数,不影响积分");
        }

        // 通知游戏管理器创建游戏实例（传递绑定的世界）
        String gameUuid = plugin.getGameManager().createGame(queue, queue.getWorld());
        queue.setGameUuid(gameUuid);

        // 清空队列映射
        for (UUID uuid : queue.getPlayers()) {
            playerQueueMap.remove(uuid);
        }

        plugin.getLogger().info("§a" + queue.getFullQueueName() + " 已开始游戏（世界: " + queue.getWorldName() + ", 模式: " +
                (queue.isRankedMode() ? "计分" : "娱乐") + "）");
    }

    /**
     * 游戏结束后的清理
     * @param queueId 队列ID
     */
    public void onGameEnd(int queueId) {
        MatchQueue queue = queues.remove(queueId);

        if (queue == null) {
            return;
        }

        String worldName = queue.getWorldName();

        // 解除世界绑定
        queue.unbindWorld();
        queue.clear();

        // 异步删除世界
        if (worldName != null) {
            plugin.getLogger().info("§e正在清理队列 #" + queueId + " 的世界: " + worldName);

            worldTemplateManager.deleteWorld(worldName).thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("§a队列 #" + queueId + " 世界清理完成");
                } else {
                    plugin.getLogger().warning("§c队列 #" + queueId + " 世界清理失败: " + worldName);
                }
            });
        }
    }

    /**
     * 重置队列（清空玩家并重新进入等待状态）
     * @param queue 队列
     */
    private void resetQueue(MatchQueue queue) {
        // 通知所有玩家
        broadcastToQueue(queue, getMessage("queue.reset"));

        // 清空映射
        for (UUID uuid : queue.getPlayers()) {
            playerQueueMap.remove(uuid);
        }

        queue.clear();
    }

    /**
     * 强制开始游戏（管理员指令）
     * @param queueId 队列ID
     * @return 是否成功
     */
    public boolean forceStartGame(int queueId) {
        MatchQueue queue = queues.get(queueId);

        if (queue == null) {
            return false;
        }

        // 检查是否绑定了世界
        if (!queue.hasWorld()) {
            plugin.getLogger().warning("§c队列 #" + queueId + " 未绑定世界，无法强制开始！");
            return false;
        }

        int forceStartMin = plugin.getConfig().getInt("queue.force-start-min", 2);

        if (queue.getPlayerCount() < forceStartMin) {
            return false;
        }

        // 直接开始游戏
        queue.setCountdown(0);
        startGame(queue);

        return true;
    }

    /**
     * 检查玩家是否在队列中
     * @param player 玩家
     * @return 是否在队列
     */
    public boolean isInQueue(Player player) {
        return playerQueueMap.containsKey(player.getUniqueId());
    }

    /**
     * 获取玩家所在队列
     * @param player 玩家
     * @return 队列 或 null
     */
    public MatchQueue getPlayerQueue(Player player) {
        Integer queueId = playerQueueMap.get(player.getUniqueId());
        return queueId != null ? queues.get(queueId) : null;
    }

    /**
     * 获取队列
     * @param queueId 队列ID
     * @return 队列 或 null
     */
    public MatchQueue getQueue(int queueId) {
        return queues.get(queueId);
    }

    /**
     * 向队列中的所有玩家广播消息
     * @param queue 队列
     * @param message 消息
     */
    private void broadcastToQueue(MatchQueue queue, String message) {
        for (UUID uuid : queue.getPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * 发送消息给玩家
     * @param player 玩家
     * @param key 消息键
     * @param replacements 替换参数
     */
    private void sendMessage(Player player, String key, String... replacements) {
        String message = getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        player.sendMessage(message);
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
     * 停止倒计时任务并清理所有队列
     */
    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // 清空所有队列并记录需要删除的世界
        List<String> worldsToDelete = new ArrayList<>();

        for (MatchQueue queue : queues.values()) {
            if (queue.hasWorld()) {
                worldsToDelete.add(queue.getWorldName());
            }
            queue.clear();
        }

        queues.clear();
        playerQueueMap.clear();

        // 清理所有世界
        for (String worldName : worldsToDelete) {
            plugin.getLogger().info("§e正在清理世界: " + worldName);
            worldTemplateManager.deleteWorld(worldName);
        }
    }

    /**
     * 获取所有队列
     * @return 队列列表
     */
    public Collection<MatchQueue> getAllQueues() {
        return queues.values();
    }
}
