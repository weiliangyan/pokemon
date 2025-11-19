package com.pokemonbr.models;

import java.util.UUID;

/**
 * 游戏玩家数据
 * 记录玩家在当前游戏中的状态
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class GamePlayer {

    private final UUID uuid;
    private final String name;

    private boolean alive;
    private boolean spectating;
    private int kills;
    private int rank;
    private UUID eliminatedBy;
    private long joinTime;
    private long eliminationTime;

    public GamePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.alive = true;
        this.spectating = false;
        this.kills = 0;
        this.rank = 0;
        this.eliminatedBy = null;
        this.joinTime = System.currentTimeMillis();
        this.eliminationTime = 0;
    }

    /**
     * 淘汰玩家
     * @param killer 击败者UUID（可为null）
     * @param currentRank 当前排名
     */
    public void eliminate(UUID killer, int currentRank) {
        this.alive = false;
        this.eliminatedBy = killer;
        this.rank = currentRank;
        this.eliminationTime = System.currentTimeMillis();
    }

    /**
     * 增加击败数
     */
    public void addKill() {
        this.kills++;
    }

    /**
     * 获取存活时间（秒）
     * @return 存活时间
     */
    public long getSurvivalTime() {
        long endTime = alive ? System.currentTimeMillis() : eliminationTime;
        return (endTime - joinTime) / 1000;
    }

    // ==================== Getter 和 Setter ====================

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isSpectating() {
        return spectating;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public UUID getEliminatedBy() {
        return eliminatedBy;
    }

    public void setEliminatedBy(UUID eliminatedBy) {
        this.eliminatedBy = eliminatedBy;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public long getEliminationTime() {
        return eliminationTime;
    }

    @Override
    public String toString() {
        return "GamePlayer{" +
                "name='" + name + '\'' +
                ", alive=" + alive +
                ", kills=" + kills +
                ", rank=" + rank +
                '}';
    }
}
