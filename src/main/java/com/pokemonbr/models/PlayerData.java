package com.pokemonbr.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 玩家数据模型
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class PlayerData {

    private UUID uuid;
    private String name;
    private int totalGames;
    private int totalPoints;
    private int totalKills;
    private int totalWins;
    private int playsToday;
    private Timestamp lastReset;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 构造函数
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.totalGames = 0;
        this.totalPoints = 0;
        this.totalKills = 0;
        this.totalWins = 0;
        this.playsToday = 0;
        this.lastReset = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // 完整构造函数（从数据库加载）
    public PlayerData(UUID uuid, String name, int totalGames, int totalPoints,
                      int totalKills, int totalWins, int playsToday,
                      Timestamp lastReset, Timestamp createdAt, Timestamp updatedAt) {
        this.uuid = uuid;
        this.name = name;
        this.totalGames = totalGames;
        this.totalPoints = totalPoints;
        this.totalKills = totalKills;
        this.totalWins = totalWins;
        this.playsToday = playsToday;
        this.lastReset = lastReset;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter 和 Setter 方法
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public void addTotalGames(int games) {
        this.totalGames += games;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public void addTotalPoints(int points) {
        this.totalPoints += points;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }

    public void addTotalKills(int kills) {
        this.totalKills += kills;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public void addTotalWins(int wins) {
        this.totalWins += wins;
    }

    public int getPlaysToday() {
        return playsToday;
    }

    public void setPlaysToday(int playsToday) {
        this.playsToday = playsToday;
    }

    public void addPlaysToday(int plays) {
        this.playsToday += plays;
    }

    public void resetPlaysToday() {
        this.playsToday = 0;
        this.lastReset = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getLastReset() {
        return lastReset;
    }

    public void setLastReset(Timestamp lastReset) {
        this.lastReset = lastReset;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 计算胜率
    public double getWinRate() {
        if (totalGames == 0) {
            return 0.0;
        }
        return (double) totalWins / totalGames * 100;
    }

    // 计算场均击败数
    public double getAverageKills() {
        if (totalGames == 0) {
            return 0.0;
        }
        return (double) totalKills / totalGames;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", totalGames=" + totalGames +
                ", totalPoints=" + totalPoints +
                ", totalKills=" + totalKills +
                ", totalWins=" + totalWins +
                ", playsToday=" + playsToday +
                ", winRate=" + String.format("%.2f", getWinRate()) + "%" +
                '}';
    }
}
