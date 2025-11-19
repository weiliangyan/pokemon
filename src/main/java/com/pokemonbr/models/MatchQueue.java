package com.pokemonbr.models;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 匹配队列模型
 * 每个队列绑定一个世界实例
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class MatchQueue {

    private final int queueId;
    private final Set<UUID> players;
    private QueueState state;
    private int countdown;
    private String gameUuid;

    private final int minPlayers;
    private final int maxPlayers;

    // 队列类型 (普通/VIP/管理员)
    private final QueueType queueType;
    // 队列索引 (如"普通队列2"中的2)
    private final int queueIndex;
    // 队列显示名称
    private final String displayName;

    // 世界绑定
    private World world;
    private String worldName;

    // 世界配置名称（用于多地图支持）
    private String worldConfigName;

    public MatchQueue(int queueId, QueueType queueType, int queueIndex, String displayName, int minPlayers, int maxPlayers) {
        this.queueId = queueId;
        this.queueType = queueType;
        this.queueIndex = queueIndex;
        this.displayName = displayName;
        this.players = new HashSet<>();
        this.state = QueueState.WAITING;
        this.countdown = 0;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.gameUuid = null;
        this.world = null;
        this.worldName = null;
    }

    /**
     * 绑定世界到队列
     * @param world 世界对象
     */
    public void bindWorld(World world) {
        this.world = world;
        this.worldName = world.getName();
    }

    /**
     * 解除世界绑定
     */
    public void unbindWorld() {
        this.world = null;
        this.worldName = null;
    }

    /**
     * 检查是否已绑定世界
     * @return 是否已绑定
     */
    public boolean hasWorld() {
        return world != null;
    }

    /**
     * 添加玩家到队列
     * @param player 玩家
     * @return 是否成功
     */
    public boolean addPlayer(Player player) {
        if (players.size() >= maxPlayers) {
            return false;
        }

        if (state != QueueState.WAITING && state != QueueState.COUNTDOWN) {
            return false;
        }

        return players.add(player.getUniqueId());
    }

    /**
     * 移除玩家
     * @param player 玩家
     * @return 是否成功
     */
    public boolean removePlayer(Player player) {
        return players.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否在队列中
     * @param player 玩家
     * @return 是否在队列
     */
    public boolean contains(Player player) {
        return players.contains(player.getUniqueId());
    }

    /**
     * 检查玩家是否在队列中
     * @param uuid 玩家UUID
     * @return 是否在队列
     */
    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    /**
     * 获取当前人数
     * @return 人数
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * 检查是否已满
     * @return 是否已满
     */
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    /**
     * 检查是否达到最小人数
     * @return 是否达到
     */
    public boolean hasMinPlayers() {
        return players.size() >= minPlayers;
    }

    /**
     * 清空队列
     */
    public void clear() {
        players.clear();
        state = QueueState.WAITING;
        countdown = 0;
        gameUuid = null;
    }

    /**
     * 递减倒计时
     * @return 剩余倒计时
     */
    public int decrementCountdown() {
        if (countdown > 0) {
            countdown--;
        }
        return countdown;
    }

    // ==================== Getter 和 Setter ====================

    public int getQueueId() {
        return queueId;
    }

    public Set<UUID> getPlayers() {
        return new HashSet<>(players);
    }

    public QueueState getState() {
        return state;
    }

    public void setState(QueueState state) {
        this.state = state;
    }

    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public String getGameUuid() {
        return gameUuid;
    }

    public void setGameUuid(String gameUuid) {
        this.gameUuid = gameUuid;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public World getWorld() {
        return world;
    }

    public String getWorldName() {
        return worldName;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public int getQueueIndex() {
        return queueIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取完整队列名称 (如"普通队列 #2")
     * @return 完整队列名称
     */
    public String getFullQueueName() {
        if (queueIndex > 1) {
            return displayName + " #" + queueIndex;
        }
        return displayName;
    }

    /**
     * 是否为计分模式
     * @return 是否计分
     */
    public boolean isRankedMode() {
        return queueType.isRanked();
    }

    /**
     * 设置世界配置名称
     * @param worldConfigName 世界配置名称
     */
    public void setWorldConfigName(String worldConfigName) {
        this.worldConfigName = worldConfigName;
    }

    /**
     * 获取世界配置名称
     * @return 世界配置名称
     */
    public String getWorldConfigName() {
        return worldConfigName;
    }

    @Override
    public String toString() {
        return "MatchQueue{" +
                "queueId=" + queueId +
                ", queueType=" + queueType.getDisplayName() +
                ", queueIndex=" + queueIndex +
                ", players=" + players.size() + "/" + maxPlayers +
                ", state=" + state +
                ", countdown=" + countdown +
                ", world=" + (worldName != null ? worldName : "none") +
                '}';
    }
}
