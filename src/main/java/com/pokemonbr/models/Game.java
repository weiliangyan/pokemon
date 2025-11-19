package com.pokemonbr.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 游戏实例
 * 管理单个游戏的完整生命周期
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class Game {

    private final String gameUuid;
    private final int queueId;
    private final World gameWorld;
    private final String worldConfigName;  // 世界配置名称（例如: flyfortress）

    private GameState state;
    private final Map<UUID, GamePlayer> players;
    private final List<UUID> alivePlayersCache;

    private int invincibilityTime;
    private int currentShrinkStage;
    private long startTime;
    private long endTime;

    public Game(String gameUuid, int queueId, World gameWorld, String worldConfigName, Set<UUID> playerUuids) {
        this.gameUuid = gameUuid;
        this.queueId = queueId;
        this.gameWorld = gameWorld;
        this.worldConfigName = worldConfigName;
        this.state = GameState.PREPARING;
        this.players = new HashMap<>();
        this.alivePlayersCache = new ArrayList<>();

        // 初始化玩家数据
        for (UUID uuid : playerUuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                String name = player.getName();
                GamePlayer gamePlayer = new GamePlayer(uuid, name);
                players.put(uuid, gamePlayer);
                alivePlayersCache.add(uuid);
            }
        }

        this.invincibilityTime = 0;
        this.currentShrinkStage = 0;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
    }

    /**
     * 淘汰玩家
     * @param victim 被淘汰者UUID
     * @param killer 击败者UUID（可为null）
     */
    public void eliminatePlayer(UUID victim, UUID killer) {
        GamePlayer gamePlayer = players.get(victim);
        if (gamePlayer == null || !gamePlayer.isAlive()) {
            return;
        }

        // 更新存活列表
        alivePlayersCache.remove(victim);

        // 计算当前排名（倒序，最后存活的是第1名）
        int currentRank = alivePlayersCache.size() + 1;

        // 淘汰玩家
        gamePlayer.eliminate(killer, currentRank);

        // 如果有击败者，增加击杀数
        if (killer != null) {
            GamePlayer killerPlayer = players.get(killer);
            if (killerPlayer != null) {
                killerPlayer.addKill();
            }
        }
    }

    /**
     * 传送玩家到出生点
     * @param player 玩家
     * @param location 出生点
     */
    public void teleportPlayerToSpawn(Player player, Location location) {
        player.teleport(location);
    }

    /**
     * 获取存活玩家数量
     * @return 存活数量
     */
    public int getAlivePlayerCount() {
        return alivePlayersCache.size();
    }

    /**
     * 获取玩家游戏数据
     * @param uuid 玩家UUID
     * @return GamePlayer 或 null
     */
    public GamePlayer getGamePlayer(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * 检查玩家是否存活
     * @param uuid 玩家UUID
     * @return 是否存活
     */
    public boolean isPlayerAlive(UUID uuid) {
        GamePlayer gamePlayer = players.get(uuid);
        return gamePlayer != null && gamePlayer.isAlive();
    }

    /**
     * 检查游戏是否结束
     * @return 是否结束
     */
    public boolean isGameOver() {
        return alivePlayersCache.size() <= 1;
    }

    /**
     * 获取胜利者
     * @return 胜利者UUID 或 null
     */
    public UUID getWinner() {
        if (alivePlayersCache.size() == 1) {
            return alivePlayersCache.get(0);
        }
        return null;
    }

    /**
     * 获取前三名玩家
     * @return 前三名列表（按排名排序）
     */
    public List<GamePlayer> getTopThreePlayers() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(GamePlayer::getRank))
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 广播消息给所有玩家
     * @param message 消息
     */
    public void broadcastMessage(String message) {
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * 广播消息给存活玩家
     * @param message 消息
     */
    public void broadcastToAlivePlayers(String message) {
        for (UUID uuid : alivePlayersCache) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * 获取游戏时长（秒）
     * @return 游戏时长
     */
    public long getGameDuration() {
        long end = (endTime > 0) ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }

    // ==================== Getter 和 Setter ====================

    public String getGameUuid() {
        return gameUuid;
    }

    public int getQueueId() {
        return queueId;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public String getWorldConfigName() {
        return worldConfigName;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return new HashMap<>(players);
    }

    public List<UUID> getAlivePlayers() {
        return new ArrayList<>(alivePlayersCache);
    }

    public int getInvincibilityTime() {
        return invincibilityTime;
    }

    public void setInvincibilityTime(int invincibilityTime) {
        this.invincibilityTime = invincibilityTime;
    }

    public int decrementInvincibilityTime() {
        if (invincibilityTime > 0) {
            invincibilityTime--;
        }
        return invincibilityTime;
    }

    public int getCurrentShrinkStage() {
        return currentShrinkStage;
    }

    public void setCurrentShrinkStage(int currentShrinkStage) {
        this.currentShrinkStage = currentShrinkStage;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getTotalPlayerCount() {
        return players.size();
    }

    @Override
    public String toString() {
        return "Game{" +
                "gameUuid='" + gameUuid + '\'' +
                ", queueId=" + queueId +
                ", state=" + state +
                ", alivePlayers=" + alivePlayersCache.size() + "/" + players.size() +
                ", duration=" + getGameDuration() + "s" +
                '}';
    }
}
